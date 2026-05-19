#!/bin/bash

set -e

echo "── Adaptive API Shield · Setup ──"
echo ""

# Check Homebrew
if ! command -v brew &>/dev/null; then
  echo "✗ Homebrew not found. Install from https://brew.sh"
  exit 1
fi

# Install Overmind if missing
if ! command -v overmind &>/dev/null; then
  echo "→ Installing Overmind..."
  brew install overmind
else
  echo "✓ Overmind $(overmind --version)"
fi

# Check Java 17+
if ! command -v java &>/dev/null; then
  echo "✗ Java not found. Install: brew install openjdk@17"
  exit 1
fi
JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VER" -lt 17 ]; then
  echo "✗ Java 17+ required (found $JAVA_VER). Install: brew install openjdk@17"
  exit 1
fi
echo "✓ Java $JAVA_VER"

# Check Python 3.10+
if ! command -v python3 &>/dev/null; then
  echo "✗ Python 3 not found. Install: brew install python@3.10"
  exit 1
fi
echo "✓ Python $(python3 --version)"

# Check Redis
if ! command -v redis-server &>/dev/null; then
  echo "→ Installing Redis..."
  brew install redis
else
  echo "✓ Redis $(redis-server --version | awk '{print $3}')"
fi

# Check Kafka
KAFKA_PATH="${KAFKA_HOME:-$HOME/Downloads/kafka_2.13-4.2.0}"
if [ ! -d "$KAFKA_PATH" ]; then
  echo "✗ Kafka not found at $KAFKA_PATH"
  echo "  Download from https://kafka.apache.org/downloads and extract to ~/Downloads/"
  echo "  Or set KAFKA_HOME to your Kafka directory."
  exit 1
else
  echo "✓ Kafka found at $KAFKA_PATH"
fi

# Make startup script executable
chmod +x scripts/start-kafka.sh
echo "✓ scripts/start-kafka.sh is executable"

# Set up Python venv if missing
if [ ! -d "ai-analyzer/.venv" ]; then
  echo "→ Creating Python venv for ai-analyzer..."
  python3 -m venv ai-analyzer/.venv
  source ai-analyzer/.venv/bin/activate
  pip install -q -r ai-analyzer/requirements.txt
  echo "✓ Python dependencies installed"
else
  echo "✓ Python venv exists"
fi

# Check .env
if [ ! -f "ai-analyzer/.env" ]; then
  echo ""
  echo "⚠  Missing ai-analyzer/.env"
  echo "   Create it with your Groq API key:"
  echo "   echo 'GROQ_API_KEY=your_key_here' > ai-analyzer/.env"
  echo "   Get a free key at: https://console.groq.com"
fi

echo ""
echo "── Setup complete. Run the stack with: ──"
echo ""
echo "   overmind start"
echo ""