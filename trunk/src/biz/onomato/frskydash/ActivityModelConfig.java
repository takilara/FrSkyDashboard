package biz.onomato.frskydash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

public class ActivityModelConfig extends Activity implements OnClickListener {
	private static final String TAG = "ModelConfig";
	private static final boolean DEBUG=true;
	private static final int CHANNEL_CONFIG_RETURN = 1;
	private static final int MODULE_CONFIG_RETURN = 2;
	private FrSkyServer server;
	
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
		doBindService();
	
		
		///TODO: Use intent to get initial Model object?
		Intent launcherIntent = getIntent();
		_modelId = launcherIntent.getIntExtra("modelId", -1);
		if(DEBUG) Log.d(TAG,"Editing the model with id:"+_modelId);
//		Log.d(TAG, "Channel Id is: "+_channelId);
		
		
		
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
		btnAddChannel.setOnClickListener(this);
		btnFrSkyAlarms.setOnClickListener(this);
		
		
		
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
			
			
			if(_modelId==-1)
			{
				if(DEBUG) Log.d(TAG,"Configure new Model object");
				_model = new Model(getApplicationContext());
				// save, to get id
				FrSkyServer.database.saveModel(_model);
				
				_model.initializeDefaultChannels();
				// save again to persist
				FrSkyServer.database.saveModel(_model);
				
			}
			else
			{
				if(DEBUG) Log.d(TAG,"Configure existing Model object (id:"+_modelId+")");
				//_model = new Model(getApplicationContext());
				//_model.loadFromDatabase(_modelId);
				_model = FrSkyServer.database.getModel(_modelId);
				
				
//				_model = new Model(getApplicationContext());
//				_model.loadFromSettings(_modelId);
				//_model = Model.createFromSettings(getApplicationContext(), _modelId);
				
			}
			
			edName.setText(_model.getName());
			
			//ArrayAdapter<CharSequence> alarmLevelAdapter = ArrayAdapter.createFromResource(this, R.array.alarm_level, android.R.layout.simple_spinner_item );
			
			ArrayAdapter<CharSequence> modelTypeAdapter  = ArrayAdapter.createFromResource(getApplicationContext(),R.array.model_types,android.R.layout.simple_spinner_item);
        	
//			for(Channel c : server.getCurrentModel().getChannels())
//			{
//				channelDescriptionAdapter.add(c);
//			}
			
			modelTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			String[] modelTypes = getApplicationContext().getResources().getStringArray(R.array.model_types);
			
			spType.setAdapter(modelTypeAdapter);
			int n=0;
			for(int i=0;i<modelTypes.length;i++)
			{
				if(DEBUG)Log.i(TAG,"Comparing "+modelTypes[i]+" to "+_model.getType());
				if(modelTypes[i].equals(_model.getType()))
				{
					spType.setSelection(i);
					break;
				}
				
			}
			//spType.setSelection(modelTypes);
			
			
			
			populateChannelList();
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

				// Save the model
				_model.setName(edName.getText().toString());
				_model.setType((String) spType.getSelectedItem());
				//_model.saveToDatabase();
				FrSkyServer.database.saveModel(_model);
				if(_model.getId()==server.getCurrentModel().getId())
				{
					if(DEBUG)Log.d(TAG,"Should update the servers.currentmodel");
					server.setCurrentModel(_model);
				}
				else
				{
					if(DEBUG)Log.d(TAG,"This is not the current model");
				}
				
				this.setResult(RESULT_OK);
				this.finish();
				break;
			case R.id.modConf_btnAddChannel:
				if(DEBUG) Log.d(TAG,"Add a channel");
				Channel c = new Channel();
//				c.setName(_model.getName()+"_"+(_model.getChannels().length+1));
//				c.setDescription("Description"+(_model.getChannels().length+1));
				//c.setName(_model.getName()+"_"+(_model.getChannels().size()+1));
				c.setDescription("Description"+(_model.getChannels().size()+1));
				c.setModelId(_model.getId());
				c.setId(-1);
				
				//_model.addChannel(c);
				
				Intent editChannelIntent = new Intent(getApplicationContext(), ActivityChannelConfig.class);
				editChannelIntent.putExtra("channel", c);
				editChannelIntent.putExtra("modelId", (int) _model.getId());	// Should edit existing model
				if(DEBUG)Log.d(TAG,"Launch channel edit with modelId: "+_model.getId());	
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
	
	private void populateChannelList()
	{
		if(DEBUG) Log.d(TAG,"Populate list of channels");
		llChannelsLayout.removeAllViews();
		
		for(Channel c:_model.getChannels().values())
		{
			if(DEBUG) Log.i(TAG,c.getDescription());
			
			LinearLayout ll = new LinearLayout(getApplicationContext());
			
			
			TextView tvDesc = new TextView(getApplicationContext());
			tvDesc.setText(c.getDescription());
			tvDesc.setLayoutParams(new LinearLayout.LayoutParams(0,LayoutParams.WRAP_CONTENT,1));
			
			ImageButton btnDelete = new ImageButton(getApplicationContext());
			btnDelete.setImageResource(R.drawable.ic_menu_delete);
			btnDelete.setScaleType(ImageView.ScaleType.CENTER_CROP);
			int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
			btnDelete.setLayoutParams(new LinearLayout.LayoutParams(height,height));
			//btnDelete.setText("Delete");
			btnDelete.setId(10000+c.getId());
			btnDelete.setOnClickListener(new OnClickListener(){
				public void onClick(View v){
					if(DEBUG) Log.d(TAG,"Delete channel "+_model.getChannels().get(v.getId()-10000).getDescription());
					showDeleteChannelDialog(_model.getChannels().get(v.getId()-10000));
				}
			});
			
			ImageButton btnEdit = new ImageButton(getApplicationContext());
			//btnEdit.setText("...");
			btnEdit.setImageResource(R.drawable.ic_menu_edit);
			btnEdit.setScaleType(ImageView.ScaleType.CENTER_CROP);
			btnEdit.setLayoutParams(new LinearLayout.LayoutParams(height,height));

			btnEdit.setId(1000+c.getId());// ID for delete should be 100+channelId
			//btnEdit.setOnClickListener(this);
			btnEdit.setOnClickListener(new OnClickListener(){
				public void onClick(View v){
					if(DEBUG) Log.d(TAG,"Edit channel "+_model.getChannels().get(v.getId()-1000).getDescription());
					// Launch editchannel with channel attached.. 
					Intent i = new Intent(getApplicationContext(), ActivityChannelConfig.class);
		    		//i.putExtra("channelId", 1);
					i.putExtra("channel", _model.getChannels().get(v.getId()-1000));
					//i.putExtra("modelId", (int) _model.getId());	// Should edit existing model
					//i.putExtra("idInModel", v.getId()-1000);
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
	            	if(DEBUG) Log.d(TAG,"Done editing the channel");
	            	Channel returnChannel = null;
	            	try
	            	{
	            		returnChannel = data.getParcelableExtra("channel");
	            		if(DEBUG) Log.d(TAG,"   This channel has id: "+returnChannel.getId());
	            		//int idInModel = data.getIntExtra("idInModel",-1);
	            		//if(idInModel>-1)
	            		//{
	            			//_model.setChannel(idInModel,returnChannel);
	            			//returnChannel.setContext(getApplicationContext());
	            			returnChannel.listenTo(returnChannel.getSourceChannelId());
	            			
	            			_model.setChannel(returnChannel);
	            			//populateChannelList();
	            			
	            		//}
	            		if(DEBUG) Log.d(TAG,"Received channel from ActivityChannelConfig: channel:"+returnChannel.getDescription()+", silent: "+returnChannel.getSilent());
	            		
	            	}
	            	catch (Exception e)
	            	{
	            		Log.e(TAG,"No return channel");
	            	}
	            	
	            	populateChannelList();
	            	break;
	            case MODULE_CONFIG_RETURN:
	            	if(DEBUG) Log.d(TAG,"Done editing FrSky alarms");
	            	
	            	break;
	    	}
	    }
	
	 private void showDeleteChannelDialog(final Channel channel)
		{
			///TODO: Modify for deletion of models
			Log.i(TAG,"Delete channel "+channel.getDescription());
			AlertDialog dialog = new AlertDialog.Builder(this).create();
			dialog.setTitle("Delete "+channel.getDescription()+"?");

			dialog.setMessage("Do you really want to delete the channel '"+channel.getDescription()+"'?");
			
			dialog.setButton(AlertDialog.BUTTON_POSITIVE,"Yes", new DialogInterface.OnClickListener() {

	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	            	_model.removeChannel(channel);
	            	populateChannelList();
	            }

	        });
	        dialog.setButton(AlertDialog.BUTTON_NEGATIVE,"No", new DialogInterface.OnClickListener() {

	            @Override
	            public void onClick(DialogInterface dialog, int which) {

	                //Stop the activity
	            	//_deleteId=-1;
	                Log.i(TAG,"Cancel Deletion");
	            }

	        });
	        dialog.show();
		}
}
