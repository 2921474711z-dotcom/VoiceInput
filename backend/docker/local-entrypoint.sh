#!/bin/sh
set -eu

mkdir -p /app/lib

if [ -f /app/runtime-classpath.txt ] && [ "$(find /app/lib -type l | wc -l)" -eq 0 ]; then
  while IFS= read -r file; do
    file="$(printf '%s' "$file" | tr -d '\r')"
    [ -n "$file" ] || continue
    if [ -f "$file" ]; then
      hash="$(echo "$file" | sha1sum | cut -c1-12)"
      ln -sf "$file" "/app/lib/${hash}.jar"
    fi
  done < /app/runtime-classpath.txt
fi

exec java -cp "/app/classes:/app/lib/*" com.voiceinput.pro.VoiceInputProApplication
