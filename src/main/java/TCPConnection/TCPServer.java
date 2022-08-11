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

    private InputStream accessReader;
    private PrintWriter accessWriter; //writes to DB
    private InputStream mapReader;
    private PrintWriter mapWriter; //writes to DB
    private ServerSocket server;
    private List<Socket> clients;
    private Thread mReadAccessThread;
    private Thread mReadMapThread;
    private SPPClient mPhoneClient;
    private boolean phone;

    public TCPServer(int port, boolean phone) {
        try {
            server = new ServerSocket(port, 0, InetAddress.getByName(null));
            clients = new ArrayList<Socket>();
            this.phone = phone;
        } catch (IOException e) {
            System.out.println("Error: " + e);
            return;
        }
        if (phone) start();
    }

    //called from SPPClient before thread start
    public void setBluetoothClient(SPPClient client) {
        mPhoneClient = client;
    }

    public void start() {
        try {
                Socket client = server.accept();
                System.out.println("Client connected: " + client.toString());
                System.out.println("Local Address: " + client.getLocalAddress());
                System.out.println("Remote Address: " + client.getRemoteSocketAddress());
                clients.add(client);
                System.out.println("Clients connected: " + clients.size());
                accessWriter = new PrintWriter(client.getOutputStream()); //access
                mReadAccessThread = new Thread(readFromAccess);
                mReadAccessThread.setPriority(Thread.MAX_PRIORITY);
                mReadAccessThread.start();
        } catch (IOException e) {
            System.out.println("Error: " + e);
        }
    }

    public void startMap() {
        try {
            System.out.println("Opening map port");
            Socket client = server.accept();
            System.out.println("Client connected: " + client.toString());
            System.out.println("Local Address: " + client.getLocalAddress());
            System.out.println("Remote Address: " + client.getRemoteSocketAddress());
            clients.add(client);
            System.out.println("Clients connected: " + clients.size());
            mapWriter = new PrintWriter(client.getOutputStream()); //access
            mReadMapThread = new Thread(readFromMap);
            mReadMapThread.setPriority(Thread.MAX_PRIORITY);
            mReadMapThread.start();
        } catch (IOException e) {
            System.out.println("Error: " + e);
        }
    }

    /** Sends a message to the client, in this case VBA via C# .dll
     *
     * @param message - the message to be sent.
     */
    public void sendDataDB(String message) {
        System.out.println(message);
        accessWriter.print(message);
        accessWriter.flush();
    }

    public void sendDataAndroid(String message) {
        mPhoneClient.sendCommand(message);
    }

    /**
     * Called from shutdown hookup to fail gracefully
     */
    public void closeAll() {
        try {
            accessWriter.close();
            accessReader.close();
            mapWriter.close();
            mapReader.close();
            server.close();
            for (Socket client: clients) {
                client.close();
            }
            accessWriter = null;
            mReadAccessThread = null;
            mReadMapThread = null;
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
            System.out.println("Access Thread listening");
            int length;
            byte[] buffer = new byte[1024];
            try {
                accessReader = clients.get(0).getInputStream();
                while ((length = accessReader.read(buffer)) != -1) {
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
                accessReader.close();
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
            System.out.println("Map Thread listening");
            int length;
            byte[] buffer = new byte[1024];
            try {
                mapReader = clients.get(1).getInputStream();
                while ((length = mapReader.read(buffer)) != -1) {
                    String line = new String(buffer, 0, length);
                    sendDataDB((line));
                }
                mapReader.close();
            }
            catch (IOException e1) {
                System.out.println("Socket Shutdown");
                System.exit(0);
            }
        }
    };
}

