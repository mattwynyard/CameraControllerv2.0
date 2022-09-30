/**
 * Copyright 2018 - Onsite Developments
 * @author Matt Wynyard November 2018
 * @version 1.1
 */

package TCPConnection;


import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;
import Bluetooth.BluetoothManager;
/**
 * Main application class for CameraApp
 * Intialiases bluetooth adapter with camera id and handles the image build.
 *
 */
public class CameraApp {

    //private static int count = 0;
    private static String path;
    //private static final int CYCLE = 100; //milliseconds

    private static File errorFile;
    private static SimpleDateFormat sdfDate;

    public static void main(String[] args) {
        System.out.println("Connecting to phone: " + args[1]);
        System.out.println("Connecting to map: " + args[2]);
        System.out.println("Inspector:" + args[3] + '\n');
        System.out.println("Thumbnails saved to:" + args[4]);
        System.out.println("Log file at:" + args[0] + '\n');
        errorFile = new File(args[0]);
        sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        TCPServer mServer;
        path = args[4];
        if (args[1].equals("True")) {
            mServer = new TCPServer(38200, true);
            System.out.println("Initialising bluetooth connection...\n");
            BluetoothManager manager = new BluetoothManager(args[3], mServer);
            manager.start();
            if (args[2].equals("True")) {
                mServer.startMap();
            }
        } else {
            System.out.println("Starting server...\n");
            mServer = new TCPServer(38200, false);
            mServer.startMap();
        }
        //Runtime.getRuntime().addShutdownHook(new Thread(ShutdownHook));
        try {
            Object lock = new Object();
            synchronized (lock) {
                while (true) {
                    lock.wait();
                }
            }
        } catch (InterruptedException ex) {
            System.exit(-1);
        }
    }

    public static void logError(String err) {
        try {
            FileWriter fw = new FileWriter(errorFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);
            sdfDate.format(new Date());
            out.println(sdfDate.format(new Date()) + ": " + err);
            out.flush();
            fw.close();
            bw.close();
            out.close();

        } catch (IOException e) {
            System.out.println("error: " + e.getMessage());
        }
    }

    /**
     * Reads in byte array to input stream to build a buffered image, then writes jpeg image to disk
     *
     * @param bytes - byte array containing pixel data for the image
     * @param name  - the photo name
     */
    public static void setIcon(byte[] bytes, final String name) {
        try {
            long start = System.currentTimeMillis();
            InputStream in = new ByteArrayInputStream(bytes);
            BufferedImage bufferedImage = ImageIO.read(in);
            File imageFile = new File(path + name + ".jpg");
            if (bufferedImage != null) {
                ImageIO.write(bufferedImage, "jpg", imageFile);
            } else {
                System.out.println("error reading input stream");
                System.out.println(bytes.length);
            }
            in.close();
            long end = System.currentTimeMillis();
            System.out.println("jpeg save time: " + (end - start) + "ms");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

//private static Runnable ShutdownHook = new Runnable() {
//        @Override
//        public void run () {
//            if (mBluetooth.mClient != null) {
//                mServer.sendDataDB("NOTRECORDING,");
//                mServer.sendDataDB("NOTCONNECTED,");
//                mServer.sendDataDB("ERROR,");
//                mServer.closeAll();
//            }
//        }
//    };
