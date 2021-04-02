package Server.Exceptions;

public class InvalidTransactionException extends Exception{

    // some dummy flag xid at first
    private int xid = -1;

    // a constructor for the exception
    public InvalidTransactionException(int xid, String msg){
        super("The transaction " + xid + " is invalid: " + msg);
        this.xid = xid;
    }
}
