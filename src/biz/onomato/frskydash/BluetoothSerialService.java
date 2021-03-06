/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package biz.onomato.frskydash;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import biz.onomato.frskydash.activities.ActivityDashboard;
import biz.onomato.frskydash.util.Logger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for incoming
 * connections, a thread for connecting with a device, and a thread for
 * performing data transmissions when connected.
 * 
 * The data from this service is collected within a buffer and the buffer is
 * passed towards the {@link FrSkyServer} class to handle parsing into valid
 * frames.
 */
public class BluetoothSerialService {
    // Debugging
    private static final String TAG = "BluetoothReadService";
    


	private static final UUID SerialPortServiceClass_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private String _deviceName;
    
    //private EmulatorView mEmulatorView;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothSerialService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
        // mEmulatorView = emulatorView;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
    	Logger.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        //mHandler.obtainMessage(Frskydash.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        mHandler.obtainMessage(ActivityDashboard.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
    	Logger.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
        	mConnectThread.cancel(); 
        	mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
    	Logger.d(TAG, "connect to: " + device);
        _deviceName = device.getName();
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
    	Logger.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
        	mConnectThread.cancel(); 
        	mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        //Message msg = mHandler.obtainMessage(Frskydash.MESSAGE_DEVICE_NAME);
        Message msg = mHandler.obtainMessage(ActivityDashboard.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        //bundle.putString(Frskydash.DEVICE_NAME, device.getName());
        bundle.putString(ActivityDashboard.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
    	Logger.d(TAG, "stop threads");


        if (mConnectThread != null) {
        	mConnectThread.cancel(); 
        	mConnectThread = null;
        }

        if (mConnectedThread != null) {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
    	//Log.w(TAG,"Trying to write");
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }
    
    public void write(int[] out) {
    	byte[] out2 = new byte[out.length];
    	for(int n=0;n<out.length;n++)
    	{
    		out2[n] = (byte) out[n];
    	}
    	write(out2);
    }
    
    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(STATE_NONE);

        // Send a failure message back to the Activity
        //Message msg = mHandler.obtainMessage(Frskydash.MESSAGE_TOAST);
        Message msg = mHandler.obtainMessage(ActivityDashboard.MESSAGE_TOAST);
        
        Bundle bundle = new Bundle();
        //bundle.putString(Frskydash.TOAST, "Unable to connect device");
        bundle.putString(ActivityDashboard.TOAST, "Unable to connect to "+_deviceName);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        setState(STATE_NONE);

        // Send a failure message back to the Activity
        //Message msg = mHandler.obtainMessage(Frskydash.MESSAGE_TOAST);
        Message msg = mHandler.obtainMessage(ActivityDashboard.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        //bundle.putString(Frskydash.TOAST, "Device connection was lost");
        bundle.putString(ActivityDashboard.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        
     // Start the service over to restart listening mode
        BluetoothSerialService.this.start();
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
    	
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
//            	BluetoothDevice hxm = device;
//            	Method m;
//            	m = hxm.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
//            	tmp = (BluetoothSocket)m.invoke(hxm, Integer.valueOf(1));
            	// MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(SerialPortServiceClass_UUID);
            	
            	
            } catch (Exception e) {
            	Logger.e(TAG, "create() failed", e);
            } 

            mmSocket = tmp;
        }

        public void run() {
        	Logger.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
            	Logger.e(TAG, "unable to connect, exception", e);
                connectionFailed();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                	Logger.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                //BluetoothSerialService.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothSerialService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            	Logger.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;
        

        public ConnectedThread(BluetoothSocket socket) {
        	Logger.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            	Logger.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            
            
        }

        public void run() {
        	Logger.i(TAG, "BEGIN mConnectedThread");
            // the buffer we read in 
            byte[] buffer = new byte[1024];
            // the number of bytes that were read
            int bytes = 0;
            // a subset of the buffer holding only the valid bytes
            byte[] bufferCopy;         

            // Keep listening to the InputStream while connected
            while (true) {
                try {
					// Read from the InputStream
					bytes = mmInStream.read(buffer);
					// now copy only the elements that were filled
					bufferCopy = new byte[bytes];
					System.arraycopy(buffer, 0, bufferCopy, 0, bytes);
					//hcpl move this parsing to frskyserver instead
					mHandler.obtainMessage(
							ActivityDashboard.MESSAGE_READ,
							bytes, -1, bufferCopy).sendToTarget();
						
                } catch (IOException e) {
                    //Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
                catch (Exception e) {
                	Logger.e(TAG,e.toString());
                	break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                //mHandler.obtainMessage(Frskydash.MESSAGE_WRITE, buffer.length, -1, buffer).sendToTarget();
                mHandler.obtainMessage(ActivityDashboard.MESSAGE_WRITE, buffer.length, -1, buffer).sendToTarget();
            } catch (IOException e) {
            	Logger.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
        	  if(mmInStream!=null) {
        		  try {mmInStream.close();} catch (Exception e) {}
        		  mmInStream = null;
        	  }
        	  if(mmOutStream!=null) {
        		  try {mmOutStream.close();} catch (Exception e) {}
        		  mmOutStream = null;
        	  }
              
            try {
                mmSocket.close();
            } catch (IOException e) {
            	Logger.e(TAG, "close() of connect socket failed", e);
            }
            mmSocket = null;
        }
    }
}
