package biz.onomato.frskydash;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class ActivityModelConfig extends Activity implements OnClickListener {
	private static final String TAG = "ModelConfig";
	private static final boolean DEBUG=true;
	private FrSkyServer server;
	
	private int _modelId;
	
	private Button btnSave,btnAddChannel; 
	
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		doBindService();
	
		
		///TODO: Use intent to get initial Model object?
		Intent launcherIntent = getIntent();
		_modelId = launcherIntent.getIntExtra("modelId", -1);
//		Log.d(TAG, "Channel Id is: "+_channelId);
		
		if(_modelId==-1)
		{
			if(DEBUG) Log.d(TAG,"Configure new Model object");
		}
		else
		{
			if(DEBUG) Log.d(TAG,"Configure existing Model object (id:"+_modelId+")");
		}
		
		// Show the form
		setContentView(R.layout.activity_modelconfig);

		// Find all form elements
		btnSave				= (Button) findViewById(R.id.modConf_btnSave);
		btnAddChannel		= (Button) findViewById(R.id.modConf_btnAddChannel);
//		tvName 				= (TextView) findViewById(R.id.chConf_tvName);
//		edDesc 				= (EditText) findViewById(R.id.chConf_edDescription);
//		edUnit 				= (EditText) findViewById(R.id.chConf_edUnit);
//		edShortUnit			= (EditText) findViewById(R.id.chConf_edShortUnit);
//		edOffset 			= (EditText) findViewById(R.id.chConf_edOffset);
//		edFactor 			= (EditText) findViewById(R.id.chConf_edFactor);
//		edPrecision 		= (EditText) findViewById(R.id.chConf_edPrecision);
//		edMovingAverage 	= (EditText) findViewById(R.id.chConf_edMovingAverage);
//		chkSpeechEnabled 	= (CheckBox) findViewById(R.id.chConf_chkSpeechEnabled);
//		
//		btnSave				= (Button) findViewById(R.id.chConf_btnSave);
//		
		// Set Listeners
		btnSave.setOnClickListener(this);
		btnAddChannel.setOnClickListener(this);
	
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
			//Log.i(TAG,"Fetch Model "+_channelId+" from Server");
			
			// Show a particular channel
//			if(_channelId>-1)
//			{
//				// Get the Channel instance
//				channel = server.getChannelById(_channelId);
//				
//				// Get configuration from config store
//
//				// Name is common from configstore and server
//				tvName.setText(channel.getName());
//				
//				// Use config from Server
//				edDesc.setText(channel.getDescription());
//				edUnit.setText(channel.getLongUnit());
//				edShortUnit.setText(channel.getShortUnit());
//				edOffset.setText(Float.toString(channel.getOffset()));
//				//edFactor.setText(Double.toString(channel.getFactor()));
//				edFactor.setText(Float.toString(channel.getFactor()));
//				edPrecision.setText(Integer.toString(channel.getPrecision()));
//				edMovingAverage.setText(Integer.toString(channel.getMovingAverage()));
//				chkSpeechEnabled.setChecked(channel.getSpeechEnabled());
//				
//				
//			}
		}

		public void onServiceDisconnected(ComponentName className) {
			server = null;
		}
	};

	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.modConf_btnSave:
				if(DEBUG) Log.d(TAG,"Save this model");
//				applyChannel();
//				Log.i(TAG,"Store settings to database for channel: "+_channelId);
//				server.saveChannelConfig(channel);
//				Log.i(TAG,"Go back to dashboard");
				
				this.setResult(RESULT_OK);
				this.finish();
				break;
			case R.id.modConf_btnAddChannel:
				if(DEBUG) Log.d(TAG,"Add a channel");
//				applyChannel();
//				Log.i(TAG,"Store settings to database for channel: "+_channelId);
//				server.saveChannelConfig(channel);
//				Log.i(TAG,"Go back to dashboard");
				
				break;
		}
	}
	

}
