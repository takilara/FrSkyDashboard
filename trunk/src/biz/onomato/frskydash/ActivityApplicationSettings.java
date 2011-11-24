package biz.onomato.frskydash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class ActivityApplicationSettings extends Activity implements OnClickListener, OnEditorActionListener {

	private static final String TAG = "Application-Settings";
	private FrSkyServer server;
	SharedPreferences settings;
	SharedPreferences.Editor editor;
	
	private View btnDeleteLogs;
	private CheckBox chkCyclicSpeakerEnabled;
	private CheckBox chkLogToRaw; 
	private CheckBox chkLogToCsv; 
	private CheckBox chkLogToHuman;
	private EditText edCyclicInterval;
	private Button btnSave;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		setContentView(R.layout.activity_applicationsettings);

		// Setup components from screen
		Log.i(TAG,"Setup widgets");
		btnDeleteLogs = findViewById(R.id.btnDeleteLogs);
		chkCyclicSpeakerEnabled = (CheckBox) findViewById(R.id.chkCyclicSpeakerEnabled);
		chkLogToRaw = (CheckBox) findViewById(R.id.chkLogToRaw); 
		chkLogToCsv = (CheckBox) findViewById(R.id.chkLogToCsv); 
		chkLogToHuman = (CheckBox) findViewById(R.id.chkLogToHuman); 
		edCyclicInterval = (EditText) findViewById(R.id.edCyclicSpeakerInterval);
		edCyclicInterval.setOnEditorActionListener(this);
		//edCyclicInterval.setImeOptions(EditorInfo.IME_ACTION_DONE|EditorInfo.IME_ACTION_UNSPECIFIED);
		btnSave = (Button) findViewById(R.id.btnSave);
		
		// Add listeners
		Log.i(TAG,"Add Listeners");
		btnDeleteLogs.setOnClickListener(this);
		chkCyclicSpeakerEnabled.setOnClickListener(this);
		chkLogToRaw.setOnClickListener(this);
		chkLogToCsv.setOnClickListener(this);
		chkLogToHuman.setOnClickListener(this);
		btnSave.setOnClickListener(this);
		//edCyclicSpeakerInterval.addTextChangedListener(this);
		
		
		// Load settings
        //settings = getPreferences(MODE_PRIVATE);

        
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
			case R.id.btnDeleteLogs:
				showDeleteDialog();
				break;
			case R.id.chkCyclicSpeakerEnabled:
				//Log.i(TAG,"Store cyclic speaker: ");
				CheckBox chkCyclicSpeakerEnabled = (CheckBox) v;
				editor.putBoolean("cyclicSpeakerEnabledAtStartup", chkCyclicSpeakerEnabled.isChecked());
				editor.commit();
				server.setCyclicSpeech(chkCyclicSpeakerEnabled.isChecked());
				
				break;
			case R.id.chkLogToCsv:
				//Log.i(TAG,"Store Log to Csv ");
				editor.putBoolean("logToCsv", ((CheckBox) v).isChecked());
				editor.commit();
				server.setLogToCsv(((CheckBox) v).isChecked());
				break;
			case R.id.chkLogToRaw:
				//Log.i(TAG,"Store Log to Raw ");
				editor.putBoolean("logToRaw", ((CheckBox) v).isChecked());
				editor.commit();
				server.setLogToRaw(((CheckBox) v).isChecked());
				break;
			case R.id.chkLogToHuman:
				//Log.i(TAG,"Store Log to Human");
				editor.putBoolean("logToHuman", ((CheckBox) v).isChecked());
				editor.commit();
				server.setLogToHuman(((CheckBox) v).isChecked());
				break;
			case R.id.btnSave:
				Log.i(TAG,"Store new interval");
				save();
				
				
				break;

				
		}
	}
	
	private void save()
	{
		Log.i(TAG,"Save current settings");
		try
		{
			editor.putBoolean("cyclicSpeakerEnabledAtStartup", chkCyclicSpeakerEnabled.isChecked());
			editor.putBoolean("logToCsv", chkLogToCsv.isChecked());
			editor.putBoolean("logToRaw", chkLogToRaw.isChecked());
			editor.putBoolean("logToHuman", chkLogToHuman.isChecked());
			editor.putInt("cyclicSpeakerInterval", Integer.parseInt(edCyclicInterval.getText().toString()));
			editor.commit();
			server.setCyclicSpeachInterval(Integer.parseInt(edCyclicInterval.getText().toString()));
			
			getApplicationContext();
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(edCyclicInterval.getWindowToken(), 0);
		}
		catch (Exception e)
		{
			
		}
	}
	
	private void showDeleteDialog()
	{
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
    	//bindService(new Intent(this, FrSkyServer.class), mConnection, Context.BIND_AUTO_CREATE);
		Log.i(TAG,"Start the server service if it is not already started");
		startService(new Intent(this, FrSkyServer.class));
		Log.i(TAG,"Try to bind to the service");
		getApplicationContext().bindService(new Intent(this, FrSkyServer.class), mConnection,0);
		//bindService(new Intent(this, FrSkyServer.class), mConnection, Context.BIND_AUTO_CREATE);
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
			
			settings = server.getSettings();
	        editor = settings.edit();
	        int interval = settings.getInt("cyclicSpeakerInterval",30);
	        Log.i(TAG,"Set interval to +"+interval);
	        Log.i(TAG,"Edit field currently at +"+edCyclicInterval.getText().toString());
	        edCyclicInterval.setText(String.valueOf(interval));
	        //edCyclicInterval.setText(interval);
	        chkCyclicSpeakerEnabled.setChecked(settings.getBoolean("cyclicSpeakerEnabledAtStartup",false));
	        chkLogToRaw.setChecked(settings.getBoolean("logToRaw",false));
	        chkLogToHuman.setChecked(settings.getBoolean("logToHuman",false));
	        chkLogToCsv.setChecked(settings.getBoolean("logToCsv",false));
	        
			//simFrame = server.sim.genFrame(ad1_raw,ad2_raw,rssirx_raw, rssitx_raw);
			 
			
			//Frame f = new Frame(simFrame);
			//Frame f = Frame.FrameFromAnalog(ad1_raw,ad2_raw,rssirx_raw, rssitx_raw);
			//Log.i(TAG,"FC (class ): "+Frame.frameToHuman(simFrame));
			
			
		}

		public void onServiceDisconnected(ComponentName className) {
			server = null;
		}
	};

	@Override
	public boolean onEditorAction(TextView editView, int actionId, KeyEvent event) {
		// Detect "ENTER"
		if(actionId == EditorInfo.IME_NULL)
		{
			save();
		}
		return true;
	}

//	@Override
//	public void afterTextChanged(Editable arg0) {
//		// TODO Auto-generated method stub
//		Log.i(TAG,"User fixed interval to: "+arg0.toString());
//		
//	}
//
//	@Override
//	public void beforeTextChanged(CharSequence s, int start, int count,
//			int after) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void onTextChanged(CharSequence s, int start, int before, int count) {
//		// TODO Auto-generated method stub
//		Log.i(TAG,"Interval changed to: "+s);
//		
//	}
	
	
}
