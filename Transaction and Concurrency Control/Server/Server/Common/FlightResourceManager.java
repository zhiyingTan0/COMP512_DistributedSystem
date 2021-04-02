package Server.Common;

import Server.Exceptions.InvalidTransactionException;
import Server.Exceptions.TransactionAbortedException;
import Server.Interface.IResourceManager;
import Server.Transaction.InactiveStatus;
import Server.Transaction.Transaction;
import Server.Transaction.LocalBuffer;

import java.rmi.RemoteException;
import java.util.Vector;

public class FlightResourceManager implements IResourceManager {

    protected String m_name = "";
    protected RMHashMap m_data = new RMHashMap();
    protected LocalBuffer localBuffer;

    public FlightResourceManager(String name){
        m_name = name;
        localBuffer = new LocalBuffer();
    }
    @Override
    public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException, InvalidTransactionException {

        Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
        Flight curObj = (Flight)readData(xid, Flight.getKey(flightNum));
        if (curObj == null)
        {
            // Doesn't exist yet, add it
            Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
            writeData(xid, newObj.getKey(), newObj);
            Trace.info("RM::addFlight(" + xid + ") created new flight " + flightNum + ", seats=" + flightSeats + ", price=$" + flightPrice);
        }
        else
        {
            // Add seats to existing flight and update the price if greater than zero
            curObj.setCount(curObj.getCount() + flightSeats);
            if (flightPrice > 0)
            {
                curObj.setPrice(flightPrice);
            }
            writeData(xid, curObj.getKey(), curObj);
            Trace.info("RM::addFlight(" + xid + ") modified existing flight " + flightNum + ", seats=" + curObj.getCount() + ", price=$" + flightPrice);
        }
        return true;
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {
        return false;
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {
        return false;
    }

    @Override
    public int newCustomer(int id) throws RemoteException {
        return 0;
    }

    @Override
    public boolean newCustomer(int id, int cid) throws RemoteException {
        return false;
    }

    @Override
    public boolean deleteFlight(int xid, int flightNum) throws RemoteException, InvalidTransactionException {
        return deleteItem(xid, Flight.getKey(flightNum));
    }

    @Override
    public boolean deleteCars(int id, String location) throws RemoteException {
        return false;
    }

    @Override
    public boolean deleteRooms(int id, String location) throws RemoteException {
        return false;
    }

    @Override
    public boolean deleteCustomer(int id, int customerID) throws RemoteException {
        return false;
    }

    @Override
    public int queryFlight(int xid, int flightNum) throws RemoteException, InvalidTransactionException {
        return queryNum(xid, Flight.getKey(flightNum));
    }

    @Override
    public int queryCars(int id, String location) throws RemoteException {
        return 0;
    }

    @Override
    public int queryRooms(int id, String location) throws RemoteException {
        return 0;
    }

    @Override
    public String queryCustomerInfo(int id, int customerID) throws RemoteException {
        return null;
    }

    @Override
    public int queryFlightPrice(int xid, int flightNum) throws RemoteException, InvalidTransactionException {
        return queryPrice(xid, Flight.getKey(flightNum));
    }

    @Override
    public int queryCarsPrice(int id, String location) throws RemoteException {
        return 0;
    }

    @Override
    public int queryRoomsPrice(int id, String location) throws RemoteException {
        return 0;
    }

    @Override
    public ReservableItem reserveFlight(int xid, int customerID, int flightNum) throws RemoteException, InvalidTransactionException {
        return reserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
    }

    @Override
    public ReservableItem reserveCar(int id, int customerID, String location) throws RemoteException {
        return null;
    }

    @Override
    public ReservableItem reserveRoom(int id, int customerID, String location) throws RemoteException {
        return null;
    }

    @Override
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException {
        return false;
    }

    @Override
    public String getName() throws RemoteException {
        return "Flight";
    }

    @Override
    public String updateCustomerOrItem(int xid, String key, RMItem item) throws RemoteException, InvalidTransactionException {
        writeData(xid, key, item);
        return null;
    }

    @Override
    public RMItem readDataWrapper(int xid, String key) throws InvalidTransactionException {
        return readData( xid,  key);
    }

    @Override
    public String summary(int xid) throws RemoteException {
        return null;
    }

    @Override
    public boolean revertReservation(int xid, int customerID, String key, int count) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + key + " " +  count +  " times");
        ReservableItem item  = (ReservableItem)readData(xid, key);
        Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + key + " which is reserved " +  item.getReserved() +  " times and is still available " + item.getCount() + " times");
        item.setReserved(item.getReserved() - count);
        item.setCount(item.getCount() + count);
        writeData(xid, item.getKey(), item);
        return true;
    }

    @Override
    public int start() throws RemoteException {
        return 0;
    }

    @Override
    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        System.out.println("Start to commit the transaction " + transactionId);
        // flush all the local buffer values to the storage
        if(!localBuffer.isActive(transactionId))
            throw new InvalidTransactionException(transactionId, "This is not a valid transaction");

        // put all the values that are in the memory into the database
        RMHashMap buffer = localBuffer.getActiveTransaction(transactionId).getLocalBuffer();
        synchronized (m_data) {
            for (String key : buffer.keySet()) {
                System.out.println("Update:(" + key + "," + buffer.get(key) + ") into the database");
                m_data.put(key, buffer.get(key));
            }
        }

        // clean up the database
        localBuffer.updateActiveTransaction(transactionId, null);
        localBuffer.addInactiveTransaction(transactionId, InactiveStatus.COMMITTED);
        System.out.println("Commit successfully the transaction " + transactionId);
        return true;
    }

    @Override
    public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
        System.out.println("Start to abort the transaction " + transactionId);
        if (!localBuffer.isActive(transactionId)){
            throw new InvalidTransactionException(transactionId, "This is not a valid transaction.");
        }

        localBuffer.updateActiveTransaction(transactionId, null);
        localBuffer.addInactiveTransaction(transactionId, InactiveStatus.ABORTED);
        System.out.println("Successfully abort the transaction " + transactionId);
    }

    @Override
    public boolean shutdown() throws RemoteException {
        new Thread() {
            @Override
            public void run() {
                System.out.print("Prepare to shutdown");
                try {
                    sleep(1000);
                } catch (InterruptedException e) {}
                System.out.println("Successfully shutdown the flight server");
                System.exit(0);
            }

        }.start();
        return true;
    }

    @Override
    public void startTransaction(int xid) throws RemoteException {
        if (!localBuffer.isActive(xid)) {
            Trace.info("Transaction has been associated with this manager");
            localBuffer.updateActiveTransaction(xid, new Transaction(xid));
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

    /*@Override
    public String analytics(int xid) throws RemoteException {
        String result = "\nFor Flight part-----------\n";
        synchronized (m_data){
            for (String key :  m_data.keySet()){
                if (m_data.get(key) instanceof Flight){
                    Flight flight = (Flight) m_data.get(key);
                    if (flight.getCount() < 3){
                        result += flight.getKey() + " with only " + flight.getCount() + "\n";
                    }
                }
            }
        }
        return result;
    }*/

    // Reads a data item
    //---------------------------------------------------------------------------
    // these three methods are bundled together to handle the local copy issue
    // if there is the local copy of something
    // use it
    // otherwise move the data to local copy and read or write it
    // Reads a data item
    protected RMItem readData(int xid, String key) throws InvalidTransactionException {
        if(!localBuffer.isActive(xid))
            throw new InvalidTransactionException(xid, "This is not a valid transaction");

        Transaction transaction = localBuffer.getActiveTransaction(xid);
        if (!transaction.contains(key)) {
            synchronized (m_data) {
                RMItem item = m_data.get(key);
                if (item != null) {
                    transaction.writeData(xid, key, (RMItem) item.clone());
                }
                else {
                    transaction.writeData(xid, key, null);
                }
            }
        }

        return transaction.readData(xid, key);
    }

    // Writes a data item
    protected void writeData(int xid, String key, RMItem value) throws InvalidTransactionException {
        if(!localBuffer.isActive(xid))
            throw new InvalidTransactionException(xid, "This is not a valid transaction");

        // write a copy locally
        // not yet pushed up to the real database
        readData(xid, key);
        Transaction transaction = localBuffer.getActiveTransaction(xid);
        transaction.writeData(xid, key, value);
    }

    // Remove the item out of storage
    protected void removeData(int xid, String key) throws InvalidTransactionException {
        if(!localBuffer.isActive(xid))
            throw new InvalidTransactionException(xid, "This is not a valid transaction");

        // write a copy locally
        // not yet pushed up to the real database
        readData(xid, key);
        Transaction transaction = localBuffer.getActiveTransaction(xid);
        transaction.writeData(xid, key, null);
    }
    //---------------------------------------------------------------------------

    // Deletes the encar item
    public boolean deleteItem(int xid, String key) throws InvalidTransactionException {
        Trace.info("RM::deleteItem(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem)readData(xid, key);
        // Check if there is such an item in the storage
        if (curObj == null)
        {
            Trace.warn("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
            return false;
        }
        else
        {
            if (curObj.getReserved() == 0)
            {
                removeData(xid, curObj.getKey());
                Trace.info("RM::deleteItem(" + xid + ", " + key + ") item deleted");
                return true;
            }
            else
            {
                Trace.info("RM::deleteItem(" + xid + ", " + key + ") item can't be deleted because some customers have reserved it");
                return false;
            }
        }
    }

    // Query the number of available seats/rooms/cars
    public int queryNum(int xid, String key) throws InvalidTransactionException {
        Trace.info("RM::queryNum(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem)readData(xid, key);
        int value = 0;
        if (curObj != null)
        {
            value = curObj.getCount();
        }
        Trace.info("RM::queryNum(" + xid + ", " + key + ") returns count=" + value);
        return value;
    }

    // Query the price of an item
    public int queryPrice(int xid, String key) throws InvalidTransactionException {
        Trace.info("RM::queryPrice(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem)readData(xid, key);
        int value = 0;
        if (curObj != null)
        {
            value = curObj.getPrice();
        }
        Trace.info("RM::queryPrice(" + xid + ", " + key + ") returns cost=$" + value);
        return value;
    }

    // Reserve an item
    public ReservableItem reserveItem(int xid, int customerID, String key, String location) throws InvalidTransactionException {
        Trace.info("RM::reserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called" );
        // Read customer object if it exists (and read lock it)
//        Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
//        if (customer == null)
//        {
//            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
//            return null;
//        }

        // Check if the item is available
        ReservableItem item = (ReservableItem)readData(xid, key);
        if (item == null)
        {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
            return null;
        }
        else if (item.getCount() == 0)
        {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--No more items");
            return null;
        }
        else
        {
//            customer.reserve(key, location, item.getPrice());
//            writeData(xid, customer.getKey(), customer);

            // Decrease the number of available items in the storage
            item.setCount(item.getCount() - 1);
            item.setReserved(item.getReserved() + 1);
            writeData(xid, item.getKey(), item);

            Trace.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
            return item;
        }
    }
    
    /***********************************************************************************************/
    //winnie--AnalyticsFlight
    public String readAllData(int xid, String idType,int quantity) throws InvalidTransactionException {
        //System.out.println("3\n");
        synchronized(m_data) {
            //System.out.println("4\n");
            String s = "\n--- BEGIN Analytics Result ---\n";
            int value=0;
            for (String key : m_data.keySet())
            {
                //System.out.println("5\n");
                if(key.contains(idType)) {
                    //String value = m_data.get(key).toString();
                    ReservableItem curObj = (ReservableItem)readData(xid,key);
                    value=curObj.getCount();

                    if(value<=quantity) {
                        s = s + "[Flight_Key= '" + key + "' ]  Remaining seats:" + value + "\n";
                    }
                }
            }
            s = s + "--- END Analytics Result ---";
            return s;
        }

    }
    @Override
    public String analyticsFlight(int xid, int quantity) throws InvalidTransactionException {
        //System.out.println("1\n");
        Trace.info("RM::analyticsFlight(" + xid + ", " + quantity + ") called");
        //System.out.println("2\n");
        String matchFlight = readAllData(xid,"flight-",quantity);
        Trace.info("RM::analyticsFlight(" + xid + ", " + quantity + ") returns = "+ matchFlight);
        return matchFlight;
    }
    
    @Override
    public String analyticsRoom(int id, int quantity) throws RemoteException {
        return null;
    }
    @Override
    public String analyticsCar(int id, int quantity) throws RemoteException {
        return null;
    }

    //winnie--Analytics Flight
    /***********************************************************************************************/

}

