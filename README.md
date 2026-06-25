# Lambda App - Sistema Pessoal de Atividades

Sistema para registrar atividades, acompanhar metas e consultar estatisticas de produtividade. O projeto usa uma arquitetura baseada em microservicos com APIs Spring Boot, mensageria via RabbitMQ, banco MySQL, bot de WhatsApp e proxy Nginx.

## Sumario

- [Visao geral](#visao-geral)
- [Arquitetura](#arquitetura)
- [Tecnologias](#tecnologias)
- [Requisitos](#requisitos)
- [Como executar com Docker](#como-executar-com-docker)
- [Como usar](#como-usar)
- [Endpoints principais](#endpoints-principais)
- [Bot do WhatsApp](#bot-do-whatsapp)
- [Desenvolvimento local](#desenvolvimento-local)
- [Testes](#testes)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Variaveis de ambiente](#variaveis-de-ambiente)

## Visao geral

O Lambda App permite:

- cadastrar usuarios e autenticar login;
- vincular um numero de WhatsApp a um usuario;
- registrar atividades por API ou por mensagens no WhatsApp;
- criar metas por categoria e periodo;
- consultar estatisticas do dia e da semana;
- receber confirmacoes e erros pelo WhatsApp quando atividades sao processadas.

Categorias suportadas:

- `ESTUDO`
- `TREINO`
- `LEITURA`
- `ALIMENTACAO`
- `OUTRO`

Periodos de meta suportados pelo bot:

- `DAILY`
- `WEEKLY`
- `MONTHLY`

> Observacao: o script inicial do banco usa enums em portugues (`DIARIO`, `SEMANAL`, `MENSAL`), enquanto o bot envia `DAILY`, `WEEKLY` e `MONTHLY`. Como as entidades tambem usam `ddl-auto=update`, valide esse ponto caso use o SQL manualmente fora do fluxo Docker/Spring.

## Arquitetura

Servicos principais:

| Servico | Porta | Responsabilidade |
| --- | ---: | --- |
| `user-service` | `8081` | Cadastro, login, consulta e vinculacao de telefone do usuario |
| `activity-service` | `8082` | Registro de atividades, estatisticas, metas e consumo de eventos |
| `whatsapp-bot` | `3000` | Integracao com WhatsApp usando Baileys, parsing de mensagens e publicacao de eventos |
| `notification-worker` | interno | Consome eventos de sucesso/erro e envia mensagens pelo bot |
| `mysql` | `3306` | Bancos `users_db` e `activity_db` |
| `rabbitmq` | `5672`, `15672` | Filas de processamento e painel de administracao |
| `nginx` | `8090` | Proxy para APIs e bot |

Fluxo resumido:

1. O usuario se cadastra na API do `user-service`.
2. O usuario conecta o bot do WhatsApp e envia `login seu@email.com`.
3. O bot vincula o telefone ao usuario.
4. Uma mensagem como `estudei 30min java` e convertida em evento `activity.create`.
5. O `activity-service` consome o evento, valida o usuario e registra a atividade.
6. O `activity-service` publica `activity.created` ou `activity.error`.
7. O `notification-worker` consome o resultado e pede ao bot para enviar a resposta no WhatsApp.

Filas RabbitMQ usadas:

- `activity.create`
- `activity.created`
- `activity.error`

## Tecnologias

Backend Java:

- Java 21
- Spring Boot 3.4.3
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Actuator
- Spring AMQP
- Spring Security no `user-service`
- JWT com `jjwt`
- Lombok
- Gradle Wrapper
- MySQL Connector/J
- H2 para testes

Node.js:

- Node.js 20 no `whatsapp-bot`
- Node.js 18 no `notification-worker`
- Baileys para WhatsApp Web
- Axios
- amqplib
- Jest no `whatsapp-bot`
- Nodemon para desenvolvimento do bot

Infraestrutura:

- Docker e Docker Compose
- MySQL 8
- RabbitMQ 3 com Management UI
- Nginx Alpine

## Requisitos

Para rodar tudo com Docker:

- Docker
- Docker Compose

Para desenvolvimento local sem Docker:

- Java 21
- Node.js 18 ou superior
- npm
- MySQL 8
- RabbitMQ

## Como executar com Docker

Na raiz do projeto:

```bash
docker compose up --build
```

Para executar em background:

```bash
docker compose up --build -d
```

Para acompanhar logs:

```bash
docker compose logs -f
```

Logs de um servico especifico:

```bash
docker compose logs -f whatsapp-bot
docker compose logs -f user-service
docker compose logs -f activity-service
```

Para parar:

```bash
docker compose down
```

Para parar e remover os dados persistidos do MySQL:

```bash
docker compose down -v
```

URLs uteis:

- API via Nginx: `http://localhost:8090`
- User Service direto: `http://localhost:8081`
- Activity Service direto: `http://localhost:8082`
- WhatsApp Bot direto: `http://localhost:3000`
- RabbitMQ Management: `http://localhost:15672`
  - usuario: `guest`
  - senha: `guest`

## Como usar

### 1. Subir a aplicacao

```bash
docker compose up --build
```

### 2. Criar um usuario

```bash
curl -X POST http://localhost:8090/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Brendo",
    "email": "brendo@example.com",
    "password": "123456"
  }'
```

Resposta esperada:

```json
{
  "id": 1
}
```

### 3. Fazer login

```bash
curl -X POST http://localhost:8090/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "brendo@example.com",
    "password": "123456"
  }'
```

Resposta esperada:

```json
{
  "token": "..."
}
```

### 4. Registrar uma atividade pela API

```bash
curl -X POST http://localhost:8090/api/activities \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "title": "Java",
    "description": "Estudo de Spring Boot",
    "category": "ESTUDO",
    "durationMinutes": 60,
    "date": "2026-06-25",
    "source": "API"
  }'
```

### 5. Criar uma meta pela API

```bash
curl -X POST http://localhost:8090/api/goals \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "category": "ESTUDO",
    "targetMinutes": 120,
    "period": "DAILY"
  }'
```

### 6. Consultar estatisticas

```bash
curl http://localhost:8090/api/activities/stats/1
```

Resposta:

```json
{
  "todayCount": 1,
  "todayMinutes": 60,
  "weekCount": 1,
  "weekMinutes": 60
}
```

## Endpoints principais

### User Service

Base via Nginx: `http://localhost:8090/api/users`

| Metodo | Rota | Descricao |
| --- | --- | --- |
| `POST` | `/register` | Cadastra usuario |
| `POST` | `/login` | Autentica usuario e retorna token JWT |
| `GET` | `/{id}` | Busca usuario por ID |
| `GET` | `/phone/{phone}` | Busca usuario por telefone |
| `GET` | `/email/{email}` | Busca usuario por email |
| `PUT` | `/{id}/phone` | Atualiza telefone vinculado ao usuario |

Payloads:

```json
{
  "name": "Brendo",
  "email": "brendo@example.com",
  "password": "123456"
}
```

```json
{
  "email": "brendo@example.com",
  "password": "123456"
}
```

```json
{
  "phone": "5511999999999"
}
```

### Activity Service

Base via Nginx: `http://localhost:8090`

| Metodo | Rota | Descricao |
| --- | --- | --- |
| `POST` | `/api/activities` | Cria atividade |
| `GET` | `/api/activities/user/{userId}` | Lista atividades de um usuario |
| `GET` | `/api/activities/stats/{userId}` | Retorna estatisticas do dia e da semana |
| `POST` | `/api/goals` | Cria meta |
| `GET` | `/api/goals/user/{userId}` | Lista metas ativas de um usuario |
| `DELETE` | `/api/goals/{id}` | Desativa uma meta |

Payload de atividade:

```json
{
  "userId": 1,
  "title": "Java",
  "description": "Estudo de Spring Boot",
  "category": "ESTUDO",
  "durationMinutes": 60,
  "date": "2026-06-25",
  "source": "API"
}
```

Payload de meta:

```json
{
  "userId": 1,
  "category": "ESTUDO",
  "targetMinutes": 120,
  "period": "DAILY"
}
```

### WhatsApp Bot

Base direta: `http://localhost:3000`

| Metodo | Rota | Descricao |
| --- | --- | --- |
| `GET` | `/health` | Status da conexao do bot com WhatsApp |
| `POST` | `/send` | Envia mensagem pelo WhatsApp |

Exemplo:

```bash
curl -X POST http://localhost:3000/send \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "5511999999999",
    "text": "Teste do Lambda Bot"
  }'
```

Via Nginx, o bot fica exposto sob `/bot`, por exemplo:

```bash
curl http://localhost:8090/bot/health
```

## Bot do WhatsApp

Ao iniciar o `whatsapp-bot`, acompanhe os logs:

```bash
docker compose logs -f whatsapp-bot
```

Quando o QR Code aparecer no terminal:

1. Abra o WhatsApp no celular.
2. Va em **Dispositivos conectados**.
3. Escolha **Conectar dispositivo**.
4. Escaneie o QR Code exibido no log.

Depois de conectar, o bot pede autorizacao do chat pessoal. Envie no chat "Voce":

```text
ativar lambda
```

Em seguida, vincule o usuario criado na API:

```text
login brendo@example.com
```

Comandos aceitos:

| Mensagem | Acao |
| --- | --- |
| `login seu@email.com` | Vincula o telefone ao usuario |
| `estudei 2h python` | Registra atividade de estudo |
| `treinei 45min` | Registra atividade de treino |
| `li 30min` | Registra atividade de leitura |
| `meta: estudar 1h por dia` | Cria meta diaria |
| `resumo` | Mostra estatisticas do dia e da semana |
| `metas` | Lista metas ativas |
| `ajuda` | Mostra ajuda do bot |

O bot tambem envia lembretes diarios de metas. O horario padrao e `18:00` no fuso `America/Sao_Paulo`, configuravel por `GOAL_REMINDER_HOUR` e `GOAL_REMINDER_MINUTE`.

## Desenvolvimento local

### User Service

```bash
cd user-service
./gradlew bootRun
```

Por padrao, usa:

- porta `8081`
- MySQL em `localhost:3306`
- banco `users_db`
- RabbitMQ em `localhost:5672`

### Activity Service

```bash
cd activity-service
./gradlew bootRun
```

Por padrao, usa:

- porta `8082`
- MySQL em `mysql:3306` quando executado com o `application.yml`
- RabbitMQ em `rabbitmq:5672` quando executado com o `application.yml`

Para rodar fora do Docker, ajuste as variaveis de ambiente ou o arquivo de configuracao para apontar para `localhost`.

### WhatsApp Bot

```bash
cd whatsapp-bot
npm install
npm run dev
```

Variaveis uteis:

```bash
PORT=3000
RABBITMQ_URL=amqp://guest:guest@localhost:5672
USER_SERVICE_URL=http://localhost:8081
ACTIVITY_SERVICE_URL=http://localhost:8082
AUTH_FOLDER=./sessions
```

### Notification Worker

```bash
cd notification-worker
npm install
npm start
```

Variaveis uteis:

```bash
RABBITMQ_URL=amqp://guest:guest@localhost:5672
WHATSAPP_BOT_URL=http://localhost:3000
```

## Testes

User Service:

```bash
cd user-service
./gradlew test
```

Activity Service:

```bash
cd activity-service
./gradlew test
```

WhatsApp Bot:

```bash
cd whatsapp-bot
npm test
```

## Estrutura do projeto

```text
.
|-- activity-service/       # API de atividades, metas, estatisticas e consumidores RabbitMQ
|-- init-db/                # Script SQL inicial para MySQL
|-- nginx/                  # Configuracao do proxy reverso
|-- notification-worker/    # Worker Node.js para notificacoes via WhatsApp
|-- user-service/           # API de usuarios, login e JWT
|-- whatsapp-bot/           # Bot WhatsApp com Baileys
|-- docker-compose.yml      # Orquestracao local
`-- README.md
```

## Variaveis de ambiente

### `user-service`

| Variavel | Padrao no Docker | Descricao |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://mysql:3306/users_db?...` | URL JDBC do banco de usuarios |
| `SPRING_DATASOURCE_USERNAME` | `root` | Usuario do banco |
| `SPRING_DATASOURCE_PASSWORD` | `root` | Senha do banco |
| `SPRING_RABBITMQ_HOST` | `rabbitmq` | Host do RabbitMQ |
| `SPRING_RABBITMQ_PORT` | `5672` | Porta do RabbitMQ |
| `SERVER_PORT` | `8081` | Porta HTTP |

### `activity-service`

| Variavel | Padrao no Docker | Descricao |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://mysql:3306/activity_db?...` | URL JDBC do banco de atividades |
| `SPRING_DATASOURCE_USERNAME` | `root` | Usuario do banco |
| `SPRING_DATASOURCE_PASSWORD` | `root` | Senha do banco |
| `SPRING_RABBITMQ_HOST` | `rabbitmq` | Host do RabbitMQ |
| `SPRING_RABBITMQ_PORT` | `5672` | Porta do RabbitMQ |
| `USER_SERVICE_URL` | `http://user-service:8081` | URL interna do servico de usuarios |
| `SERVER_PORT` | `8082` | Porta HTTP |

### `whatsapp-bot`

| Variavel | Padrao | Descricao |
| --- | --- | --- |
| `PORT` | `3000` | Porta HTTP do bot |
| `RABBITMQ_URL` | `amqp://guest:guest@rabbitmq:5672` | URL do RabbitMQ |
| `USER_SERVICE_URL` | `http://user-service:8081` | URL do servico de usuarios |
| `ACTIVITY_SERVICE_URL` | `http://activity-service:8082` | URL do servico de atividades |
| `AUTH_FOLDER` | `./sessions` | Pasta onde a sessao do WhatsApp e salva |
| `SELF_CHAT_JID` | vazio | Chat autorizado manualmente, se quiser fixar por ambiente |
| `GOAL_REMINDER_HOUR` | `18` | Hora do lembrete de metas |
| `GOAL_REMINDER_MINUTE` | `0` | Minuto do lembrete de metas |
| `BAILEYS_LOG_LEVEL` | `warn` | Nivel de log do Baileys |

### `notification-worker`

| Variavel | Padrao | Descricao |
| --- | --- | --- |
| `RABBITMQ_URL` | `amqp://guest:guest@rabbitmq:5672` | URL do RabbitMQ |
| `WHATSAPP_BOT_URL` | `http://whatsapp-bot:3000` | URL do bot usada para enviar mensagens |

## Banco de dados

O Docker Compose sobe um MySQL 8 com volume persistente `mysql_data`. O script `init-db/01-init.sql` cria:

- banco `users_db`;
- banco `activity_db`;
- tabela `users`;
- tabela `activities`;
- tabela `whatsapp_messages`;
- tabela `goals`.

As aplicacoes Spring tambem estao configuradas com `spring.jpa.hibernate.ddl-auto=update`, entao o schema pode ser ajustado automaticamente a partir das entidades.

## Observabilidade

Os servicos Spring incluem Spring Actuator com exposicao ampla configurada nos arquivos `application.yml`. Exemplos:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

O `docker-compose.yml` tambem possui blocos comentados para Prometheus e Grafana, que podem ser habilitados futuramente com as configuracoes correspondentes.

## Problemas comuns

### O bot nao conecta no WhatsApp

- Verifique os logs com `docker compose logs -f whatsapp-bot`.
- Escaneie o QR Code exibido no terminal.
- Se a sessao estiver corrompida, remova a pasta `whatsapp-bot/sessions` e conecte novamente.

### A API retorna usuario nao encontrado ao usar o bot

- Crie o usuario pela API.
- Envie `login seu@email.com` no WhatsApp antes de registrar atividades.
- Confira se o telefone salvo no usuario corresponde ao numero que esta enviando mensagens.

### Activity Service nao encontra o User Service

- Com Docker, confirme que `USER_SERVICE_URL=http://user-service:8081`.
- Fora do Docker, use `USER_SERVICE_URL=http://localhost:8081`.

### RabbitMQ nao recebe mensagens

- Abra `http://localhost:15672` e confira as filas.
- Verifique se `whatsapp-bot`, `activity-service` e `notification-worker` estao apontando para a mesma URL do RabbitMQ.
