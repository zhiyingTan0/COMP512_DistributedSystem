echo "Edit file run_middleware.sh to include instructions for launching the middleware"
java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ Middleware.TCPMiddleware $1 $2 $3 $4
