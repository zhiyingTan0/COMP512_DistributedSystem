import Server.Common.*;
import Server.Interface.IResourceManager;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Vector;

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


    public static void main(String[] args) {

        // arguments check
        if (args.length == 3){
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

    }

    public void init() {
        statusMap.put(server_names[0], false);
        statusMap.put(server_names[1], false);
        statusMap.put(server_names[2], false);
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
        // TODO: maybe the if statement could be deleted
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
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException {
        synchronized (serverToManagerMap.get("Flights")){
            System.out.println("The request has been forwarded to Flight Resource Manager...");
            return serverToManagerMap.get("Flights").addFlight(id, flightNum, flightSeats, flightPrice);
        }
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {
        synchronized (serverToManagerMap.get("Cars")){
            System.out.println("The request has been forwarded to Car Resource Manager...");
            return serverToManagerMap.get("Cars").addCars(id, location, numCars, price);
        }
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {
        synchronized (serverToManagerMap.get("Rooms")){
            System.out.println("The request has been forwarded to Room Resource Manager...");
            return serverToManagerMap.get("Rooms").addRooms(id, location, numRooms, price);
        }
    }

    @Override
    public int newCustomer(int xid) throws RemoteException {
        synchronized (this){
            Trace.info("RM::newCustomer(" + xid + ") called");
            // Generate a globally unique ID for the new customer
            int cid = Integer.parseInt(String.valueOf(xid) +
                    String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                    String.valueOf(Math.round(Math.random() * 100 + 1)));
            Customer customer = new Customer(cid);
            writeData(xid, customer.getKey(), customer);
            Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
            return cid;
        }
    }

    @Override
    public boolean newCustomer(int xid, int customerID) throws RemoteException {
        synchronized (this){
            Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") called");
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
    }




    // needs careful handling starting this point...
    @Override
    public boolean deleteFlight(int id, int flightNum) throws RemoteException {
        synchronized (serverToManagerMap.get("Flights")){
            System.out.println("The request has been forwarded to Flight Resource Manager...");
            return serverToManagerMap.get("Flights").deleteFlight(id, flightNum);
        }
    }

    @Override
    public boolean deleteCars(int id, String location) throws RemoteException {
        synchronized (serverToManagerMap.get("Cars")){
            System.out.println("The request has been forwarded to Car Resource Manager...");
            return serverToManagerMap.get("Cars").deleteCars(id, location);
        }
    }

    @Override
    public boolean deleteRooms(int id, String location) throws RemoteException {
        synchronized (serverToManagerMap.get("Rooms")){
            System.out.println("The request has been forwarded to Room Resource Manager...");
            return serverToManagerMap.get("Rooms").deleteRooms(id, location);
        }
    }

    @Override
    public boolean deleteCustomer(int xid, int customerID) throws RemoteException {
        Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
            return false;
        } else {
            // Increase the reserved numbers of all reservable items which the customer reserved.
            RMHashMap reservations = customer.getReservations();
            for (String reservedKey : reservations.keySet()) {
                ReservedItem reserveditem = customer.getReservedItem(reservedKey);
                Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " " + reserveditem.getCount() + " times");
                // determine the genre of the item
                ReservableItem item = null;
                if (serverToManagerMap.get("Cars").readDataWrapper(xid, reserveditem.getKey()) != null) {
                    item = (ReservableItem) serverToManagerMap.get("Cars").readDataWrapper(xid, reserveditem.getKey());
                }
                if (serverToManagerMap.get("Rooms").readDataWrapper(xid, reserveditem.getKey()) != null) {
                    item = (ReservableItem) serverToManagerMap.get("Rooms").readDataWrapper(xid, reserveditem.getKey());
                }
                if (serverToManagerMap.get("Flights").readDataWrapper(xid, reserveditem.getKey()) != null) {
                    item = (ReservableItem) serverToManagerMap.get("Flights").readDataWrapper(xid, reserveditem.getKey());
                }
                //item = (ReservableItem) readData(xid, reserveditem.getKey());
                assert item != null;
                System.out.println(item);
                Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " which is reserved " + item.getReserved() + " times and is still available " + item.getCount() + " times");
                item.setReserved(item.getReserved() - reserveditem.getCount());
                item.setCount(item.getCount() + reserveditem.getCount());

                // push back the items to the corresponding managers
                if (item instanceof Car){
                    serverToManagerMap.get("Cars").updateCustomorOrItem(xid, item.getKey(), item);
                }
                if (item instanceof Room){
                    serverToManagerMap.get("Rooms").updateCustomorOrItem(xid, item.getKey(), item);
                }
                if (item instanceof Flight){
                    serverToManagerMap.get("Flights").updateCustomorOrItem(xid, item.getKey(), item);
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
    public int queryFlight(int id, int flightNumber) throws RemoteException {
        System.out.println("The request has been forwarded to Flight Resource Manager...");
        return serverToManagerMap.get("Flights").queryFlight(id, flightNumber);
    }

    @Override
    public int queryCars(int id, String location) throws RemoteException {
        System.out.println("The request has been forwarded to Car Resource Manager...");
        return serverToManagerMap.get("Cars").queryCars(id, location);
    }

    @Override
    public int queryRooms(int id, String location) throws RemoteException {
        System.out.println("The request has been forwarded to Room Resource Manager...");
        return serverToManagerMap.get("Rooms").queryRooms(id, location);
    }

    @Override
    public String queryCustomerInfo(int xid, int customerID) throws RemoteException {
        Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ") called");
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
    public int queryFlightPrice(int id, int flightNumber) throws RemoteException {
        System.out.println("The request has been forwarded to Flight Resource Manager...");
        return serverToManagerMap.get("Flights").queryFlightPrice(id, flightNumber);
    }

    @Override
    public int queryCarsPrice(int id, String location) throws RemoteException {
        System.out.println("The request has been forwarded to Car Resource Manager...");
        return serverToManagerMap.get("Cars").queryCarsPrice(id, location);
    }

    @Override
    public int queryRoomsPrice(int id, String location) throws RemoteException {
        System.out.println("The request has been forwarded to Room Resource Manager...");
        return serverToManagerMap.get("Rooms").queryRoomsPrice(id, location);
    }

    @Override
    public ReservableItem reserveFlight(int id, int customerID, int flightNumber) throws RemoteException {
        System.out.println("The request has been forwarded to Flight Resource Manager...");
        try {
            // first check whether the given customer exists or not
            Customer customer = getCustomer(id, customerID);
            if (customer == null){
                Trace.warn("RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist");
                // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
                return null;
            }
            synchronized (customer){
                // reserve the room!
                ReservableItem item = serverToManagerMap.get("Flights").reserveFlight(id, customerID, flightNumber);

                // check whether or not reserving is successful
                if (item == null){
                    return null;
                }
                // we are good!
                customer.reserve(Flight.getKey(flightNumber), String.valueOf(flightNumber), item.getPrice());
                this.updateCustomorOrItem(id, customer.getKey(), customer);
                return item;
            }

        }catch (Exception e){
            System.out.println("Connection error happened, trying to reconnect...");
            connectServer(server_host_names[1], FIGHT_RS_NUM, "Flights");
        }
        return null;
    }

    @Override
    public ReservableItem reserveCar(int id, int customerID, String location) throws RemoteException {
        System.out.println("The request has been forwarded to Car Resource Manager...");
        try {
            // first check whether the given customer exists or not
            Customer customer = getCustomer(id, customerID);
            if (customer == null){
                Trace.warn("RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist");
                // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
                return null;
            }
            synchronized (customer){
                // reserve the room!
                ReservableItem item = serverToManagerMap.get("Cars").reserveCar(id, customerID, location);

                // check whether or not reserving is successful
                if (item == null){
                    return null;
                }
                // we are good!
                customer.reserve(Car.getKey(location), location, item.getPrice());
                this.updateCustomorOrItem(id, customer.getKey(), customer);
                return item;
            }

        }catch (Exception e){
            System.out.println("Connection error happened, trying to reconnect...");
            connectServer(server_host_names[2], CAR_RS_NUM, "Cars");
        }
        return null;
    }

    @Override
    public ReservableItem reserveRoom(int id, int customerID, String location) throws RemoteException {
        System.out.println("The request has been forwarded to Room Resource Manager...");
        try {
            Customer customer = getCustomer(id, customerID);
            // first check whether the given customer exists or not
            if (customer == null){
                Trace.warn("RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist");
                // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
               return null;
            }

            synchronized (customer){
                // reserve the room!
                ReservableItem item = serverToManagerMap.get("Rooms").reserveRoom(id, customerID, location);

                // check whether or not reserving is successful
                if (item == null){
                    return null;
                }

                // we are good!
                customer.reserve(Room.getKey(location), location, item.getPrice());
                this.updateCustomorOrItem(id, customer.getKey(), customer);
                return item;
            }
        }catch (Exception e){
            System.out.println("Connection error happened, trying to reconnect...");
            connectServer(server_host_names[0], ROOM_RS_NUM, "Rooms");
        }
        return null;
    }

    @Override
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException {
        for (String flightNumber : flightNumbers){
            int flightNum = Integer.parseInt(flightNumber);
            ReservableItem item = reserveFlight(id, customerID, flightNum);

            if (item == null){
                System.out.println("The customer " + customerID + " has failed to book flight " + flightNumber);
                System.out.println("The reservation has terminated.");
                return false;
            }
        }
        if (car){
            ReservableItem item = reserveCar(id, customerID, location);
            if (item != null){
                System.out.println("The customer " + customerID + " has failed to book car in  " + location);
                System.out.println("The reservation has terminated.");
                return false;
            }
        }

        if (room) {
            ReservableItem item = reserveRoom(id, customerID, location);
            if (item != null){
                System.out.println("The customer " + customerID + " has failed to book room in  " + location);
                System.out.println("The reservation has terminated.");
                return false;
            }
        }
        return true;
    }

    public Customer getCustomer(int xid, int customerID) throws RemoteException{
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
           return null;
        } else {
           return customer;
        }
    }

    @Override
    public String getName() throws RemoteException {
        return null;
    }

    @Override
    public String updateCustomorOrItem(int xid, String key, RMItem item) throws RemoteException {
        writeData(xid, key, item);
        return null;
    }

    @Override
    public RMItem readDataWrapper(int xid, String key) {
        return readData( xid,  key);
    }

    @Override
    public String summary(int xid) throws RemoteException {
        synchronized (m_data){
            String result = "";
            for (String key :  m_data.keySet()){
                if (m_data.get(key) instanceof Customer){
                    System.out.println("Found a customer!!!");
                    // go to loop the reserved items
                    Customer customer = (Customer) m_data.get(key);
                    //System.out.println(customer.getReservations().values());
                    result = result + "Customer " + customer.getID() + "---- starts ---------------\n";
                    for (RMItem item : customer.getReservations().values()){
                        result = result + item.toString() + "\n";
                    }
                    result = result + "Customer " + customer.getID() + "---- finishes ---------------\n";
                }
            }
            return result;
        }
    }
    /***************************************************************************/
    //winnie
    @Override
    public String analyticsFlight(int xid, int quantity) throws RemoteException {
        System.out.println("The request has been forwarded to Flight Resource Manager...");
        return serverToManagerMap.get("Flights").analyticsFlight(xid, quantity);
    }
    @Override
    public String analyticsRoom(int xid, int quantity) throws RemoteException {
        System.out.println("The request has been forwarded to Room Resource Manager...");
        return serverToManagerMap.get("Rooms").analyticsRoom(xid, quantity);
    }
    @Override
    public String analyticsCar(int xid, int quantity) throws RemoteException {
        System.out.println("The request has been forwarded to Car Resource Manager...");
        return serverToManagerMap.get("Cars").analyticsCar(xid, quantity);
    }
    //winnie
    /***************************************************************************/
    
    /*@Override
    public String analytics(int xid) throws RemoteException {
       return serverToManagerMap.get("Rooms").analytics(xid) + serverToManagerMap.get("Flights").analytics(xid) + serverToManagerMap.get("Cars").analytics(xid);
    }*/

    // Reads a data item
    protected RMItem readData(int xid, String key) {
        synchronized (m_data) {
            RMItem item = m_data.get(key);
            if (item != null) {
                return (RMItem) item.clone();
            }
            return null;
        }
    }

    // Writes a data item
    protected void writeData(int xid, String key, RMItem value) {
        synchronized (m_data) {
            m_data.put(key, value);
        }
    }

    // Remove the item out of storage
    protected void removeData(int xid, String key) {
        synchronized (m_data) {
            m_data.remove(key);
        }
    }

    // Deletes the encar item
    protected boolean deleteItem(int xid, String key) {
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
    protected int queryNum(int xid, String key) {
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
    protected int queryPrice(int xid, String key) {
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
    protected boolean reserveItem(int xid, int customerID, String key, String location) {
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
}
