#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-http://localhost:8080/api/v1}"
DEPARTMENT_COUNT="${DEPARTMENT_COUNT:-3}"
POSITION_COUNT="${POSITION_COUNT:-6}"
EMPLOYEE_COUNT="${EMPLOYEE_COUNT:-40}"
RUN_ID="${SEED_RUN_ID:-$(date +%Y%m%d%H%M%S)-$RANDOM}"

for count_name in DEPARTMENT_COUNT POSITION_COUNT EMPLOYEE_COUNT; do
    count="${!count_name}"
    if [[ ! "$count" =~ ^[0-9]+$ ]]; then
        printf 'Erro: %s deve ser um inteiro maior ou igual a zero.\n' "$count_name" >&2
        exit 1
    fi
done

if (( EMPLOYEE_COUNT > 0 && (DEPARTMENT_COUNT == 0 || POSITION_COUNT == 0) )); then
    printf 'Erro: colaboradores precisam de pelo menos um departamento e um cargo.\n' >&2
    exit 1
fi

if ! command -v curl >/dev/null 2>&1 || ! command -v jq >/dev/null 2>&1; then
    printf 'Erro: instale curl e jq para executar este script.\n' >&2
    exit 1
fi

post_json() {
    local endpoint="$1"
    local payload="$2"
    local response
    local status
    local body

    if ! response="$(curl --silent --show-error \
        --request POST \
        --header 'Content-Type: application/json' \
        --data "$payload" \
        --write-out $'\n%{http_code}' \
        "${API_BASE_URL}${endpoint}")"; then
        printf 'Erro de comunicação ao chamar %s%s.\n' "$API_BASE_URL" "$endpoint" >&2
        return 1
    fi

    status="${response##*$'\n'}"
    body="${response%$'\n'*}"
    if [[ "$status" != "201" ]]; then
        printf 'Erro ao chamar %s%s: HTTP %s\n%s\n' \
            "$API_BASE_URL" "$endpoint" "$status" "$body" >&2
        return 1
    fi

    printf '%s' "$body"
}

random_item() {
    local values=("$@")
    printf '%s' "${values[$((RANDOM % ${#values[@]}))]}"
}

department_ids=()
position_ids=()

printf 'Populando %s departamentos, %s cargos e %s colaboradores em %s\n' \
    "$DEPARTMENT_COUNT" "$POSITION_COUNT" "$EMPLOYEE_COUNT" "$API_BASE_URL"

index=1
while (( index <= DEPARTMENT_COUNT )); do
    printf -v sequence '%03d' "$index"
    payload="$(jq -cn \
        --arg name "Teste Departamento ${RUN_ID}-${sequence}" \
        --arg costCenterCode "TST-${RUN_ID}-${sequence}" \
        '{name: $name, costCenterCode: $costCenterCode}')"
    response="$(post_json '/departments' "$payload")"
    department_id="$(jq -er '.id | strings' <<< "$response")"
    department_ids+=("$department_id")
    printf 'Departamento %s/%s criado: %s\n' "$index" "$DEPARTMENT_COUNT" "$department_id"
    index=$((index + 1))
done

position_titles=(
    "Analista de Operacoes"
    "Engenheiro de Software"
    "Especialista de Dados"
    "Gerente de Projetos"
    "Analista de Recursos Humanos"
    "Coordenador Financeiro"
    "Arquiteto de Solucoes"
    "Product Manager"
)
position_levels=("Junior" "Pleno" "Senior" "Especialista" "Coordenador" "Gerente")

index=1
while (( index <= POSITION_COUNT )); do
    printf -v sequence '%03d' "$index"
    title="$(random_item "${position_titles[@]}")"
    level="$(random_item "${position_levels[@]}")"
    payload="$(jq -cn \
        --arg title "${title} ${RUN_ID}-${sequence}" \
        --arg level "$level" \
        '{title: $title, level: $level}')"
    response="$(post_json '/positions' "$payload")"
    position_id="$(jq -er '.id | strings' <<< "$response")"
    position_ids+=("$position_id")
    printf 'Cargo %s/%s criado: %s\n' "$index" "$POSITION_COUNT" "$position_id"
    index=$((index + 1))
done

first_names=("Alex" "Beatriz" "Caio" "Daniela" "Eduardo" "Fernanda" "Gabriel" "Helena" "Igor" "Juliana")
last_names=("Almeida" "Barbosa" "Costa" "Dias" "Esteves" "Ferreira" "Gomes" "Lima" "Mendes" "Oliveira")

index=1
while (( index <= EMPLOYEE_COUNT )); do
    printf -v sequence '%05d' "$index"
    first_name="$(random_item "${first_names[@]}")"
    last_name="$(random_item "${last_names[@]}")"
    department_id="$(random_item "${department_ids[@]}")"
    position_id="$(random_item "${position_ids[@]}")"
    payload="$(jq -cn \
        --arg name "${first_name} ${last_name} ${RUN_ID}-${sequence}" \
        --arg email "seed-${RUN_ID}-${sequence}@example.com" \
        --arg departmentId "$department_id" \
        --arg positionId "$position_id" \
        '{name: $name, email: $email, departmentId: $departmentId, positionId: $positionId}')"
    response="$(post_json '/employees' "$payload")"
    employee_id="$(jq -er '.id | strings' <<< "$response")"
    printf 'Colaborador %s/%s criado: %s\n' "$index" "$EMPLOYEE_COUNT" "$employee_id"
    index=$((index + 1))
done

printf 'População concluída com sucesso.\n'
