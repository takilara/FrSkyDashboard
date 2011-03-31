package biz.onomato.frskydash;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;

public class ActivityScanDevices extends Activity {
	 private static final String TAG = "ScanActivity";
	
	 private BluetoothDevice target;
	 private ArrayAdapter<String> mArrayAdapter;
	    //private final BluetoothSocket mmSocket;
	    //private final BluetoothDevice mmDevice;

	 
	 @Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_scandevices);
			
			
			BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			
			
			Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
			mArrayAdapter = new ArrayAdapter<String>(getApplicationContext(),R.id.scan_list_devices);
			// If there are paired devices
			if (pairedDevices.size() > 0) {
			    // Loop through paired devices
			    for (BluetoothDevice device : pairedDevices) {
			        // Add the name and address to an array adapter to show in a ListView
			        mArrayAdapter.add(device.getName() +  "\n" + device.getAddress());
			        //device.
			        Log.i(TAG,device.getAddress()+":"+device.getName());
			        if(device.getName()=="FrSky1")
			        {
			        	target=device;
			        }
			    }
			}
			
			BluetoothSocket tmp = null;
			//mmDevice = target;

			UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
			
			try {
	            // MY_UUID is the app's UUID string, also used by the server code
	            tmp = target.createRfcommSocketToServiceRecord(myUUID);
	        } catch (IOException e) { }
	        //mmSocket = tmp;
	        
	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            tmp.connect();
	        } catch (IOException connectException) {
	            // Unable to connect; close the socket and get out
	            try {
	                tmp.close();
	            } catch (IOException closeException) { }
	            return;
	        }

			
	 }
	
}
