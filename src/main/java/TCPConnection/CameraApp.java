/**
 * Copyright 2018 - Onsite Developments
 * @author Matt Wynyard November 2018
 * @version 1.1
 */

package TCPConnection;


import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import Bluetooth.BluetoothManager;
import Bluetooth.SPPClient;
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
                mBluetooth.mClient.mTCP.sendDataDB("NOTRECORDING,");
                mBluetooth.mClient.mTCP.sendDataDB("NOTCONNECTED,");
                mBluetooth.mClient.mTCP.sendDataDB("ERROR,");
                mBluetooth.mClient.mTCP.closeAll();
                mBluetooth.mClient.closeAll();
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
        System.out.println("Inspector:" + args[1] + '\n');
        System.out.println("Thumbnails saved to:" + args[2] + '\n');
        path = args[2];
        if (args[0].equals("True")) {
            mServer = new TCPServer(38200, true);
            System.out.println("Initialising bluetooth connection...\n");
            mBluetooth = new BluetoothManager(args[1], mServer);
            mBluetooth.start();
            mServer.startMap();
        } else {
            System.out.println("Starting server...\n");
            mServer = new TCPServer(38200, false);
            mServer.startMap();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(ShutdownHook));
        while(true) {
            try {
                Thread.sleep(100);
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
                System.out.println("buffered image is null");
                System.out.println(bytes.length);
            }
            in.close();
            long end = System.currentTimeMillis();
            System.out.println("jpeg save time: " + (end - start));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}