package biz.onomato.frskydash;

import android.app.Application;
import android.content.Intent;
import android.util.Log;





public class MyApp extends Application  {
	private static final String TAG="Application";	

	
	public Channel AD1,AD2,RSSIrx,RSSItx;
	
	public MyApp(){
		Log.i(TAG,"Constructor");
	}

	@Override
	public void onCreate()
	{
		Log.i(TAG,"onCreate");
	}
	

	// perform any cleanup
	public void die()
	{
		Log.i(TAG,"Shutting Down");

		//Intent intent = new Intent(this, FrSkyServer.class);
		//stopService(intent);
	}

}

