package biz.onomato.frskydash.activities;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.R;
import biz.onomato.frskydash.domain.Channel;
import biz.onomato.frskydash.domain.Model;
import biz.onomato.frskydash.util.Logger;

/**
 * Activity for crud actions on Model
 * 
 * @author Espen Solbu
 *
 */
public class ActivityModelConfig extends ActivityBase implements OnClickListener {
	private static final String TAG = "ModelConfig";
	//private static final boolean DEBUG=true;

	//private FrSkyServer server;
	
	// TODO see if this reference is needed, probably local working copy for
	// edits, but if save automatically this isn't really needed since we can
	// work directly on the server collection then?
	private Model _model;
	
	/**
	 * reference to the model we are working on with this activity
	 */
	// TODO compare with targetModel in ActivityBase
	private int _modelId;
	
	private Button btnAddChannel,btnFrSkyAlarms;
	
	// TODO remove from layout instead? No longer in user
	private Button btnSave;
	
	private LinearLayout llChannelsLayout;
	private EditText edName;
	private Spinner spType;
	
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		//doBindService();
	
		// When using this activity to create a new model the id will be -1,
		// only when editing an existing model the id will be given
		Intent launcherIntent = getIntent();
		_modelId = launcherIntent.getIntExtra("modelId", -1);
		Logger.d(TAG,"Editing the model with id:"+_modelId);
		
		// Show the form
		setContentView(R.layout.activity_modelconfig);

		// Find all form elements
		btnSave				= (Button) findViewById(R.id.modConf_btnSave);
		btnAddChannel		= (Button) findViewById(R.id.modConf_btnAddChannel);
		btnFrSkyAlarms		= (Button) findViewById(R.id.modConf_btnFrSkyAlarms);
		edName				= (EditText) findViewById(R.id.modConf_edName);
		llChannelsLayout	= (LinearLayout) findViewById(R.id.llChannelsLayout);
		spType 				= (Spinner) findViewById(R.id.modConf_spType);

		// Set Listeners
		btnSave.setOnClickListener(this);
		btnSave.setVisibility(View.GONE);
		btnAddChannel.setOnClickListener(this);
		btnFrSkyAlarms.setOnClickListener(this);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		// make sure we persist the updates to the model object on pause of this
		// activity
		saveModel();
		Intent i = new Intent();
		i.putExtra(MODEL_ID_KEY,_model.getId());
		this.setResult(RESULT_OK,i);
		this.finish();
	}


	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.modConf_btnSave:
				saveModel();
				
				this.setResult(RESULT_OK);
				this.finish();
				break;
			case R.id.modConf_btnAddChannel:
				Logger.d(TAG,"Add a channel");
				// hcpl let activity create a new channel instead, only pass model
				// id so we avoid the use of parcellable objects
//				Channel c = new Channel();
////				c.setName(_model.getName()+"_"+(_model.getChannels().length+1));
////				c.setDescription("Description"+(_model.getChannels().length+1));
//				//c.setName(_model.getName()+"_"+(_model.getChannels().size()+1));
//				c.setDescription("Description"+(_model.getChannels().size()+1));
//				c.setModelId(_model.getId());
//				c.setId(-1);
				//_model.addChannel(c);				
				Intent editChannelIntent = new Intent(getApplicationContext(), ActivityChannelConfig.class);
//				editChannelIntent.putExtra("channel", c);
				// editChannelIntent.putExtra(ActivityChannelConfig.EXTRA_CHANNEL_REF,
				// c.getId());
				editChannelIntent.putExtra(ActivityChannelConfig.EXTRA_MODEL_ID,
					(int) _model.getId()); // Should edit existing model
				// also add the nr of channels
				// hcpl: channel activity will get this information elsewhere
//				editChannelIntent.putExtra(
//					ActivityChannelConfig.EXTRA_MODEL_NR_CHANNELS, _model
//							.getChannels().size());
				Logger.d(TAG, "Launch channel edit with modelId: " + _model.getId());
				//editChannelIntent.putExtra("idInModel", v.getId()-1000);
	    		startActivityForResult(editChannelIntent,CHANNEL_CONFIG_RETURN);
				
				//populateChannelList();
				break;
			case R.id.modConf_btnFrSkyAlarms:
				Intent i = new Intent(this,ActivityModuleSettings.class);
				i.putExtra("modelId", (int) _model.getId());	// Should edit existing model
				startActivityForResult(i,MODULE_CONFIG_RETURN);
				break;
		}
	}

	/**
	 * helper for saving the model. This will redirect to the actual save model
	 * method on the server side.
	 */
	private void saveModel()
	{
		Logger.d(TAG,"Save this model");

		// Save the model
		_model.setName(edName.getText().toString());
		_model.setType((String) spType.getSelectedItem());
		//_model.saveToDatabase();
		//FrSkyServer.database.saveModel(_model);

		FrSkyServer.saveModel(_model);
//		if(_model.getId()==server.getCurrentModel().getId())
//		{
//			if(FrSkyServer.D)Log.d(TAG,"Should update the servers.currentmodel");
//			server.setCurrentModel(_model);
//		}
//		else
//		{
//			if(FrSkyServer.D)Log.d(TAG,"This is not the current model");
//		}
	}
	
	private void populateChannelList()
	{
		Logger.d(TAG,"Populate list of channels");
		llChannelsLayout.removeAllViews();
		// iterate all channels for the current model
		for(Channel c:_model.getChannels().values())
		{
			Logger.i(TAG,"Id: "+ c.getId());
			Logger.i(TAG,c.getDescription());
			Logger.i(TAG,"SourceChannelId: "+ c.getSourceChannelId());
			
			LinearLayout ll = new LinearLayout(getApplicationContext());
			
			//description of channel
			TextView tvDesc = new TextView(getApplicationContext());
			tvDesc.setText(c.getDescription());
			tvDesc.setLayoutParams(new LinearLayout.LayoutParams(0,LayoutParams.WRAP_CONTENT,1));
			
			// delete option for channel
			ImageButton btnDelete = new ImageButton(getApplicationContext());
			btnDelete.setImageResource(R.drawable.ic_menu_delete);
			btnDelete.setScaleType(ImageView.ScaleType.CENTER_CROP);
			int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
			btnDelete.setLayoutParams(new LinearLayout.LayoutParams(height,height));
			//delete id is 10.000 + channelId
			btnDelete.setId(10000+c.getId());
			btnDelete.setOnClickListener(new OnClickListener(){
				public void onClick(View v){
					Logger.d(TAG,"Delete channel "+_model.getChannels().get(v.getId()-10000).getDescription());
					showDeleteChannelDialog(_model.getChannels().get(v.getId()-10000));
				}
			});
			
			//edit button
			ImageButton btnEdit = new ImageButton(getApplicationContext());
			btnEdit.setImageResource(R.drawable.ic_menu_edit);
			btnEdit.setScaleType(ImageView.ScaleType.CENTER_CROP);
			btnEdit.setLayoutParams(new LinearLayout.LayoutParams(height,height));
			btnEdit.setId(1000+c.getId());// ID for delete should be 100+channelId
			btnEdit.setOnClickListener(new OnClickListener(){
				public void onClick(View v){
					Logger.d(TAG,"Edit channel "+_model.getChannels().get(v.getId()-1000).getDescription());
					// Launch edit channel with channel id attached.. 
					Intent i = new Intent(getApplicationContext(), ActivityChannelConfig.class);
					i.putExtra(ActivityChannelConfig.EXTRA_CHANNEL_ID, v.getId()-1000);
					//also need to pass model reference
					i.putExtra(ActivityChannelConfig.EXTRA_MODEL_ID, _model.getId());
		    		startActivityForResult(i,CHANNEL_CONFIG_RETURN);
				}
			});
			
			ll.addView(tvDesc);
			ll.addView(btnEdit);
			ll.addView(btnDelete);
			
			ll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
			//ll.setGravity();
			
			llChannelsLayout.addView(ll);
		}
	}
	

	 protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	    {
	    	switch (requestCode)
	    	{
	            case CHANNEL_CONFIG_RETURN:
	            	Logger.d(TAG,"Done editing the channel");
	            	Channel returnChannel = null;
	            	try
	            	{
	            		//returnChannel = data.getParcelableExtra("channel");
						returnChannel = server
								.getCurrentModel()
								.getChannels()
								.get(data.getExtras().getInt(
										ActivityChannelConfig.EXTRA_CHANNEL_ID, -1));
	            		Logger.d(TAG,"   This channel has id: "+returnChannel.getId());
	            		//int idInModel = data.getIntExtra("idInModel",-1);
	            		//if(idInModel>-1)
	            		//{
	            			//_model.setChannel(idInModel,returnChannel);
	            			//returnChannel.setContext(getApplicationContext());
				// FIXED get reference to source channel itself first before
				// setting
				returnChannel.setSourceChannel(FrSkyServer
						.getChannel(returnChannel.getSourceChannelId()));
	            			//returnChannel.registerListenerForServerCommands();
	            			
	            			//_model.setChannel(returnChannel);
	            			//populateChannelList();
	            			
	            		//}
	            			//_model.getChannels().get(returnChannel.getSourceChannelId()).registerListener();
	            			Logger.d(TAG,"Received channel from ActivityChannelConfig: channel:"+returnChannel.getDescription()+", silent: "+returnChannel.getSilent());
	            		
	            	}
	            	catch (Exception e)
	            	{
	            		Logger.e(TAG,"No return channel");
	            	}
	            	
	            	populateChannelList();
	            	break;
	            case MODULE_CONFIG_RETURN:
	            	Logger.d(TAG,"Done editing FrSky alarms");
	            	
	            	break;
	    	}
	    }
	


	 private void showDeleteChannelDialog(final Channel channel)
	 {
		 Bundle args = new Bundle();
	     args.putInt(DELETE_CHANNEL_ID_KEY, channel.getId());
	     args.putString(DELETE_CHANNEL_DESCRIPTION_KEY, channel.getDescription());
	     args.putInt(DELETE_CHANNEL_FROM_MODEL_ID_KEY, _model.getId());
			
	     removeDialog(DIALOG_DELETE_CHANNEL); 
		 showDialog(DIALOG_DELETE_CHANNEL,args);
	 }
	 

	 
	 
	 
	/* (non-Javadoc)
	 * @see biz.onomato.frskydash.activities.ActivityBase#onModelChanged()
	 */
	@Override
	protected void onCurrentModelChanged() {
		// do nothing
		
	}

	/* (non-Javadoc)
	 * @see biz.onomato.frskydash.activities.ActivityBase#onServerConnected()
	 */
	@Override
	void onServerConnected() {
		// check if a model id is present. Id == -1 is no model ID present so
		// time to create a new model instead
		if(_modelId==-1)
		{
			Logger.d(TAG,"Configure new Model object");
			// create new object with the default name so it will be overwritten
			// on save
			_model = new Model(Model.DEFAULT_MODEL_NAME);
			_model.initializeDefaultChannels();
			// persist
			FrSkyServer.addModel(_model);
		}
		// otherwise this was an existing model that we want to get from
		// server only for now
		else {
			Logger.d(TAG,"Configure existing Model object (id:"+_modelId+")");
			_model = FrSkyServer.modelMap.get(_modelId);
		}
		
		edName.setText(_model.getName());
		
		//ArrayAdapter<CharSequence> alarmLevelAdapter = ArrayAdapter.createFromResource(this, R.array.alarm_level, android.R.layout.simple_spinner_item );
		// init all model types
		ArrayAdapter<CharSequence> modelTypeAdapter  = ArrayAdapter.createFromResource(getApplicationContext(),R.array.model_types,android.R.layout.simple_spinner_item);
    	
//		for(Channel c : server.getCurrentModel().getChannels())
//		{
//			channelDescriptionAdapter.add(c);
//		}
		
		modelTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		String[] modelTypes = getApplicationContext().getResources().getStringArray(R.array.model_types);
		
		spType.setAdapter(modelTypeAdapter);
		for(int i=0;i<modelTypes.length;i++)
		{
			Logger.i(TAG,"Comparing "+modelTypes[i]+" to "+_model.getType());
			if(modelTypes[i].equals(_model.getType()))
			{
				spType.setSelection(i);
				break;
			}
			
		}
		//spType.setSelection(modelTypes);
		// refresh channels
		populateChannelList();
	}

	/* (non-Javadoc)
	 * @see biz.onomato.frskydash.activities.ActivityBase#onServerDisconnected()
	 */
	@Override
	void onServerDisconnected() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see biz.onomato.frskydash.activities.ActivityBase#onModelMapChanged()
	 */
	@Override
	protected void onModelMapChanged() {
		// This gets called when models or channels are changed
		populateChannelList();
		
	}
}
