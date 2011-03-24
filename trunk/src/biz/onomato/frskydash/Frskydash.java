package biz.onomato.frskydash;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;

import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TextView;
import java.util.Locale;




public class Frskydash extends TabActivity {
	
	private static final String TAG = "Tab Host"; 
    MyApp globals;
    
    private SimulatorService mSimService;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"onCreate");
        setContentView(R.layout.main);
        
        
        
        // Do any globals
        globals = ((MyApp)getApplicationContext());
        
        

        
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

    }
    
    @Override
    public void onBackPressed() {
    	Log.i(TAG,"Back pressed");
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


