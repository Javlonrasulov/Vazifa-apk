#!/bin/bash
set -e
for db in vazifa_prod vazifa_dev; do
  sudo -u postgres psql -d "$db" -f /tmp/add-rest-time.sql
  if ! sudo -u postgres psql -d "$db" -tAc "SELECT 1 FROM pg_type WHERE typname='announcement_status'" | grep -q 1; then
    sudo -u postgres psql -d "$db" -c "CREATE TYPE announcement_status AS ENUM ('active','cancelled','expired')"
  fi
  sudo -u postgres psql -d "$db" -f /tmp/add-announcements.sql
  sudo -u postgres psql -d "$db" -f /tmp/add-push-outbox.sql
  if [ "$db" = "vazifa_prod" ]; then
    app_user="vazifa_app_prod"
  else
    app_user="vazifa_app_dev"
  fi
  sudo -u postgres psql -d "$db" -c "GRANT ALL ON TABLE announcements, announcement_recipients, announcement_attachments, push_outbox TO $app_user; GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO $app_user;"
done
