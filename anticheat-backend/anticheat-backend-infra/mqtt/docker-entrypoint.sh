#!/bin/sh
set -e

CONF_DIR=/mosquitto/config
PASSWD_FILE=$CONF_DIR/passwd
CONF_FILE=$CONF_DIR/mosquitto.conf

if [ -z "$MQTT_USER" ] || [ -z "$MQTT_PASSWORD" ]; then
  echo "[MQTT] ERROR: MQTT_USER and MQTT_PASSWORD must be set." >&2
  exit 1
fi

# Generate config file (avoids bind-mount permission issues on Windows)
cat > "$CONF_FILE" <<EOF
listener 1883
allow_anonymous false
password_file $PASSWD_FILE
EOF

echo "[MQTT] Generating password file for user: $MQTT_USER"
mosquitto_passwd -b -c "$PASSWD_FILE" "$MQTT_USER" "$MQTT_PASSWORD"
chown mosquitto:mosquitto "$PASSWD_FILE"
chmod 0600 "$PASSWD_FILE"

echo "[MQTT] Starting Mosquitto broker..."
exec mosquitto -c "$CONF_FILE"
