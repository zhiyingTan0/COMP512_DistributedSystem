package Server.Interface;

import Server.Common.RMItem;
import Server.Common.ReservableItem;
import Server.Exceptions.InvalidTransactionException;
import Server.Exceptions.TransactionAbortedException;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.*;

/** 
 * Simplified version from CSE 593 Univ. of Washington
 *
 * Distributed  System in Java.
 * 
 * failure reporting is done using two pieces, exceptions and boolean 
 * return values.  Exceptions are used for systemy things. Return
 * values are used for operations that would affect the consistency
 * 
 * If there is a boolean return value and you're not sure how it 
 * would be used in your implementation, ignore it.  I used boolean
 * return values in the interface generously to allow flexibility in 
 * implementation.  But don't forget to return true when the operation
 * has succeeded.
 */

public interface IResourceManager extends Remote 
{
    /**
     * Add seats to a flight.
     *
     * In general this will be used to create a new
     * flight, but it should be possible to add seats to an existing flight.
     * Adding to an existing flight should overwrite the current price of the
     * available seats.
     *
     * @return Success
     */
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;
    
    /**
     * Add car at a location.
     *
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     *
     * @return Success
     */
    public boolean addCars(int id, String location, int numCars, int price)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;
   
    /**
     * Add room at a location.
     *
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     *
     * @return Success
     */
    public boolean addRooms(int id, String location, int numRooms, int price)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;
			    
    public String analyticsFlight(int xid, int quantity) throws RemoteException, InvalidTransactionException;
    public String analyticsRoom(int xid, int quantity) throws RemoteException, InvalidTransactionException;
    public String analyticsCar(int xid, int quantity) throws RemoteException, InvalidTransactionException;

    /**
     * Add customer.
     *
     * @return Unique customer identifier
     */
    public int newCustomer(int id)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;
    
    /**
     * Add customer with id.
     *
     * @return Success
     */
    public boolean newCustomer(int id, int cid)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;

    /**
     * Delete the flight.
     *
     * deleteFlight implies whole deletion of the flight. If there is a
     * reservation on the flight, then the flight cannot be deleted
     *
     * @return Success
     */   
    public boolean deleteFlight(int id, int flightNum)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;
    
    /**
     * Delete all cars at a location.
     *
     * It may not succeed if there are reservations for this location
     *
     * @return Success
     */		    
    public boolean deleteCars(int id, String location)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;

    /**
     * Delete all rooms at a location.
     *
     * It may not succeed if there are reservations for this location.
     *
     * @return Success
     */
    public boolean deleteRooms(int id, String location)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;
    
    /**
     * Delete a customer and associated reservations.
     *
     * @return Success
     */
    public boolean deleteCustomer(int id, int customerID)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;

    /**
     * Query the status of a flight.
     *
     * @return Number of empty seats
     */
    public int queryFlight(int id, int flightNumber)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;

    /**
     * Query the status of a car location.
     *
     * @return Number of available cars at this location
     */
    public int queryCars(int id, String location)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;

    /**
     * Query the status of a room location.
     *
     * @return Number of available rooms at this location
     */
    public int queryRooms(int id, String location)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;

    /**
     * Query the customer reservations.
     *
     * @return A formatted bill for the customer
     */
    public String queryCustomerInfo(int id, int customerID)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;
    
    /**
     * Query the status of a flight.
     *
     * @return Price of a seat in this flight
     */
    public int queryFlightPrice(int id, int flightNumber)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;

    /**
     * Query the status of a car location.
     *
     * @return Price of car
     */
    public int queryCarsPrice(int id, String location)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;

    /**
     * Query the status of a room location.
     *
     * @return Price of a room
     */
    public int queryRoomsPrice(int id, String location)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;

    /**
     * Reserve a seat on this flight.
     *
     * @return Success
     */
    public ReservableItem reserveFlight(int id, int customerID, int flightNumber)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;

    /**
     * Reserve a car at this location.
     *
     * @return Success
     */
    public ReservableItem reserveCar(int id, int customerID, String location)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;

    /**
     * Reserve a room at this location.
     *
     * @return Success
     */
    public ReservableItem reserveRoom(int id, int customerID, String location)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;

    /**
     * Reserve a bundle for the trip.
     *
     * @return Success
     */
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room)
            throws RemoteException, InvalidTransactionException, TransactionAbortedException;

    /**
     * Convenience for probing the resource manager.
     *
     * @return Name
     */
    public String getName()
        throws RemoteException;


    public String updateCustomerOrItem(int xid, String key, RMItem item) throws RemoteException, InvalidTransactionException;

    public RMItem readDataWrapper(int xid, String key) throws RemoteException, InvalidTransactionException;

    public String summary(int xid) throws RemoteException;

    /**
     * remove the transaction back to the inventory
     *
     * @return
     */
    public boolean revertReservation(int xid, int customerID, String key, int count)
            throws RemoteException,TransactionAbortedException, InvalidTransactionException;


    /**
     * this method would start the transaction
     *
     * @return the xid
     */
    public int start()
            throws RemoteException;

    /**
     * this method would commit the transaction indicated by the xid
     *
     * @return true if successful
     */
    public boolean commit(int transactionId)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException;

    /**
     * this would abort the indicated transaction
     *
     */
    public void abort(int transactionId)
            throws RemoteException, InvalidTransactionException;

    /**
     * shutdown all the servers
     *
     * @return true if successful
     */
    public boolean shutdown()
            throws RemoteException;


    /**
     * add this transaction to a transaction manager
     *
     */
    public void startTransaction(int xid)
            throws RemoteException;

    public int getPrice(int transactionID, String key, int amount )
            throws InvalidTransactionException, RemoteException;

}
