import Server.Transaction.Transaction;
import Server.Transaction.LocalBuffer;

public class MiddlewareTransactionManager extends LocalBuffer {

    private int nextXid = 0;
    private int timeToLive;
    private RMIMiddleware middleware;

    public MiddlewareTransactionManager(int timeToLive, RMIMiddleware middleware){
        this.middleware = middleware;
        this.timeToLive = timeToLive;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    while (true){
                        synchronized (activeTransactions){
                            for (Integer key : activeTransactions.keySet()){
                                if (activeTransactions.get(key) != null && activeTransactions.get(key).hasExpired()){
                                    System.out.println("The transaction " + key + " has reached the time-to-live");
                                    System.out.println("Ready to abort it");
                                    middleware.abort(key);
                                }
                            }
                        }
                        Thread.sleep(3500);
                    }
                }catch (Exception e){
                   e.printStackTrace();
                }
            }
        }).start();
    }

    public int startTransaction(){
        nextXid ++;
        int xid = nextXid;
        updateActiveTransaction(xid, new Transaction(xid, timeToLive));
        return xid;
    }
}
