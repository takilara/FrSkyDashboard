package biz.onomato.frskydash;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.TabActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.UUID;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;




public class Frskydash extends TabActivity {
	
	private static final String TAG = "Tab Host"; 
    //MyApp globals;
	private FrSkyServer server;
	
    private static final boolean D = true;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    //Well known SPP UUID (will *probably* map to RFCOMM channel 1 (default) if not in use);
    //see comments in onResume().
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
    public static final String DEVICE_NAME = "device_name";
    private String mConnectedDeviceName = null;
    public static final String TOAST = "toast";
    private static BluetoothSerialService mSerialService = null;
    
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    
    private Menu _menu;
    

    private static String address = "00:21:86:CB:E7:46"; //<== hardcode your robot (server) MAC address here...
    //00:19:5D:EE:39:BA	-	FrSky1
    //00:21:86:CB:E7:46	-	NOR-654824J-1
    
    
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"onCreate");
        setContentView(R.layout.main);
        
        doBindService();
        
        // Do any globals
        //globals = ((MyApp)getApplicationContext());
        
        //Start the server service
        this.startService(new Intent().setClass(this, FrSkyServer.class));
        
        //globals.createChannel("AD1", "Main cell voltage", 0, (float) 0.5, "V","Volt");
        
        Resources res = getResources(); // Resource object to get Drawables
        TabHost tabHost = getTabHost();  // The activity TabHost
        TabHost.TabSpec spec;  // Resusable TabSpec for each tab
        Intent intent;  // Reusable Intent for each tab

        // Create an Intent to launch an Activity for the tab (to be reused)
        intent = new Intent().setClass(this, DashboardActivity.class);

        // Initialize a TabSpec for each tab and add it to the TabHost
        spec = tabHost.newTabSpec("dashboard").setIndicator("Dashboard",
                          //res.getDrawable(R.drawable.ic_tab_artists))
        			   res.getDrawable(R.drawable.icon))
                      .setContent(intent);
        tabHost.addTab(spec);

        // ModuleSettings
        intent = new Intent().setClass(this, ModuleSettingsActivity.class);
        spec = tabHost.newTabSpec("modulesettings").setIndicator("Module Settings",
                          res.getDrawable(R.drawable.icon))
                      .setContent(intent);
        tabHost.addTab(spec);

        // Channel config
        
        intent = new Intent().setClass(this, ChannelConfigActivity.class);
        spec = tabHost.newTabSpec("channelconfig").setIndicator("Channel Config",
                          res.getDrawable(R.drawable.icon))
                      .setContent(intent);
        tabHost.addTab(spec);

        // Application settings
        intent = new Intent().setClass(this, ModuleSettingsActivity.class);
        spec = tabHost.newTabSpec("applicationsettings").setIndicator("Application Settings",
                          res.getDrawable(R.drawable.icon))
                      .setContent(intent);
        tabHost.addTab(spec);
        
        // Simulator
        intent = new Intent().setClass(this, SimulatorActivity.class);
        spec = tabHost.newTabSpec("simulator").setIndicator("Simulator",
                          res.getDrawable(R.drawable.icon))
                      .setContent(intent);
        tabHost.addTab(spec);
        
        
        tabHost.setCurrentTab(0);
        
        
        Log.i(TAG,"Check for BT");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        	Log.i(TAG,"Device does not support Bluetooth");
        	// Disable all BT related menu items
        	// Display message stating only sim is available
        	notifyBtNotEnabled();
        }
        
        // popup to enable BT if not enabled
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else
        {
        	//MenuItem tItem = (MenuItem)  _menu.findItem(R.id.connect_bluetooth);
        	//tItem.setEnabled(true);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	Log.i(TAG,"Create Menu");
    	super.onCreateOptionsMenu(menu);
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.menu, menu);
    	
    	return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
    	super.onPrepareOptionsMenu(menu);
    	MenuItem tConItem = (MenuItem)  menu.findItem(R.id.connect_bluetooth);
    	MenuItem tDisConItem = (MenuItem)  menu.findItem(R.id.disconnect_bluetooth);
    	if (!mBluetoothAdapter.isEnabled()) {
    		tConItem.setEnabled(false);
    		tDisConItem.setEnabled(false);
    	}
    	else
    	{
    		tConItem.setEnabled(true);
    		tDisConItem.setEnabled(true);
    	}
    	
    	if(server.getConnectionState()==BluetoothSerialService.STATE_NONE)
    	{
    		tConItem.setVisible(true);
    		tDisConItem.setVisible(false);
    	}
    	else
    	{
    		tConItem.setVisible(false);
    		tDisConItem.setVisible(true);
    	}
    		
    	
		return true;
    }
	
    public void notifyBtNotEnabled()
    {
    	Toast.makeText(this, "Bluetooth not enabled, only simulations are available", Toast.LENGTH_LONG).show();
    	//MenuItem tItem = (MenuItem)  _menu.findItem(R.id.connect_bluetooth);
    	//tItem.setEnabled(false);
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        
        case REQUEST_CONNECT_DEVICE:

            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                //mSerialService.connect(device);         
                
                // pass responsibility to the server
                server.connect(device);
            }
            break;

        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "BT now enabled");
                //MenuItem tItem = (MenuItem)  findViewById(R.id.connect_bluetooth);
                
    //            finishDialogNoBluetooth();                
            }
            else
            {
            	Log.d(TAG,"BT not enabled");
            	// Disable all BT related menu items
            	// Display message stating only sim is available
            	notifyBtNotEnabled();
            }
        }
    } 
    
    


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	Log.i(TAG,"User has clicked something");
    	switch(item.getItemId()) 
    	{
    		case R.id.settings:
    			Log.i(TAG,"User clicked on Settings");
    			//Toast.makeText(this, "User clicked on Settings", Toast.LENGTH_LONG).show();
    			break;
    		
    		case R.id.connect_bluetooth:
    			if (server.getConnectionState() == BluetoothSerialService.STATE_NONE) {
            		// Launch the DeviceListActivity to see devices and do scan
            		Intent serverIntent = new Intent(this, DeviceListActivity.class);
            		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            	}
            	else
                	if (server.getConnectionState() == BluetoothSerialService.STATE_CONNECTED) {
                		// Connected, reconnect
                		server.reconnectBt();
                		//mSerialService.stop();
    		    		//mSerialService.start();
                	}
                return true;
    		case R.id.disconnect_bluetooth:
    			server.disconnect();
    			break;
             
    			
    	}
    	return true;
    	
    }
    
    
    //public int getConnectionState() {
//		return mSerialService.getState();
//	}
    
    void doBindService() {
    	//bindService(new Intent(this, FrSkyServer.class), mConnection, Context.BIND_AUTO_CREATE);
		Log.i(TAG,"Start the server service if it is not already started");
		startService(new Intent(this, FrSkyServer.class));
		Log.i(TAG,"Try to bind to the service");
		getApplicationContext().bindService(new Intent(this, FrSkyServer.class), mConnection,0);
		//bindService(new Intent(this, FrSkyServer.class), mConnection, Context.BIND_AUTO_CREATE);
    }
    
    void doUnbindService() {
            if (server != null) {
            // Detach our existing connection.
	        	try {
	        		unbindService(mConnection);
	        	}
	        	catch (Exception e)
	        	{}
        }
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			server = ((FrSkyServer.MyBinder) binder).getService();
			Log.i(TAG,"Bound to Service");
			
		}

		public void onServiceDisconnected(ComponentName className) {
			server = null;
		}
	};
    
    @Override
    public void onBackPressed() {
    	Log.i(TAG,"Back pressed");
    	//globals.die();
    	super.onBackPressed();
    	return;
    }
    
    public void onDestroy(){
    	super.onDestroy();
    	//mTts.stop();
    	Log.i(TAG,"onDestroy");
    }
    
    public void onPause(){
    	super.onPause();
    	//mTts.stop();
    	Log.i(TAG,"onPause");
    }
    
    public void onResume(){
    	super.onResume();
    	//mTts.stop();
    	Log.i(TAG,"onResume");
    }
    
    public void onStop(){
    	super.onStop();
    	//mTts.stop();
    	Log.i(TAG,"onStop");
    }

    
    
        
   
    
    
    
}


