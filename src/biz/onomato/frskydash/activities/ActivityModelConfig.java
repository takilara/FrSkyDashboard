package biz.onomato.frskydash.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
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
	private static final int CHANNEL_CONFIG_RETURN = 1;
	private static final int MODULE_CONFIG_RETURN = 2;
	//private FrSkyServer server;
	private static final String DELETE_CHANNEL_DESCRIPTION_KEY = "channelDescription";
	private static final String DELETE_CHANNEL_ID_KEY = "channelId";
	
	private Model _model;
	private int _modelId;
	
	private Button btnAddChannel,btnFrSkyAlarms;
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
	public void onPause()
	{
		super.onPause();
		saveModel();
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
				editChannelIntent.putExtra(
					ActivityChannelConfig.EXTRA_MODEL_NR_CHANNELS, _model
							.getChannels().size()); 
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
	
	public void saveModel()
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
	            			returnChannel.setSourceChannel(returnChannel.getSourceChannelId());
	            			returnChannel.registerListenerForServerCommands();
	            			
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
			
	     removeDialog(DIALOG_DELETE_CHANNEL); 
		 showDialog(DIALOG_DELETE_CHANNEL,args);
	 }
	 
	 @Override
		protected Dialog onCreateDialog(int id,Bundle args) {
			super.onCreateDialog(id,args);
			AlertDialog dialog;
			Logger.i(TAG, "Make a dialog on context: " + this.getPackageName());

			switch (id) {
			case DIALOG_DELETE_CHANNEL:
				String mDescription = args.getString(DELETE_CHANNEL_DESCRIPTION_KEY);
				final int mId = args.getInt(DELETE_CHANNEL_ID_KEY);
				dialog = new AlertDialog.Builder(this).create();
				dialog.setTitle("Delete "+mDescription+"?");

				dialog.setMessage("Do you really want to delete the channel '"+mDescription+"'?");
				
				dialog.setButton(AlertDialog.BUTTON_POSITIVE,"Yes", new DialogInterface.OnClickListener() {

		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		            	
		            	_model.removeChannel(mId);
		            	populateChannelList();
		            }

		        });
		        dialog.setButton(AlertDialog.BUTTON_NEGATIVE,"No", new DialogInterface.OnClickListener() {

		            @Override
		            public void onClick(DialogInterface dialog, int which) {

		                //Stop the activity
		            	//_deleteId=-1;
		            	Logger.i(TAG,"Cancel Deletion");
		            }

		        });
				break;
			default:
				dialog = null;
			}
			return dialog;
		} 
	 
	 
	 
	/* (non-Javadoc)
	 * @see biz.onomato.frskydash.activities.ActivityBase#onModelChanged()
	 */
	@Override
	protected void onModelChanged() {
		// do nothing
		
	}

	/* (non-Javadoc)
	 * @see biz.onomato.frskydash.activities.ActivityBase#onServerConnected()
	 */
	@Override
	void onServerConnected() {
		// TODO Auto-generated method stub
		if(_modelId==-1)
		{
			Logger.d(TAG,"Configure new Model object");
			_model = new Model("New Model");
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
}
