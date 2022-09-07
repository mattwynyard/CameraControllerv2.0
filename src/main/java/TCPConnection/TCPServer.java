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
    private boolean hasPhone;
    private boolean hasMap;
    private boolean phoneConnected = false;
    private boolean mapConnected = false;

    public TCPServer(int port, boolean hasPhone) {
        try {
            server = new ServerSocket(port, 0, InetAddress.getByName(null));
            clients = new ArrayList<Socket>();
            this.hasPhone = hasPhone;
        } catch (IOException e) {
            System.out.println("Error: " + e);
            return;
        }
        start();
    }

    //called from SPPClient before thread start
    public void setBluetoothClient(SPPClient client) {
        mPhoneClient = client;
    }

    public void start() {
        try {
                Socket client = server.accept();
                clients.add(client);
                System.out.println("Client connected: " + client.toString());
                System.out.println("Clients connected: " + clients.size());
                accessWriter = new PrintWriter(client.getOutputStream()); //access
                mReadAccessThread = new Thread(readFromAccess);
                mReadAccessThread.setPriority(Thread.MAX_PRIORITY);
                mReadAccessThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startMap() {
        try {
            System.out.println("Opening map port");
            Socket client = server.accept();
            this.hasMap = true;
            clients.add(client);
            System.out.println("Client connected: " + client.toString());
            System.out.println("Clients connected: " + clients.size());
            mapWriter = new PrintWriter(client.getOutputStream()); //map
            mReadMapThread = new Thread(readFromMap);
            if (!phoneConnected) mReadMapThread.setPriority(Thread.MAX_PRIORITY);
            mReadMapThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Sends a message to the client, in this case VBA via C# .dll
     *
     * @param message - the message to be sent.
     */
    public synchronized void sendDataDB(String message) {
        System.out.println(message);
        accessWriter.print(message);
        accessWriter.flush();
    }

    public void sendDataMap(String message) {
        System.out.println(message);
        mapWriter.print(message);
        mapWriter.flush();
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
                phoneConnected = true;
                while ((length = accessReader.read(buffer)) != -1) {
                    String line = new String(buffer, 0, length);
                    if (line.equals("Start")) {
                        //if (this.hasPhone){
                            sendDataAndroid(line);
                        //}
                    } else if (line.equals("Stop")) {
                        //if (this.phone) {
                            sendDataAndroid(line);
                        //}
                    } else if (line.contains("Time")){
                        //if (this.phone) {
                            sendDataAndroid(line);
                        //}
                    } else {
                        System.out.println(line);
                    }
                }
                accessReader.close();
            }
            catch (IOException err) {
                err.printStackTrace();
                phoneConnected = false;
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
            byte[] buffer = new byte[4 * 1024];
            try {
                mapReader = clients.get(1).getInputStream();
                mapConnected = true;
                while ((length = mapReader.read(buffer)) != -1) {
                    String line = new String(buffer, 0, length);
                    sendDataDB((line));
                }
                mapReader.close();
            }
            catch (Exception err) {
                err.printStackTrace();
                mapConnected = false;
                System.out.println("Map Socket Shutdown");
                clients.remove(1);
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    err.printStackTrace();
                }
                //startMap();
            }
        }
    };
}

