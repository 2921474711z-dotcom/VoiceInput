#!/bin/sh
set -eu

mkdir -p /app/lib

if [ -f /app/runtime-classpath.txt ] && [ "$(find /app/lib -type l | wc -l)" -eq 0 ]; then
  tr ';' '\n' < /app/runtime-classpath.txt | while IFS= read -r file; do
    file="$(printf '%s' "$file" | tr -d '\r')"
    [ -n "$file" ] || continue

    case "$file" in
      [A-Za-z]:\\maven-repository\\*)
        suffix="$(printf '%s' "$file" | sed 's#^[A-Za-z]:\\\\maven-repository\\\\##' | tr '\\' '/')"
        file="/m2/$suffix"
        ;;
      [A-Za-z]:/maven-repository/*)
        suffix="${file#?:/maven-repository/}"
        file="/m2/$suffix"
        ;;
    esac

    if [ -f "$file" ]; then
      hash="$(echo "$file" | sha1sum | cut -c1-12)"
      ln -sf "$file" "/app/lib/${hash}.jar"
    fi
  done
fi

exec java -cp "/app/classes:/app/lib/*" com.voiceinput.pro.VoiceInputProApplication
