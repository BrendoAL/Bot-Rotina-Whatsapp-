CREATE DATABASE IF NOT EXISTS users_db;
CREATE DATABASE IF NOT EXISTS activity_db;

-- users_db

USE users_db;

CREATE TABLE IF NOT EXISTS users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    phone      VARCHAR(30),
    created_at DATETIME DEFAULT NOW(),
    active     BOOLEAN DEFAULT TRUE
);

-- activity_db

USE activity_db;

CREATE TABLE IF NOT EXISTS activities (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    title            VARCHAR(200),
    description      TEXT,
    category         ENUM('ESTUDO','TREINO','LEITURA','ALIMENTACAO','OUTRO') NOT NULL,
    duration_minutes INT NOT NULL,
    date             DATE NOT NULL,
    source           ENUM('API','WHATSAPP') DEFAULT 'API',
    created_at       DATETIME DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS whatsapp_messages (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone_number     VARCHAR(30) NOT NULL,
    message_text     TEXT NOT NULL,
    parsed_category  VARCHAR(50),
    parsed_duration  INT,
    parsed_title     VARCHAR(200),
    processed        BOOLEAN DEFAULT FALSE,
    activity_id      BIGINT,
    created_at       DATETIME DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS goals (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    category         ENUM('ESTUDO','TREINO','LEITURA','ALIMENTACAO','OUTRO') NOT NULL,
    target_minutes   INT NOT NULL,
    period           ENUM('DIARIO','SEMANAL','MENSAL') NOT NULL,
    active           BOOLEAN DEFAULT TRUE,
    created_at       DATETIME DEFAULT NOW()
);