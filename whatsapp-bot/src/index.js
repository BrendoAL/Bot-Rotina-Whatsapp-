import makeWASocket, {
    areJidsSameUser,
    DisconnectReason,
    fetchLatestBaileysVersion,
    jidNormalizedUser,
    useMultiFileAuthState,
} from '@whiskeysockets/baileys'

import qrcode from 'qrcode-terminal'
import { createServer } from 'http'
import { readFile, writeFile } from 'fs/promises'
import { parseMessage } from './parser.js'
import { publishActivityCreate } from './publisher.js'
import { handleCommand } from './commands.js'
import pino from 'pino'
import axios from 'axios'

const AUTH_FOLDER = process.env.AUTH_FOLDER || './sessions'
const SELF_CHAT_FILE = `${AUTH_FOLDER}/self-chat.json`
const ACTIVITY_SERVICE_URL = process.env.ACTIVITY_SERVICE_URL || 'http://activity-service:8082'
const PORT = process.env.PORT || 3000
const USER_SERVICE_URL = process.env.USER_SERVICE_URL || 'http://user-service:8081'
const ACTIVATE_SELF_CHAT_COMMAND = 'ativar lambda'

let sockInstance = null
let connectionStatus = 'starting'
let lastQrAt = null
let ownJid = null
let ownLid = null
let ownPhone = null
let welcomeSent = false
let lastGoalReminderDate = null
let goalReminderLoopStarted = false
let selfChatJid = null

function normalizePhone(phone) {
    return String(phone || '').replace(/\D/g, '')
}

function toWhatsappJid(phone) {
    const normalized = normalizePhone(phone)
    if (!normalized) return null
    if (ownPhone && normalized === ownPhone && selfChatJid) return selfChatJid
    return `${normalized}@s.whatsapp.net`
}

function isUserJid(jid) {
    return jid.endsWith('@s.whatsapp.net') || jid.endsWith('@lid')
}

function extractText(message) {
    return message?.conversation
        || message?.extendedTextMessage?.text
        || message?.imageMessage?.caption
        || message?.videoMessage?.caption
        || null
}

function isOwnChat(remoteJid) {
    return Boolean(remoteJid && (
        (selfChatJid && remoteJid === selfChatJid)
        || (ownJid && areJidsSameUser(remoteJid, ownJid))
        || (ownLid && areJidsSameUser(remoteJid, ownLid))
        || remoteJid === ownJid
        || remoteJid === ownLid
    ))
}

async function loadSelfChatJid() {
    if (process.env.SELF_CHAT_JID) {
        selfChatJid = process.env.SELF_CHAT_JID
        return
    }

    try {
        const data = JSON.parse(await readFile(SELF_CHAT_FILE, 'utf8'))
        selfChatJid = data.selfChatJid || null
    } catch {
        selfChatJid = null
    }
}

async function saveSelfChatJid(jid) {
    selfChatJid = jid
    await writeFile(SELF_CHAT_FILE, JSON.stringify({ selfChatJid: jid }, null, 2))
}

function isBotResponse(text) {
    const trimmed = String(text || '').trim()
    return [
        '✅',
        '❌',
        '📊',
        '📋',
        '🎯',
        '🤖',
        '⏳',
    ].some(prefix => trimmed.startsWith(prefix))
}

function formatCategory(cat) {
    const map = {
        ESTUDO: 'Estudo',
        TREINO: 'Treino',
        LEITURA: 'Leitura',
        ALIMENTACAO: 'Alimentação',
        OUTRO: 'Atividade',
    }
    return map[cat] || cat
}

function formatPeriod(period) {
    const map = {
        DAILY: 'dia',
        WEEKLY: 'semana',
        MONTHLY: 'mês',
    }
    return map[period] || String(period || '').toLowerCase()
}

function formatDuration(minutes) {
    const total = Number(minutes || 0)
    const hours = Math.floor(total / 60)
    const mins = total % 60

    if (hours && mins) return `${hours}h${String(mins).padStart(2, '0')}`
    if (hours) return `${hours}h`
    return `${mins}min`
}

function localDateString(date = new Date()) {
    return date.toLocaleDateString('en-CA', { timeZone: 'America/Sao_Paulo' })
}

function reminderWindowReached(now = new Date()) {
    const hour = Number(process.env.GOAL_REMINDER_HOUR || 18)
    const minute = Number(process.env.GOAL_REMINDER_MINUTE || 0)
    const parts = new Intl.DateTimeFormat('pt-BR', {
        timeZone: 'America/Sao_Paulo',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
    }).formatToParts(now)

    const currentHour = Number(parts.find(part => part.type === 'hour')?.value || 0)
    const currentMinute = Number(parts.find(part => part.type === 'minute')?.value || 0)

    return currentHour > hour || (currentHour === hour && currentMinute >= minute)
}

async function createGoal(userId, parsed) {
    const { data } = await axios.post(`${ACTIVITY_SERVICE_URL}/api/goals`, {
        userId,
        category: parsed.category,
        targetMinutes: parsed.durationMinutes,
        period: parsed.period,
    })
    return data
}

async function sendGoalReminder(sock) {
    if (!ownPhone || !selfChatJid || !reminderWindowReached()) return

    const today = localDateString()
    if (lastGoalReminderDate === today) return

    const user = await getUserByPhone(ownPhone)
    if (!user) return

    const { data: goals } = await axios.get(`${ACTIVITY_SERVICE_URL}/api/goals/user/${user.id}`)
    if (!goals.length) return

    const lines = goals
        .map(goal => `• ${formatCategory(goal.category)}: ${formatDuration(goal.targetMinutes)}/${formatPeriod(goal.period)}`)
        .join('\n')

    await sock.sendMessage(selfChatJid, {
        text: `🎯 Lembrete das suas metas:\n\n${lines}`,
    })
    lastGoalReminderDate = today
}

function startGoalReminderLoop(sock) {
    if (goalReminderLoopStarted) return

    goalReminderLoopStarted = true
    sendGoalReminder(sock).catch(err => console.error('[BOT] Erro no lembrete de metas:', err.message))
    setInterval(() => {
        if (!sockInstance) return
        sendGoalReminder(sockInstance).catch(err => console.error('[BOT] Erro no lembrete de metas:', err.message))
    }, 60 * 1000)
}

async function sendWelcome(sock) {
    if (welcomeSent || !selfChatJid) return

    welcomeSent = true
    const text =
        '🤖 Lambda Bot conectado.\n\n' +
        'Use este chat "Você" para falar comigo.\n\n' +
        'Primeiro vincule seu usuário:\n' +
        'login seu@email.com\n\n' +
        'Depois registre atividades. Exemplo:\n' +
        'estudei 30min java\n' +
        'treinei 45min\n\n' +
        'Comandos: resumo, metas, ajuda'

    await sock.sendMessage(selfChatJid, { text })
}

async function getUserByPhone(phone) {
    try {
        const { data } = await axios.get(`${USER_SERVICE_URL}/api/users/phone/${phone}`)
        return data
    } catch {
        return null
    }
}

// ── HTTP Server ──────────────────────────────────────────────
const server = createServer(async (req, res) => {
    if (req.method === 'POST' && req.url === '/send') {
        let body = ''
        req.on('data', chunk => body += chunk)
        req.on('end', async () => {
            try {
                const { phone, text } = JSON.parse(body)
                const jid = toWhatsappJid(phone)
                if (sockInstance && jid && text) {
                    await sockInstance.sendMessage(jid, { text })
                    res.writeHead(200)
                    res.end(JSON.stringify({ ok: true }))
                } else {
                    res.writeHead(400)
                    res.end(JSON.stringify({ error: 'missing phone/text or not connected' }))
                }
            } catch (err) {
                res.writeHead(500)
                res.end(JSON.stringify({ error: err.message }))
            }
        })
        return
    }

    if (req.method === 'GET' && req.url === '/health') {
        res.writeHead(200)
        res.end(JSON.stringify({
            status: 'ok',
            whatsapp: connectionStatus,
            connected: sockInstance !== null,
            ownPhone,
            selfChatJid,
            lastQrAt,
        }))
        return
    }

    res.writeHead(404)
    res.end()
})

server.listen(PORT, () => console.log(`[BOT] Servidor HTTP na porta ${PORT}`))

// ── WhatsApp ─────────────────────────────────────────────────
async function connectToWhatsApp() {
    await loadSelfChatJid()

    const { state, saveCreds } = await useMultiFileAuthState(AUTH_FOLDER)
    let version

    try {
        const result = await fetchLatestBaileysVersion()
        version = result.version
    } catch (err) {
        console.warn('[BOT] Não consegui buscar a versão mais recente do WhatsApp Web. Usando padrão do Baileys:', err.message)
    }

    if (version) console.log('[BOT] Usando WhatsApp Web versão:', version)

    const sock = makeWASocket({
        ...(version ? { version } : {}),
        auth: state,
        browser: ['Lambda Bot', 'Chrome', '1.0.0'],
        printQRInTerminal: false,
        logger: pino({ level: process.env.BAILEYS_LOG_LEVEL || 'warn' }),
    })

    sock.ev.on('connection.update', async (update) => {
        const { connection, lastDisconnect, qr } = update

        if (qr) {
            connectionStatus = 'qr'
            lastQrAt = new Date().toISOString()
            console.log('\n========================================')
            console.log('  Escaneie este QR Code no WhatsApp:')
            console.log('  WhatsApp > Dispositivos conectados > Conectar dispositivo')
            console.log('========================================\n')
            qrcode.generate(qr, { small: true })
        }

        if (connection === 'close') {
            const statusCode = lastDisconnect?.error?.output?.statusCode
            const shouldReconnect = statusCode !== DisconnectReason.loggedOut
            sockInstance = null
            connectionStatus = shouldReconnect ? 'reconnecting' : 'logged_out'
            console.log(`[BOT] Desconectado. StatusCode: ${statusCode}. Reconectando: ${shouldReconnect}`)
            if (shouldReconnect) setTimeout(connectToWhatsApp, 5000)
        }

        if (connection === 'open') {
            ownJid = jidNormalizedUser(sock.user?.id)
            ownLid = sock.user?.lid ? jidNormalizedUser(sock.user.lid) : null
            ownPhone = normalizePhone(ownJid?.replace('@s.whatsapp.net', ''))
            console.log('[BOT] ✅ Conectado ao WhatsApp!')
            console.log(`[BOT] Número conectado: ${ownPhone || 'desconhecido'}`)
            if (selfChatJid) {
                console.log(`[BOT] Chat autorizado: ${selfChatJid}`)
            } else {
                console.log(`[BOT] Chat pessoal ainda não autorizado. Envie "${ACTIVATE_SELF_CHAT_COMMAND}" no chat "Você".`)
            }
            sockInstance = sock
            connectionStatus = 'connected'
            sendWelcome(sock).catch(err => console.error('[BOT] Erro ao enviar boas-vindas:', err.message))
            startGoalReminderLoop(sock)
        }
    })

    sock.ev.on('creds.update', saveCreds)

    sock.ev.on('messages.upsert', async ({ messages, type }) => {
        if (type !== 'notify') return

        for (const msg of messages) {
            const remoteJid = msg.key.remoteJid || ''
            if (!isUserJid(remoteJid)) continue

            const text = extractText(msg.message)
            if (!text) continue

            const normalizedText = text.trim().toLowerCase()
            if (msg.key.fromMe && normalizedText === ACTIVATE_SELF_CHAT_COMMAND) {
                await saveSelfChatJid(remoteJid)
                welcomeSent = false
                console.log(`[BOT] Chat pessoal autorizado: ${selfChatJid}`)
                await sock.sendMessage(remoteJid, {
                    text: '✅ Chat autorizado. A partir de agora só respondo aqui.',
                })
                await sendWelcome(sock)
                continue
            }

            const fromOwnChat = isOwnChat(remoteJid)
            if (msg.key.fromMe && !fromOwnChat) {
                console.log(`[BOT] Ignorando mensagem enviada por você em chat não autorizado: ${remoteJid}`)
                continue
            }

            const phone = fromOwnChat
                ? ownPhone
                : normalizePhone(remoteJid.replace(/@(s\.whatsapp\.net|lid)$/, ''))
            const target = fromOwnChat ? remoteJid : phone

            if (!phone) continue
            if (fromOwnChat && isBotResponse(text)) continue

            console.log(`[BOT] Mensagem de ${fromOwnChat ? 'Você' : phone}: "${text}"`)

            try {
                const parsed = parseMessage(text)

                if (parsed.type === 'COMMAND') {
                    await handleCommand(sock, phone, parsed.command, text, target)
                } else if (parsed.type === 'GOAL') {
                    const user = await getUserByPhone(phone)
                    if (!user) {
                        await sock.sendMessage(target, {
                            text: '❌ Número não vinculado.\nEnvie: *login seu@email.com*',
                        })
                        continue
                    }

                    await createGoal(user.id, parsed)
                    await sock.sendMessage(target, {
                        text: `🎯 Meta criada: ${formatDuration(parsed.durationMinutes)} de ${formatCategory(parsed.category)} por ${formatPeriod(parsed.period)}.\nVou te lembrar todos os dias por volta das ${process.env.GOAL_REMINDER_HOUR || 18}:00.`,
                    })
                } else if (parsed.type === 'ACTIVITY') {
                    if (!parsed.durationMinutes) {
                        await sock.sendMessage(target, {
                            text: '❌ Não consegui identificar a duração.\nExemplo: *estudei 2h java* ou *treinei 45min*',
                        })
                        continue
                    }

                    const user = await getUserByPhone(phone)
                    if (!user) {
                        await sock.sendMessage(target, {
                            text: '❌ Número não vinculado.\nEnvie: *login seu@email.com*',
                        })
                        continue
                    }

                    await publishActivityCreate({
                        userId: user.id,
                        description: phone,
                        category: parsed.category,
                        durationMinutes: parsed.durationMinutes,
                        title: parsed.title,
                        date: new Date().toISOString().split('T')[0],
                        source: 'WHATSAPP',
                    })
                    await sock.sendMessage(target, { text: '⏳ Registrando atividade...' })
                } else {
                    await handleCommand(sock, phone, 'HELP', text, target)
                }
            } catch (err) {
                console.error('[BOT] Erro:', err.message)
            }
        }
    })
}

connectToWhatsApp()
