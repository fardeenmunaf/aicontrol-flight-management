#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

start_h2() {
  nohup bash "$SCRIPT_DIR/start-h2-server.sh" > "$SCRIPT_DIR/h2-server.log" 2>&1 &
}

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

echo "Starting H2..."
start_h2
sleep 2

show_menu


