#!/usr/bin/env bash
# =============================================================================
# seed.sh — Popula o banco com dados realistas de produção
# =============================================================================
set -euo pipefail

BASE="http://localhost:8080"
H="Content-Type: application/json"

# ── helpers ──────────────────────────────────────────────────────────────────
post()   { curl -s -X POST -H "$H" -H "Authorization: Bearer $TOKEN" "$BASE$1" -d "$2"; }
patch()  { curl -s -X POST -H "$H" -H "Authorization: Bearer $TOKEN" "$BASE$1" -d "${2:-{}}"; }
get_all(){ curl -s -H "Authorization: Bearer $TOKEN" "$BASE$1"; }
id()     { echo "$1" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['id'])" 2>/dev/null; }
nr()     { echo "$1" | python3 -c "import sys,json; print(json.load(sys.stdin)['numero'])"; }

# Cria atendente ou busca pelo email se já existir
get_or_create_atendente() {
  local body="$1" email="$2"
  local resp=$(post /api/admin/atendentes "$body")
  local found=$(id "$resp")
  if [ -z "$found" ]; then
    found=$(get_all /api/admin/atendentes | python3 -c "
import sys,json; data=json.load(sys.stdin)
items=data if isinstance(data,list) else data.get('content',data.get('items',[]))
for i in items:
  if i.get('email')=='$email': print(i['id']); break
")
  fi
  echo "$found"
}

# Cria cliente ou busca pelo email se já existir
get_or_create_cliente() {
  local body="$1" email="$2"
  local resp=$(post /api/admin/clientes "$body")
  local found=$(id "$resp")
  if [ -z "$found" ]; then
    found=$(get_all /api/admin/clientes | python3 -c "
import sys,json; data=json.load(sys.stdin)
items=data if isinstance(data,list) else data.get('content',data.get('items',[]))
for i in items:
  if i.get('email')=='$email': print(i['id']); break
")
  fi
  echo "$found"
}

# Cria serviço ou busca pelo nome se já existir
get_or_create_servico() {
  local body="$1" nome="$2"
  local resp=$(post /api/admin/servicos "$body")
  local found=$(id "$resp")
  if [ -z "$found" ]; then
    found=$(get_all /api/admin/servicos | python3 -c "
import sys,json; data=json.load(sys.stdin)
items=data if isinstance(data,list) else data.get('content',data.get('items',[]))
for i in items:
  if i.get('nome')=='$nome': print(i['id']); break
")
  fi
  echo "$found"
}

# Cria peça ou busca pelo código se já existir
get_or_create_peca() {
  local body="$1" codigo="$2"
  local resp=$(post /api/admin/pecas "$body")
  local found=$(id "$resp")
  if [ -z "$found" ]; then
    found=$(get_all /api/admin/pecas | python3 -c "
import sys,json; data=json.load(sys.stdin)
items=data if isinstance(data,list) else data.get('content',data.get('items',[]))
for i in items:
  if i.get('codigo')=='$codigo': print(i['id']); break
")
  fi
  echo "$found"
}

echo "▶ Autenticando..."
LOGIN=$(curl -s -X POST -H "$H" "$BASE/api/auth/login" \
  -d '{"login":"meuadmin","senha":"senha_forte"}')
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
echo "  Token obtido."

# =============================================================================
# ATENDENTES
# =============================================================================
echo ""
echo "▶ Cadastrando atendentes..."

AT1_ID=$(get_or_create_atendente '{"nome":"Carlos Eduardo Mendes","email":"carlos.mendes@oficina.com.br","telefone":"(11) 98234-5678"}' "carlos.mendes@oficina.com.br")
AT2_ID=$(get_or_create_atendente '{"nome":"Fernanda Lima Souza","email":"fernanda.lima@oficina.com.br","telefone":"(11) 97654-3210"}' "fernanda.lima@oficina.com.br")
AT3_ID=$(get_or_create_atendente '{"nome":"Ricardo Aparecido Nunes","email":"ricardo.nunes@oficina.com.br","telefone":"(11) 96543-2109"}' "ricardo.nunes@oficina.com.br")

echo "  $AT1_ID — Carlos Mendes"
echo "  $AT2_ID — Fernanda Lima"
echo "  $AT3_ID — Ricardo Nunes"

# =============================================================================
# CLIENTES
# =============================================================================
echo ""
echo "▶ Cadastrando clientes..."

C1_ID=$(get_or_create_cliente '{"nome":"João Paulo Ferreira","email":"joao.ferreira@gmail.com","telefone":"(11) 99123-4567","documento":"529.982.247-25"}' "joao.ferreira@gmail.com")
C2_ID=$(get_or_create_cliente '{"nome":"Maria Clara Oliveira","email":"maria.oliveira@hotmail.com","telefone":"(11) 98876-5432","documento":"111.444.777-35"}' "maria.oliveira@hotmail.com")
C3_ID=$(get_or_create_cliente '{"nome":"Auto Peças Rodrigues Ltda","email":"contato@autopecasrodrigues.com.br","telefone":"(11) 3345-6789","documento":"12.345.678/0001-09"}' "contato@autopecasrodrigues.com.br")
C4_ID=$(get_or_create_cliente '{"nome":"Rafael Henrique Barbosa","email":"rafael.barbosa@yahoo.com.br","telefone":"(11) 97345-8901","documento":"407.302.170-27"}' "rafael.barbosa@yahoo.com.br")
C5_ID=$(get_or_create_cliente '{"nome":"Transportadora Veloz ME","email":"operacoes@velozme.com.br","telefone":"(11) 3210-9876","documento":"98.765.432/0001-09"}' "operacoes@velozme.com.br")
C6_ID=$(get_or_create_cliente '{"nome":"Luciana Pereira dos Santos","email":"luciana.santos@gmail.com","telefone":"(11) 96789-0123","documento":"863.051.247-44"}' "luciana.santos@gmail.com")

echo "  $C1_ID — João Ferreira"
echo "  $C2_ID — Maria Oliveira"
echo "  $C3_ID — Auto Peças Rodrigues"
echo "  $C4_ID — Rafael Barbosa"
echo "  $C5_ID — Transportadora Veloz"
echo "  $C6_ID — Luciana Santos"

# =============================================================================
# VEÍCULOS
# =============================================================================
echo ""
echo "▶ Cadastrando veículos..."

V1=$(post /api/admin/veiculos "{\"marca\":\"Toyota\",\"modelo\":\"Corolla\",\"ano\":2021,\"cor\":\"Prata\",\"placa\":\"ABC1D23\",\"clienteId\":\"$C1_ID\"}")
V2=$(post /api/admin/veiculos "{\"marca\":\"Honda\",\"modelo\":\"Civic\",\"ano\":2019,\"cor\":\"Preto\",\"placa\":\"DEF2E45\",\"clienteId\":\"$C2_ID\"}")
V3=$(post /api/admin/veiculos "{\"marca\":\"Volkswagen\",\"modelo\":\"Gol\",\"ano\":2018,\"cor\":\"Branco\",\"placa\":\"GHI3F67\",\"clienteId\":\"$C2_ID\"}")
V4=$(post /api/admin/veiculos "{\"marca\":\"Ford\",\"modelo\":\"Transit\",\"ano\":2020,\"cor\":\"Branco\",\"placa\":\"JKL4G89\",\"clienteId\":\"$C3_ID\"}")
V5=$(post /api/admin/veiculos "{\"marca\":\"Chevrolet\",\"modelo\":\"Onix\",\"ano\":2022,\"cor\":\"Vermelho\",\"placa\":\"MNO5H01\",\"clienteId\":\"$C4_ID\"}")
V6=$(post /api/admin/veiculos "{\"marca\":\"Fiat\",\"modelo\":\"Ducato\",\"ano\":2017,\"cor\":\"Branco\",\"placa\":\"PQR6I23\",\"clienteId\":\"$C5_ID\"}")
V7=$(post /api/admin/veiculos "{\"marca\":\"Fiat\",\"modelo\":\"Toro\",\"ano\":2023,\"cor\":\"Cinza\",\"placa\":\"STU7J45\",\"clienteId\":\"$C5_ID\"}")
V8=$(post /api/admin/veiculos "{\"marca\":\"Renault\",\"modelo\":\"Kwid\",\"ano\":2021,\"cor\":\"Azul\",\"placa\":\"VWX8K67\",\"clienteId\":\"$C6_ID\"}")

V1_ID=$(id "$V1"); echo "  $V1_ID — Toyota Corolla (João)"
V2_ID=$(id "$V2"); echo "  $V2_ID — Honda Civic (Maria)"
V3_ID=$(id "$V3"); echo "  $V3_ID — VW Gol (Maria)"
V4_ID=$(id "$V4"); echo "  $V4_ID — Ford Transit (Rodrigues)"
V5_ID=$(id "$V5"); echo "  $V5_ID — Chevrolet Onix (Rafael)"
V6_ID=$(id "$V6"); echo "  $V6_ID — Fiat Ducato (Veloz)"
V7_ID=$(id "$V7"); echo "  $V7_ID — Fiat Toro (Veloz)"
V8_ID=$(id "$V8"); echo "  $V8_ID — Renault Kwid (Luciana)"

# =============================================================================
# SERVIÇOS
# =============================================================================
echo ""
echo "▶ Cadastrando serviços..."

S1_ID=$(get_or_create_servico '{"nome":"Troca de Óleo e Filtro","descricao":"Substituição do óleo do motor e filtro de óleo conforme especificação do fabricante.","precoBase":180.00}' "Troca de Óleo e Filtro")
S2_ID=$(get_or_create_servico '{"nome":"Alinhamento e Balanceamento","descricao":"Alinhamento geométrico das rodas e balanceamento dinâmico dos pneus.","precoBase":150.00}' "Alinhamento e Balanceamento")
S3_ID=$(get_or_create_servico '{"nome":"Revisão de Freios","descricao":"Inspeção e substituição de pastilhas, discos e fluido de freio quando necessário.","precoBase":320.00}' "Revisão de Freios")
S4_ID=$(get_or_create_servico '{"nome":"Troca de Correia Dentada","descricao":"Substituição da correia dentada e tensor conforme intervalo do fabricante.","precoBase":680.00}' "Troca de Correia Dentada")
S5_ID=$(get_or_create_servico '{"nome":"Diagnóstico Eletrônico","descricao":"Leitura e interpretação de códigos de falha via scanner automotivo.","precoBase":120.00}' "Diagnóstico Eletrônico")
S6_ID=$(get_or_create_servico '{"nome":"Troca de Amortecedores","descricao":"Substituição do par de amortecedores dianteiro ou traseiro.","precoBase":550.00}' "Troca de Amortecedores")
S7_ID=$(get_or_create_servico '{"nome":"Higienização do Ar-Condicionado","descricao":"Limpeza do sistema de ar-condicionado e substituição de filtro de cabine.","precoBase":220.00}' "Higienização do Ar-Condicionado")
S8_ID=$(get_or_create_servico '{"nome":"Funilaria e Pintura","descricao":"Reparo de amassados, riscos e retoques de pintura automotiva.","precoBase":1200.00}' "Funilaria e Pintura")

echo "  $S1_ID — Troca de Óleo"
echo "  $S2_ID — Alinhamento/Balanceamento"
echo "  $S3_ID — Revisão de Freios"
echo "  $S4_ID — Correia Dentada"
echo "  $S5_ID — Diagnóstico Eletrônico"
echo "  $S6_ID — Amortecedores"
echo "  $S7_ID — Ar-Condicionado"
echo "  $S8_ID — Funilaria e Pintura"

# =============================================================================
# PEÇAS (com estoque)
# =============================================================================
echo ""
echo "▶ Cadastrando peças e estoque..."

P1_ID=$(get_or_create_peca  '{"nome":"Filtro de Óleo Bosch","codigo":"FO-BOSCH-001","precoUnitario":45.90,"qtdEstoque":0,"qtdMinima":5}' "FO-BOSCH-001")
P2_ID=$(get_or_create_peca  '{"nome":"Pastilha de Freio Dianteira Fras-le","codigo":"PF-FRASLE-D01","precoUnitario":189.90,"qtdEstoque":0,"qtdMinima":4}' "PF-FRASLE-D01")
P3_ID=$(get_or_create_peca  '{"nome":"Correia Dentada Gates","codigo":"CD-GATES-K015","precoUnitario":320.00,"qtdEstoque":0,"qtdMinima":2}' "CD-GATES-K015")
P4_ID=$(get_or_create_peca  '{"nome":"Amortecedor Dianteiro Monroe","codigo":"AD-MONROE-001","precoUnitario":380.00,"qtdEstoque":0,"qtdMinima":2}' "AD-MONROE-001")
P5_ID=$(get_or_create_peca  '{"nome":"Filtro de Cabine Mahle","codigo":"FC-MAHLE-001","precoUnitario":55.00,"qtdEstoque":0,"qtdMinima":6}' "FC-MAHLE-001")
P6_ID=$(get_or_create_peca  '{"nome":"Óleo Motor 5W30 Sintético Mobil 1L","codigo":"OM-MOBIL-5W30","precoUnitario":38.50,"qtdEstoque":0,"qtdMinima":20}' "OM-MOBIL-5W30")
P7_ID=$(get_or_create_peca  '{"nome":"Disco de Freio Ventilado Fremax","codigo":"DF-FREMAX-V01","precoUnitario":265.00,"qtdEstoque":0,"qtdMinima":2}' "DF-FREMAX-V01")
P8_ID=$(get_or_create_peca  '{"nome":"Vela de Ignição NGK Iridium","codigo":"VI-NGK-IR001","precoUnitario":89.90,"qtdEstoque":0,"qtdMinima":8}' "VI-NGK-IR001")
P9_ID=$(get_or_create_peca  '{"nome":"Fluido de Freio DOT 4 Bosch 500ml","codigo":"FF-BOSCH-D4","precoUnitario":32.00,"qtdEstoque":0,"qtdMinima":10}' "FF-BOSCH-D4")
P10_ID=$(get_or_create_peca '{"nome":"Tensor de Correia Dentada INA","codigo":"TC-INA-K015","precoUnitario":145.00,"qtdEstoque":0,"qtdMinima":2}' "TC-INA-K015")

echo "  $P1_ID — Filtro de Óleo"
echo "  $P2_ID — Pastilha Freio"
echo "  $P3_ID — Correia Dentada"
echo "  $P4_ID — Amortecedor"
echo "  $P5_ID — Filtro Cabine"
echo "  $P6_ID — Óleo Motor"
echo "  $P7_ID — Disco de Freio"
echo "  $P8_ID — Vela Ignição"
echo "  $P9_ID — Fluido Freio"
echo "  $P10_ID — Tensor Correia"

echo ""
echo "▶ Registrando entradas de estoque..."
post /api/admin/pecas/$P1_ID/entrada  '{"quantidade":30,"observacao":"Compra inicial — NF 1001"}' > /dev/null
post /api/admin/pecas/$P2_ID/entrada  '{"quantidade":20,"observacao":"Compra inicial — NF 1002"}' > /dev/null
post /api/admin/pecas/$P3_ID/entrada  '{"quantidade":10,"observacao":"Compra inicial — NF 1003"}' > /dev/null
post /api/admin/pecas/$P4_ID/entrada  '{"quantidade":8,"observacao":"Compra inicial — NF 1004"}' > /dev/null
post /api/admin/pecas/$P5_ID/entrada  '{"quantidade":25,"observacao":"Compra inicial — NF 1005"}' > /dev/null
post /api/admin/pecas/$P6_ID/entrada  '{"quantidade":80,"observacao":"Compra inicial — NF 1006"}' > /dev/null
post /api/admin/pecas/$P7_ID/entrada  '{"quantidade":10,"observacao":"Compra inicial — NF 1007"}' > /dev/null
post /api/admin/pecas/$P8_ID/entrada  '{"quantidade":40,"observacao":"Compra inicial — NF 1008"}' > /dev/null
post /api/admin/pecas/$P9_ID/entrada  '{"quantidade":30,"observacao":"Compra inicial — NF 1009"}' > /dev/null
post /api/admin/pecas/$P10_ID/entrada '{"quantidade":8,"observacao":"Compra inicial — NF 1010"}' > /dev/null
echo "  Estoque registrado."

# =============================================================================
# ORDENS DE SERVIÇO
# =============================================================================
echo ""
echo "▶ Criando Ordens de Serviço..."

# ── OS 1: ENTREGUE (João / Corolla) ─────────────────────────────────────────
OS1=$(post /api/admin/ordens "{\"clienteId\":\"$C1_ID\",\"veiculoId\":\"$V1_ID\",\"atendenteId\":\"$AT1_ID\",\"observacoes\":\"Cliente relatou barulho ao frear e óleo baixo.\"}")
OS1_ID=$(id "$OS1"); OS1_NR=$(nr "$OS1")
post /api/admin/ordens/$OS1_ID/servicos "{\"servicoId\":\"$S1_ID\",\"quantidade\":1,\"observacao\":\"Óleo 5W30 sintético\"}" > /dev/null
post /api/admin/ordens/$OS1_ID/servicos "{\"servicoId\":\"$S3_ID\",\"quantidade\":1}" > /dev/null
post /api/admin/ordens/$OS1_ID/pecas   "{\"pecaId\":\"$P1_ID\",\"quantidade\":1}" > /dev/null
post /api/admin/ordens/$OS1_ID/pecas   "{\"pecaId\":\"$P6_ID\",\"quantidade\":4}" > /dev/null
post /api/admin/ordens/$OS1_ID/pecas   "{\"pecaId\":\"$P2_ID\",\"quantidade\":1}" > /dev/null
patch /api/admin/ordens/$OS1_ID/iniciar-diagnostico
patch /api/admin/ordens/$OS1_ID/enviar-orcamento
curl -s -X POST -H "$H" "$BASE/api/public/ordens/$OS1_NR/aprovar" > /dev/null
patch /api/admin/ordens/$OS1_ID/iniciar-execucao "{\"atendenteId\":\"$AT1_ID\"}"
patch /api/admin/ordens/$OS1_ID/finalizar
patch /api/admin/ordens/$OS1_ID/entregar
echo "  OS $OS1_NR — ENTREGUE (João / Corolla)"

# ── OS 2: ENTREGUE (Maria / Civic) ──────────────────────────────────────────
OS2=$(post /api/admin/ordens "{\"clienteId\":\"$C2_ID\",\"veiculoId\":\"$V2_ID\",\"atendenteId\":\"$AT2_ID\",\"observacoes\":\"Revisão dos 30.000 km.\"}")
OS2_ID=$(id "$OS2"); OS2_NR=$(nr "$OS2")
post /api/admin/ordens/$OS2_ID/servicos "{\"servicoId\":\"$S1_ID\",\"quantidade\":1}" > /dev/null
post /api/admin/ordens/$OS2_ID/servicos "{\"servicoId\":\"$S2_ID\",\"quantidade\":1}" > /dev/null
post /api/admin/ordens/$OS2_ID/servicos "{\"servicoId\":\"$S5_ID\",\"quantidade\":1}" > /dev/null
post /api/admin/ordens/$OS2_ID/pecas   "{\"pecaId\":\"$P1_ID\",\"quantidade\":1}" > /dev/null
post /api/admin/ordens/$OS2_ID/pecas   "{\"pecaId\":\"$P6_ID\",\"quantidade\":5}" > /dev/null
post /api/admin/ordens/$OS2_ID/pecas   "{\"pecaId\":\"$P8_ID\",\"quantidade\":4}" > /dev/null
patch /api/admin/ordens/$OS2_ID/iniciar-diagnostico
patch /api/admin/ordens/$OS2_ID/enviar-orcamento
curl -s -X POST -H "$H" "$BASE/api/public/ordens/$OS2_NR/aprovar" > /dev/null
patch /api/admin/ordens/$OS2_ID/iniciar-execucao "{\"atendenteId\":\"$AT2_ID\"}"
patch /api/admin/ordens/$OS2_ID/finalizar
patch /api/admin/ordens/$OS2_ID/entregar
echo "  OS $OS2_NR — ENTREGUE (Maria / Civic)"

# ── OS 3: ENTREGUE (Transportadora / Ducato) ────────────────────────────────
OS3=$(post /api/admin/ordens "{\"clienteId\":\"$C5_ID\",\"veiculoId\":\"$V6_ID\",\"atendenteId\":\"$AT3_ID\",\"observacoes\":\"Correia dentada vencida + amortecedores danificados.\"}")
OS3_ID=$(id "$OS3"); OS3_NR=$(nr "$OS3")
post /api/admin/ordens/$OS3_ID/servicos "{\"servicoId\":\"$S4_ID\",\"quantidade\":1}" > /dev/null
post /api/admin/ordens/$OS3_ID/servicos "{\"servicoId\":\"$S6_ID\",\"quantidade\":1}" > /dev/null
post /api/admin/ordens/$OS3_ID/pecas   "{\"pecaId\":\"$P3_ID\",\"quantidade\":1}" > /dev/null
post /api/admin/ordens/$OS3_ID/pecas   "{\"pecaId\":\"$P10_ID\",\"quantidade\":1}" > /dev/null
post /api/admin/ordens/$OS3_ID/pecas   "{\"pecaId\":\"$P4_ID\",\"quantidade\":2}" > /dev/null
patch /api/admin/ordens/$OS3_ID/iniciar-diagnostico
patch /api/admin/ordens/$OS3_ID/enviar-orcamento
curl -s -X POST -H "$H" "$BASE/api/public/ordens/$OS3_NR/aprovar" > /dev/null
patch /api/admin/ordens/$OS3_ID/iniciar-execucao "{\"atendenteId\":\"$AT3_ID\"}"
patch /api/admin/ordens/$OS3_ID/finalizar
patch /api/admin/ordens/$OS3_ID/entregar
echo "  OS $OS3_NR — ENTREGUE (Veloz / Ducato)"

# ── OS 4: ENTREGUE (Rafael / Onix) ──────────────────────────────────────────
OS4=$(post /api/admin/ordens "{\"clienteId\":\"$C4_ID\",\"veiculoId\":\"$V5_ID\",\"atendenteId\":\"$AT1_ID\",\"observacoes\":\"Ar-condicionado não estava gelando.\"}")
OS4_ID=$(id "$OS4"); OS4_NR=$(nr "$OS4")
post /api/admin/ordens/$OS4_ID/servicos "{\"servicoId\":\"$S7_ID\",\"quantidade\":1}" > /dev/null
post /api/admin/ordens/$OS4_ID/pecas   "{\"pecaId\":\"$P5_ID\",\"quantidade\":1}" > /dev/null
patch /api/admin/ordens/$OS4_ID/iniciar-diagnostico
patch /api/admin/ordens/$OS4_ID/enviar-orcamento
curl -s -X POST -H "$H" "$BASE/api/public/ordens/$OS4_NR/aprovar" > /dev/null
patch /api/admin/ordens/$OS4_ID/iniciar-execucao "{\"atendenteId\":\"$AT1_ID\"}"
patch /api/admin/ordens/$OS4_ID/finalizar
patch /api/admin/ordens/$OS4_ID/entregar
echo "  OS $OS4_NR — ENTREGUE (Rafael / Onix)"

# ── OS 5: EM EXECUÇÃO (Rodrigues / Transit) ─────────────────────────────────
OS5=$(post /api/admin/ordens "{\"clienteId\":\"$C3_ID\",\"veiculoId\":\"$V4_ID\",\"atendenteId\":\"$AT2_ID\",\"observacoes\":\"Freios rangendo muito. Disco e pastilha a verificar.\"}")
OS5_ID=$(id "$OS5"); OS5_NR=$(nr "$OS5")
post /api/admin/ordens/$OS5_ID/servicos "{\"servicoId\":\"$S3_ID\",\"quantidade\":1}" > /dev/null
post /api/admin/ordens/$OS5_ID/servicos "{\"servicoId\":\"$S5_ID\",\"quantidade\":1}" > /dev/null
post /api/admin/ordens/$OS5_ID/pecas   "{\"pecaId\":\"$P2_ID\",\"quantidade\":2}" > /dev/null
post /api/admin/ordens/$OS5_ID/pecas   "{\"pecaId\":\"$P7_ID\",\"quantidade\":2}" > /dev/null
post /api/admin/ordens/$OS5_ID/pecas   "{\"pecaId\":\"$P9_ID\",\"quantidade\":1}" > /dev/null
patch /api/admin/ordens/$OS5_ID/iniciar-diagnostico
patch /api/admin/ordens/$OS5_ID/enviar-orcamento
curl -s -X POST -H "$H" "$BASE/api/public/ordens/$OS5_NR/aprovar" > /dev/null
patch /api/admin/ordens/$OS5_ID/iniciar-execucao "{\"atendenteId\":\"$AT2_ID\"}"
echo "  OS $OS5_NR — EM EXECUÇÃO (Rodrigues / Transit)"

# ── OS 6: EM EXECUÇÃO (Luciana / Kwid) ──────────────────────────────────────
OS6=$(post /api/admin/ordens "{\"clienteId\":\"$C6_ID\",\"veiculoId\":\"$V8_ID\",\"atendenteId\":\"$AT3_ID\",\"observacoes\":\"Funilaria lateral e retoque de pintura lado motorista.\"}")
OS6_ID=$(id "$OS6"); OS6_NR=$(nr "$OS6")
post /api/admin/ordens/$OS6_ID/servicos "{\"servicoId\":\"$S8_ID\",\"quantidade\":1,\"observacao\":\"Lateral esquerda\"}" > /dev/null
patch /api/admin/ordens/$OS6_ID/iniciar-diagnostico
patch /api/admin/ordens/$OS6_ID/enviar-orcamento
curl -s -X POST -H "$H" "$BASE/api/public/ordens/$OS6_NR/aprovar" > /dev/null
patch /api/admin/ordens/$OS6_ID/iniciar-execucao "{\"atendenteId\":\"$AT3_ID\"}"
echo "  OS $OS6_NR — EM EXECUÇÃO (Luciana / Kwid)"

# ── OS 7: AGUARDANDO APROVAÇÃO (Veloz / Toro) ───────────────────────────────
OS7=$(post /api/admin/ordens "{\"clienteId\":\"$C5_ID\",\"veiculoId\":\"$V7_ID\",\"atendenteId\":\"$AT1_ID\",\"observacoes\":\"Revisão preventiva 50.000 km.\"}")
OS7_ID=$(id "$OS7"); OS7_NR=$(nr "$OS7")
post /api/admin/ordens/$OS7_ID/servicos "{\"servicoId\":\"$S1_ID\",\"quantidade\":1}" > /dev/null
post /api/admin/ordens/$OS7_ID/servicos "{\"servicoId\":\"$S2_ID\",\"quantidade\":1}" > /dev/null
post /api/admin/ordens/$OS7_ID/pecas   "{\"pecaId\":\"$P1_ID\",\"quantidade\":1}" > /dev/null
post /api/admin/ordens/$OS7_ID/pecas   "{\"pecaId\":\"$P6_ID\",\"quantidade\":5}" > /dev/null
patch /api/admin/ordens/$OS7_ID/iniciar-diagnostico
patch /api/admin/ordens/$OS7_ID/enviar-orcamento
echo "  OS $OS7_NR — AGUARDANDO APROVAÇÃO (Veloz / Toro)"

# ── OS 8: AGUARDANDO APROVAÇÃO (Maria / Gol) ────────────────────────────────
OS8=$(post /api/admin/ordens "{\"clienteId\":\"$C2_ID\",\"veiculoId\":\"$V3_ID\",\"atendenteId\":\"$AT2_ID\",\"observacoes\":\"Troca de correia dentada preventiva.\"}")
OS8_ID=$(id "$OS8"); OS8_NR=$(nr "$OS8")
post /api/admin/ordens/$OS8_ID/servicos "{\"servicoId\":\"$S4_ID\",\"quantidade\":1}" > /dev/null
post /api/admin/ordens/$OS8_ID/pecas   "{\"pecaId\":\"$P3_ID\",\"quantidade\":1}" > /dev/null
post /api/admin/ordens/$OS8_ID/pecas   "{\"pecaId\":\"$P10_ID\",\"quantidade\":1}" > /dev/null
patch /api/admin/ordens/$OS8_ID/iniciar-diagnostico
patch /api/admin/ordens/$OS8_ID/enviar-orcamento
echo "  OS $OS8_NR — AGUARDANDO APROVAÇÃO (Maria / Gol)"

# ── OS 9: EM DIAGNÓSTICO (João / Corolla) ───────────────────────────────────
OS9=$(post /api/admin/ordens "{\"clienteId\":\"$C1_ID\",\"veiculoId\":\"$V1_ID\",\"atendenteId\":\"$AT3_ID\",\"observacoes\":\"Check engine aceso. Solicitado diagnóstico eletrônico.\"}")
OS9_ID=$(id "$OS9"); OS9_NR=$(nr "$OS9")
post /api/admin/ordens/$OS9_ID/servicos "{\"servicoId\":\"$S5_ID\",\"quantidade\":1}" > /dev/null
patch /api/admin/ordens/$OS9_ID/iniciar-diagnostico
echo "  OS $OS9_NR — EM DIAGNÓSTICO (João / Corolla)"

# ── OS 10: CANCELADA (Rafael / Onix) ────────────────────────────────────────
OS10=$(post /api/admin/ordens "{\"clienteId\":\"$C4_ID\",\"veiculoId\":\"$V5_ID\",\"atendenteId\":\"$AT1_ID\",\"observacoes\":\"Verificar ruído na suspensão dianteira.\"}")
OS10_ID=$(id "$OS10"); OS10_NR=$(nr "$OS10")
post /api/admin/ordens/$OS10_ID/servicos "{\"servicoId\":\"$S6_ID\",\"quantidade\":1}" > /dev/null
patch /api/admin/ordens/$OS10_ID/iniciar-diagnostico
post /api/admin/ordens/$OS10_ID/cancelar > /dev/null
echo "  OS $OS10_NR — CANCELADA (Rafael / Onix)"

# =============================================================================
echo ""
echo "✅ Seed concluído!"
echo ""
echo "  Atendentes : 3"
echo "  Clientes   : 6"
echo "  Veículos   : 8"
echo "  Serviços   : 8"
echo "  Peças      : 10"
echo "  OS         : 10 (4 entregues, 2 em execução, 2 aguard. aprovação, 1 em diagnóstico, 1 cancelada)"
