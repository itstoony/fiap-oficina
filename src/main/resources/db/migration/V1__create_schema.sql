-- ============================================================
-- V1 — Schema inicial do sistema de gestão de oficina mecânica
-- ============================================================

-- ── Segurança ──────────────────────────────────────────────
CREATE TABLE usuarios (
    id          UUID         PRIMARY KEY,
    login       VARCHAR(255) NOT NULL UNIQUE,
    senha       VARCHAR(255) NOT NULL,
    nome        VARCHAR(255)
);

-- ── Atendimento ────────────────────────────────────────────
CREATE TABLE clientes (
    id               UUID         PRIMARY KEY,
    nome             VARCHAR(255) NOT NULL,
    email            VARCHAR(255) NOT NULL,
    telefone         VARCHAR(255) NOT NULL,
    documento_numero VARCHAR(14),
    documento_tipo   VARCHAR(4),
    criado_em        TIMESTAMP    NOT NULL,
    atualizado_em    TIMESTAMP    NOT NULL
);

CREATE TABLE atendentes (
    id            UUID         PRIMARY KEY,
    nome          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    telefone      VARCHAR(255) NOT NULL,
    criado_em     TIMESTAMP    NOT NULL,
    atualizado_em TIMESTAMP    NOT NULL
);

CREATE TABLE veiculos (
    id            UUID         PRIMARY KEY,
    marca         VARCHAR(255) NOT NULL,
    modelo        VARCHAR(255) NOT NULL,
    ano           INTEGER      NOT NULL,
    cor           VARCHAR(255) NOT NULL,
    placa         VARCHAR(8),
    cliente_id    UUID         NOT NULL REFERENCES clientes(id),
    criado_em     TIMESTAMP    NOT NULL,
    atualizado_em TIMESTAMP    NOT NULL
);

-- ── Administração ──────────────────────────────────────────
CREATE TABLE servicos (
    id            UUID           PRIMARY KEY,
    nome          VARCHAR(255)   NOT NULL,
    descricao     VARCHAR(1000)  NOT NULL,
    preco_base    NUMERIC(10, 2) NOT NULL,
    criado_em     TIMESTAMP      NOT NULL,
    atualizado_em TIMESTAMP      NOT NULL
);

-- ── Estoque ────────────────────────────────────────────────
CREATE TABLE pecas (
    id              UUID           PRIMARY KEY,
    nome            VARCHAR(255)   NOT NULL,
    codigo          VARCHAR(255)   NOT NULL UNIQUE,
    preco_unitario  NUMERIC(10, 2) NOT NULL,
    qtd_estoque     INTEGER        NOT NULL,
    qtd_reservada   INTEGER        NOT NULL,
    qtd_minima      INTEGER        NOT NULL,
    criado_em       TIMESTAMP      NOT NULL,
    atualizado_em   TIMESTAMP      NOT NULL
);

CREATE TABLE movimentacoes_estoque (
    id                UUID         PRIMARY KEY,
    peca_id           UUID         NOT NULL REFERENCES pecas(id),
    tipo              VARCHAR(25)  NOT NULL,
    quantidade        INTEGER      NOT NULL,
    os_id             UUID,
    observacao        VARCHAR(500),
    data_movimentacao TIMESTAMP    NOT NULL
);

-- ── Execução ───────────────────────────────────────────────
CREATE TABLE ordens_de_servico (
    id                   UUID           PRIMARY KEY,
    numero               VARCHAR(20)    NOT NULL UNIQUE,
    status               VARCHAR(25)    NOT NULL,
    cliente_id           UUID           NOT NULL REFERENCES clientes(id),
    veiculo_id           UUID           NOT NULL REFERENCES veiculos(id),
    atendente_id         UUID           REFERENCES atendentes(id),
    valor_total          NUMERIC(10, 2) NOT NULL,
    observacoes          TEXT,
    data_abertura        TIMESTAMP      NOT NULL,
    data_inicio_execucao TIMESTAMP,
    data_fim_execucao    TIMESTAMP,
    criado_em            TIMESTAMP      NOT NULL,
    atualizado_em        TIMESTAMP      NOT NULL
);

CREATE TABLE itens_servico (
    id                  UUID           PRIMARY KEY,
    ordem_de_servico_id UUID           NOT NULL REFERENCES ordens_de_servico(id),
    servico_id          UUID           NOT NULL REFERENCES servicos(id),
    quantidade          INTEGER        NOT NULL,
    preco_unitario      NUMERIC(10, 2) NOT NULL,
    observacao          VARCHAR(500),
    criado_em           TIMESTAMP      NOT NULL,
    atualizado_em       TIMESTAMP      NOT NULL
);

CREATE TABLE itens_peca (
    id                  UUID           PRIMARY KEY,
    ordem_de_servico_id UUID           NOT NULL REFERENCES ordens_de_servico(id),
    peca_id             UUID           NOT NULL REFERENCES pecas(id),
    quantidade          INTEGER        NOT NULL,
    preco_unitario      NUMERIC(10, 2) NOT NULL,
    criado_em           TIMESTAMP      NOT NULL,
    atualizado_em       TIMESTAMP      NOT NULL
);

-- ── Índices para consultas frequentes ──────────────────────
CREATE INDEX idx_veiculos_cliente_id        ON veiculos(cliente_id);
CREATE INDEX idx_ordens_cliente_id          ON ordens_de_servico(cliente_id);
CREATE INDEX idx_ordens_veiculo_id          ON ordens_de_servico(veiculo_id);
CREATE INDEX idx_ordens_status              ON ordens_de_servico(status);
CREATE INDEX idx_ordens_numero              ON ordens_de_servico(numero);
CREATE INDEX idx_movimentacoes_peca_id      ON movimentacoes_estoque(peca_id);
CREATE INDEX idx_itens_servico_os_id        ON itens_servico(ordem_de_servico_id);
CREATE INDEX idx_itens_peca_os_id           ON itens_peca(ordem_de_servico_id);
