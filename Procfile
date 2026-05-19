redis:     redis-server --save "" --daemonize no
kafka:     ./scripts/start-kafka.sh
gateway:   ./gradlew :shield-gateway:bootRun
backend:   ./gradlew :demo-backend:bootRun
analyzer:  cd ai-analyzer && .venv/bin/uvicorn main:app --reload --port 8090