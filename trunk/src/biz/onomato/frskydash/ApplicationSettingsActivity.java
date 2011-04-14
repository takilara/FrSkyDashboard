package biz.onomato.frskydash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class ApplicationSettingsActivity extends Activity implements OnClickListener {

	private static final String TAG = "Application-Settings";
	private FrSkyServer server;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_applicationsettings);
		
		View btnDeleteLogs = findViewById(R.id.btnDeleteLogs);
        btnDeleteLogs.setOnClickListener(this);
		
		
        
        doBindService();
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		//test.setText(oAd1.toString());
	}
	
	public void onClick(View v)
	{
		switch(v.getId())
		{
			case R.id.btnDeleteLogs:
				Log.i(TAG,"Delete all logs please");
				AlertDialog dialog = new AlertDialog.Builder(this).create();
				dialog.setTitle("Delete Logs");

				dialog.setMessage("Do you really want to delete all logs?");
				
				dialog.setButton(AlertDialog.BUTTON_POSITIVE,"Yes", new DialogInterface.OnClickListener() {

		            @Override
		            public void onClick(DialogInterface dialog, int which) {

		                //Stop the activity
		                server.deleteAllLogFiles();
		            }

		        });
		        dialog.setButton(AlertDialog.BUTTON_NEGATIVE,"No", new DialogInterface.OnClickListener() {

		            @Override
		            public void onClick(DialogInterface dialog, int which) {

		                //Stop the activity
		                Log.i(TAG,"Cancel Deletion");
		            }

		        });
		        dialog.show();

			break;
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
			//simFrame = server.sim.genFrame(ad1_raw,ad2_raw,rssirx_raw, rssitx_raw);
			 
			
			//Frame f = new Frame(simFrame);
			//Frame f = Frame.FrameFromAnalog(ad1_raw,ad2_raw,rssirx_raw, rssitx_raw);
			//Log.i(TAG,"FC (class ): "+Frame.frameToHuman(simFrame));
			
			
		}

		public void onServiceDisconnected(ComponentName className) {
			server = null;
		}
	};
	
	
}
