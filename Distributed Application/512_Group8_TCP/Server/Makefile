all: java.policy compile-server-tcp

java.policy: FORCE
	@echo "Creating server java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;


RMIAll.jar: Server/Interface/IResourceManager.java Server/Common/*.java Middleware/*.java
	@echo "Compiling TCP server interface and commons"
	javac Server/Interface/IResourceManager.java Server/Common/*.java Middleware/*.java
	jar cvf RMIAll.jar Server/Interface/IResourceManager.class Server/Common/*.class Middleware/*.class

compile-server-tcp: Server/TCP/*.java Server/Interface/IResourceManager.java Server/Common/*.java Middleware/*.java
	javac Server/TCP/*.java Server/Common/*.java Middleware/*.java Server/Interface/IResourceManager.java

clean:
	rm -f Server/Common/*.class Server/TCP/*.class Middleware/*.class Server/Interface/IResourceManager.class
	rm -f java.policy
