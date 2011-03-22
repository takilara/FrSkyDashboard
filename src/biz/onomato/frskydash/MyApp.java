package biz.onomato.frskydash;

import android.app.Application;
import android.util.Log;
import java.util.HashMap;
import java.util.Locale;




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
	
	private String TAG="Globals";
	
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
	
	public boolean parseFrame(int[] frame)
	{
		boolean ok=true;
		
		
		switch(frame[1])
		{
			// Analog values
			case 0xfe:
				ok = parseAnalogFrame(frame);
				break;
			
			default:
				ok=false;
				break;
		}
		return ok;
	}
	
	public boolean parseAnalogFrame(int[] frame)
	{
		boolean ok=true;
		int ad1,ad2 = -1;
		int rssirx,rssitx=-1;
		
		// only do bytestuff decoding if neccessary
		if(frame.length>11)
		{
			frame = frameDecode(frame);
		}
		ad1 = frame[2];
		ad2 = frame[3];
		rssirx = frame[4];
		rssitx = (int) frame[5]/2;

		
		return ok;
	}
	
	public int[] frameDecode(int[] frame)
	{
		if(frame.length>11)
		{
			int[] outFrame = new int[11];
			
			outFrame[0] = frame[0];
			outFrame[1] = frame[1];
			int xor = 0x00;
			int i = 2;
			
			for(int n=2;n<frame.length;n++)
			{
				if(frame[n]!=0x7d)
				{
					outFrame[i] = frame[n]^xor;
					i++;
					xor = 0x00;
				}
				else
				{
					xor = 0x20;
				}
			}
			
			Log.i("FRAME decode","Pre:  "+frameToHuman(frame));
			Log.i("FRAME decode","Post: "+frameToHuman(outFrame));
			
			return outFrame;
		}
		else
		{
			return frame;
		}	
	}
	
	public String frameToHuman(int[] frame)
	{
		StringBuffer buf = new StringBuffer();
//		byte[] inB = new byte[in.length()];
//		char[] inC = in.toCharArray();
//		inB = in.getBytes();
		for(int n=0;n<frame.length;n++)
		{
			String hex = Integer.toHexString(frame[n]);
			// Need to append in case it returns 0xf etc
			if(hex.length()==1)
			{
				buf.append('0');
			}
			buf.append(hex);
			buf.append(' ');
		}
		String out = buf.toString();
		return out;
	}
	    

}

