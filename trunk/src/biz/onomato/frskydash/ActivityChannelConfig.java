package biz.onomato.frskydash;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class ActivityChannelConfig extends Activity implements OnClickListener {
	private static final String TAG = "ChannelConfig";
	private int _channelId = -1;
	private FrSkyServer server;
	private Channel channel;
	private TextView tvName;
	private EditText edDesc,edUnit,edShortUnit,edOffset,edFactor,edPrecision,edMovingAverage;
	private CheckBox chkSpeechEnabled;
	private Button btnSave,btnDefaults;
	//chConf_edVoice
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		doBindService();
		
		Intent launcherIntent = getIntent();
		_channelId = launcherIntent.getIntExtra("channelId", -1);
		Log.d(TAG, "Channel Id is: "+_channelId);
		
		// Show the form
		setContentView(R.layout.activity_channelconfig);

		// Find all form elements
		tvName 				= (TextView) findViewById(R.id.chConf_tvName);
		edDesc 				= (EditText) findViewById(R.id.chConf_edDescription);
		edUnit 				= (EditText) findViewById(R.id.chConf_edUnit);
		edShortUnit			= (EditText) findViewById(R.id.chConf_edShortUnit);
		edOffset 			= (EditText) findViewById(R.id.chConf_edOffset);
		edFactor 			= (EditText) findViewById(R.id.chConf_edFactor);
		edPrecision 		= (EditText) findViewById(R.id.chConf_edPrecision);
		edMovingAverage 	= (EditText) findViewById(R.id.chConf_edMovingAverage);
		chkSpeechEnabled 	= (CheckBox) findViewById(R.id.chConf_chkSpeechEnabled);
		
		btnSave				= (Button) findViewById(R.id.chConf_btnSave);
		btnDefaults				= (Button) findViewById(R.id.chConf_btnDefaults);
		
		btnSave.setOnClickListener(this);
		btnDefaults.setOnClickListener(this);
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
			Log.i(TAG,"Fetch channel "+_channelId+" from Server");
			// Show a particular channel
			if(_channelId>-1)
			{
				// Get the Channel instance
				channel = server.getChannelById(_channelId);
				
				// Update the form with the channel values
				tvName.setText(channel.getName());
				edDesc.setText(channel.getDescription());
				edUnit.setText(channel.getLongUnit());
				edShortUnit.setText(channel.getShortUnit());
				edOffset.setText(Double.toString(channel.getOffset()));
				edFactor.setText(Double.toString(channel.getFactor()));
				edPrecision.setText(Integer.toString(channel.getPrecision()));
				edMovingAverage.setText(Integer.toString(channel.getMovingAverage()));
				chkSpeechEnabled.setChecked(channel.getSpeechEnabled());
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			server = null;
		}
	};

	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.chConf_btnSave:
				Log.i(TAG,"Apply settings to channel: "+_channelId);
				applyChannel();
				Log.i(TAG,"Store settings to database for channel: "+_channelId);
				server.saveChannelConfig(channel);
				Log.i(TAG,"Go back to dashboard");
				this.setResult(RESULT_OK);
				this.finish();
				break;
			case R.id.chConf_btnDefaults:
				Log.i(TAG,"Reset to default channel: "+_channelId);
				Log.i(TAG,"Store settings to database for channel: "+_channelId);
				break;
		}
	}
	
	private void applyChannel()
	{
		Log.i(TAG,"Apply the settings");
		
		int prec = Integer.parseInt(edPrecision.getText().toString());
		channel.setPrecision(prec);
		
		double fact = Double.parseDouble(edFactor.getText().toString());
		channel.setFactor(fact);
		
		double offs = Double.parseDouble(edOffset.getText().toString());
		channel.setOffset(offs);

		channel.setLongUnit(edUnit.getText().toString());
		channel.setShortUnit(edShortUnit.getText().toString());
		
		channel.setDescription(edDesc.getText().toString());
		
		channel.setSpeechEnabled(chkSpeechEnabled.isChecked());
		
		//needs to be done last to clean out "buffer"
		int ma = Integer.parseInt(edMovingAverage.getText().toString());
		channel.setMovingAverage(ma);
		
		
	}
}
