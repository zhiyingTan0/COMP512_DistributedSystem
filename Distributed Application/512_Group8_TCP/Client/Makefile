all: java.policy compile-client

java.policy: FORCE
	@echo "Creating client java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

../Server/RMIAll.jar: ../Server/Server/Interface/IResourceManager.java ../Server/Server/Common/*.java ../Server/Middleware/*.java
	make -C ../Server/ RMIAll.jar

compile-client: ../Server/RMIAll.jar Client/*.java
	javac -cp ../Server/RMIAll.jar Client/*.java

clean:
	rm -f Client/*.class
	rm -f ../Server/RMIInterface.jar
	rm -f java.policy
