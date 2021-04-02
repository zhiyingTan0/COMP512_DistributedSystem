all: java.policy compile-middleware-rmi

java.policy: FORCE
	@echo "Creating middleware java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

../Server/RMIInterface.jar: ../Server/Server/Interface/IResourceManager.java
	make -C ../Server/ RMIInterface.jar

../Server/RMIAll.jar: ../Server/Server/Interface/IResourceManager.java ../Server/Server/Common/*.java ../Server/Server/LockManager/*.java ../Server/Server/Transaction/*.java ../Server/Server/Exceptions/*.java
	make -C ../Server/ RMIAll.jar

compile-middleware-rmi: ../Server/RMIAll.jar
	javac -cp ../Server/RMIAll.jar ./*.java

clean:
	rm -f ./*.class
	rm -f ../Server/RMIInterface.jar
	rm -f java.policy