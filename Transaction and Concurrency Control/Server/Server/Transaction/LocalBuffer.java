package Server.Transaction;
import java.util.HashMap;

public class LocalBuffer {

    protected HashMap<Integer, InactiveStatus> inactiveTransactions = new HashMap<>();
    protected HashMap<Integer, Transaction> activeTransactions = new HashMap<>();

    // check whether or not this transaction is active according to the hashmap
    public boolean isActive(int xid){
        synchronized (inactiveTransactions){
            synchronized (activeTransactions){
                return !inactiveTransactions.containsKey(xid) && activeTransactions.containsKey(xid);
            }
        }
    }

    //-----------------------------
    // these methods are for active transactions
    public void updateActiveTransaction(int xid, Transaction transaction){
        synchronized (activeTransactions){
            activeTransactions.put(xid, transaction);
        }
    }

    public Transaction getActiveTransaction(int transactionID){
        synchronized (activeTransactions){
            return activeTransactions.get(transactionID);
        }
    }

    //-----------------------------
    // these methods are for inactive transactions
    public void addInactiveTransaction(int xid, InactiveStatus inactiveStatus){
        synchronized (activeTransactions){
            inactiveTransactions.put(xid, inactiveStatus);
        }
    }

    public InactiveStatus getInactiveTransaction(int transactionID){
        synchronized (activeTransactions){
            return inactiveTransactions.get(transactionID);
        }
    }
    //-----------------------------

}


