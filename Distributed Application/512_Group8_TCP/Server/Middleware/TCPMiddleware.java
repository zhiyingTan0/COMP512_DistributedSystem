package Middleware;

import Server.Common.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class TCPMiddleware extends ResourceManager {

    // in our TCP design
    // same as RMI, the middleware itself is a resource manager for customers
    private static TCPMiddleware middleware = null;
    private ServerSocket serverSocket;
    private static int middleware_port = 61108;

    private TCPMiddlewareClient flightManager;
    private TCPMiddlewareClient carManager;
    private TCPMiddlewareClient roomManager;

    // some info about the servers
    private static String flight_rm_hostname = "localhost";
    private static int flight_rm_port = 61408;

    private static String room_rm_hostname = "localhost";
    private static int room_rm_port = 61208;

    private static String car_rm_hostname = "localhost";
    private static int car_rm_port = 61308;

    public TCPMiddleware(String p_name, String flightIP, int flightPort, String carIP, int carPort, String roomIP, int roomPort) {
        super(p_name);

        // set up three proxies to communicate with server
        // the only functions of these are just passing the commands
        // to different resource managers
        flightManager = new TCPMiddlewareClient(flightIP, flightPort);
        carManager = new TCPMiddlewareClient(carIP, carPort);
        roomManager = new TCPMiddlewareClient(roomIP, roomPort);
    }

    public static void main(String args[]) {
        // find the three resource managers before running its own server socket
        // get the server names, server ports from input
        // thus, the client would wait until all the servers are connected
        // the input order should be flight car room!!!!
        try {
            if (args.length > 2) {
                flight_rm_hostname = args[0];
                car_rm_hostname = args[1];
                room_rm_hostname = args[2];
            }

            middleware = new TCPMiddleware("TCPMiddleware",
                    flight_rm_hostname, flight_rm_port, car_rm_hostname, car_rm_port, room_rm_hostname, room_rm_port);
            middleware.start(middleware_port);

            // safety check to close everything before real stop
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    middleware.close();
                }
            });
        } catch (Exception e) {
            System.out.println("Some error happened on the middleware");
            middleware.close();
            System.exit(1);
        }
    }

    public void start(int server_port_num) {
        try {
            if (serverSocket == null) {
                // create a new one
                serverSocket = new ServerSocket(server_port_num);
                System.out.println("The middleware is listening on port: " + server_port_num + "...");
                // enter a loop to listen for the requests for the client
                // every time create a new thread to handle this
                while (true) {
                    new TCPMiddlewareHandler(serverSocket.accept()).start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // a general stop function to close everything
    public void close() {
        try {
            flightManager.stop();
            carManager.stop();
            roomManager.stop();
            serverSocket.close();
            System.out.println("The middleware has been shutdown.");
        } catch (Exception e) {
            System.out.println("Error on closing the client sockets");
        }
    }

    public String execute(Vector<String> parsedCommand) {
        return super.execute(parsedCommand);
    }

    @Override
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) {
        Trace.info("The command has been forward to flight resource manager");
        String command = String.format("AddFlight,%d,%d,%d,%d", id, flightNum, flightSeats, flightPrice);
        return convertToBoolean(deliverToRM(flightManager, command, 'B'));
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int price) {
        Trace.info("The command has been forward to car resource manager");
        String command = String.format("AddCars,%d,%s,%d,%d", id, location, numCars, price);
        return convertToBoolean(deliverToRM(carManager, command, 'B'));
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int price) {
        Trace.info("The command has been forward to room resource manager");
        String command = String.format("AddRooms,%d,%s,%d,%d", id, location, numRooms, price);
        return convertToBoolean(deliverToRM(roomManager, command, 'B'));
    }

    @Override
    public boolean deleteFlight(int id, int flightNum) {
        Trace.info("The command has been forward to flight resource manager");
        String command = String.format("DeleteFlight,%d,%d", id, flightNum);
        return convertToBoolean(deliverToRM(flightManager, command, 'B'));
    }

    @Override
    public boolean deleteCars(int id, String location) {
        Trace.info("The command has been forward to car resource manager");
        String command = String.format("DeleteCars,%d,%s", id, location);
        return convertToBoolean(deliverToRM(carManager, command, 'B'));
    }

    @Override
    public boolean deleteRooms(int id, String location) {
        Trace.info("The command has been forward to room resource manager");
        String command = String.format("DeleteRooms,%d,%s", id, location);
        return convertToBoolean(deliverToRM(roomManager, command, 'B'));
    }

    @Override
    public boolean deleteCustomer(int xid, int customerID) {
        Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
            return false;
        } else {
            synchronized (customer) {
                // Increase the reserved numbers of all reservable items which the customer reserved.
                for (String key : customer.getReservations().keySet()) {
                    // get the type of the reservation
                    String type = key.split("-")[0];
                    ReservedItem reserveditem = customer.getReservedItem(key);
                    String command = String.format("RemoveReservation,%d,%d,%s,%d", xid, customerID, reserveditem.getKey(), reserveditem.getCount());
                    try {
                        boolean isSuccessful =  deleteItemWrapper(type, command);
                        if (!isSuccessful){
                            Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") - has failed due to unexpected reserved item");
                        }
                    } catch (Exception e) {
                        return false;
                    }
                }
                // Remove the customer from the storage
                removeData(xid, customer.getKey());
                Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
            }
            return true;
        }
    }

    @Override
    public int queryFlight(int id, int flightNumber) {
        Trace.info("The command has been forward to flight resource manager");
        String command = String.format("QueryFlight,%d,%d", id, flightNumber);
        return convertToInt(deliverToRM(flightManager, command, 'I'));
    }

    @Override
    public int queryCars(int id, String location) {
        Trace.info("The command has been forward to car resource manager");
        String command = String.format("QueryCars,%d,%s", id, location);
        return convertToInt(deliverToRM(carManager, command, 'I'));
    }

    @Override
    public int queryRooms(int id, String location) {
        Trace.info("The command has been forward to room resource manager");
        String command = String.format("QueryRooms,%d,%s", id, location);
        return convertToInt(deliverToRM(roomManager, command, 'I'));
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber) {
        Trace.info("The command has been forward to flight resource manager");
        String command = String.format("QueryFlightPrice,%d,%d", id, flightNumber);
        return convertToInt(deliverToRM(flightManager, command, 'I'));
    }

    @Override
    public int queryCarsPrice(int id, String location) {
        Trace.info("The command has been forward to car resource manager");
        String command = String.format("QueryCarsPrice,%d,%s", id, location);
        return convertToInt(deliverToRM(carManager, command, 'I'));
    }

    @Override
    public int queryRoomsPrice(int id, String location) {
        Trace.info("The command has been forward to room resource manager");
        String command = String.format("QueryRoomsPrice,%d,%s", id, location);
        return convertToInt(deliverToRM(roomManager, command, 'I'));
    }

    @Override
    public boolean reserveFlight(int xid, int customerID, int flightNumber) {
        String key = Flight.getKey(flightNumber);
        Trace.info("RM::reserveFlight(" + xid + ", customer=" + customerID + ", " + key + ") called");
        // check whether or not the customer exists
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + flightNumber + ")  failed--customer doesn't exist");
            return false;
        }

        // the total order for requiring the lock fixed
        // thus, it would not result in deadlock
        synchronized (customer) {
            synchronized (flightManager) {
                int price = -1;

                try {
                    price = convertToInt(flightManager.sendCommand(String.format("getItemPrice,%d,%s,%d", xid, key, 1)));
                    if (price < 0) {
                        Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + flightNumber + ")  failed--item unavailable");
                        return false;
                    }
                    boolean isSuccessful = false;
                    String c = String.format("ReserveFlight,%d,%d,%d", xid, customerID, flightNumber);
                    isSuccessful = convertToBoolean(flightManager.sendCommand(c));
                    if (isSuccessful) {
                        customer.reserve(key, String.valueOf(flightNumber), price);
                        writeData(xid, customer.getKey(), customer);
                        return true;
                    }
                    Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + flightNumber + ")  failed--Could not reserve item");
                    return false;
                } catch (Exception e) {
                }
            }
        }
        return false;
    }

    @Override
    public boolean reserveCar(int xid, int customerID, String location) {
        String key = Car.getKey(location);
        Trace.info("RM::reserveCar(" + xid + ", customer=" + customerID + ", " + key + ") called");
        // check whether or not the customer exists
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }
        synchronized (customer) {
            synchronized (carManager) {
                int price = -1;
                try {
                    price = convertToInt(carManager.sendCommand(String.format("getItemPrice,%d,%s,%d", xid, key, 1)));
                    if (price < 0) {
                        Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--item unavailable");
                        return false;
                    }
                    boolean isSuccessful = false;
                    isSuccessful = convertToBoolean(carManager.sendCommand(String.format("ReserveCar,%d,%d,%s", xid, customerID, location)));
                    if (isSuccessful) {
                        customer.reserve(key, location, price);
                        writeData(xid, customer.getKey(), customer);
                        return true;
                    }
                    Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--Could not reserve item");
                    return false;
                } catch (Exception e) {
                }
            }
        }
        return false;
    }

    @Override
    public boolean reserveRoom(int xid, int customerID, String location) {
        String key = Room.getKey(location);
        Trace.info("RM::reserveRoom(" + xid + ", customer=" + customerID + ", " + key + ") called");
        // check whether or not the customer exists
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }
        synchronized (customer) {
            synchronized (roomManager) {
                int price = -1;
                try {
                    price = convertToInt(roomManager.sendCommand(String.format("getItemPrice,%d,%s,%d", xid, key, 1)));
                    if (price < 0) {
                        Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--item unavailable");
                        return false;
                    }
                    boolean isSuccessful = false;
                    isSuccessful = convertToBoolean(roomManager.sendCommand(String.format("ReserveRoom,%d,%d,%s", xid, customerID, location)));
                    if (isSuccessful) {
                        customer.reserve(key, location, price);
                        writeData(xid, customer.getKey(), customer);
                        return true;
                    }
                    Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + location + ")  failed--Could not reserve item");
                    return false;
                } catch (Exception e) {
                }
            }
        }
        return false;
    }

    // the concurrency of this method has been passed to each small sub-functions to avoid recursive lock
    public boolean bundle(int xid, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) {
        Trace.info("RM::bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ") called");
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }

        // first reserve the flight
        for (String flightNumber : flightNumbers) {
            int flightNum = Integer.parseInt(flightNumber);
            String key = Flight.getKey(flightNum);
            int price = -1;
            try {
                price = convertToInt(flightManager.sendCommand(String.format("getItemPrice,%d,%s,%d", xid, key, 1)));
                if (price < 0) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + flightNum + " doesn't have enough spots");
                    return false;
                }

                // now we are good to reserve the flight
                boolean hasSuccessful = reserveFlight(xid, customerID, flightNum);
                if (!hasSuccessful) {
                    Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--flight-" + flightNum + " has failed");
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // try to determine the car
        if (car) {
            boolean hasSuccessful = reserveCar(xid, customerID, location);
            if (!hasSuccessful) {
                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--car-" + location + " has failed");
                return false;
            }
        }

        // determine the room
        if (room) {
            boolean hasSuccessful = reserveRoom(xid, customerID, location);
            if (!hasSuccessful) {
                Trace.warn("RM:bundle(" + xid + ", customer=" + customerID + ", " + flightNumbers.toString() + ", " + location + ")  failed--room-" + location + " has failed");
                return false;
            }
        }
        Trace.info("RM:bundle() -- succeeded");
        return true;
    }


    public String analytics(int xid, int bound) {
        String result = "";
        try {
            result += flightManager.sendCommand(String.format("Analytics,%d,%d", xid, bound)) + "\n";
            result += carManager.sendCommand(String.format("Analytics,%d,%d", xid, bound)) + "\n";
            result += roomManager.sendCommand(String.format("Analytics,%d,%d", xid, bound)) + "\n";
        } catch (Exception e) {
            result = "There is an error in retrieving the analytics\n";
        }
        return result;
    }


    private boolean deleteItemWrapper(String type, String command) throws IOException {
        if (type.equals("flight")) {
            synchronized (flightManager) {
                flightManager.sendCommand(command);
            }
            return true;
        }
        if (type.equals("car")) {
            synchronized (carManager) {
                carManager.sendCommand(command);
            }
            return true;
        }
        if (type.equals("room")) {
            synchronized (roomManager) {
                roomManager.sendCommand(command);
            }
            return true;
        }
        return false;
    }

    private String deliverToRM(TCPMiddlewareClient proxy, String command, char type) {
        String result;
        try {
            synchronized (proxy) {
                result = proxy.sendCommand(command);
                if (result.equals("")) {
                    proxy.connectServer();
                    return proxy.sendCommand(command);
                }
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (type == 'B') return "false";
            if (type == 'I') return "-1";
            return "";
        }
    }

    public String getName() {
        return m_name;
    }

    private boolean convertToBoolean(String s) {
        try {
            return Boolean.parseBoolean(s);
        } catch (Exception e) {
            System.out.println("Error on converting the string to boolean!!!");
            return false;
        }
    }

    private int convertToInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            System.out.println("Error on converting the string to integer!!!");
            return -1;
        }
    }


    //-------------------------------------------------------------------------//
    // inner handler class
    // also a thread
    private class TCPMiddlewareHandler extends Thread {

        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public TCPMiddlewareHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            System.out.println("Accepting request!");
            // start to handle the request
            try {
                String input = takeInput();
                Vector<String> parsedCommand = Utils.parse(input);
                if (parsedCommand == null) {
                    out.println("");
                    System.out.println("Invalid Input");
                    close();
                    return;
                }
                String result = execute(parsedCommand);
                out.println(result);
                close();
            } catch (Exception e) {
            }
        }

        public String takeInput() throws IOException{
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String input = in.readLine();
            return input;
        }

        public void close() throws IOException{
            in.close();
            out.close();
            clientSocket.close();
        }
    }

    // Basically same as client's client
    // acts like a proxy
    // all this class has to do is to reach the server
    public static class TCPMiddlewareClient {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        // these two variable will change if input is provided
        private String host;
        private int port;

        private boolean hasPromptConnection = false;

        public TCPMiddlewareClient(String host, int port) {
            this.host = host;
            this.port = port;
            this.connectServer();
        }

        // this method is responsible for connecting the server with given port number passed in constructor.
        // and also forwarding the message
        public void connectServer() {
            boolean hasPrompted = true;
            try {
                while (true) {
                    try {
                        clientSocket = new Socket(host, port);
                        out = new PrintWriter(clientSocket.getOutputStream(), true);
                        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        if (!hasPromptConnection) {
                            System.out.println("Connected to host: " + this.host + " port:" + this.port + " :)");
                            hasPromptConnection = true;
                        }
                        break;
                    } catch (IOException e) {
                        if (hasPrompted) {
                            System.out.println("Waiting for host: " + this.host + " port:" + this.port + " to be connected");
                            hasPrompted = false;
                        }
                    }
                    Thread.sleep(500);
                }
            }
            catch (Exception e) {
                System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
                e.printStackTrace();
                System.exit(1);
            }

        }

        public String readFeedback() throws IOException{
            String input;
            String feedback = "";
            while ((input = in.readLine()) != null) {
                if (feedback.length() == 0)
                    feedback += input;
                else
                    feedback += "\n" + input;
            }
            return feedback;
        }

        public String sendCommand(String message) throws IOException {
            out.println(message);
            String feedback = readFeedback();
            connectServer();
            return feedback;
        }

        public void stop() {
            try {
                in.close();
                out.close();
                clientSocket.close();
            } catch(Exception e) {
                System.err.println((char)27 + "[31;1mUnable to quit TCPMiddlewareClient: " + (char)27 + "[0m" + e.toString());
            }
        }
    }
}