/**
 * Copyright 2018 - Onsite Developments
 * @author Matt Wynyard November 2018
 * @version 1.1
 */

package TCPConnection;


import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import javax.imageio.ImageIO;
import Bluetooth.BluetoothManager;
/**
 * Main application class for CameraApp
 * Intialiases bluetooth adapter with camera id and handles the image build.
 *
 */
public class CameraApp {

    private static BluetoothManager mBluetooth;
    private static TCPServer mServer;
    private static int count = 0;
    private static String path;

    private static Runnable ShutdownHook = new Runnable() {
        @Override
        public void run () {
            if (mBluetooth.mClient != null) {
                mServer.sendDataDB("NOTRECORDING,");
                mServer.sendDataDB("NOTCONNECTED,");
                mServer.sendDataDB("ERROR,");
                mServer.closeAll();
            }
        }
    };

    /**
     * Main program entry point - takes an arguement sent from access which is the camera id,
     * to setup bluetooth adapter name
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("Connecting to phone: " + args[0]);
        System.out.println("Connecting to map: " + args[1]);
        System.out.println("Inspector:" + args[2] + '\n');
        System.out.println("Thumbnails saved to:" + args[3] + '\n');
        //System.out.println("Connecting to port:" + args[4] + '\n');
        path = args[3];
        if (args[0].equals("True")) {
            mServer = new TCPServer(38200, true);
            System.out.println("Initialising bluetooth connection...\n");
            mBluetooth = new BluetoothManager(args[2], mServer);
            mBluetooth.start();
            if (args[1].equals("True")) {
                mServer.startMap();
            }
        } else {
            System.out.println("Starting server...\n");
            mServer = new TCPServer(38200, false);
            mServer.startMap();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(ShutdownHook));
        while(true) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Reads in byte array to input stream to build a buffered image, then writes jpeg image to disk
     * @param bytes - byte array containing pixel data for the image
     * @param name - the photo name
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
            System.out.println("jpeg save time: " + (end - start));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}