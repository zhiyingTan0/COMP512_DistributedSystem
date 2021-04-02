package Server.Exceptions;

public class TransactionAbortedException extends Exception {

    // some dummy flag xid at first
    private int xid = -1;

    // a constructor for the exception
    public TransactionAbortedException(int xid, String msg){
        super("The transaction " + xid + " has already been aborted: " + msg);
        this.xid = xid;
    }
}
