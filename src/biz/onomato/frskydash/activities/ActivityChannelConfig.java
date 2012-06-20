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
	
	/**
	 * identifier for logging
	 */
	private static final String TAG = "ChannelConfig";

	/**
	 * use this to get the channel reference (from currentModel) 
	 */
	protected static final String EXTRA_CHANNEL_ID = "channelRef";

	/**
	 * use this to reference the current model we want to create a channel for
	 */
	protected static final String EXTRA_MODEL_ID = "modelId";

	/**
	 * identifier for the current channel we are working on
	 */
	private int _channelId = -1;
	// TODO is the channel object needed?
	private Channel channel;

	/**
	 * identifier for the current model we are working on. Channel is always
	 * linked to a model!
	 */
	private int _modelId=-1;
	// TODO is the model object needed?
	private Model _model;
	
	/**
	 * server reference
	 */
	private FrSkyServer server;
	
	/**
	 * preferences
	 */
	SharedPreferences settings;
	SharedPreferences.Editor editor;
	
	/**
	 * widgets
	 */
	//private TextView tvName;
	private EditText edDesc,edUnit,edShortUnit,edOffset,edFactor,edPrecision,edMovingAverage;
	private CheckBox chkSpeechEnabled;
	private Spinner spSourceChannel;
	
	// TODO remove from layout completely
	private Button btnSave;
	
	/**
	 * a collection of sourcechannels
	 */
	private ArrayList<Channel> sourceChannels;
	
	//chConf_edVoice
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		// connect to server
		doBindService();
		
		// hcpl: moved selection of channel to onBind method since server
		// instance is then known
		
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

		//TODO better to remove from layout completely
		btnSave				= (Button) findViewById(R.id.chConf_btnSave);
		btnSave.setVisibility(View.GONE);
		// register listener
		btnSave.setOnClickListener(this);
	}

	/**
	 * Helper to connect with {@link FrSkyServer} instance. Check {
	 * {@link #mConnection} for logic when connection with server is
	 * established. We need to wait for that connection since we need
	 * information like current model.
	 */
	private void doBindService() {
		Logger.i(TAG,"Start the server service if it is not already started");
		startService(new Intent(this, FrSkyServer.class));
		Logger.i(TAG,"Try to bind to the service");
		getApplicationContext().bindService(new Intent(this, FrSkyServer.class), mConnection,0);
    }

	// hcpl: commented since no longer referenced, if needed call this in mConnection 
//	/**
//	 * Helper to disconnect from {@link FrSkyServer} instance
//	 */
//    private void doUnbindService() {
//    	// FIXME no longer in use? Why not on destroy?
//    	// always check if server available 
//		if (server != null) {
//			// Detach our existing connection.
//			try {
//				unbindService(mConnection);
//			} catch (Exception e) {
//				// hcpl: try to avoid empty catch blocks. Better to log the
//				// error and if desired disable that error logging in the Logger
//				// class. Otherwise it can be hard to find these kind of errors
//				Logger.e(TAG, "Error on unbinding "
//						+ this.getClass().toString(), e);
//			}
//		}
//    }
    
	/**
	 * helper to retrieve selected channel that came with intent. This can only
	 * be executed once you have a proper server connection since Model map from
	 * server will be referenced.
	 */
	private void getSelectedChannel() {
		Intent launcherIntent = getIntent();
		try {
			// hcpl: do not use parcelling or you'll get a new, different object
			// instance. Instead pass a reference and with that reference
			// collect the same object from the current model channels map
			_modelId = launcherIntent.getIntExtra(EXTRA_MODEL_ID, -1);
			_channelId = launcherIntent.getIntExtra(EXTRA_CHANNEL_ID, -1);
			// hcpl: I moved the logging to here also
			Logger.d(TAG, "Channel config launched with attached Model ID: "+_modelId);
			Logger.d(TAG, "Channel config launched with attached Channel ID: "+_channelId);
			// if valid IDs passed we can fetch these objects from server
			if (_modelId != -1 && _channelId != -1) {
				// TODO local object not really needed...
				channel = FrSkyServer.modelMap.get(_modelId).getChannels()
						.get(_channelId);
			}
			// otherwise we need to prepare for a new channel instead
			else {
				Logger.i(TAG, "New channel, creating a new one");
				// in this case local object is required unless we save right away 
				// TODO consider saving channel at this point
				prepareNewChannel(launcherIntent);
			}
			// end with some logging about channel details
			Logger.d(TAG, "The current channel is: "
					+ channel.getDescription());
			Logger.d(TAG, "channel context is: " + FrSkyServer.getContext());
			Logger.d(TAG,"SourceChannelId: "+channel.getSourceChannelId());
			// channel.setContext(getApplicationContext());
		} catch (Exception e) {
			Logger.d(TAG, "Channel config launched without attached channel");
			//channel = null;
			// hcpl: avoid code duplication
			prepareNewChannel(launcherIntent);
		}
	}
	
	/**
	 * prepare a new Channel object instance
	 * 
	 * @param intent
	 * @return
	 */
	private void prepareNewChannel(Intent intent){
		// this will register for commands right away
		channel = new Channel();
//		c.setName(_model.getName()+"_"+(_model.getChannels().length+1));
//		c.setDescription("Description"+(_model.getChannels().length+1));
		//c.setName(_model.getName()+"_"+(_model.getChannels().size()+1));
		// use default channel description so it will be overwritten on save
		channel.setDescription(Channel.DEFAULT_CHANNEL_DESCRIPTION);
		channel.setModelId(intent.getIntExtra(EXTRA_MODEL_ID,0));
		channel.setId(-1);
		// perform insert
		FrSkyServer.saveChannel(channel);
		// update local references
		_channelId = channel.getId();
		_modelId = channel.getModelId();
	}
    
    private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			// get server reference
			server = ((FrSkyServer.MyBinder) binder).getService();
			Logger.i(TAG,"Bound to Service");
			//Logger.i(TAG,"Fetch channel "+_channelId+" from Server");
			
			// get the selected channel now that we have references to models at
			// server
			getSelectedChannel();			
			
			// init preferences
			settings = server.getSettings();
	        editor = settings.edit();
	        
	        //if no model id was given we need to work on the current model
	        if(_modelId==-1)
			{
	        	Logger.d(TAG,"Configure new Model object");
				_model = FrSkyServer.getCurrentModel();
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
		        	for(Channel ch : FrSkyHub.getInstance().getChannels().values())
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
					
							/**
							 * keep track of first selection since first
							 * selection will be on initialisation of view only
							 * and doesn't have to be counted as an update of
							 * the sourcechannel
							 */
							private boolean firstSelection = true;

					@Override
				    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				        // your code here
				    	if(firstSelection)
				    	{
				    		firstSelection = false;
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
				saveChannel();
				
				// Enable the listener:
				//channel.registerListener();
				
				//Intent i = new Intent(getApplicationContext(), ActivityModelConfig.class);
	    		//i.putExtra("channelId", 1);
				
				//Only add channel to return if new channel (id=-1)
				if(_channelId==-1)
				{
					Intent i = new Intent();
					//i.putExtra("channel", channel);
					//i.putExtra("idInModel",_idInModel);
					//pass channel and model id instead
					i.putExtra(EXTRA_CHANNEL_ID, channel.getId());
	
//					Logger.d(TAG,"Sending Parcelled channel back: Description:"+channel.getDescription()+", silent: "+channel.getSilent());
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
	
	/**
	 * Save all changes to this channel.
	 */
	private void saveChannel()
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
		Channel sourceChannel = (Channel) spSourceChannel.getSelectedItem();
		// could be that user didn't make any selection
		if( sourceChannel != null ){
			Logger.d(TAG,"Try to set source channel to:"+sourceChannel.toString()+" (ID: "+sourceChannel.getId()+")");
			channel.setSourceChannel(sourceChannel);
		}
		
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
			channel.registerListenerForChannelUpdates();
			FrSkyServer.modelMap.get(channel.getModelId()).addChannel(channel);
			FrSkyServer.saveChannel(channel);
			//or SAVE_MODEL, modelId
		}
		else
		{
			// FIXME check this
			Logger.d(TAG,"This is a new model, delay saving");
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		// log information here
		Logger.i(TAG, "onPause");
		// make sure to persist the channel updates we did in this activity.
		// THIS HAS TO BE DONE BEFORE SOURCECHANNELS ARE CLEARED!!
		saveChannel();
		// remove all source channels to prevent holding reference. This is clean up!
		if( sourceChannels != null )
			sourceChannels.clear();
		// inform with result ok
		Intent i = new Intent();
		i.putExtra(EXTRA_CHANNEL_ID, channel.getId());
		this.setResult(RESULT_OK,i);
		this.finish();
		// hcpl: is this no longer needed?
		// mTts.stop();
		// doUnbindService();
	}

}
