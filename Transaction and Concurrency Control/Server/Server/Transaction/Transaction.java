package Server.Transaction;

import Server.Common.RMHashMap;
import Server.Common.RMItem;
import Server.Interface.IResourceManager;

import java.util.ArrayList;

public class Transaction {

    private int xid;
    // the data that this transaction is going to write
    private RMHashMap m_data = new RMHashMap();
    private long lastAccessed = getCurrentTime();
    // the list of involved resource managers
    private ArrayList<IResourceManager> managers = new ArrayList<>();
    // in milliseconds
    private int timeToLive = -1;

    public Transaction(int xid, int timeToLive){
        this.xid = xid;
        this.timeToLive = timeToLive;
        this.lastAccessed = getCurrentTime();
    }

    public Transaction(int xid){
        this.xid = xid;
    }

    public boolean hasExpired(){
        // get the current time
        long currentTime = getCurrentTime();
        // check whether or not it has expired
        return (currentTime - lastAccessed > timeToLive);
    }

    public long getCurrentTime(){
        return System.currentTimeMillis();
    }

    // this method would update the value of lastAccessed attribute
    public void tick(){
        lastAccessed = getCurrentTime();
    }

    public void addRM(IResourceManager rm){
        if (!managers.contains(rm))
            managers.add(rm);
    }

    public ArrayList<IResourceManager> getManagers(){
        return managers;
    }

    public RMHashMap getLocalBuffer(){
        return m_data;
    }

    // these two methods are borrowed from the original template code
    // the m_data are just a local copy
    // Writes a data item
    public void writeData(int xid, String key, RMItem value)
    {
        synchronized(m_data) {
            m_data.put(key, value);
        }
    }

    // Reads a data item
    public RMItem readData(int xid, String key)
    {
        synchronized(m_data) {
            RMItem item = m_data.get(key);
            if (item != null) {
                return (RMItem)item.clone();
            }
            return null;
        }
    }

    public boolean contains(String key) {
        synchronized (m_data) {
            return m_data.containsKey(key);
        }
    }
}
