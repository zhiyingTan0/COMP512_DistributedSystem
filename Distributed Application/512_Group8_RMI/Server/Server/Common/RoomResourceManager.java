package Server.Common;

import Server.Interface.IResourceManager;

import java.rmi.RemoteException;
import java.util.Vector;

public class RoomResourceManager implements IResourceManager {

    protected String m_name = "";
    protected RMHashMap m_data = new RMHashMap();

    public RoomResourceManager(String name){
        m_name = name;
    }
    @Override
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException {
        return false;
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {
        return false;
    }

    // Create a new room location or add rooms to an existing location
    // NOTE: if price <= 0 and the room location already exists, it maintains its current price
    @Override
    public boolean addRooms(int xid, String location, int count, int price) throws RemoteException {
        Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
        Room curObj = (Room)readData(xid, Room.getKey(location));
        if (curObj == null)
        {
            // Room location doesn't exist yet, add it
            Room newObj = new Room(location, count, price);
            writeData(xid, newObj.getKey(), newObj);
            Trace.info("RM::addRooms(" + xid + ") created new room location " + location + ", count=" + count + ", price=$" + price);
        } else {
            // Add count to existing object and update price if greater than zero
            curObj.setCount(curObj.getCount() + count);
            if (price > 0)
            {
                curObj.setPrice(price);
            }
            writeData(xid, curObj.getKey(), curObj);
            Trace.info("RM::addRooms(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
        }
        return true;
    }
    //winnie
    @Override
    public String analyticsFlight(int id, int quantity) throws RemoteException {
        return null;
    }
    @Override
    public String analyticsCar(int id, int quantity) throws RemoteException {
        return null;
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
    public boolean deleteFlight(int id, int flightNum) throws RemoteException {
        return false;
    }

    @Override
    public boolean deleteCars(int id, String location) throws RemoteException {
        return false;
    }

    @Override
    public boolean deleteRooms(int xid, String location) throws RemoteException {
        return deleteItem(xid, Room.getKey(location));
    }

    @Override
    public boolean deleteCustomer(int id, int customerID) throws RemoteException {
        return false;
    }

    @Override
    public int queryFlight(int id, int flightNumber) throws RemoteException {
        return 0;
    }

    @Override
    public int queryCars(int id, String location) throws RemoteException {
        return 0;
    }

    @Override
    public int queryRooms(int xid, String location) throws RemoteException {
        return queryNum(xid, Room.getKey(location));
    }

    @Override
    public String queryCustomerInfo(int id, int customerID) throws RemoteException {
        return null;
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber) throws RemoteException {
        return 0;
    }

    @Override
    public int queryCarsPrice(int id, String location) throws RemoteException {
        return 0;
    }

    @Override
    public int queryRoomsPrice(int xid, String location) throws RemoteException {
        return queryPrice(xid, Room.getKey(location));
    }

    @Override
    public ReservableItem reserveFlight(int id, int customerID, int flightNumber) throws RemoteException {
        return null;
    }

    @Override
    public ReservableItem reserveCar(int id, int customerID, String location) throws RemoteException {
        return null;
    }

    @Override
    public ReservableItem reserveRoom(int xid, int customerID, String location) throws RemoteException {
        return reserveItem(xid, customerID, Room.getKey(location), location);
    }

    @Override
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException {
        return false;
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
        return null;
    }

    /*@Override
    public String analytics(int xid) throws RemoteException {
        String result = "\nFor Room part-----------\n";
        synchronized (m_data){
            for (String key :  m_data.keySet()){
                if (m_data.get(key) instanceof Room){
                    Room room = (Room) m_data.get(key);
                    if (room.getCount() < 3){
                        result += room.getKey() + " with only " + room.getCount() + "\n";
                    }
                }
            }
        }
        return result;
    }*/

    // Reads a data item
    protected RMItem readData(int xid, String key)
    {
        synchronized(m_data) {
            RMItem item = m_data.get(key);
            if (item != null) {
                return (RMItem)item.clone();
            }
            return null;
        }
    }

    // Writes a data item
    protected void writeData(int xid, String key, RMItem value)
    {
        synchronized(m_data) {
            m_data.put(key, value);
        }
    }

    // Remove the item out of storage
    protected void removeData(int xid, String key)
    {
        synchronized(m_data) {
            m_data.remove(key);
        }
    }

    // Deletes the encar item
    protected boolean deleteItem(int xid, String key)
    {
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
    protected int queryNum(int xid, String key)
    {
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
    protected int queryPrice(int xid, String key)
    {
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
    protected ReservableItem reserveItem(int xid, int customerID, String key, String location)
    {
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
	//winnie--AnalyticsRoom
	public String readAllData(int xid, String idType,int quantity)
	{
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
	public String analyticsRoom(int xid, int quantity) {
		//System.out.println("1\n");
		Trace.info("RM::analyticsRoom(" + xid + ", " + quantity + ") called");
		//System.out.println("2\n");
		String matchRoom = readAllData(xid,"room-",quantity);
		Trace.info("RM::analyticsRoom(" + xid + ", " + quantity + ") returns = "+ matchRoom);
		return matchRoom;
	}
	
	//winnie--Analytics Flight
	/***********************************************************************************************/
}

