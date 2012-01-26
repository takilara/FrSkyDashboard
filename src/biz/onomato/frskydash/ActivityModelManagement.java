package biz.onomato.frskydash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityModelManagement extends Activity implements OnClickListener {
	private static final String TAG = "Model Management";
	private FrSkyServer server;
	private static final int MODEL_CONFIG_RETURN=0;
	private LinearLayout llModelsLayout;
	private Button btnAddModel;
	private RadioButton rbCurrentModel;
	private boolean DEBUG=true;
	private long _deleteId=-1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		setContentView(R.layout.activity_modelmanagement);

		// Setup components from screen
		if(DEBUG) Log.i(TAG,"Setup widgets");
		llModelsLayout = (LinearLayout) findViewById(R.id.llModelsLayout);
		btnAddModel = (Button) findViewById(R.id.btnAddModel);
		
		// Add listeners
		if(DEBUG) Log.i(TAG,"Add Listeners");
		//btnAddModel.setOnClickListener(this);
		btnAddModel.setOnClickListener(new OnClickListener() {
			public void onClick(View v){
				if(DEBUG) Log.d(TAG,"User Clicked Add Model");
				Intent i = new Intent(getApplicationContext(), ActivityModelConfig.class);
	    		i.putExtra("modelId", (int) -1);	// Should create new model
	    		startActivityForResult(i,MODEL_CONFIG_RETURN);
			}
		});
		
		
        doBindService();
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		//test.setText(oAd1.toString());
	}
	
	public void onClick(View v)
	{
		int id = v.getId();
		if(id>10000)		// Select current Model
		{
			int ii = id -10000;
			rbCurrentModel.setChecked(false);
			rbCurrentModel = (RadioButton) findViewById(id);
			rbCurrentModel.setChecked(true);
			//Model m = new Model(getApplicationContext());
			//m.loadFromDatabase(ii);
			Model m = server.database.getModel(ii);
			
			if(server!=null)
			{
				server.setCurrentModel(m);
				Toast.makeText(this, m.getName() + " set as the active model", Toast.LENGTH_LONG).show();
			}
		}
		else if(id>1000)	// EDIT
		{
			int ii=id-1000;
			Intent i = new Intent(this, ActivityModelConfig.class);
    		i.putExtra("modelId", (int) ii);	// Should edit existing model
    		
    		startActivityForResult(i,MODEL_CONFIG_RETURN);
		}
		else if(id>100) // DELETE
		{
			int ii=id-100;
			if(DEBUG) Log.d(TAG,"Delete model with id:"+ii);
			showDeleteDialog(ii);
			
		}
		else
		{
//			switch(v.getId())
//			{
//			///TODO: replace with actions for models
//				case R.id.btnAddModel:
//					if(DEBUG) Log.d(TAG,"User Clicked Add Model");
//					Intent i = new Intent(this, ActivityModelConfig.class);
//		    		i.putExtra("modelId", (long) -1);	// Should create new model
//		    		startActivityForResult(i,MODEL_CONFIG_RETURN);
//					break;
//				default:
//					if(DEBUG) Log.w(TAG,"Unknown button:"+v.getId());
//			}
		}
	}
	
	private void populateModelList()
	{
		// populate with models

		//llModelsLayout
		llModelsLayout.removeAllViews();

		long currentModelId = -1;
		if(server==null)
		{
			currentModelId=-1;
		}
		else
		{
			currentModelId=server.getCurrentModel().getId();
		}
		
		//
		//DBAdapterModel db = new DBAdapterModel(getApplicationContext());
		//db.open();
		//Cursor c = db.getAllModels();
		
		int n = 0;
		//while(n < c.getCount())
		for(Model m : server.database.getModels())
		{
			if(DEBUG)Log.d(TAG,"Add Model (id,name): "+m.getId()+", "+m.getName());
			LinearLayout ll = new LinearLayout(getApplicationContext());
			
			
			int id = m.getId();
			
			TextView tvName = new TextView(getApplicationContext());
			tvName.setText(m.getName());
			tvName.setLayoutParams(new LinearLayout.LayoutParams(0,LayoutParams.WRAP_CONTENT,1));
			
			RadioButton rdThisModel = new RadioButton(getApplicationContext());
			rdThisModel.setId(10000+id);
			rdThisModel.setOnClickListener(this);
			if(id==currentModelId)
			{
				rdThisModel.setChecked(true);
			}
			else
			{
				rdThisModel.setChecked(false);
			}
			
			
			Button btnDelete = new Button(getApplicationContext());
			btnDelete.setText("Delete");
			btnDelete.setId(100+id); // ID for delete should be 100+channelId
			btnDelete.setOnClickListener(this);
			
			Button btnEdit = new Button(getApplicationContext());
			btnEdit.setText("...");
			btnEdit.setId(1000+id);// ID for delete should be 100+channelId
			btnEdit.setOnClickListener(this);
			
//			ImageView iv = new ImageView(getApplicationContext());
//			iv.setImageResource(R.drawable.ic_modeltype_helicopter);
//			//iv.setBackgroundColor(0xffff0000);
//			iv.setColorFilter(0xffaaaaaa);
			
			//iv.setLayoutParams(new LinearLayout.LayoutParams(20,20));
			
			//ll.addView(iv);
			ll.addView(rdThisModel);
			ll.addView(tvName);
			
			ll.addView(btnEdit);
			ll.addView(btnDelete);
			
			
			ll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
			//ll.setGravity();

			
			llModelsLayout.addView(ll);
			
//			LayoutParams params = ll.getLayoutParams();
//			params.width = LayoutParams.MATCH_PARENT;
//			//params.height = LayoutParams.WRAP_CONTENT;
			n++;
		}
//		c.deactivate();
//		db.close();
	}
	
	
	private void showDeleteDialog(int id)
	{
		///TODO: Modify for deletion of models
		//final Model m = new Model(getApplicationContext());
		//m.loadFromDatabase(id);
		final Model m = server.database.getModel(id); 
		Log.i(TAG,"Delete model with id:"+id);
		_deleteId = id;
		AlertDialog dialog = new AlertDialog.Builder(this).create();
		dialog.setTitle("Delete "+m.getName()+"?");

		dialog.setMessage("Do you really want to delete the model '"+m.getName()+"'?");
		
		dialog.setButton(AlertDialog.BUTTON_POSITIVE,"Yes", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
            	//TODO: Remove, make global to class?
            	
            	//Channel.deleteChannelsForModel(getApplicationContext(),m);
            	server.database.deleteAllChannelsForModel(m);
            	server.database.deleteModel(_deleteId);
//            	DBAdapterModel db = new DBAdapterModel(getApplicationContext());
//            	db.open();
//            	db.deleteModel(_deleteId);
//            	db.close();
//            	
            	populateModelList();
                //Stop the activity
                //server.deleteAllLogFiles();
            }

        });
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE,"No", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                //Stop the activity
            	_deleteId=-1;
                Log.i(TAG,"Cancel Deletion");
            }

        });
        dialog.show();
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
			
			populateModelList();
			
			rbCurrentModel = (RadioButton) findViewById((int) (10000+server.getCurrentModel().getId()));
			rbCurrentModel.setChecked(true);
			
	        
		}

		public void onServiceDisconnected(ComponentName className) {
			server = null;
		}
	};

	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
    	switch (requestCode)
    	{
    		case MODEL_CONFIG_RETURN:
    			switch(resultCode)
	        	{
	        		case RESULT_OK:
	        			if(DEBUG) Log.i(TAG,"User saved new settings");
	        			break;
	        		case RESULT_CANCELED:
	        			if(DEBUG) Log.i(TAG,"User cancelled with back");
	        			break;
	        	}
    			break;
    	}
    	///TODO: update model list
    	populateModelList();
    }
}