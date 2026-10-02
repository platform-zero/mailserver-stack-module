#!/bin/bash
set -euo pipefail
# Keep the key and signing configuration stable across container restarts.
domain="${DOMAIN:?DOMAIN must be set}"
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

# The volume root is created as root:root with mode 0750. Allow the Rspamd
# worker to traverse it while keeping the private key accessible only to it.
chgrp _rspamd "$base"
chmod 0750 "$base"
chown -R _rspamd:_rspamd "${base}/dkim"
chmod 0700 "${base}/dkim"
chmod 0600 "$private_key"
runuser -u _rspamd -- test -r "$private_key"
if supervisorctl status rspamd | grep -q RUNNING; then
    echo "Restarting Rspamd..."
    supervisorctl restart rspamd
else
    echo "Rspamd is not running yet; DKIM configuration will load on service start."
fi
echo "Rspamd DKIM setup complete!"
