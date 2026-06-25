const amqp = require('amqplib');
const axios = require('axios');

const RABBITMQ_URL = process.env.RABBITMQ_URL || 'amqp://guest:guest@localhost:5672';
const WHATSAPP_BOT_URL = process.env.WHATSAPP_BOT_URL || 'http://localhost:3000';

async function sendWhatsApp(phone, text) {
  try {
    await axios.post(`${WHATSAPP_BOT_URL}/send`, { phone, text });
  } catch (err) {
    console.error('[WORKER] Falha ao enviar mensagem:', err.response?.data || err.message);
  }
}

async function start() {
  console.log('[WORKER] Conectando ao RabbitMQ...');
  const connection = await amqp.connect(RABBITMQ_URL);
  const channel = await connection.createChannel();

  await channel.assertQueue('activity.created', { durable: true });
  await channel.assertQueue('activity.error', { durable: true });

  console.log('[WORKER] Aguardando eventos...');

  //Atividade criada
  channel.consume('activity.created', async (msg) => {
    if (!msg) return;

    const event = JSON.parse(msg.content.toString());
    console.log('[WORKER] activity.created:', event);

    if (event.phone) {
      const duration = event.durationMinutes
        ? formatDuration(event.durationMinutes)
        : '';
      const category = formatCategory(event.category);
      const title = event.title ? ` — ${event.title}` : '';

      await sendWhatsApp(
        event.phone,
        `✅ Registrado! ${duration} de ${category}${title}`
      );
    }

    channel.ack(msg);
  });

  //Erro ao criar atividade
  channel.consume('activity.error', async (msg) => {
    if (!msg) return;

    const event = JSON.parse(msg.content.toString());
    console.log('[WORKER] activity.error:', event);

    const originalRequest = event.originalRequest || {};
    const phone = event.phone || originalRequest.description;

    if (phone) {
      await sendWhatsApp(
        phone,
        `❌ Não consegui registrar: ${event.error || 'erro desconhecido'}\nMensagem original: "${originalRequest.title || originalRequest.rawMessage || ''}"`
      );
    }

    channel.ack(msg);
  });
}

function formatCategory(cat) {
  const map = {
    ESTUDO: 'Estudo',
    TREINO: 'Treino',
    LEITURA: 'Leitura',
    ALIMENTACAO: 'Alimentação',
    OUTRO: 'Atividade',
  };
  return map[cat] || cat;
}

function formatDuration(minutes) {
  const total = Number(minutes || 0);
  const hours = Math.floor(total / 60);
  const mins = total % 60;

  if (hours && mins) return `${hours}h${String(mins).padStart(2, '0')}`;
  if (hours) return `${hours}h`;
  return `${mins}min`;
}

start().catch(err => {
  console.error('[WORKER] Erro fatal:', err);
  process.exit(1);
});
