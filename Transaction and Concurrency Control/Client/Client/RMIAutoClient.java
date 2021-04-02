package Client;


import Server.Interface.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;

import java.util.*;

public class RMIAutoClient extends Client implements Runnable
{
    private static String s_serverHost = "localhost";
    private static int s_serverPort = 60008;
    private static String s_serverName = "Server";
    private static String s_rmiPrefix = "group_8_";
    private int id = -1;
    private int throughput = -1;
    private static long wholeTime = 10000;

    // how many transactions are done within a second
    private static final int THROUGHPUT = 2;

    private ArrayList<Long> avgTime = new ArrayList<>();

    public RMIAutoClient()
    {
        super();
    }

    public RMIAutoClient(int id, int throughput)
    {
        super();
        this.id = id;
        this.throughput = throughput;
    }

    public static void main(String args[])
    {
        int numOfClients = 0;
        if (args.length > 0)
        {
            s_serverHost = args[0];
        }
        if (args.length > 1)
        {
            s_serverName = args[1];
        }
        if (args.length > 2)
        {
            numOfClients= Integer.valueOf(args[2]);
        }
        if (args.length > 4)
        {
            System.err.println((char)27 + "[31;1mClient exception: " + (char)27 +
                    "[0mUsage: java client.RMIClient [server_hostname [server_rmiobject]]");
            System.exit(1);
        }

        // Set the security policy
        if (System.getSecurityManager() == null)
        {
            System.setSecurityManager(new SecurityManager());
        }

        // start the automatic performance evaluation
        System.out.println("This is the part for performance evaluation");
        Thread[] clientThreads = new Thread[numOfClients];
        try {
           // create a bunch of clients and start the whole process
            for (int i = 0; i < numOfClients; i++){
                System.out.println("Created a client");
                clientThreads[i] = new Thread(new RMIAutoClient(i, THROUGHPUT));
            }

            for (int i = 0; i < numOfClients; i++){
                clientThreads[i].start();
            }

            for (int i = 0; i < numOfClients; i++){
                clientThreads[i].join();
            }


        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public ArrayList<Long> askSingleManager() throws Exception {
        ArrayList<Long> times = new ArrayList<>();
        // first check point
        times.add(getCurrentTime());
        // we are going to ask for the car manager
        // randomise the parameters
        String[] places = {"Montreal", "Tokyo", "Chengdu"};
        int index = (int)(Math.random() * places.length);
        int price = (int)(Math.random() * 10 + 1);
        int amount = (int)(Math.random() * 10 + 1);
        int customerID = (int)(Math.random() * 100 + 1);

        int xid = m_resourceManager.start();
        m_resourceManager.reserveRoom(xid, customerID, places[index]);
        m_resourceManager.newCustomer(xid, customerID);
        m_resourceManager.queryRooms(xid, places[index]);
        m_resourceManager.queryRoomsPrice(xid, places[index]);
        m_resourceManager.commit(xid);
        System.out.println("Finished a transaction");
        return times;
    }

    public ArrayList<Long> askMultipleManagers() throws Exception {
        ArrayList<Long> times = new ArrayList<>();
        // first check point
        times.add(getCurrentTime());
        // we are going to ask for the many managers
        // randomise the parameters
        String[] places = {"Montreal", "Tokyo", "Chengdu"};
        int index = (int)(Math.random() * places.length);
        int price = (int)(Math.random() * 10 + 1);
        int amount = (int)(Math.random() * 5 + 1);
        int customerID = (int)(Math.random() * 100 + 1);
        int flightNumber = (int)(Math.random() * 20 + 1);

        int xid = m_resourceManager.start();
        m_resourceManager.newCustomer(xid, customerID);
        m_resourceManager.addRooms(xid, places[index], price, amount);
        m_resourceManager.queryCarsPrice(xid, places[index]);
        m_resourceManager.reserveFlight(xid, customerID, flightNumber);
        m_resourceManager.commit(xid);
        System.out.println("Finished a transaction");
        return times;
    }

    public long getCurrentTime(){
        return System.currentTimeMillis();
    }

    public void connectServer()
    {
        connectServer(s_serverHost, s_serverPort, s_serverName);
    }

    public void connectServer(String server, int port, String name)
    {
        try {
            boolean first = true;
            while (true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(server, port);
                    m_resourceManager = (IResourceManager)registry.lookup(s_rmiPrefix + name);
                    //System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
                    break;
                }
                catch (NotBoundException|RemoteException e) {
                    if (first) {
                        //System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix + name + "]");
                        first = false;
                    }
                }
                Thread.sleep(500);
            }
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void run() {
        long startTime = getCurrentTime();
        connectServer();
        System.out.println("Start a client!");
        while(getCurrentTime() - startTime < wholeTime){
            try{
                askMultipleManagers();
                Thread.sleep( 1000 / throughput + ((int)Math.random() * 20 - 10));
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        System.out.println("The client " + id + " has finished.");
    }
}

