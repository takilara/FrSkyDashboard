/*
 * Copyright 2011-2013, Espen Solbu, Hans Cappelle
 * 
 * This file is part of FrSky Dashboard.
 *
 *  FrSky Dashboard is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FrSky Dashboard is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FrSky Dashboard.  If not, see <http://www.gnu.org/licenses/>.
 */

package biz.onomato.frskydash.activities;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import biz.onomato.frskydash.R;
import biz.onomato.frskydash.util.Logger;

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
			
			if(mBluetoothAdapter!=null)
			{
				Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
				mArrayAdapter = new ArrayAdapter<String>(getApplicationContext(),R.id.scan_list_devices);
				// If there are paired devices
				if (pairedDevices.size() > 0) {
				    // Loop through paired devices
				    for (BluetoothDevice device : pairedDevices) {
				        // Add the name and address to an array adapter to show in a ListView
				        //mArrayAdapter.add(device.getName() +  "\n" + device.getAddress());
				        //device.
				    	Logger.i(TAG,device.getAddress()+":"+device.getName());
				        if(device.getName()=="FrSky1")
				        {
				        	Toast.makeText(this, "MAC: "+device.getAddress(), Toast.LENGTH_LONG).show();
				        	target=device;
				        }
				    }
				}
			}
			else
			{
				Toast.makeText(this, "No bluetooth adapter", Toast.LENGTH_LONG).show();
			}

			
	 }
	
}
