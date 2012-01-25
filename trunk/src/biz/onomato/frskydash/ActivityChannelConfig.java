package biz.onomato.frskydash;

import java.util.ArrayList;

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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class ActivityChannelConfig extends Activity implements OnClickListener {
	private static final String TAG = "ChannelConfig";
	private static final boolean DEBUG=true;
	private long _channelId = -1;
	private int _idInModel = -1;
	private FrSkyServer server;
	SharedPreferences settings;
	SharedPreferences.Editor editor;
	
	private Channel channel;
	private TextView tvName;
	private EditText edDesc,edUnit,edShortUnit,edOffset,edFactor,edPrecision,edMovingAverage;
	private CheckBox chkSpeechEnabled;
	private Spinner spSourceChannel;
	private Button btnSave;
	
	
	//chConf_edVoice
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		doBindService();
		
		Intent launcherIntent = getIntent();
		try
		{
			channel = launcherIntent.getParcelableExtra("channel");
			_idInModel = launcherIntent.getIntExtra("idInModel", -1);
			_channelId = channel.getId();
			Log.d(TAG,"Channel config launched with attached channel: "+channel.getDescription());
			Log.d(TAG,"channel context is: "+channel.getContext());
			channel.setContext(getApplicationContext());
			Log.d(TAG,"channel context is: "+channel.getContext());
		}
		catch(Exception e)
		{
			Log.d(TAG,"Channel config launched without attached channel");
			channel = null;
			_channelId = launcherIntent.getIntExtra("channelId", -1);
		}
		
		Log.d(TAG, "Channel Id is: "+_channelId);
		
		// Show the form
		setContentView(R.layout.activity_channelconfig);

		// Find all form elements
		spSourceChannel		= (Spinner)  findViewById(R.id.chConf_spSourceChannel);
		edDesc 				= (EditText) findViewById(R.id.chConf_edDescription);
		edUnit 				= (EditText) findViewById(R.id.chConf_edUnit);
		edShortUnit			= (EditText) findViewById(R.id.chConf_edShortUnit);
		edOffset 			= (EditText) findViewById(R.id.chConf_edOffset);
		edFactor 			= (EditText) findViewById(R.id.chConf_edFactor);
		edPrecision 		= (EditText) findViewById(R.id.chConf_edPrecision);
		edMovingAverage 	= (EditText) findViewById(R.id.chConf_edMovingAverage);
		chkSpeechEnabled 	= (CheckBox) findViewById(R.id.chConf_chkSpeechEnabled);
		
		btnSave				= (Button) findViewById(R.id.chConf_btnSave);
		
		
		btnSave.setOnClickListener(this);
	
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
			
			settings = server.getSettings();
	        editor = settings.edit();
	        
			// Show a particular channel

//	        if((_channelId>-1) && (channel==null))
//			{
//				// Get the Channel instance
//				channel = server.getChannelById((int) _channelId);
//			}
			
	        if(channel!=null)
	        {
				// Get configuration from config store

	        	//Channel[] sourceChannels = new Channel[server.getSourceChannels().length+server.getCurrentModel().getChannels().size()];
	        	ArrayList<Channel> sourceChannels = new ArrayList<Channel>();
	        	
	        	int n =0;
	        	
	        	// Add all server channels
	        	for(Channel c : server.getSourceChannels())
	        	{
	        		//sourceChannels[n] = c;
	        		sourceChannels.add(c);
	        		n++;
	        	}
	        	
	        	// Add channels from this model
	        	//Channel.getChannelsForModel(context, model)
	        	//TODO: Need to get channels for another model than currentmodel
	        	
	        	//for(Channel c : server.getCurrentModel().getChannels())
	        	for(Channel c : Channel.getChannelsForModel(getApplicationContext(),channel.getModelId()))
	        	{
	        		if(DEBUG)Log.i(TAG,String.format("Comparing '%s' to '%s'",channel.getDescription(),c.getDescription()));
	        		if(c.getId()!=channel.getId())
	        		{
	        			//Log.e(TAG,channel+" is different from "+c);
	        			//Log.e(TAG,channel.getId()+" is different from "+c.getId());

	        			// Channel is different
	        			//sourceChannels[n] = c;
	        			sourceChannels.add(c);
		        		n++;
	        		}
	        		else
	        		{
	        			// Channel is the same, ignore it
	        			if(DEBUG)Log.i(TAG,channel+" is same as "+c+", ignore it");
	        		}
	        		
	        	}
	        	
	        	
				//ArrayAdapter<Channel> channelDescriptionAdapter  = new ArrayAdapter<Channel> (getApplicationContext(),android.R.layout.simple_spinner_item,server.getSourceChannels());
	        	ArrayAdapter<Channel> channelDescriptionAdapter  = new ArrayAdapter<Channel> (getApplicationContext(),android.R.layout.simple_spinner_item,sourceChannels);
	        	
//				for(Channel c : server.getCurrentModel().getChannels())
//				{
//					channelDescriptionAdapter.add(c);
//				}
				channelDescriptionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				
				
				spSourceChannel.setAdapter(channelDescriptionAdapter);
				
				//TODO: set correct startup source channel
				long len = channelDescriptionAdapter.getCount();
				for(int i=0;i<len;i++)
				{
					Channel c = (Channel) spSourceChannel.getItemAtPosition(i);
					if(c.getId()==channel.getSourceChannelId())
					{
						spSourceChannel.setSelection(i);
					}
				}
				
				
								

				// Name is common from configstore and server
				//tvName.setText(channel.getName());
				
				
				// Use config from Server
				edDesc.setText(channel.getDescription());
				edUnit.setText(channel.getLongUnit());
				edShortUnit.setText(channel.getShortUnit());
				edOffset.setText(Float.toString(channel.getOffset()));
				//edFactor.setText(Double.toString(channel.getFactor()));
				edFactor.setText(Float.toString(channel.getFactor()));
				edPrecision.setText(Integer.toString(channel.getPrecision()));
				edMovingAverage.setText(Integer.toString(channel.getMovingAverage()));
				chkSpeechEnabled.setChecked(channel.getSpeechEnabled());
				
				// Use config from config store
				//edShortUnit.setText(cShortUnit);
				
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
				
				

				
				//Intent i = new Intent(getApplicationContext(), ActivityModelConfig.class);
	    		//i.putExtra("channelId", 1);
				Intent i = new Intent();
				i.putExtra("channel", channel);
				i.putExtra("idInModel",_idInModel);
				
				Log.i(TAG,"Go back to dashboard");
				if(server.getCurrentModel().getId() == channel.getModelId())
				{
					if(DEBUG) Log.d(TAG,"This is the current model, please replace run setChannel ");
					server.getCurrentModel().setChannel(_idInModel,channel);
				}
				else
				{
					if(DEBUG) Log.d(TAG,"This is NOT the current model");
				}
				if(DEBUG) Log.d(TAG,"Sending Parcelled channel back: Description:"+channel.getDescription()+", silent: "+channel.getSilent()+"_idInModel:"+_idInModel);
				this.setResult(RESULT_OK,i);
				
				this.finish();
				break;
		}
	}
	
	private void applyChannel()
	{
		Log.i(TAG,"Apply the settings");
		
		int prec = Integer.parseInt(edPrecision.getText().toString());
		channel.setPrecision(prec);
		
		float fact = Float.valueOf(edFactor.getText().toString());
		channel.setFactor(fact);
		
		float offs = Float.valueOf(edOffset.getText().toString());
		channel.setOffset(offs);

		channel.setLongUnit(edUnit.getText().toString());
		channel.setShortUnit(edShortUnit.getText().toString());
		
		channel.setDescription(edDesc.getText().toString());
		
		channel.setSpeechEnabled(chkSpeechEnabled.isChecked());
		
		//needs to be done last to clean out "buffer"
		int ma = Integer.parseInt(edMovingAverage.getText().toString());
		channel.setMovingAverage(ma);
		
		Channel c = (Channel) spSourceChannel.getSelectedItem();
		if(DEBUG)Log.d(TAG,"Try to set source channel to:"+c.toString()+" (ID: "+c.getId()+")");
		channel.listenTo(c);
		
		//channel.setDirtyFlag(true);
		
		//Save to regular persistant settings only if this is a "raw/server" channel
//		if(_channelId>-1)
//		{
//			//TODO: remove at some point
//			if(DEBUG) Log.d(TAG,"This is a server channel, save settings to persistant store (not database)");
//			channel.saveToConfig(settings);
//		}
//		else
//		{
		if(DEBUG) Log.d(TAG,"This is a model channel for modelId: "+channel.getModelId());
		if(channel.getModelId()>-1)
		{
			if(DEBUG) Log.d(TAG,"This is an existing model, feel free to save");
			channel.saveToDatabase();
		}
		else
		{
			if(DEBUG) Log.d(TAG,"This is a new model, delay saving");
		}
		//}
		
		
	}
	
	
}
