package biz.onomato.frskydash;

import android.util.Log;

public class Alarm {

	private static final String TAG = "Alarm";
	public static final int ALARMTYPE_UNDEFINED=-1;
	public static final int ALARMTYPE_FRSKY=1;
	
	public static final int ALARMLEVEL_OFF=0;
	public static final int ALARMLEVEL_LOW=1;
	public static final int ALARMLEVEL_MID=2;
	public static final int ALARMLEVEL_HIGH=3;
	
	public int threshold;
	public int type;
	public int level;
	public boolean greaterthan;
	
	
	public Alarm(int alarmtype,int alarmlevel,boolean alarmgreaterthan,int alarmthreshold)
	{
		type=alarmtype;
		level = alarmlevel;
		greaterthan = alarmgreaterthan;
		threshold = alarmthreshold;
		Log.i(TAG,"Created Alarm: "+toString());
	}
	
	public String toString()
	{
		
		return String.format("Type: %s, Level: %s, Threshold: %s, Greaterthan: %s",type,level,threshold,greaterthan);
	}
	

}
