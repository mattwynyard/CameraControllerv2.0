package TCPConnection;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import Bluetooth.SPPClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class TCPServer {

    private InputStream in;
    private ServerSocket server;
    private List<Socket> clients;
    private Thread mReadThread;
    private PrintWriter writer; //writes to DB
    private SPPClient mPhoneClient;
    private boolean phone = false;

    public TCPServer(int port, boolean phone) {
        try {
            server = new ServerSocket(port, 0, InetAddress.getByName(null));
            clients = new ArrayList<Socket>();
            this.phone = phone;
        } catch (IOException e) {
            System.out.println("Error: " + e);
            return;
        }
        start();
    }

    public void setBluetoothClient(SPPClient client) {
        mPhoneClient = client;
    }

    public void start() {
        try {
            // wait for a connection

            while (true) {
                Socket client = server.accept();
                writer = new PrintWriter(client.getOutputStream()); //access
                System.out.println("Client connected: " + client.toString());
                System.out.println("Local Address: " + client.getLocalAddress());
                System.out.println("Remote Address: " + client.getRemoteSocketAddress());
                System.out.println("Clients connected: " + clients.size());
                clients.add(client);
                if (clients.size() == 1){
                    mReadThread = new Thread(readFromAccess);
                    mReadThread.setPriority(Thread.MAX_PRIORITY);
                    mReadThread.start();
                } else {

                }


           }
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
            for (Socket client: clients) {
                client.close();
            }
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
    private Runnable readFromAccess = new Runnable() {
        @Override
        public void run() {
            System.out.println("Read Thread listening");
            int length;
            byte[] buffer = new byte[1024];
            try {
                in = clients.get(0).getInputStream();
                while ((length = in.read(buffer)) != -1) {
                    String line = new String(buffer, 0, length);
                    if (line.equals("Start")) {
                        sendDataAndroid(line);
                    } else if (line.equals("Stop")) {
                        sendDataAndroid(line);
                    } else if (line.contains("Time")){
                        sendDataAndroid(line);
                    } else {
                        System.out.println(line);
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

    private Runnable readFromMap = new Runnable() {
        @Override
        public void run() {
            System.out.println("Read Thread listening");
            int length;
            byte[] buffer = new byte[1024];
            try {
                in = clients.get(1).getInputStream();
                while ((length = in.read(buffer)) != -1) {
                    String line = new String(buffer, 0, length);
                    System.out.println(line);
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

