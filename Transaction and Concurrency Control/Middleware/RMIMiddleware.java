import Server.Common.*;
import Server.Exceptions.InvalidTransactionException;
import Server.Exceptions.TransactionAbortedException;
import Server.Interface.IResourceManager;
import Server.LockManager.DeadlockException;
import Server.LockManager.LockManager;
import Server.LockManager.TransactionLockObject;
import Server.Transaction.InactiveStatus;
import Server.Transaction.Transaction;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class RMIMiddleware implements IResourceManager {

    private static final String MIDDLEWARE_NAME = "Middleware";
    private static String s_rmiPrefix = "group_8_";

    // this is the port number used for the middleware
    private static final int PORT_NUMBER = 60008;

    private static final int ROOM_RS_NUM = 60108;
    private static final int FIGHT_RS_NUM = 60208;
    private static final int CAR_RS_NUM = 60308;

    private static int server_ports[] = {ROOM_RS_NUM, FIGHT_RS_NUM, CAR_RS_NUM};
    private static String server_host_names[] = {"localhost", "localhost", "localhost"};
    private static String server_names[] = {"Rooms", "Flights", "Cars"};
    // this is the hashmap to store the three resource managers
    private static HashMap<String, IResourceManager> serverToManagerMap = new HashMap<>();
    // this is the hashmap to store the status of the resource manager servers
    // true means it is connected, false not
    private static HashMap<String, Boolean> statusMap = new HashMap<>();

    // this is used to indicate how many servers have been found by the middleware
    private int foundServers = 0;

    private static RMIMiddleware INSTANCE = new RMIMiddleware();

    // some attributed might related to handle customers
    // in this case, the middleware itself could be regarded as a customer manager
    protected String m_name = "";
    protected RMHashMap m_data = new RMHashMap();

    private MiddlewareTransactionManager transactionManager;
    private LockManager lockManager;
    private int timeToLive = 25000;

    public static void main(String[] args) {

        // arguments check
        if (args.length == 3) {
            System.out.println("Configuring the middleware...");
            // assign the host names
            // flight at first
            server_host_names[1] = args[0];
            // cars
            server_host_names[2] = args[1];
            // room at last
            server_host_names[0] = args[2];
        }

        System.out.println("The middleware is running!");
        // check the existence of middleware
        // if not, create a new one
        if (INSTANCE == null) {
            INSTANCE = new RMIMiddleware();
        }
        INSTANCE.init();
        INSTANCE.createEntry();

        // try to look for other servers
        INSTANCE.findSpecificServes();

        INSTANCE.initTransactionAndLockManagers();

    }

    public void init() {
        statusMap.put(server_names[0], false);
        statusMap.put(server_names[1], false);
        statusMap.put(server_names[2], false);
        INSTANCE.m_name = "Customer";
    }

    public void initTransactionAndLockManagers() {
        transactionManager = new MiddlewareTransactionManager(timeToLive, this);
        lockManager = new LockManager();
    }

    // This method would setup or create RMI middleware entry accordingly
    // Most of the code would be the same as RMI resource manager
    public void createEntry() {
        // Dynamically generate the stub (client proxy)
        try {
            IResourceManager resourceManager = (IResourceManager) UnicastRemoteObject.exportObject(INSTANCE, 0);

            // Bind the remote object's stub in the registry
            Registry l_registry;
            try {
                l_registry = LocateRegistry.createRegistry(PORT_NUMBER);
            } catch (RemoteException e) {
                l_registry = LocateRegistry.getRegistry(PORT_NUMBER);
            }

            // this step is the same as the template server
            final Registry registry = l_registry;
            registry.rebind(s_rmiPrefix + MIDDLEWARE_NAME, resourceManager);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        registry.unbind(s_rmiPrefix + MIDDLEWARE_NAME);
                        System.out.println("'" + MIDDLEWARE_NAME + "' resource manager unbound");
                    } catch (Exception e) {
                        System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
                        e.printStackTrace();
                    }
                }
            });
            System.out.println("'" + MIDDLEWARE_NAME + "' resource manager server ready and bound to '" + s_rmiPrefix + MIDDLEWARE_NAME + "'");
            System.out.println("The port number of the middleware is " + PORT_NUMBER);
            System.out.println("-------------------------------------------------------");

        } catch (RemoteException e) {
            System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

    }

    // this method would allow the middleware to find the specific resource manager servers
    // namely the room, car, and flight servers
    public void findSpecificServes() {
        System.out.println("Searching for servers...");

        if (!statusMap.get(server_names[0])) {
            // find the room resource manager server at first
            connectServer(server_host_names[0], ROOM_RS_NUM, server_names[0]);
            statusMap.put(server_names[0], true);
            INSTANCE.foundServers++;
        }
        if (!statusMap.get(server_names[1])) {
            // find the flight recourse manager server
            connectServer(server_host_names[1], FIGHT_RS_NUM, server_names[1]);
            statusMap.put(server_names[1], true);
            INSTANCE.foundServers++;
        }
        if (!statusMap.get(server_names[2])) {
            // find the car recourse manager server
            connectServer(server_host_names[2], CAR_RS_NUM, server_names[2]);
            statusMap.put(server_names[2], true);
            INSTANCE.foundServers++;
        }

        System.out.println("All servers have been connected!");
    }

    // this method would connect the specific server by name, port, and host name
    // most of the code of this method is based on the same method name of Client
    public void connectServer(String server_host_name, int port, String server_name) {
        try {
            boolean first = true;
            while (true) {
                try {
                    Registry registry = LocateRegistry.getRegistry(server_host_name, port);
                    IResourceManager manager = (IResourceManager) registry.lookup(s_rmiPrefix + server_name);
                    // put the manager to hashmap
                    serverToManagerMap.put(server_name, manager);
                    System.out.println("Connected to '" + server_name + "' server [" + server_host_name + ":" + port + "/" + s_rmiPrefix + server_name + "]");
                    break;
                } catch (NotBoundException | RemoteException e) {
                    if (first) {
                        System.out.println("Waiting for '" + server_name + "' server [" + server_host_name + ":" + port + "/" + s_rmiPrefix + server_name + "]");
                        first = false;
                    }
                }
                Thread.sleep(500);
            }
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    // actual services
    @Override
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        System.out.println("The request has been forwarded to Flight Resource Manager...");
        prepare(id, Flight.getKey(flightNum), TransactionLockObject.LockType.LOCK_WRITE, serverToManagerMap.get("Flights"));
        return serverToManagerMap.get("Flights").addFlight(id, flightNum, flightSeats, flightPrice);

    }

    @Override
    public boolean addCars(int id, String location, int numCars, int price)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        System.out.println("The request has been forwarded to Car Resource Manager...");
        prepare(id, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE, serverToManagerMap.get("Cars"));
        return serverToManagerMap.get("Cars").addCars(id, location, numCars, price);

    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int price)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        System.out.println("The request has been forwarded to Room Resource Manager...");
        prepare(id, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE, serverToManagerMap.get("Rooms"));
        return serverToManagerMap.get("Rooms").addRooms(id, location, numRooms, price);

    }

    @Override
    public int newCustomer(int xid) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        Trace.info("RM::newCustomer(" + xid + ") called");
        // Generate a globally unique ID for the new customer
        int cid = Integer.parseInt(String.valueOf(xid) +
                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                String.valueOf(Math.round(Math.random() * 100 + 1)));
        Customer customer = new Customer(cid);
        prepare(xid, customer.getKey(), TransactionLockObject.LockType.LOCK_WRITE, this);
        writeData(xid, customer.getKey(), customer);
        Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
        return cid;
    }

    @Override
    public boolean newCustomer(int xid, int customerID)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") called");
        prepare(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE, this);
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            customer = new Customer(customerID);
            writeData(xid, customer.getKey(), customer);
            Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") created a new customer");
            return true;
        } else {
            Trace.info("INFO: RM::newCustomer(" + xid + ", " + customerID + ") failed--customer already exists");
            return false;
        }
    }

    // needs careful handling starting this point...
    @Override
    public boolean deleteFlight(int id, int flightNum)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        System.out.println("The request has been forwarded to Flight Resource Manager...");
        prepare(id, Flight.getKey(flightNum), TransactionLockObject.LockType.LOCK_WRITE, serverToManagerMap.get("Flights"));
        return serverToManagerMap.get("Flights").deleteFlight(id, flightNum);

    }

    @Override
    public boolean deleteCars(int id, String location)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        System.out.println("The request has been forwarded to Car Resource Manager...");
        prepare(id, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE, serverToManagerMap.get("Cars"));
        return serverToManagerMap.get("Cars").deleteCars(id, location);
    }

    @Override
    public boolean deleteRooms(int id, String location)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        System.out.println("The request has been forwarded to Room Resource Manager...");
        prepare(id, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE, serverToManagerMap.get("Cars"));
        return serverToManagerMap.get("Rooms").deleteRooms(id, location);
    }

    @Override
    public boolean deleteCustomer(int xid, int customerID)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
        // read this customer to get some info at first
        prepare(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ, this);
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
            return false;
        } else {
            // Increase the reserved numbers of all reservable items which the customer reserved.
            // get the write lock at first
            // before real delete stuff happens
            getLock(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
            RMHashMap reservations = customer.getReservations();
            for (String reservedKey : reservations.keySet()) {
                ReservedItem reserveditem = customer.getReservedItem(reservedKey);
                Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " " + reserveditem.getCount() + " times");
                // determine the genre of the item
                ReservedItem item = customer.getReservedItem(reservedKey);
                if ((reservedKey.startsWith("car"))){
                    getLock(xid, reservedKey, TransactionLockObject.LockType.LOCK_WRITE);
                    distribute(xid, serverToManagerMap.get("Cars"));
                    serverToManagerMap.get("Cars").revertReservation(xid, customerID, item.getKey(), item.getCount());
                }
                if (reservedKey.startsWith("room")) {
                    getLock(xid, reservedKey, TransactionLockObject.LockType.LOCK_WRITE);
                    distribute(xid, serverToManagerMap.get("Rooms"));
                    serverToManagerMap.get("Rooms").revertReservation(xid, customerID, item.getKey(), item.getCount());
                }
                if (reservedKey.startsWith("flight")) {
                    getLock(xid, reservedKey, TransactionLockObject.LockType.LOCK_WRITE);
                    distribute(xid, serverToManagerMap.get("Flights"));
                    serverToManagerMap.get("Flights").revertReservation(xid, customerID, item.getKey(), item.getCount());
                }
                //writeData(xid, item.getKey(), item);
            }
            // Remove the customer from the storage
            removeData(xid, customer.getKey());
            Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
            return true;
        }
    }

    @Override
    public int queryFlight(int id, int flightNumber) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        System.out.println("The request has been forwarded to Flight Resource Manager...");
        prepare(id, Flight.getKey(flightNumber), TransactionLockObject.LockType.LOCK_READ, serverToManagerMap.get("Flights"));
        return serverToManagerMap.get("Flights").queryFlight(id, flightNumber);
    }

    @Override
    public int queryCars(int id, String location) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        System.out.println("The request has been forwarded to Car Resource Manager...");
        prepare(id, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ, serverToManagerMap.get("Cars"));
        return serverToManagerMap.get("Cars").queryCars(id, location);
    }

    @Override
    public int queryRooms(int id, String location) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        System.out.println("The request has been forwarded to Room Resource Manager...");
        prepare(id, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ, serverToManagerMap.get("Rooms"));
        return serverToManagerMap.get("Rooms").queryRooms(id, location);
    }

    @Override
    public String queryCustomerInfo(int xid, int customerID)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ") called");
        prepare(xid, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ, this);
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn("RM::queryCustomerInfo(" + xid + ", " + customerID + ") failed--customer doesn't exist");
            // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
            return "";
        } else {
            Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ")");
            System.out.println(customer.getBill());
            return customer.getBill();
        }
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        System.out.println("The request has been forwarded to Flight Resource Manager...");
        prepare(id, Flight.getKey(flightNumber), TransactionLockObject.LockType.LOCK_READ, serverToManagerMap.get("Flights"));
        return serverToManagerMap.get("Flights").queryFlightPrice(id, flightNumber);
    }

    @Override
    public int queryCarsPrice(int id, String location)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        System.out.println("The request has been forwarded to Car Resource Manager...");
        prepare(id, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ, serverToManagerMap.get("Cars"));
        return serverToManagerMap.get("Cars").queryCarsPrice(id, location);
    }

    @Override
    public int queryRoomsPrice(int id, String location)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        System.out.println("The request has been forwarded to Room Resource Manager...");
        prepare(id, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ, serverToManagerMap.get("Rooms"));
        return serverToManagerMap.get("Rooms").queryRoomsPrice(id, location);
    }

    @Override
    public ReservableItem reserveFlight(int id, int customerID, int flightNumber)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        System.out.println("The request has been forwarded to Flight Resource Manager...");

        // first check whether the given customer exists or not
        // get the shared lock for customer
        prepare(id, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ, this);
        Customer customer = getCustomer(id, customerID);
        if (customer == null) {
            Trace.warn("RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist");
            // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
            return null;
        }

        // reserve the flight!
        // get the lock at first
        getLock(id, Flight.getKey(flightNumber), TransactionLockObject.LockType.LOCK_WRITE);
        distribute(id, serverToManagerMap.get("Flights"));
        ReservableItem item = serverToManagerMap.get("Flights").reserveFlight(id, customerID, flightNumber);
        // check whether or not reserving is successful
        if (item == null) {
            System.out.println("Reserve failed!");
            return null;
        }
        // we are good!
        // again acquire the locks!
        getLock(id, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
        customer.reserve(Flight.getKey(flightNumber), String.valueOf(flightNumber), item.getPrice());
        this.updateCustomerOrItem(id, customer.getKey(), customer);
        return item;
    }

    @Override
    public ReservableItem reserveCar(int id, int customerID, String location)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        System.out.println("The request has been forwarded to Car Resource Manager...");
        // first check whether the given customer exists or not
        // get the shared lock for customer
        prepare(id, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ, this);
        Customer customer = getCustomer(id, customerID);
        if (customer == null) {
            Trace.warn("RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist");
            // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
            return null;
        }

        // reserve the car!
        // get the lock at first
        getLock(id, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        distribute(id, serverToManagerMap.get("Cars"));
        ReservableItem item = serverToManagerMap.get("Cars").reserveCar(id, customerID, location);
        // check whether or not reserving is successful
        if (item == null) {
            System.out.println("Reserve failed!");
            return null;
        }
        // we are good!
        // again acquire the locks!
        getLock(id, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
        customer.reserve(Car.getKey(location),location, item.getPrice());
        this.updateCustomerOrItem(id, customer.getKey(), customer);
        return item;
    }

    @Override
    public ReservableItem reserveRoom(int id, int customerID, String location)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        System.out.println("The request has been forwarded to Room Resource Manager...");
        // first check whether the given customer exists or not
        // get the shared lock for customer
        prepare(id, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ, this);
        Customer customer = getCustomer(id, customerID);
        if (customer == null) {
            Trace.warn("RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist");
            // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
            return null;
        }

        // reserve the car!
        // get the lock at first
        getLock(id, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
        distribute(id, serverToManagerMap.get("Rooms"));
        ReservableItem item = serverToManagerMap.get("Rooms").reserveRoom(id, customerID, location);
        // check whether or not reserving is successful
        if (item == null) {
            System.out.println("Reserve failed!");
            return null;
        }
        // we are good!
        // again acquire the locks!
        getLock(id, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
        customer.reserve(Room.getKey(location),location, item.getPrice());
        this.updateCustomerOrItem(id, customer.getKey(), customer);
        return item;
    }

    @Override
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        Trace.info("RM::bundle(" + id + ", customer=" + customerID + ", " +
                flightNumbers.toString() + ", " + location + ") called" );
        // no matter what, get the info about the customer at first
        prepare(id, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_READ, this);
        // we are ready to read the customer
        Customer customer = (Customer)readData(id, Customer.getKey(customerID));
        if (customer == null){
            System.out.println("RM:bundle(" + id + ", customer=" + customerID +
                    ", " + flightNumbers.toString() + ", " + location + ")  failed--customer doesn't exist");
        }

        // pile up the flight numbers to handle the case for duplicated flight numbers
        HashMap<String, Integer> numToCount = new HashMap<String, Integer>();

        for (String flightNumber : flightNumbers) {
            if (numToCount.containsKey(flightNumber)){
                numToCount.put(flightNumber, numToCount.get(flightNumber) + 1);
            }
            else{
                numToCount.put(flightNumber, 1);
            }
        }

        // start to check the flights
        for (String flightNum : numToCount.keySet()){
            // convert it to int at first
            int num = Integer.parseInt(flightNum);
            getLock(id, Flight.getKey(num), TransactionLockObject.LockType.LOCK_READ);
            distribute(id, serverToManagerMap.get("Flights"));
            if (serverToManagerMap.get("Flights").getPrice(id, Flight.getKey(num), numToCount.get(flightNum)) < 0){
                System.out.println("bundle failed because the flight  " + flightNum + " may not exist or does not enough storage.");
                return false;
            }
        }
        System.out.println("Flight check success.");
        // finished check flights

        if (car){
            // check the car availability
            getLock(id, Car.getKey(location), TransactionLockObject.LockType.LOCK_READ);
            distribute(id,serverToManagerMap.get("Cars"));
            if (serverToManagerMap.get("Cars").getPrice(id, Car.getKey(location), 1) < 0){
                System.out.println("bundle failed because the car at " + location + " may not exist or does not enough storage.");
                return false;
            }
            System.out.println("Car check success.");
        }

        if (room){
            // check the car availability
            getLock(id, Room.getKey(location), TransactionLockObject.LockType.LOCK_READ);
            distribute(id,serverToManagerMap.get("Rooms"));
            if (serverToManagerMap.get("Rooms").getPrice(id, Room.getKey(location), 1) < 0){
                System.out.println("bundle failed because the room at " + location + " may not exist or does not enough storage.");
                return false;
            }
            System.out.println("Room check success.");
        }

        // now every thing is good
        // ready to reserve everything!!!
        System.out.println("Everything has been checked. Ready to reserve");

        getLock(id, Customer.getKey(customerID), TransactionLockObject.LockType.LOCK_WRITE);
        if (car){
            getLock(id, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
            serverToManagerMap.get("Cars").reserveCar(id, customerID, location);
            customer.reserve(Car.getKey(location), location,
                    serverToManagerMap.get("Cars").getPrice(id, Car.getKey(location), 1));
            System.out.println("Finished reserve car");
        }

        if (room){
            getLock(id, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
            serverToManagerMap.get("Rooms").reserveRoom(id, customerID, location);
            customer.reserve(Room.getKey(location), location,
                    serverToManagerMap.get("Rooms").getPrice(id, Room.getKey(location), 1));
            System.out.println("Finished reserve room");
        }
        //updateCustomerOrItem(id, customer.getKey(), customer);
        // reserve the flights
        for (String flightNum : numToCount.keySet())
        {
            for (int i = 0; i < numToCount.get(flightNum); i++){
                int price = serverToManagerMap
                        .get("Flights").getPrice(id, Flight.getKey(Integer.parseInt(flightNum)), 1);
                getLock(id, Flight.getKey(Integer.parseInt(flightNum)), TransactionLockObject.LockType.LOCK_WRITE);
                serverToManagerMap.get("Flights").reserveFlight(id, customerID, Integer.parseInt(flightNum));
                customer.reserve(Flight.getKey(Integer.parseInt(flightNum)), flightNum, price);
                updateCustomerOrItem(id, customer.getKey(), customer);
            }
        }
        System.out.println("Finished reserve flight");
        System.out.println("Successfully reserved everything requested");
        return true;
    }

    public Customer getCustomer(int xid, int customerID) throws RemoteException, InvalidTransactionException {
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            return null;
        } else {
            return customer;
        }
    }

    @Override
    public String getName() throws RemoteException {
        return "Customer";
    }

    @Override
    public String updateCustomerOrItem(int xid, String key, RMItem item) throws RemoteException, InvalidTransactionException {
        writeData(xid, key, item);
        return null;
    }

    @Override
    public RMItem readDataWrapper(int xid, String key) throws InvalidTransactionException {
        return readData(xid, key);
    }

    @Override
    public String summary(int xid) throws RemoteException {
        synchronized (m_data) {
            String result = "";
            for (String key : m_data.keySet()) {
                if (m_data.get(key) instanceof Customer) {
                    System.out.println("Found a customer!!!");
                    // go to loop the reserved items
                    Customer customer = (Customer) m_data.get(key);
                    //System.out.println(customer.getReservations().values());
                    result = result + "Customer " + customer.getID() + "---- starts ---------------\n";
                    for (RMItem item : customer.getReservations().values()) {
                        result = result + item.toString() + "\n";
                    }
                    result = result + "Customer " + customer.getID() + "---- finishes ---------------\n";
                }
            }
            return result;
        }
    }

    @Override
    public boolean revertReservation(int xid, int customerID, String key, int count) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        return true;
    }

    @Override
    public int start() throws RemoteException {
        int xid = transactionManager.startTransaction();
        Trace.info("Start the transaction with xid " + xid);
        return xid;
    }

    @Override
    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        System.out.println("Start to commit the transaction " + transactionId);
        checkIllegalTransaction(transactionId);

        // tell the related resource managers to commit the transaction
        ArrayList<IResourceManager> managers =
                transactionManager.getActiveTransaction(transactionId).getManagers();
        System.out.println("------------------------------------");
        for (IResourceManager manager : managers) {
            System.out.println(manager.getName());
            // handle the customer separately
            if (!("Customer").equals(manager.getName())) {
                System.out.println("Tell " + manager.getName() + " to commit the transaction " + transactionId);
                manager.commit(transactionId);
            } else {
                RMHashMap data = transactionManager.getActiveTransaction(transactionId).getLocalBuffer();
                synchronized (data) {
                    Set<String> keyset = data.keySet();
                    for (String key : keyset) {
                        m_data.put(key, data.get(key));
                    }
                }
            }
        }

        // some clean up for the database
        transactionManager.updateActiveTransaction(transactionId, null);
        transactionManager.addInactiveTransaction(transactionId, InactiveStatus.COMMITTED);

        // release the locks
        lockManager.UnlockAll(transactionId);
        System.out.println("Committed successfully with transaction ID " + transactionId);
        return true;
    }

    @Override
    public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
        System.out.println("Start to abort the transaction " + transactionId);
        try {
            checkIllegalTransaction(transactionId);
        } catch (TransactionAbortedException e) {
            throw new InvalidTransactionException
                    (transactionId, "The transaction " + transactionId + " has already been aborted");
        }

        // tell the related resource managers to abort the transaction
        for (IResourceManager manager : transactionManager.getActiveTransaction(transactionId).getManagers()) {
            if (!("Customer").equals(manager.getName())) {
                System.out.println("Tell " + manager.getName() + " to abort the transaction " + transactionId);
                manager.abort(transactionId);
            }
        }

        // some clean up for the database
        transactionManager.updateActiveTransaction(transactionId, null);
        transactionManager.addInactiveTransaction(transactionId, InactiveStatus.ABORTED);

        // release the locks
        lockManager.UnlockAll(transactionId);
    }

    @Override
    public boolean shutdown() throws RemoteException {
        System.out.println("Start to shutdown the servers");
        serverToManagerMap.get("Flights").shutdown();
        serverToManagerMap.get("Rooms").shutdown();
        serverToManagerMap.get("Cars").shutdown();
        new Thread() {
            @Override
            public void run() {
                System.out.print("Prepare to shutdown middleware");
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                }
                System.out.println("Successfully shutdown the middleware");
                System.exit(0);
            }

        }.start();
        return true;
    }

    @Override
    public void startTransaction(int xid) throws RemoteException {
        if (!transactionManager.isActive(xid)) {
            Trace.info("Transaction has been associated with this manager");
            transactionManager.updateActiveTransaction(xid, new Transaction(xid, timeToLive));
        }
    }

    @Override
    public int getPrice(int transactionID, String key, int amount) throws InvalidTransactionException, RemoteException {
        ReservableItem item = (ReservableItem) readData(transactionID, key);
        if (item == null)
        {
            Trace.warn("RM::reserveItem(" + transactionID + ", " + key + ") failed--item doesn't exist");
            return -1;
        }
        else if (item.getCount() < amount)
        {
            Trace.warn("RM::reserveItem(" + transactionID + ", " + key + ") failed--Not enough items");
            return -1;
        }
        return item.getPrice();
    }

    public void getLock(int xid, String data, TransactionLockObject.LockType lockType)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        try {
            if (!lockManager.Lock(xid, data, lockType)) {
                // there are some exceptions on getting the lock
                System.out.println("Failed to get the specific lock for " + data);
                throw new InvalidTransactionException(xid, "The lock manager has failed to get the lock for " + data);
            }
        } catch (DeadlockException e) {
            System.out.println("Deadlock has happened");
            System.out.println("The transaction " + xid + " would be aborted...");
            abort(xid);
            throw new TransactionAbortedException(xid, "The transaction has been aborted because of a deadlock");
        }
    }

    /***************************************************************************/
    //winnie
    @Override
    public String analyticsFlight(int xid, int quantity) throws RemoteException, InvalidTransactionException {
        System.out.println("The request has been forwarded to Flight Resource Manager...");
        return serverToManagerMap.get("Flights").analyticsFlight(xid, quantity);
    }

    @Override
    public String analyticsRoom(int xid, int quantity) throws RemoteException, InvalidTransactionException {
        System.out.println("The request has been forwarded to Room Resource Manager...");
        return serverToManagerMap.get("Rooms").analyticsRoom(xid, quantity);
    }

    @Override
    public String analyticsCar(int xid, int quantity) throws RemoteException, InvalidTransactionException {
        System.out.println("The request has been forwarded to Car Resource Manager...");
        return serverToManagerMap.get("Cars").analyticsCar(xid, quantity);
    }
    //winnie

    /***************************************************************************/
    
    /*@Override
    public String analytics(int xid) throws RemoteException {
       return serverToManagerMap.get("Rooms").analytics(xid) + serverToManagerMap.get("Flights").analytics(xid) + serverToManagerMap.get("Cars").analytics(xid);
    }*/

    //---------------------------------------------------------------------------
    // these three methods are bundled together to handle the local copy issue
    // if there is the local copy of something
    // use it
    // otherwise move the data to local copy and read or write it
    // Reads a data item
    protected RMItem readData(int xid, String key) throws InvalidTransactionException {
        if (!transactionManager.isActive(xid))
            throw new InvalidTransactionException(xid, "This is not a valid transaction");

        Transaction transaction = transactionManager.getActiveTransaction(xid);
        if (!transaction.contains(key)) {
            synchronized (m_data) {
                RMItem item = m_data.get(key);
                if (item != null) {
                    transaction.writeData(xid, key, (RMItem) item.clone());
                } else {
                    transaction.writeData(xid, key, null);
                }
            }
        }

        return transaction.readData(xid, key);
    }

    // Writes a data item
    protected void writeData(int xid, String key, RMItem value) throws InvalidTransactionException {
        if (!transactionManager.isActive(xid))
            throw new InvalidTransactionException(xid, "This is not a valid transaction");

        // write a copy locally
        // not yet pushed up to the real database
        readData(xid, key);
        Transaction transaction = transactionManager.getActiveTransaction(xid);
        transaction.writeData(xid, key, value);
    }

    // Remove the item out of storage
    protected void removeData(int xid, String key) throws InvalidTransactionException {
        if (!transactionManager.isActive(xid))
            throw new InvalidTransactionException(xid, "This is not a valid transaction");

        // write a copy locally
        // not yet pushed up to the real database
        readData(xid, key);
        Transaction transaction = transactionManager.getActiveTransaction(xid);
        transaction.writeData(xid, key, null);
    }
    //---------------------------------------------------------------------------

    // Deletes the encar item
    protected boolean deleteItem(int xid, String key) throws InvalidTransactionException {
        Trace.info("RM::deleteItem(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(xid, key);
        // Check if there is such an item in the storage
        if (curObj == null) {
            Trace.warn("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
            return false;
        } else {
            if (curObj.getReserved() == 0) {
                removeData(xid, curObj.getKey());
                Trace.info("RM::deleteItem(" + xid + ", " + key + ") item deleted");
                return true;
            } else {
                Trace.info("RM::deleteItem(" + xid + ", " + key + ") item can't be deleted because some customers have reserved it");
                return false;
            }
        }
    }

    // Query the number of available seats/rooms/cars
    protected int queryNum(int xid, String key) throws InvalidTransactionException {
        Trace.info("RM::queryNum(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(xid, key);
        int value = 0;
        if (curObj != null) {
            value = curObj.getCount();
        }
        Trace.info("RM::queryNum(" + xid + ", " + key + ") returns count=" + value);
        return value;
    }

    // Query the price of an item
    protected int queryPrice(int xid, String key) throws InvalidTransactionException {
        Trace.info("RM::queryPrice(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(xid, key);
        int value = 0;
        if (curObj != null) {
            value = curObj.getPrice();
        }
        Trace.info("RM::queryPrice(" + xid + ", " + key + ") returns cost=$" + value);
        return value;
    }

    // Reserve an item
    protected boolean reserveItem(int xid, int customerID, String key, String location) throws InvalidTransactionException {
        Trace.info("RM::reserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called");
        // Read customer object if it exists (and read lock it)
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }

        // Check if the item is available
        ReservableItem item = (ReservableItem) readData(xid, key);
        if (item == null) {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
            return false;
        } else if (item.getCount() == 0) {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--No more items");
            return false;
        } else {
            customer.reserve(key, location, item.getPrice());
            writeData(xid, customer.getKey(), customer);

            // Decrease the number of available items in the storage
            item.setCount(item.getCount() - 1);
            item.setReserved(item.getReserved() + 1);
            writeData(xid, item.getKey(), item);

            Trace.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
            return true;
        }
    }


    public void checkIllegalTransaction(int xid) throws TransactionAbortedException, InvalidTransactionException {
        if (transactionManager.getActiveTransaction(xid) == null) {
            // now the transaction is not active
            // throw the exceptions accordingly
            System.out.println("Some error has occurred for this transaction...");
            System.out.println("Please check the feedback for the exception on the client side...");
            if (transactionManager.getInactiveTransaction(xid) == null) {
                throw new InvalidTransactionException(xid, "TM: There is no transaction " + xid);
            }
            if (transactionManager.getInactiveTransaction(xid).equals(InactiveStatus.COMMITTED)) {
                throw new InvalidTransactionException(xid, "TM: The transaction has already been committed before.");
            }
            if (transactionManager.getInactiveTransaction(xid).equals(InactiveStatus.ABORTED)) {
                throw new TransactionAbortedException(xid, "TM: The transaction has been aborted before.");
            }
        } else {
            // update (touch) the last accessed attribute of this transaction
            transactionManager.getActiveTransaction(xid).tick();
        }
    }

    public void distribute(int xid, IResourceManager manager) {
        // ignore if already added
        transactionManager.getActiveTransaction(xid).addRM(manager);
        try {
            manager.startTransaction(xid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void prepare(int xid, String key, TransactionLockObject.LockType type, IResourceManager manager)
            throws TransactionAbortedException, InvalidTransactionException, RemoteException {
        checkIllegalTransaction(xid);
        System.out.println("Waiting the lock...");
        getLock(xid, key, type);
        System.out.println("Got the lock!");
        distribute(xid, manager);
    }
}
