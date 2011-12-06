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
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityModelManagement extends Activity implements OnClickListener {
	private static final String TAG = "Model Management";
	private FrSkyServer server;
	private static final int MODEL_CONFIG_RETURN=0;
	private LinearLayout llModelsLayout;
	private Button btnAddModel;
	private boolean DEBUG=true;
	
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
		btnAddModel.setOnClickListener(this);
		
		populateModelList();
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
		switch(v.getId())
		{
		///TODO: replace with actions for models
			case R.id.btnDeleteLogs:
				showDeleteDialog();
				break;
			case R.id.btnAddModel:
				if(DEBUG) Log.d(TAG,"User Clicked Add Model");
				Intent i = new Intent(this, ActivityModelConfig.class);
	    		i.putExtra("modelId", (long) -1);	// Should create new model
	    		startActivityForResult(i,MODEL_CONFIG_RETURN);
				break;
		}
	}
	
	private void populateModelList()
	{
		// populate with models

		//llModelsLayout
		llModelsLayout.removeAllViews();
		
		DBAdapterModel db = new DBAdapterModel(getApplicationContext());
		db.open();
		Cursor c = db.getAllModels();
		int n = 0;
		while(n < c.getCount())
		{
			if(n==0)
			{
				c.moveToFirst();
			}
			else
			{
				c.moveToNext();
				
			}
			n++;
			if(DEBUG)Log.d(TAG,"Add Model (id,name): "+c.getLong(0)+", "+c.getString(1));
			LinearLayout ll = new LinearLayout(getApplicationContext());
			
			
			
			
			TextView tvName = new TextView(getApplicationContext());
			tvName.setText(c.getString(1));
			
			
			Button btnDelete = new Button(getApplicationContext());
			btnDelete.setText("Delete");
			
			Button btnEdit = new Button(getApplicationContext());
			btnEdit.setText("...");
			
			ll.addView(tvName);
			ll.addView(btnDelete);
			ll.addView(btnEdit);
			
			ll.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
			//ll.setGravity();

			
			llModelsLayout.addView(ll);
			
//			LayoutParams params = ll.getLayoutParams();
//			params.width = LayoutParams.FILL_PARENT;
//			//params.height = LayoutParams.WRAP_CONTENT;
			
		}
		db.close();
	}
	
	
	private void showDeleteDialog()
	{
		///TODO: Modify for deletion of models
		Log.i(TAG,"Delete all logs please");
		AlertDialog dialog = new AlertDialog.Builder(this).create();
		dialog.setTitle("Delete Logs");

		dialog.setMessage("Do you really want to delete all logs?");
		
		dialog.setButton(AlertDialog.BUTTON_POSITIVE,"Yes", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                //Stop the activity
                server.deleteAllLogFiles();
            }

        });
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE,"No", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                //Stop the activity
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
