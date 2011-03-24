package biz.onomato.frskydash;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class SimulatorService extends Service {
	private static final String TAG="SimulatorService";
	MyApp globals;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		Log.i(TAG,"Created");
		globals = ((MyApp)getApplicationContext());	
	}
	
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
//        mNM.cancel(NOTIFICATION);

    	Log.i(TAG,"destroyed");
        // Tell the user we stopped.
        Toast.makeText(this, "service destroyed", Toast.LENGTH_SHORT).show();
    }

}
