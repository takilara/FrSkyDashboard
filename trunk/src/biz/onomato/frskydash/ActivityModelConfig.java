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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

public class ActivityModelConfig extends Activity implements OnClickListener {
	private static final String TAG = "ModelConfig";
	private static final boolean DEBUG=true;
	private static final int CHANNEL_CONFIG_RETURN = 1;
	private FrSkyServer server;
	
	private Model _model;
	private long _modelId;
	
	private Button btnSave,btnAddChannel;
	private LinearLayout llChannelsLayout;
	private EditText edName;
	
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		doBindService();
	
		
		///TODO: Use intent to get initial Model object?
		Intent launcherIntent = getIntent();
		_modelId = launcherIntent.getLongExtra("modelId", -1);
		if(DEBUG) Log.d(TAG,"Editing the model with id:"+_modelId);
//		Log.d(TAG, "Channel Id is: "+_channelId);
		
		if(_modelId==-1)
		{
			if(DEBUG) Log.d(TAG,"Configure new Model object");
			_model = new Model(getApplicationContext());
		}
		else
		{
			if(DEBUG) Log.d(TAG,"Configure existing Model object (id:"+_modelId+")");
			_model = new Model(getApplicationContext());
			_model.loadFromDatabase(_modelId);
			
			
//			_model = new Model(getApplicationContext());
//			_model.loadFromSettings(_modelId);
			//_model = Model.createFromSettings(getApplicationContext(), _modelId);
			
		}
		
		// Show the form
		setContentView(R.layout.activity_modelconfig);

		// Find all form elements
		btnSave				= (Button) findViewById(R.id.modConf_btnSave);
		btnAddChannel		= (Button) findViewById(R.id.modConf_btnAddChannel);
		edName				= (EditText) findViewById(R.id.modConf_edName);
		llChannelsLayout	= (LinearLayout) findViewById(R.id.llChannelsLayout);
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
	
		
		
		edName.setText(_model.getName());
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
				_model.saveToDatabase();
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
				Channel c = new Channel(getApplicationContext());
				c.setName(_model.getName()+"_"+(_model.getChannels().length+1));
				c.setDescription("Description"+(_model.getChannels().length+1));
				_model.addChannel(c);
				populateChannelList();
				break;
		}
	}
	
	private void populateChannelList()
	{
		if(DEBUG) Log.d(TAG,"Populate list of channels");
		llChannelsLayout.removeAllViews();
		int n = 0;
		for(Channel c:_model.getChannels())
		{
			if(DEBUG) Log.i(TAG,c.getDescription());
			
			LinearLayout ll = new LinearLayout(getApplicationContext());
			
			
			TextView tvDesc = new TextView(getApplicationContext());
			tvDesc.setText(c.getDescription());
			tvDesc.setLayoutParams(new LinearLayout.LayoutParams(0,LayoutParams.WRAP_CONTENT,1));
			
			Button btnDelete = new Button(getApplicationContext());
			btnDelete.setText("Delete");
			btnDelete.setId(10000+n);
			btnDelete.setOnClickListener(new OnClickListener(){
				public void onClick(View v){
					if(DEBUG) Log.d(TAG,"Delete channel "+_model.getChannels()[v.getId()-10000].getDescription());
					showDeleteChannelDialog(_model.getChannels()[v.getId()-10000]);
				}
			});
			
			Button btnEdit = new Button(getApplicationContext());
			btnEdit.setText("...");
			btnEdit.setId(1000+n);// ID for delete should be 100+channelId
			//btnEdit.setOnClickListener(this);
			btnEdit.setOnClickListener(new OnClickListener(){
				public void onClick(View v){
					if(DEBUG) Log.d(TAG,"Edit channel "+_model.getChannels()[v.getId()-1000].getDescription());
					// Launch editchannel with channel attached.. 
					Intent i = new Intent(getApplicationContext(), ActivityChannelConfig.class);
		    		//i.putExtra("channelId", 1);
					i.putExtra("channel", _model.getChannels()[v.getId()-1000]);
					i.putExtra("idInModel", v.getId()-1000);
		    		startActivityForResult(i,CHANNEL_CONFIG_RETURN);
				}
			});
			
			ll.addView(tvDesc);
			ll.addView(btnEdit);
			ll.addView(btnDelete);
			
			
			ll.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
			//ll.setGravity();

			
			llChannelsLayout.addView(ll);
			
			n++;
			
			
			
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
	            		int idInModel = data.getIntExtra("idInModel",-1);
	            		if(idInModel>-1)
	            		{
	            			_model.setChannel(idInModel,returnChannel);
	            			//populateChannelList();
	            			
	            		}
	            		if(DEBUG) Log.d(TAG,"Received channel from ActivityChannelConfig: channel:"+returnChannel.getDescription()+", silent: "+returnChannel.getSilent());
	            		
	            	}
	            	catch (Exception e)
	            	{
	            		Log.e(TAG,"No return channel");
	            	}
	            	
	            	populateChannelList();
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
