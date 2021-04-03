package Server.TCP;

import Server.Common.ResourceManager;
import Server.Common.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class TCPResourceManager extends ResourceManager {

    private static TCPResourceManager manager = null;
    private ServerSocket serverSocket;
    private static String hostname = "localhost";
    private static int port = 70000;

    public TCPResourceManager(String p_name)
    {
        super(p_name);
    }

    public static void main(String[] args) {
        String name = null;
        // the server port number would be fixed
        // Room - 60208
        // Car - 60308
        // Flight - 60408
        try {
            if (args.length > 0) {
                name = args[0];
                if (name.equals("Rooms")){
                    port = 61208;
                }
                if (name.equals("Cars")){
                    port = 61308;
                }
                if (name.equals("Flights")){
                    port = 61408;
                }
            }

            // safety check before launching every thing
            if (port > 69999){
                System.out.println("Please enter the correct names");
                System.out.println("The correct format should be: Rooms, Cars, or Flights");
                System.exit(1);
                return;
            }

            if (args.length > 1) {
                hostname = args[0];
            }
            manager = new TCPResourceManager(name);

            // add some shutdown hook to ensure every port is closed
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    manager.stop();
                }
            });
            System.out.println("The " + name + "Manager is running now!");
            System.out.println("Starting '" + hostname + ":" + port + "'");
            manager.start(port);
        } catch(Exception e) {
            System.out.println((char)27 + "[31;1mResource Manager exception: " + (char)27 + e.toString());
            System.exit(1);
        }
    }

    // The is start method to create a new socket with specific port number
    // and continue listening on this port
    private void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Listening on port: " + port);
            while (true){
                new TCPServerHandler(serverSocket.accept()).start();
            }
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            stop();
        }
    }

    // Method for close this socket
    public void stop() {
        try {
            serverSocket.close();
            System.out.println("'" + this.getName() + ":" + port + "' Server Socket closed");
        }
        catch(IOException e) {
            System.err.println((char)27 + "[31;1mResource Manager exception: " + (char)27 + e.toString());
        }
    }

    // This is handler thread
    // It uses utils class method parse to get the input
    // and let
    private static class TCPServerHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public TCPServerHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                String inputLine = takeInput();
                Vector<String> parsedCommand = Utils.parse(inputLine);
                if (parsedCommand == null) {
                    out.println("");
                    close();
                    System.out.println("Invalid Input!");
                    return;
                }
                String result = manager.execute(parsedCommand);
                out.println(result);
                close();
            } catch(IOException e) {
                System.err.println((char)27 + "[31;1mResource Manager exception: " + (char)27 + "[0m" + e.getLocalizedMessage());
            }
        }

        public String takeInput() throws IOException{
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String input = in.readLine();
            return input;
        }

        // Close client socket
        public void close() throws IOException{
            in.close();
            out.close();
            clientSocket.close();
        }
    }
}
