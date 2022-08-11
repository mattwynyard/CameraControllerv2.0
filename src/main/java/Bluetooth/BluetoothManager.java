/**
 * Copyright 2018 - Onsite Developments
 * @author Matt Wynyard November 2018
 * @version 0.2
 */

package Bluetooth;

import TCPConnection.TCPServer;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import java.io.IOException;
import java.util.Vector;
import javax.bluetooth.UUID;


public class BluetoothManager implements DiscoveryListener {
	
	private LocalDevice mLocalDevice;
	private RemoteDevice mRemoteDevice;
	private DiscoveryAgent mAgent;
	private String id;
	final Object lock = new Object();
	final Object enquiryLock = new Object();
	final Object searchLock = new Object();
    //vector containing the devices discovered, kept as Vector in case we need to a more remote devices
	private Vector<RemoteDevice> mDevices = new Vector();
	private String connectionURL = null;
	public SPPClient mClient;
	public TCPServer mServer;
	private long start;
	private long stop;

	public BluetoothManager(String id, TCPServer server) {
		try {	
			this.mLocalDevice = LocalDevice.getLocalDevice();
			mDevices.clear();
			this.id = id;
			this.mServer = server;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Main entry point for Bluetooth manager. Intialises the discovery agent and then searches for bluetooth devices
	 * Device discovery and connection is handled by the bluecove callbacks.
	 */
	public void start() {
		System.out.println("Local Bluetooth Address: " + mLocalDevice.getBluetoothAddress());
		System.out.println("Local Computer Name: " + mLocalDevice.getFriendlyName());
        start = System.currentTimeMillis();
		try {
			synchronized (enquiryLock) {
			mAgent = mLocalDevice.getDiscoveryAgent();
			try {
				mAgent.startInquiry(DiscoveryAgent.LIAC, this);
			} catch (BluetoothStateException e){
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
				enquiryLock.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		int deviceCount = mDevices.size();
		if (deviceCount <= 0) {
			System.out.println("No Devices Found.");
			mAgent.cancelInquiry(this);
		} else {
            System.out.println("Device count: " + deviceCount);
			for (int i = 0; i < deviceCount; i++) {
				mRemoteDevice = mDevices.elementAt(i);
				try {
					System.out.println((i + 1) + ". " + mRemoteDevice.getBluetoothAddress() +
							" (" + mRemoteDevice.getFriendlyName(true) + ")");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
		/**
	 * Called when a remote device OnSite_BLT_Adapter is found. Searches for service on that device to connect to.
	 * @param remoteDevice - the Onsite Bluetooth Adapter
	 * @param agent - local devices discovery agent
	 * @param client - this
	 */
	public void connect(RemoteDevice remoteDevice, DiscoveryAgent agent, BluetoothManager client) {
		UUID[] uuidSet = new UUID[1];
        uuidSet[0]=new UUID("0003000000001000800000805F9B34FB", false);
        int[] attrIds = { 0x0003 }; //RFCOMM
        System.out.println("\nSearching for service...");
        try {
        	synchronized(searchLock) {
        		agent.searchServices(attrIds, uuidSet, remoteDevice, client);
				searchLock.wait();
        	}
		} catch (BluetoothStateException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if(connectionURL == null){
			System.out.println("Device does not support Simple SPP Service.");

		}
	}

	//***BLUECOVE CALLBACKS
	/**
	 * This call back method will be called for each discovered bluetooth devices.
	 * Each device added to device vector.
	 * @param btDevice - The Remote Device discovered.
	 * @param cod - The class of device record. Contains information on the bluetooth device.
	 * 
	 */
	public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
        try {
            System.out.println("Device discovered: " + btDevice.getFriendlyName(false));
        } catch (IOException e) {
            e.printStackTrace();
        }
		try {
			if (btDevice.getFriendlyName(false).equals("OnSite_BLT_Adapter_" + id)) {
                System.out.println("Trusted: " + btDevice.isTrustedDevice());
                System.out.println("Authenticated: " + btDevice.isAuthenticated());
				mAgent.cancelInquiry(this);
                connect(btDevice, mAgent, this);
                mDevices.addElement(btDevice);
            }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This callback will be called when services found by DiscoveryListener during service search
	 * @param transID - the transaction ID of the service search that is posting the result.
	 * @param servRecord - a list of services found during the search request.
	 */
		public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
			synchronized (lock) {
				lock.notifyAll();
			}
			System.out.println("Service discovered");
			connectionURL = servRecord[0].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, true);
			System.out.println(connectionURL);
			//URL needed for connection to android bluetooth server
			mAgent.cancelServiceSearch(transID);
			//Creates client running on new thread on specified url
			mClient = new SPPClient(connectionURL, mServer);
			if (mClient.isConnected()) {
				stop = System.currentTimeMillis();
				System.out.println("Device discovery: " + ((stop - start)/ 1000) + " s");
				mClient.start();
			} else {
				System.out.println("Client failed to connect");
			}
		}

		/**
		 * Called when service search completed
		 * @param transID - the transaction ID identifying the request which initiated the service search
		 * @param respCode - the response code that indicates the status of the transaction
		 */
		public void serviceSearchCompleted(int transID, int respCode) {

			synchronized(searchLock) {
				searchLock.notifyAll();
			}
			switch (respCode) {
			case DiscoveryListener.SERVICE_SEARCH_COMPLETED:
				System.out.println("SERVICE_SEARCH_COMPLETED");
				break;
		
			case DiscoveryListener.SERVICE_SEARCH_TERMINATED:
				System.out.println("SERVICE_SEARCH_TERMINATED");
				break;
		
			case DiscoveryListener.SERVICE_SEARCH_ERROR:
				System.out.println("SERVICE_SEARCH_ERROR");
				break;
				
			case DiscoveryListener.SERVICE_SEARCH_NO_RECORDS:
				System.out.println("SERVICE_SEARCH_NO_RECORDS");
				break;
				
			case DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE:
				System.out.println("SERVICE_SEARCH_DEVICE_NOT_REACHABLE");
				break;
		
			default :
				System.out.println("Unknown Response Code");
				break;
			}
		}
	/**
	 * This callback method will be called when the device discovery is
	 * completed.
	 * @param discType integer value for discovery result.
	 */
	public void inquiryCompleted(int discType) {
		synchronized(enquiryLock){
			enquiryLock.notifyAll();
		}
		switch (discType) {
		case DiscoveryListener.INQUIRY_COMPLETED :
			System.out.println("INQUIRY_COMPLETED");
			break;
	
		case DiscoveryListener.INQUIRY_TERMINATED :
			System.out.println("INQUIRY_TERMINATED");
			break;
	
		case DiscoveryListener.INQUIRY_ERROR :
			System.out.println("INQUIRY_ERROR");
			break;
	
		default :
			System.out.println("Unknown Response Code");
			break;
		}
	}//end method
} //end class