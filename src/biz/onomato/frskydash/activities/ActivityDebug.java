package biz.onomato.frskydash.activities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.R;
import biz.onomato.frskydash.FrSkyServer.MyBinder;
import biz.onomato.frskydash.R.id;
import biz.onomato.frskydash.R.layout;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class ActivityDebug extends Activity implements OnClickListener {
	private static final String TAG = "DebugActivity";
	//private static final boolean DEBUG=true;
	private FrSkyServer server;
	
	private Button btnSchema,btnChannels,btnModels,btnExportDb;
	private ToggleButton btnWatchdogEnabled;
	private CheckBox chkHubEnabled,chkFilePlaybackEnabled;
	
	
	//chConf_edVoice
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		doBindService();
		
		// Show the form
		setContentView(R.layout.activity_debug);

		// Find all form elements
		btnExportDb			= (Button) findViewById(R.id.debug_btnExportDb);
		btnWatchdogEnabled  = (ToggleButton) findViewById(R.id.debug_watchdogEnabled);
		chkHubEnabled		= (CheckBox) findViewById(R.id.debug_chk_hubEnabled);
		chkFilePlaybackEnabled		= (CheckBox) findViewById(R.id.debug_chk_filePlaybackEnabled);
		
		
		
		btnExportDb.setOnClickListener(this);
		btnWatchdogEnabled.setOnClickListener(this);
		chkHubEnabled.setOnClickListener(this);
		chkFilePlaybackEnabled.setOnClickListener(this);
		
		// button for showing sensor hub data
		((Button) findViewById(R.id.button_show_hub_data))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						startActivity(new Intent(getApplicationContext(),
								ActivityHubData.class));
					}
				});
		
	
	}
	
	
	void doBindService() {
		Log.i(TAG,"Start the server service if it is not already started");
		startService(new Intent(this, FrSkyServer.class));
		Log.i(TAG,"Try to bind to the service");
		getApplicationContext().bindService(new Intent(this, FrSkyServer.class), mConnection,0);
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
	        // ADD stuff here
			btnWatchdogEnabled.setChecked(server.getWatchdogEnabled());
			chkHubEnabled.setChecked(server.getHubEnabled());
			chkFilePlaybackEnabled.setChecked(server.getFilePlaybackEnabled());
			
			
			//server.setWatchdogEnabled(btnWatchdogEnabled.isChecked());
			// Enable server buttons
		}

		public void onServiceDisconnected(ComponentName className) {
			server = null;
		}
	};

	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.debug_watchdogEnabled:
				if(server!=null)
				{
					server.setWatchdogEnabled(btnWatchdogEnabled.isChecked());
				}
				break;
			case R.id.debug_btnExportDb:
				Log.i(TAG,"Exporting DB to sdcard");
				new ExportDatabaseFileTask().execute();
				break;
			case R.id.debug_chk_hubEnabled:
				server.setHubEnabled(((CheckBox) v).isChecked());
				break;
			case R.id.debug_chk_filePlaybackEnabled:
				server.setFilePlaybackEnabled(((CheckBox) v).isChecked());
				break;
				
		}
	}
	
	
	private class ExportDatabaseFileTask extends AsyncTask<String, Void, Boolean> {

        // automatically done on worker thread (separate from UI thread)
        protected Boolean doInBackground(final String... args) {

           File dbFile =
                    new File(Environment.getDataDirectory() + "/data/biz.onomato.frskydash/databases/frsky");

           File exportDir = new File(Environment.getExternalStorageDirectory(), "");
           if (!exportDir.exists()) {
              exportDir.mkdirs();
           }
           File file = new File(exportDir, dbFile.getName()+".db");

           try {
              file.createNewFile();
              this.copyFile(dbFile, file);
              Log.d(TAG,"Database exported");
              //Toast.makeText(getApplicationContext(),"Database exported", Toast.LENGTH_LONG).show();
              return true;
           } catch (IOException e) {
              Log.e(TAG, e.getMessage(), e);
              return false;
           }
        }

        protected void onPostExecute(Boolean result)
        {
        	if(result)
        	{
        		Toast.makeText(getApplicationContext(),"Database exported", Toast.LENGTH_LONG).show();
        	}
        	else
        	{
        		Toast.makeText(getApplicationContext(),"Export Failed", Toast.LENGTH_LONG).show();
        	}
        }

        void copyFile(File src, File dst) throws IOException {
           FileChannel inChannel = new FileInputStream(src).getChannel();
           FileChannel outChannel = new FileOutputStream(dst).getChannel();
           try {
              inChannel.transferTo(0, inChannel.size(), outChannel);
           } finally {
              if (inChannel != null)
                 inChannel.close();
              if (outChannel != null)
                 outChannel.close();
           }
        }

     }
	
	
}
