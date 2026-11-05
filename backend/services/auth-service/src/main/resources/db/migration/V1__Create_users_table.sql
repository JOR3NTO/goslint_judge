--Nueva base de datos para postgres 
CREATE TABLE users (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name      VARCHAR(255) NOT NULL DEFAULT 'Usuario',
    last_name       VARCHAR(255) NOT NULL DEFAULT 'Prueba',
    username        VARCHAR(100) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(50)  NOT NULL DEFAULT 'STUDENT',
    institution     VARCHAR(255),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);