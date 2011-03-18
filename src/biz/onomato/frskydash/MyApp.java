package biz.onomato.frskydash;

import android.app.Application;
import android.util.Log;
import java.util.HashMap;

public class MyApp extends Application {
	private int MAX_CHANNELS=4;
	private int[] hRaw;
	private float[] hVal;
	private String[] hName;
	private String[] hDescription;
	private float[] hOffset;
	private float[] hFactor;
	private String[] hUnit;
	private String[] hLongUnit;
	private int channels=0;
	private Channel[] objs;
	
	public MyApp(){
		hRaw = new int[MAX_CHANNELS];
		hVal = new float[MAX_CHANNELS];
		hName = new String[MAX_CHANNELS];
		hDescription = new String[MAX_CHANNELS];
		hOffset = new float[MAX_CHANNELS];
		hFactor = new float[MAX_CHANNELS];
		hUnit = new String[MAX_CHANNELS];
		hLongUnit = new String[MAX_CHANNELS];
		objs = new Channel[MAX_CHANNELS];

	}
	
	public int createChannel(String name,String description,float offset,float factor,String unit,String longUnit)
	{
		Channel AD1 =  new Channel(name, description, offset, factor, unit, longUnit);
		objs[channels] = AD1;
		Log.i("MyApp","createChannel");
		hRaw[channels]=-1;
		hVal[channels]=-1;
		hName[channels]=name;
		hDescription[channels]=description;
		hOffset[channels]=offset;
		hFactor[channels]=factor;
		hUnit[channels]=unit;
		hLongUnit[channels]=longUnit;
		channels += 1;
		return channels-1;
	}
	
	public Channel getChannelById(int id)
	{
		return objs[id];
	}
	
	
	public float setChannelById(int id,int rawVal)
	{
		objs[id].setRaw(rawVal);
		
		Log.i("MyApp","Set channel to some value");
		hRaw[id] = rawVal;
		hVal[id]=hRaw[id]*hFactor[id]+hOffset[id];
		return (float) (hVal[id]);
	}

}

