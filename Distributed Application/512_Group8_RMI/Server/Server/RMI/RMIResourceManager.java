// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.RMI;

import Server.Interface.*;
import Server.Common.*;

import java.rmi.NotBoundException;
import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RMIResourceManager extends ResourceManager 
{
	private static String s_serverName = "Server";
	//TODO: ADD YOUR GROUP NUMBER TO COMPLETE
	private static String s_rmiPrefix = "group_8_";
	private static int PORT_NUMBER = 60108;

	public static void main(String args[])
	{
		if (args.length > 0)
		{
			s_serverName = args[0];
		}
			
		// Create the RMI server entry
		try {

//			// origin------------------------------------------------------------
//			// Create a new Server object
//			RMIResourceManager server = new RMIResourceManager(s_serverName);
//			// origin------------------------------------------------------------


			IResourceManager server = null;
			// new way to create the resource manager specifically by name
			if ("Rooms".equals(s_serverName)){
				PORT_NUMBER = 60108;
				server = new RoomResourceManager(s_serverName);
			} else if ("Cars".equals(s_serverName)){
				PORT_NUMBER = 60308;
				server = new CarResourceManager(s_serverName);
			} else if ("Flights".equals(s_serverName)){
				PORT_NUMBER = 60208;
				server = new FlightResourceManager(s_serverName);
			}

			assert server != null;

			System.out.println("This is " + s_serverName + " running now!");

			// Dynamically generate the stub (client proxy)
			IResourceManager resourceManager = (IResourceManager)UnicastRemoteObject.exportObject(server, 0);

			// Bind the remote object's stub in the registry
			Registry l_registry;
			try {
				l_registry = LocateRegistry.createRegistry(PORT_NUMBER);
			} catch (RemoteException e) {
				l_registry = LocateRegistry.getRegistry(PORT_NUMBER);
			}
			final Registry registry = l_registry;
			registry.rebind(s_rmiPrefix + s_serverName, resourceManager);

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						registry.unbind(s_rmiPrefix + s_serverName);
						System.out.println("'" + s_serverName + "' resource manager unbound");
					}
					catch(Exception e) {
						System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
						e.printStackTrace();
					}
				}
			});                                       
			System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_serverName + "'");
		}
		catch (Exception e) {
			System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}

		// Create and install a security manager
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}
	}

	public RMIResourceManager(String name)
	{
		super(name);
	}
}
