#!/bin/bash
set -euo pipefail

# Keep the key and signing configuration stable across container restarts.
domain="${MAIL_DOMAIN:-${DOMAIN:?DOMAIN must be set}}"
base="/tmp/docker-mailserver/rspamd"
private_key="${base}/dkim/rsa-2048-mail-${domain}.private.txt"
signing_config="${base}/override.d/dkim_signing.conf"
if [[ -f "$private_key" && -f "$signing_config" ]]; then
    echo "Reusing persisted Rspamd DKIM key for ${domain}."
elif [[ -e "$private_key" || -e "$signing_config" ]]; then
    echo "Incomplete persisted DKIM configuration for ${domain}; refusing to rotate the key." >&2
    exit 1
else
    echo "Generating DKIM keys for Rspamd..."
    setup config dkim
fi

if supervisorctl status rspamd | grep -q RUNNING; then
    echo "Restarting Rspamd..."
    supervisorctl restart rspamd
else
    echo "Rspamd is not running yet; DKIM configuration will load on service start."
fi
echo "Rspamd DKIM setup complete!"
