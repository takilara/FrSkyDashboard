package biz.onomato.frskydash.activities;

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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.R;
import biz.onomato.frskydash.domain.Channel;
import biz.onomato.frskydash.domain.Model;
import biz.onomato.frskydash.hub.FrSkyHub;
import biz.onomato.frskydash.util.Logger;

/**
 * Activity for managing Channel objects and their properties.
 * 
 * @author Espen Solbu
 *
 */
public class ActivityChannelConfig extends Activity implements OnClickListener {
	
	private static final String TAG = "ChannelConfig";
	//private static final boolean DEBUG=true;

	/**
	 * use this to get the channel reference (from currentModel) 
	 */
	protected static final String EXTRA_CHANNEL_REF = "channelRef";
	
	/**
	 * identifiers
	 */
	private long _channelId = -1;
	//private int _idInModel = -1;
	private int _modelId=-1;
	private Model _model;
	private FrSkyServer server;
	SharedPreferences settings;
	SharedPreferences.Editor editor;
	
	private Channel channel;
	//private TextView tvName;
	private EditText edDesc,edUnit,edShortUnit,edOffset,edFactor,edPrecision,edMovingAverage;
	private CheckBox chkSpeechEnabled;
	private Spinner spSourceChannel;
	private Button btnSave;
	private boolean first=true;
	
	ArrayList<Channel> sourceChannels;
	
	//chConf_edVoice
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		doBindService();
		
		// hcpl: moved selection of channel to onBind method since server
		// instance is then known
		
		//_modelId = launcherIntent.getIntExtra("modelId", -1);
		//if(DEBUG)Log.d(TAG, "working model has id: "+_modelId);
		
		Logger.d(TAG, "Channel Id is: "+_channelId);
		
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
		
		// register listener
		btnSave.setOnClickListener(this);
	}
	
	void doBindService() {
		Logger.i(TAG,"Start the server service if it is not already started");
		startService(new Intent(this, FrSkyServer.class));
		Logger.i(TAG,"Try to bind to the service");
		getApplicationContext().bindService(new Intent(this, FrSkyServer.class), mConnection,0);
    }
    
    void doUnbindService() {
		if (server != null) {
			// Detach our existing connection.
			try {
				unbindService(mConnection);
			} catch (Exception e) {
				// hcpl: try to avoid empty catch blocks. Better to log the
				// error and if desired disable that error logging in the Logger
				// class. Otherwise it can be hard to find these kind of errors
				Logger.e(TAG, "Error on unbinding "
						+ this.getClass().toString(), e);
			}
		}
    }
    
	/**
	 * helper to retrieve selected channel that came with intent
	 */
	private void getSelectedChannel() {
		Intent launcherIntent = getIntent();
		try {
			// hcpl: do not use this or you'll get a new, different object
			// instance
			channel = server.getCurrentModel().getChannels()
					.get(launcherIntent.getIntExtra(EXTRA_CHANNEL_REF, 0));
			// in case this is a new channel creation rely on previous system
			if( channel == null )
				channel = launcherIntent.getParcelableExtra("channel");
			// _idInModel = launcherIntent.getIntExtra("idInModel", -1);
			_channelId = channel.getId();
			_modelId = channel.getModelId();
			Logger.d(TAG, "Channel config launched with attached channel: "
					+ channel.getDescription());
			Logger.d(TAG, "channel context is: " + FrSkyServer.getContext());
			// channel.setContext(getApplicationContext());
			Logger.d(TAG, "channel context is: " + FrSkyServer.getContext());

		} catch (Exception e) {
			Logger.d(TAG, "Channel config launched without attached channel");
			//channel = null;
			channel = new Channel();
			_channelId = launcherIntent.getIntExtra("channelId", -1);
			_modelId = -1;

		}
	}
    
    private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			server = ((FrSkyServer.MyBinder) binder).getService();
			Logger.i(TAG,"Bound to Service");
			Logger.i(TAG,"Fetch channel "+_channelId+" from Server");
			
			getSelectedChannel();			
			
			settings = server.getSettings();
	        editor = settings.edit();
	        
	        if(_modelId==-1)
			{
	        	Logger.d(TAG,"Configure new Model object");
				//_model = new Model(getApplicationContext());
				_model = server.getCurrentModel();
			}
			else
			{
				Logger.d(TAG,"Configure existing Model object (id:"+_modelId+")");
				//_model = new Model(getApplicationContext());
				//_model.loadFromDatabase(_modelId);
				//_model = FrSkyServer.database.getModel(_modelId);
				_model = FrSkyServer.modelMap.get(_modelId);
			}
	        
			
	        if(channel!=null)
	        {
	        	// ArrayList<Channel> sourceChannels = _model.getAllowedSourceChannels();
	        	sourceChannels = _model.getAllowedSourceChannels();
	        	
	        	/**
	        	 * Prototype Hub support
	        	 * eso
	        	 * 
	        	 * Add channel list from Hub to source list
	        	 */
	        	if(server.getHubEnabled())
	        	{
		        	for(Channel ch : FrSkyHub.getInstance().getSourceChannels().values())
	        		//for(Channel ch : FrSkyHub.getSourceChannels().values())
		        	{
		        		sourceChannels.add(ch);
		        	}
	        	}
	        	
	        	//int n =0;
   	
	        	/**
	        	 * remove self from list
	        	 */
	        	sourceChannels.remove(channel);

	        	ArrayAdapter<Channel> channelDescriptionAdapter  = new ArrayAdapter<Channel> (getApplicationContext(),android.R.layout.simple_spinner_item,sourceChannels);
				channelDescriptionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				
				spSourceChannel.setAdapter(channelDescriptionAdapter);
				
				
				// eso: set correct startup channel
				long len = channelDescriptionAdapter.getCount();
				for(int i=0;i<len;i++)
				{
					Channel c = (Channel) spSourceChannel.getItemAtPosition(i);
					if(c.getId()==channel.getSourceChannelId())
					{
						spSourceChannel.setSelection(i);
						break;
					}
				}
				
				spSourceChannel.setOnItemSelectedListener(new OnItemSelectedListener() {
				    @Override
				    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				        // your code here
				    	if(first)
				    	{
				    		first = false;
				    	}
				    	else
				    	{
					    	if(parentView.getSelectedItem()instanceof Channel)
					    	{
					    		Channel channel = (Channel) parentView.getSelectedItem();
					    		Logger.d(TAG,"User selecte channel "+channel+" the units are: "+channel.getLongUnit());
					    		String longUnit = channel.getLongUnit();
					    		String shortUnit = channel.getShortUnit();
					    		
					    		if(longUnit.length()>0)
					    		{
					    			edUnit.setText(longUnit);
					    		}
					    		
					    		if(shortUnit.length()>0)
					    		{
					    			edShortUnit.setText(shortUnit);
					    		}
					    	}
				    	}
				    }

				    @Override
				    public void onNothingSelected(AdapterView<?> parentView) {
				        // your code here
				    }

				});
				
				edDesc.setText(channel.getDescription());
				edUnit.setText(channel.getLongUnit());
				edShortUnit.setText(channel.getShortUnit());
				edOffset.setText(Float.toString(channel.getOffset()));
				//edFactor.setText(Double.toString(channel.getFactor()));
				edFactor.setText(Float.toString(channel.getFactor()));
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
				Logger.i(TAG,"Apply settings to channel: "+_channelId);
				applyChannel();
				
				// Enable the listener:
				//channel.registerListener();
				
				//Intent i = new Intent(getApplicationContext(), ActivityModelConfig.class);
	    		//i.putExtra("channelId", 1);
				
				//Only add channel to return if new channel (id=-1)
				if(_channelId==-1)
				{
					Intent i = new Intent();
					i.putExtra("channel", channel);
					//i.putExtra("idInModel",_idInModel);
	
					Logger.d(TAG,"Sending Parcelled channel back: Description:"+channel.getDescription()+", silent: "+channel.getSilent());
					this.setResult(RESULT_OK,i);
				}
				else
				{
					this.setResult(RESULT_OK);
				}
				
				this.finish();
				break;
		}
	}
	
	private void applyChannel()
	{
		Logger.i(TAG,"Apply the settings");
		
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
		
		// get reference to user selection
		Channel c = (Channel) spSourceChannel.getSelectedItem();
		Logger.d(TAG,"Try to set source channel to:"+c.toString()+" (ID: "+c.getId()+")");
		channel.setSourceChannel(c);
		
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
		Logger.d(TAG,"This is a model channel for modelId: "+channel.getModelId());
		if(channel.getModelId()>-1)
		{
			Logger.d(TAG,"This is an existing model, feel free to save");
			//channel.saveToDatabase();
			//FrSkyServer.database.saveChannel(channel);
			channel.registerListener();
			FrSkyServer.modelMap.get(channel.getModelId()).setChannel(channel);
			FrSkyServer.saveChannel(channel);
			//or SAVE_MODEL, modelId
		}
		else
		{
			Logger.d(TAG,"This is a new model, delay saving");
		}
	}
	
	public void onPause() {

		super.onPause();
		Logger.i(TAG, "onPause");
		// remove all source channels to prevent holding reference 
		sourceChannels.clear();
		
		finish();
		
		
		// mTts.stop();
		// doUnbindService();
		

	}

}
