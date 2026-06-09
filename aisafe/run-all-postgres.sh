#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

# Load .env if present
if [ -f "$ENV_FILE" ]; then
  set -a
  . "$ENV_FILE"
  set +a
fi

# Validate required settings
missing=()
for var in AISAFE_PERSISTENCE_UNIT AISAFE_DB_DIALECT AISAFE_DB_DRIVER AISAFE_DB_URL AISAFE_DB_USERNAME AISAFE_DB_PASSWORD; do
  if [ -z "${!var}" ]; then
    missing+=("$var")
  fi
done
if [ "${#missing[@]}" -ne 0 ]; then
  echo "Missing required environment variables: ${missing[*]}"
  echo "Create a .env file in $SCRIPT_DIR or export the variables before running."
  exit 1
fi

# Optional dry-run to verify configuration loading
if [ "${AISAFE_DRY_RUN}" = "1" ]; then
  echo "AISAFE_PERSISTENCE_UNIT=${AISAFE_PERSISTENCE_UNIT}"
  echo "AISAFE_DB_DIALECT=${AISAFE_DB_DIALECT}"
  echo "AISAFE_DB_DRIVER=${AISAFE_DB_DRIVER}"
  echo "AISAFE_DB_URL=${AISAFE_DB_URL}"
  echo "AISAFE_DB_USERNAME=${AISAFE_DB_USERNAME}"
  echo "AISAFE_DB_PASSWORD=***"
  exit 0
fi

show_menu() {
  while true; do
    clear
    echo "================================"
    echo "       AISAFE RUN MENU"
    echo "================================"
    echo ""
    echo "1 - Backoffice Console"
    echo "2 - Weather Client"
    echo "3 - Air Transport Console"
    echo "0 - Exit"
    echo ""
    read -r -p "Select an option: " choice

    case "$choice" in
      1)
        echo "Running Backoffice..."
        bash "$SCRIPT_DIR/run-backoffice.sh"
        read -r -p "Press Enter to continue..." _
        ;;
      2)
        echo "Running Weather Client..."
        bash "$SCRIPT_DIR/run-weatherclient.sh"
        read -r -p "Press Enter to continue..." _
        ;;
      3)
        echo "Running Air Transport..."
        bash "$SCRIPT_DIR/run-airtransport.sh"
        read -r -p "Press Enter to continue..." _
        ;;
      0)
        exit 0
        ;;
    esac
  done
}

show_menu