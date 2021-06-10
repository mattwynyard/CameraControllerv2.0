package TCPConnection;

import java.io.*;
import java.net.*;

import Bluetooth.SPPClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class TCPServer {

    private InputStream in;
    private ServerSocket server;
    private Socket client;
    private Thread mReadThread;
    private PrintWriter writer; //writes to DB
    private SPPClient mPhoneClient;

    /**
     * TCPServer constructor which creates a new server socket on local host port 38200 then calls start() method
     */
    public TCPServer(SPPClient client) {
        try {
            // create the main server socket
            server = new ServerSocket(38200, 0, InetAddress.getByName(null));
            mPhoneClient = client;
        } catch (IOException e) {
            System.out.println("Error: " + e);
            return;
        }
        start();
    }

    public void start() {
        try {
            // wait for a connection
            client = server.accept();
            System.out.println("Succesful connection Access");
            writer = new PrintWriter(client.getOutputStream());

            mReadThread = new Thread(readFromClient);
            mReadThread.setPriority(Thread.MAX_PRIORITY);
            mReadThread.start();

        } catch (IOException e) {
            System.out.println("Error: " + e);
            sendDataDB(e.toString());
        }
    }

    /** Sends a message to the client, in this case VBA via C# .dll
     *
     * @param message - the message to be sent.
     */
    public void sendDataDB(String message) {
        System.out.println(message);
        writer.print(message);
        writer.flush();
    }

    public void sendDataAndroid(String message) {
        mPhoneClient.sendCommand(message);
    }

    /**
     * Called from shutdown hookup to fail gracefully
     */
    public void closeAll() {
        try {
            writer.close();
            in.close();
            server.close();
            client.close();
            writer = null;
            mReadThread = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * To run in the background,  reads incoming data
     * from the client - access
     */
    private Runnable readFromClient = new Runnable() {

        @Override
        public void run() {
            System.out.println("Read Thread listening");
            int length;
            byte[] buffer = new byte[1024];
            try {
                in = client.getInputStream();
                while ((length = in.read(buffer)) != -1) {
                    String line = new String(buffer, 0, length);
                    if (line.equals("Start")) {
                        sendDataAndroid(line);
                    } else if (line.equals("Stop")) {
                        sendDataAndroid(line);
                    } else if (line.contains("Time")){
                        sendDataAndroid(line);
                    }
                }
                in.close();
            }
            catch (IOException e1) {
                System.out.println("Socket Shutdown");
                System.exit(0);

            }
        }
    };
}

