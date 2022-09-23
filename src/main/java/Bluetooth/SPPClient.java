/**
 * Copyright 2018 - Onsite Developments
 * @author Matt Wynyard November 2018
 * @version 0.2
 */

package Bluetooth;

import java.io.*;
import java.math.BigInteger;
import java.util.Arrays;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import TCPConnection.CameraApp;
import TCPConnection.TCPServer;


public class SPPClient extends Thread {

    private boolean connected = false;
    private StreamConnection mStreamConnection;
    private OutputStream out; //Android out
    private InputStream in; //Android in
    private PrintWriter writer; //Android writer
    //private BufferedReader reader; //Android reader
    public TCPServer mTCP;
    private Thread mReadThread;

    public SPPClient(String connectionURL, TCPServer server) {
        this.mTCP = server;
        try {
            mStreamConnection = (StreamConnection) Connector.open(connectionURL);
            connected = true;
        } catch (IOException e) {
            CameraApp.logError(e.toString());
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void run() {
        if (connected) {
            System.out.println("Connection to Android succesful...");
        }
        try {
            out = mStreamConnection.openOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(out));
            mTCP.setBluetoothClient(this);
            mReadThread = new Thread(readFromAndroid);
            mReadThread.setPriority(Thread.MAX_PRIORITY);
            mReadThread.start();
        } catch (Exception e) {
            CameraApp.logError(e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Called from shutdown hookup to fail gracefully
     */
    public void closeAll() {
        try {
            out.close();
            in.close();
            writer = null;
            //reader = null;
            mStreamConnection = null;
            mReadThread = null;
        } catch (IOException e) {
            CameraApp.logError(e.toString());
            e.printStackTrace();
        }
    }

//    public static void executeOnNewThread(final Runnable runnable) {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                runnable.run();
//            }
//        }).start();
//    }

    public byte[] clearBuffer() {
        return new byte[1024];
    }

//    private String decodeIntToString(byte[] buffer, int offset) {
//        ByteArrayOutputStream temp = new ByteArrayOutputStream();
//        temp.write(buffer, offset, 4);
//        int value = new BigInteger(temp.toByteArray()).intValue();
//        try {
//            temp.close();
//        }catch (IOException e) {
//            e.printStackTrace();
//        }
//        return String.valueOf(value);
//    }

    private int decodeInteger(byte[] buffer, int offset) {
        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        temp.write(buffer, offset, 4);
        int value = new BigInteger(temp.toByteArray()).intValue();
        try {
            temp.close();
        } catch (IOException e) {
            e.printStackTrace();
            CameraApp.logError(e.toString());
        }
        return value;
    }

//    private String decodeString(byte[] buffer, int offset, int length) {
//        String message = null;
//        ByteArrayOutputStream temp = new ByteArrayOutputStream();
//        temp.write(buffer, offset, length);
//        try {
//            message = new String(temp.toByteArray(), "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        try {
//            temp.close();
//        }catch (IOException e) {
//            e.printStackTrace();
//        }
//        return message;
//    }

    /**
     * Sends command to Android phone via bluetooth connection
     *
     * @param command - the command to send
     */
    public void sendCommand(String command) {
        writer.println(command);
        writer.flush();
    }

    private final Runnable readFromAndroid = new Runnable() {
        @Override
        public void run() {
        System.out.println("Reading From Android");
            try {
                in = mStreamConnection.openInputStream();
                byte[] buffer = new byte[1024];
                ByteArrayOutputStream mMessageOut = new ByteArrayOutputStream();
                ByteArrayOutputStream mPhotoOut = new ByteArrayOutputStream();
                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                int len;
                String recording;
                int battery;
                int error;
                String message;
                String photoName;
                boolean metadata = true;
                int payloadSize = 0;
                int messageSize;
                int photoSize;

                while ((len = in.read(buffer)) != -1) {
                    byteBuffer.write(buffer, 0, len); //read in total buffer from socket
                    if (metadata) {
                        payloadSize = decodeInteger(Arrays.copyOfRange(byteBuffer.toByteArray(), 0, 4), 0);
                    }
                    metadata = false;
                    if (byteBuffer.size() >= payloadSize) {
                        messageSize = decodeInteger(Arrays.copyOfRange(byteBuffer.toByteArray(), 4, 8), 0);
                        photoSize = decodeInteger(Arrays.copyOfRange(byteBuffer.toByteArray(), 8, 12), 0);
                        mMessageOut.write(byteBuffer.toByteArray(), 12, 1);
                        recording = mMessageOut.toString("UTF-8");
                        mMessageOut.reset();
                        if (recording.equals("N")) {
                            mTCP.sendDataDB("NOTRECORDING,");
                        } else {
                            mTCP.sendDataDB("RECORDING,");
                        }
                        mMessageOut.write(byteBuffer.toByteArray(), 13, 4);
                        battery = decodeInteger(mMessageOut.toByteArray(), 0);
                        mMessageOut.reset();
                        mTCP.sendDataDB("B:" + battery + ",");
                        mMessageOut.write(byteBuffer.toByteArray(), 17, 4);
                        error = decodeInteger(mMessageOut.toByteArray(), 0);
                        mMessageOut.reset();
                        mTCP.sendDataDB("E:" + error + ",");
                        mMessageOut.write(byteBuffer.toByteArray(), 21, messageSize);
                        message = new String(mMessageOut.toByteArray(), "UTF-8");
                        mMessageOut.reset();
                        mTCP.sendDataDB(message);
                        try {
                            if (byteBuffer.size() > payloadSize) {
                                if (photoSize != 0) {
                                    mPhotoOut.write(byteBuffer.toByteArray(), 21 + messageSize, photoSize);
                                    System.out.println("photoOut: "+ mPhotoOut.size());
                                    photoName = message.substring(26, 52); //handle millisecond
                                    final String photo = photoName;
                                    CameraApp.setIcon(mPhotoOut.toByteArray(), photo);
                                    //mTCP.sendDataDB(message);
                                }
                                ByteArrayOutputStream tempBuffer = new ByteArrayOutputStream();
                                tempBuffer.write(byteBuffer.toByteArray(), payloadSize, byteBuffer.size() - payloadSize);
                                byteBuffer.reset();
                                byteBuffer.write(tempBuffer.toByteArray());
                                metadata = true;
                                tempBuffer.close();
                            } else {
                                if (photoSize != 0) {
                                    mPhotoOut.write(byteBuffer.toByteArray(), 21 + messageSize, photoSize);
                                    System.out.println("photoOut: " + mPhotoOut.size());
                                    photoName = message.substring(26, 52); //handle millisecond
                                    final String photo = photoName;
                                    CameraApp.setIcon(mPhotoOut.toByteArray(), photo);
                                    //mTCP.sendDataDB(message);
                                    byteBuffer.reset();
                                    mPhotoOut.reset();
                                    metadata = true;
                                } else {
                                    byteBuffer.reset();
                                    metadata = true;
                                }
                            }
                            //mTCP.sendDataDB(message);
                        } catch (Exception e){
                            e.printStackTrace();
                            metadata = true;
                            byteBuffer.reset();
                            mPhotoOut.reset();
                            mMessageOut.reset();
                            buffer = clearBuffer();
                            mTCP.sendDataAndroid("Stop");
                            mTCP.sendDataDB("NOTRECORDING,");
                            CameraApp.logError(e.toString());
                        }
                    }
                }
                System.out.println(("error reading bluetooth stream"));
                CameraApp.logError("error reading bluetooth stream");
                if (mTCP != null) {
                    mTCP.sendDataAndroid("Stop");
                    mTCP.sendDataDB("NOTCONNECTED,");
                    mTCP.sendDataDB("NOTRECORDING,");
                }
                closeAll();
                System.exit(0);
            } catch (Exception e) {
                CameraApp.logError(e.toString());
                closeAll();
                System.exit(-1);
                e.printStackTrace();
            }
        }
    };
}




