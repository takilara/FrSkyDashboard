package biz.onomato.frskydash;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.TabActivity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;

import android.os.Bundle;
import android.os.IBinder;
import android.widget.TabHost;
import android.widget.TextView;
import java.util.Locale;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;




public class Frskydash extends TabActivity {
	
	private static final String TAG = "Tab Host"; 
    //MyApp globals;
	private FrSkyServer server;
	private int REQUEST_ENABLE_BT;
    
    
    
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
        
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        	Log.i(TAG,"Device does not support Bluetooth");
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	super.onCreateOptionsMenu(menu);
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.menu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    	switch(item.getItemId()) 
    	{
    		case R.id.settings:
    			Log.i(TAG,"User clicked on Settings");
    			break;
    		case R.id.scan_bluetooth:
    			Log.i(TAG,"User clicked on Scan");
    			if (mBluetoothAdapter != null){ 
	    			if(!mBluetoothAdapter.isEnabled())
	    			{
	    				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	    			    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	    			}
	    			else
	    			{
	    				Intent intent = new Intent().setClass(getApplicationContext(), ActivityScanDevices.class);
	                	startActivity(intent);
	    			}
    			}
    			break;
    		case R.id.connect_bluetooth:
    			Log.i(TAG,"User clicked on Connect");
    			break;	
    			
    	}
    	return true;
    	
    }
    
    
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
        	Log.i(TAG,"Check for bt");
            if (resultCode == RESULT_OK) {
            	//if(server!=null)	server.createSpeaker();
            	Log.i(TAG,"BT now enabled");
                
            	Intent intent = new Intent().setClass(getApplicationContext(), ActivityScanDevices.class);
            	startActivity(intent);
                //sayHello();
            } else {
            	Log.i(TAG,"BT NOT enabled");
            }
        }
    }
    
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


