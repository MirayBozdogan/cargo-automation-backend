#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_dir"
for program in psql java; do
    command -v "$program" >/dev/null || { echo "Gerekli program bulunamadı: $program" >&2; exit 1; }
done

# Yalnız key=value satırlarını okur; dosyayı shell kodu olarak çalıştırmaz.
if [[ -f .env.local.properties ]]; then
    while IFS= read -r line || [[ -n "$line" ]]; do
        line="${line%$'\r'}"
        [[ "$line" == *=* && "$line" != \#* ]] || continue
        key="${line%%=*}"; value="${line#*=}"
        case "$key" in
            DB_URL) DB_URL="${DB_URL:-$value}" ;;
            DB_USERNAME) DB_USERNAME="${DB_USERNAME:-$value}" ;;
            DB_PASSWORD) DB_PASSWORD="${DB_PASSWORD:-$value}" ;;
        esac
    done < .env.local.properties
fi
: "${DB_URL:?DB_URL tanımlanmalı.}"
: "${DB_USERNAME:?DB_USERNAME tanımlanmalı.}"
: "${DB_PASSWORD:?DB_PASSWORD tanımlanmalı.}"

url_pattern='^jdbc:postgresql://([^/:?]+):([0-9]+)/([A-Za-z0-9_]+)$'
if [[ ! "$DB_URL" =~ $url_pattern ]]; then
    echo "DB_URL biçimi jdbc:postgresql://host:port/veritabani olmalıdır; ek URL seçenekleri desteklenmez." >&2
    exit 1
fi
export PGHOST="${BASH_REMATCH[1]}" PGPORT="${BASH_REMATCH[2]}" PGDATABASE="${BASH_REMATCH[3]}"
if [[ "$PGDATABASE" != *_dev && "$PGDATABASE" != *_test ]]; then
    echo "Testler yalnız adı _dev veya _test ile biten veritabanlarında çalıştırılır." >&2
    exit 1
fi
export PGUSER="$DB_USERNAME" PGPASSWORD="$DB_PASSWORD"
schema="cargo_tests_$(date +%s)_$$_${RANDOM}"
created=false
cleanup() {
    if [[ "$created" == true ]]; then
        psql -X --no-password -v ON_ERROR_STOP=1 -q -c "DROP SCHEMA IF EXISTS $schema CASCADE" >/dev/null 2>&1 || {
            echo "Geçici şema temizlenemedi: $schema" >&2; return 1;
        }
        echo "Geçici test şeması temizlendi."
    fi
}
trap cleanup EXIT
psql -X --no-password -v ON_ERROR_STOP=1 -q -c "CREATE SCHEMA $schema"
created=true

export SPRING_DATASOURCE_URL="$DB_URL?currentSchema=$schema"
export SPRING_DATASOURCE_USERNAME="$DB_USERNAME" SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD"
export SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA="$schema"
export SPRING_PROFILES_ACTIVE=test SPRING_SQL_INIT_MODE=never DEV_ADMIN_ENABLED=false
export SPRING_JPA_HIBERNATE_DDL_AUTO=update
# Gerçek oturum anahtarını test ortamına taşımaya gerek yoktur.
export JWT_SECRET="${JWT_SECRET:-Y2FyZ28tdGVzdHMtb25seS1rZXktMzItYnl0ZXMtbG9uZw==}"
echo "Testler ayrı geçici şemada çalışıyor."
./mvnw clean verify
