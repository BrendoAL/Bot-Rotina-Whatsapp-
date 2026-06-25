import axios from 'axios'

const ACTIVITY_SERVICE_URL = process.env.ACTIVITY_SERVICE_URL || 'http://activity-service:8082'
const USER_SERVICE_URL = process.env.USER_SERVICE_URL || 'http://user-service:8081'

async function sendMessage(sock, phone, text) {
    const jid = String(phone).includes('@') ? phone : `${phone}@s.whatsapp.net`
    await sock.sendMessage(jid, { text })
}

async function getUserByPhone(phone) {
    try {
        const { data } = await axios.get(`${USER_SERVICE_URL}/api/users/phone/${phone}`)
        return data
    } catch {
        return null
    }
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

function addDays(dateString, days) {
    const date = new Date(`${dateString}T00:00:00-03:00`)
    date.setDate(date.getDate() + days)
    return localDateString(date)
}

function buildCategoryLines(activities) {
    const byCategory = new Map()

    for (const activity of activities) {
        const current = byCategory.get(activity.category) || { count: 0, minutes: 0 }
        current.count += 1
        current.minutes += Number(activity.durationMinutes || 0)
        byCategory.set(activity.category, current)
    }

    if (!byCategory.size) return 'Nenhuma atividade registrada.'

    return [...byCategory.entries()]
        .sort((a, b) => b[1].minutes - a[1].minutes)
        .map(([category, stats]) => `• ${formatCategory(category)}: ${formatDuration(stats.minutes)} (${stats.count})`)
        .join('\n')
}

export async function handleCommand(sock, phone, command, originalText, target = phone) {
    try {
        switch (command) {

            case 'STATS': {
                const user = await getUserByPhone(phone)
                if (!user) {
                    await sendMessage(sock, target, '❌ Número não vinculado.\nEnvie: *login seu@email.com*')
                    return
                }
                const { data } = await axios.get(`${ACTIVITY_SERVICE_URL}/api/activities/stats/${user.id}`)
                const { data: activities } = await axios.get(`${ACTIVITY_SERVICE_URL}/api/activities/user/${user.id}`)
                const today = localDateString()
                const weekStart = addDays(today, -6)
                const todayActivities = activities.filter(a => a.date === today)
                const weekActivities = activities.filter(a => a.date >= weekStart && a.date <= today)

                await sendMessage(sock, target,
                    `📊 *Suas estatísticas de hoje*\n\n` +
                    `✅ Atividades: ${data.todayCount}\n` +
                    `⏱ Tempo: ${formatDuration(data.todayMinutes)}\n` +
                    `${buildCategoryLines(todayActivities)}\n\n` +
                    `📅 *Esta semana:*\n` +
                    `✅ Atividades: ${data.weekCount}\n` +
                    `⏱ Tempo: ${formatDuration(data.weekMinutes)}\n` +
                    `${buildCategoryLines(weekActivities)}`
                )
                break
            }

            case 'GOALS': {
                const user = await getUserByPhone(phone)
                if (!user) {
                    await sendMessage(sock, target, '❌ Número não vinculado.\nEnvie: *login seu@email.com*')
                    return
                }
                const { data } = await axios.get(`${ACTIVITY_SERVICE_URL}/api/goals/user/${user.id}`)
                if (!data.length) {
                    await sendMessage(sock, target, '📋 Nenhuma meta cadastrada.\nCrie com: *meta: estudar 1h por dia*')
                    return
                }
                const lines = data.map(g => `• ${formatCategory(g.category)}: ${formatDuration(g.targetMinutes)}/${formatPeriod(g.period)}`).join('\n')
                await sendMessage(sock, target, `🎯 *Suas metas ativas:*\n\n${lines}`)
                break
            }

            case 'LOGIN': {
                const email = originalText.replace(/login\s+/i, '').trim()
                try {
                    const { data: user } = await axios.get(`${USER_SERVICE_URL}/api/users/email/${email}`)
                    await axios.put(`${USER_SERVICE_URL}/api/users/${user.id}/phone`, { phone })
                    await sendMessage(sock, target, `✅ Vinculado com sucesso!\nOlá, *${user.name}*! Pode começar a registrar suas atividades.`)
                } catch {
                    await sendMessage(sock, target, `❌ Email não encontrado: ${email}`)
                }
                break
            }

            case 'HELP':
            default: {
                await sendMessage(sock, target,
                    `🤖 *Bot de Produtividade*\n\n` +
                    `*Registrar atividade:*\n` +
                    `• "estudei 2h python"\n` +
                    `• "treinei 45min"\n` +
                    `• "li 30min"\n` +
                    `• "estudei 1h30 java"\n\n` +
                    `*Comandos:*\n` +
                    `• *resumo* — stats de hoje\n` +
                    `• *metas* — listar metas\n` +
                    `• *meta: estudar 1h por dia* — criar meta\n` +
                    `• *ajuda* — este menu\n\n` +
                    `_Primeiro acesso? Envie: login seu@email.com_`
                )
                break
            }
        }
    } catch (err) {
        console.error('[COMMANDS] Erro:', err.message)
        await sendMessage(sock, target, '❌ Ocorreu um erro. Tente novamente.')
    }
}
