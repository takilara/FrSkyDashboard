package biz.onomato.frskydash;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

public class FrSkyServer extends Service {
	
	private Handler speakHandler;
    private Runnable runnableSpeaker;
    
	private static final String TAG="FrSkyServerService";
	
    private int MY_DATA_CHECK_CODE;
	
	private Long counter = 0L; 
	private NotificationManager nm;
	private Timer timer = new Timer();
	private final Calendar time = Calendar.getInstance();
	

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
    private void showNotification() {
    	 CharSequence text = "FrSkyServer Started";
    	 Notification notification = new Notification(R.drawable.icon, text, System.currentTimeMillis());
    	 //notification.defaults |= Notification.FLAG_ONGOING_EVENT;
    	 notification.flags = Notification.DEFAULT_LIGHTS;
    	 notification.flags |= Notification.FLAG_ONGOING_EVENT;
    	 notification.flags |= Notification.FLAG_NO_CLEAR;
    	 //notification.flags |= Notification.FLAG_FOREGROUND_SERVICE; 
    	 
    	 //Intent notificationIntent = new Intent(this,MyApp.class);
    	 Intent notificationIntent = new Intent(this,Frskydash.class);
    	 notificationIntent.setAction(Intent.ACTION_MAIN);
         notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

    	 
    	 // http://developer.android.com/guide/topics/ui/notifiers/notifications.html
    	 PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
    			 notificationIntent, 0);
    	notification.setLatestEventInfo(this, "FrSkyDash",
    	      text, contentIntent);
    	//nm.notify("Service Started", notification);
    	nm.notify(56, notification);
    	    }
	
	@Override
	public void onCreate()
	{
		Log.i(TAG,"onCreate");
		super.onCreate();
		
      
        
        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		Toast.makeText(this,"Service created at " + time.getTime(), Toast.LENGTH_LONG).show();
		showNotification();		
		

		
		//incrementCounter();

       
	}
	

	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.i(TAG,"Receieved startCommand ");
		//return super.onStartCommand(intent,flags,startId);
		return START_STICKY;
		
		
		
		
		
	}
	
	public void die()
	{
		Log.i(TAG,"Die, perform cleanup");
		stopSelf();
	}
	

	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Log.i(TAG,"onDestroy");
		
	      
	      nm.cancel(56);
	      Toast.makeText(this, "Service destroyed at " + time.getTime() + "; counter is at: " + counter, Toast.LENGTH_LONG).show();
	      counter=null;
	}

}
