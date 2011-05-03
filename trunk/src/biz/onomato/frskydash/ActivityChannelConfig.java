package biz.onomato.frskydash;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

public class ActivityChannelConfig extends Activity {
	private static final String TAG = "ChannelConfig";
	private int _channelId = -1;
	private FrSkyServer server;
	private Channel channel;
	private TextView tvName;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		doBindService();
		

		
		Intent launcherIntent = getIntent();
		_channelId = launcherIntent.getIntExtra("channelId", -1);
		Log.d(TAG, "Channel Id is: "+_channelId);
		
		setContentView(R.layout.activity_channelconfig);
		tvName.findViewById(R.id.chConf_tvName);
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
			if(_channelId>-1)
			{
				channel = server.getChannelById(_channelId);
//				tvName.setText(channel.getName());
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			server = null;
		}
	};
}
