package Client;

import java.io.*;
import java.net.Socket;

public class TCPClient extends Client {

    // some parameters globally
    private static String serverHostName = "localhost";
    private static int serverPort = 61108;

    private PrintWriter out;
    private BufferedReader in;
    private Socket clientSocket;

    private boolean hasPromptedConncet = false;

    // those are from server side
    private String host;
    private int port;

    public TCPClient(String addr, int port) throws IOException
    {
        this.host = addr;
        this.port = port;
        this.connectServer();
    }

    public static void main(String[] args) {
        try {
            // input is optional
            if (args.length > 0) {
                serverHostName = args[0];
            }

            // instantiate a concrete client instance
            TCPClient client = new TCPClient(serverHostName, serverPort);
            client.start();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    client.shutdown();
                }
            });
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // This method is for closing client
    public void shutdown(){
        try{
            in.close();
            out.close();
            clientSocket.close();
        }catch (Exception e){
            System.out.println("Error on shutting down the client!!!");
        }
    }

    @Override
    public void connectServer() {
        boolean hasPrompted = false;
        // try to connect to the server based on IP
        while (true){
            try {
                clientSocket = new Socket(host, port);
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                // sanity check for connection
                if (!hasPromptedConncet){
                    System.out.println("Connected to the middleware: " + this.host + "  with port number: " + this.port);
                    hasPromptedConncet = true;
                }
                // get out of the loop
                break;
            } catch (IOException e) {
                if (!hasPrompted){
                   System.out.println("Waiting for the middleware...");
                }
                hasPrompted = true;
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean sendToServer(String args, String s, String f, ResponseType type) {
        try {
            String feedback  = sendMsg(args);
            System.out.println("The response from the server is: \n");
            //System.out.println(feedback);
            //System.out.println(feedback.length());
            if (feedback.length() == 0){
                return false;
            }

            // determine the type of the response
            // and provide the corresponding feedback
            switch (type){
                case BOOLEAN:
                    if (Client.toBoolean(feedback)){
                        System.out.println(s);
                        return true;
                    }
                    break;
                case INTEGER:
                    System.out.println(s + Client.toInt(feedback));
                    return true;
                case STRING:
                    System.out.println(s + feedback);
                    return true;
            }

            System.out.println(f);
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    // This method is to read feedback line by line
    public String readFeedback() throws IOException{
        String feedback = "";
        String inputLine;
        while ((inputLine = in.readLine()) != null){
            if (feedback.length() == 0){
                feedback += inputLine;
            }else {
                feedback += "\n" + inputLine;
            }
        }
        return feedback;
    }

    public String sendMsg(String msg) throws IOException {
        out.println(msg);
        // initialize an empty string at first
        String feedback = readFeedback();
        connectServer();
        return feedback;
    }
}
