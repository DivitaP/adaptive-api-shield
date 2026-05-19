for port in 8080 8081 8090; do
  pid=$(lsof -ti :$port)
  if [ -n "$pid" ]; then
    echo "Killing process on port $port (PID $pid)"
    kill -9 $pid
  fi
done