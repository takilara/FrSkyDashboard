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
import java.util.Arrays;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothSerialService {
    // Debugging
    private static final String TAG = "BluetoothReadService";
    private static final boolean D = true;


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
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
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
        if (D) Log.d(TAG, "start");

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
        if (D) Log.d(TAG, "connect to: " + device);
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
        if (D) Log.d(TAG, "connected");

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
        Log.d(TAG, "stop threads");


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
                Log.e(TAG, "create() failed", e);
            } 

            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
            	Log.e(TAG, "unable to connect, exception", e);
                connectionFailed();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
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
                Log.e(TAG, "close() of connect socket failed", e);
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
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            
            
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            //List  b = new LinkedList();
            //ArrayList<Byte> b = new ArrayList<Byte>();
            //int ptr=0;
            //int endpos = 0;
            //int startpos = 0;
            int bytes = 0;
           
            //hcpl modified to read single bytes from buffer
            int b;
            // the frame we are going to send over once complete
            int[] frame = new int[Frame.SIZE_TELEMETRY_FRAME];
            // if we need to xor the current byte or not
            boolean xor = false;
            // current position in frame
            int currentFrameIndex = -1;            

            // Keep listening to the InputStream while connected
            while (true) {
                try {
					// Read from the InputStream
					bytes = mmInStream.read(buffer);
					// start parsing input stream byte per byte
					for( int i=0 ; i< bytes ; i++){
						// b = mmInStream.read();
						b = buffer[i];
						// handle byte stuffing first
						if (b == Frame.STUFFING_TELEMETRY_FRAME) {
							// indicate we need to xor the next one
							xor = true;
							// and drop this byte
							continue;
						}
						// we encountered a byte stuff indicator in previous
						// iteration so we need to XOR here to unstuff and make
						// sure to bypass the start/stop byte detection.
						if (xor) {
							// perform xor operation
							b ^= Frame.XOR_TELEMETRY_FRAME;
							// disable xor flag again for next iteration
							// wait to unset the xor operation flag since we'll
							// need it in next steps to skip the start/stop byte
							// detection
							// xor = false;
							Log.d(TAG, "XOR operation detected, unstuffed to "
									+ Integer.toHexString(b));
						} 
						// if we encounter a start byte we need to indicate
						// we're in a frame or if at the end handle the frame
						// and continue
						if (b == Frame.START_STOP_TELEMETRY_FRAME && !xor) {
							// if currentFrameIndex is not set we have to start
							// a new frame here
							if (currentFrameIndex < 0) {
								// init current frame index at beginning
								currentFrameIndex = 0;
								// and copy this first byte in the frame
								frame[currentFrameIndex++] = b;
							}
							// otherwise we were already collecting a frame so
							// this indicates we are at the end now. At this
							// point a frame is available that we can send over.
							else if (currentFrameIndex == Frame.SIZE_TELEMETRY_FRAME - 1) {
								// just complete the frame we were collecting
								frame[currentFrameIndex] = b;
								// this way the length is confirmed
								// Send the obtained bytes to the UI Activity
								mHandler.obtainMessage(
										ActivityDashboard.MESSAGE_READ,
										frame.length, -1, frame).sendToTarget();
								// once information is handled we can reset the
								// frame
								frame = new int[Frame.SIZE_TELEMETRY_FRAME];
								// we can already set this start byte to the
								// beginning of the frame here
								currentFrameIndex = -1;
							}
							// if for some reason we got 2 times a 0x7e byte
							// after each other or the size of the frame was
							// different we can't do anything with the previous
							// collected information. We can log a debug message
							// and drop the frame to start over again.
							else {
								// log debug info here
								Log.d(TAG,
										"Start/stop byte telemetry frame at wrong position: 0x"
												+ Integer.toHexString(b)
												+ " current frame so far: "
												+ Arrays.toString(frame)
												+ ". Frame was reset and this start/stop counted as start.");
								// reset frame and counts this start/stop bit as
								// beginning
								currentFrameIndex = 0;
								frame = new int[Frame.SIZE_TELEMETRY_FRAME];
								frame[currentFrameIndex++] = b;
							}
						}
						// otherwise we are handling a valid byte that has to be
						// put in the frame we are collecting. But only when we
						// are currently working on a frame!
						else if (currentFrameIndex >= 0
								&& currentFrameIndex < Frame.SIZE_TELEMETRY_FRAME - 1) {
							frame[currentFrameIndex++] = b;
						}
						// finally it's possible that we receive bytes without
						// being in a frame, just discard them for now. These
						// are probably from missing frames etc
						else {
							// log debug info here
							Log.d(TAG,
									"Received data while not in telemetry frame recording mode, dropped byte: 0x"
											+ Integer.toHexString(b));
						}
						//don't forget to unset the xor flag so we can continue normal byte operation on next iteration
						xor = false;
					}
					//hcpl oringinal logic
                    
					//Log.i(TAG,"Read "+bytes+" new bytes.");

                    //mEmulatorView.write(buffer, bytes);
                    // Send the obtained bytes to the UI Activity

                    // Fix buffer to Frame here
                    // for each byte in current buffer, copy to framebuffer.
                    //Log.i(TAG,"Writing bytes to framebuffer positions: "+ptr+"-"+(ptr+bytes));
//                    for(int n=0;n<bytes;n++)
//                    {
//                    	b.add((byte) buffer[n]);
//                    	//framebuffer[ptr]=buffer[n];
//                    	ptr++;
//                    }
//                    //Log.i(TAG,"b now at "+b.size()+" elements.");
//                    if(b.size()>=11)
//                    {
//                    	boolean containsFullFrame = true;	// assume a frame is in there
//                    	while(containsFullFrame)
//                    	{
//	                    	// find first 7e
//	                    	//Log.i(TAG,"possible complete frame");
//	                    	startpos = b.indexOf((byte) 0x7e);
//	                    	// need to check if next byte also is 0x7e..
//	                    	if(b.size()>startpos)
//	                    	{
//		                    	if(b.get(startpos+1)==0x7e)
//		                    	{
//		                    		startpos++;
//		                    	}
//	                    	}
//	                    	
//	                    	
//	                    	//Log.i(TAG,"Startpos: "+startpos);
//	                    	// find second 7e
//	                    	List<Byte> d = new ArrayList<Byte>();
//	                    	d = b.subList(startpos+1, b.size());
//	                    	//Log.i(TAG,d.toString());
//	                    	endpos = d.indexOf((byte) 0x7e)+startpos+1;
//	                    	//Log.i(TAG,"Endpos: "+endpos);
//	                    	if((startpos!=-1) && (endpos!=-1) && (endpos>startpos))
//	                    	{
//	                    		//Log.i(TAG,"We have complete frame:");
//	                    		List<Byte> e = new ArrayList<Byte>();
//		                    	e = b.subList(startpos, endpos+1);
//	                    		
//	                    		byte[] frame = new byte[e.size()];
//	                    		//Log.i(TAG,"Made a byte array");
//	                    		
//	                    		for (int n=0;n<frame.length;n++)
//	                    		{
//	                    			frame[n]=(byte) e.get(n);
//	                    		}
//	                    		//Log.i(TAG,"Removing items from b, old size:"+b.size());
//	                    		e.clear();
//	            
//	                    		//mHandler.obtainMessage(Frskydash.MESSAGE_READ, frame.length, -1, frame).sendToTarget();
//	                    		mHandler.obtainMessage(ActivityDashboard.MESSAGE_READ, frame.length, -1, frame).sendToTarget();
//	                    		
//	                    		//Log.i(TAG,"recheck after transmission");
//	                    		startpos = b.indexOf((byte) 0x7e);
//		                    	//Log.i(TAG,"Startpos: "+startpos);
//		                    	// find second 7e
//		                    	endpos = b.subList(startpos+1, b.size()).indexOf((byte) 0x7e)+1;
//		                    	//Log.i(TAG,"Endpos: "+endpos);
//		                    	if((startpos!=-1) && (endpos!=-1) && (startpos!=endpos) && (b.size()>=11))
//		                    	{
//		                    		containsFullFrame=true;
//		                    		//Log.i(TAG,"please repeat");
//		                    	}
//		                    	else
//		                    	{
//		                    		containsFullFrame=false;
//		                    	}
//	                    		
//	                    		
//	                    	}
//	                    	else
//	                    	{
//	                    		containsFullFrame=false;
//	                    		//Log.i(TAG, "not yet a complete frame (start,end,size): ("+startpos+","+endpos+","+b.size()+")");
//	                    	}
//                    	}
//                    			
//                    	
//                    	
//                    }
//
//                    String a = buffer.toString();
//                    a = "";
						
                } catch (IOException e) {
                    //Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
                catch (Exception e) {
                	Log.e(TAG,e.toString());
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
                Log.e(TAG, "Exception during write", e);
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
                Log.e(TAG, "close() of connect socket failed", e);
            }
            mmSocket = null;
        }
    }
}
