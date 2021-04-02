# COMP512-Project_Group_8

Make sure to execute the Makefile at first for all of the three processes.

To run the RMI resource manager:

```
cd Server/
./run_server.sh [<rmi_name>] # starts a single ResourceManager. The parameter should be the resource manager name.
./run_servers.sh # convenience script for starting multiple resource managers. However, some issues come with it. Not recommended.
```

To run the RMI client:

```
cd Client
./run_client.sh [<server_hostname> [<server_rmi_name>]] # e.g. (./run_client.sh localhost Middleware)
```

To run the RMI Middleware:

```
cd Middleware/
./run_middleware_test.sh # all the servers are in localhost
./run_client.sh [<server_hostname> <server_hostname> <server_hostname>] # provide the hostnames of the managers
```

Hope we are gonna have a nice semester!!!

