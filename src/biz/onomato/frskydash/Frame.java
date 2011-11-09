package biz.onomato.frskydash;

import android.util.Log;

public class Frame {
	private static final String TAG="Frame";
	
	public static final int FRAMETYPE_UNDEFINED=-1;
	public static final int FRAMETYPE_FRSKY_ALARM=1;
	public static final int FRAMETYPE_ANALOG=0xfe;
	public static final int FRAMETYPE_INPUT_REQUEST_ALL=0xf8;
	public static final int FRAMETYPE_ALARM1_RSSI=0xf7;
	public static final int FRAMETYPE_ALARM2_RSSI=0xf6;
	public static final int FRAMETYPE_ALARM1_AD1=0xfc;
	public static final int FRAMETYPE_ALARM2_AD1=0xfb;
	public static final int FRAMETYPE_ALARM1_AD2=0xfa;
	public static final int FRAMETYPE_ALARM2_AD2=0xf9;
	
	// these need to correspond real deal
	public static final int ALARMLEVEL_OFF=0;
	public static final int ALARMLEVEL_LOW=1;
	public static final int ALARMLEVEL_MID=2;
	public static final int ALARMLEVEL_HIGH=3;
	
	
	public int frametype;
	private int[] _frame;
	private int[] _frameRaw;
	
	public int alarmChannel;
	public int alarmNumber;
	public int alarmLevel;
	public int alarmThreshold;
	public int alarmGreaterThan;
	public String alarmGreaterThanString; 
	
	
	public int ad1,ad2,rssirx,rssitx = 0;
	
	public Frame(int[] frame)
	{
		if((frame.length>10) && (frame.length<30))
		{
			//Log.i(TAG,"Constructor");
			_frameRaw = frame;
			// fix bytestuffing
			if(frame.length>11)
			{
				frame = frameDecode(frame);
			}
			
			_frame = frame;
			
			int _threshold;
			int _greaterthan;
			int _level;
			
			switch(frame[1])
			{
				// Analog values
				case FRAMETYPE_ANALOG:
					frametype=FRAMETYPE_ANALOG;
					ad1 = frame[2];
					ad2 = frame[3];
					rssirx = frame[4];
					rssitx = (int) frame[5]/2;
					break;
				case FRAMETYPE_INPUT_REQUEST_ALL:
					frametype=FRAMETYPE_INPUT_REQUEST_ALL;
					break;
				case FRAMETYPE_ALARM1_AD1:
					frametype=FRAMETYPE_FRSKY_ALARM;
					alarmChannel = Channel.CHANNELTYPE_AD1;
					alarmNumber = 0;
					
					break;
				case FRAMETYPE_ALARM2_AD1:
					frametype=FRAMETYPE_FRSKY_ALARM;
					alarmChannel = Channel.CHANNELTYPE_AD1;
					alarmNumber = 1;
					break;	
				case FRAMETYPE_ALARM1_AD2:
					frametype=FRAMETYPE_FRSKY_ALARM;
					alarmChannel = Channel.CHANNELTYPE_AD2;
					alarmNumber = 0;
					break;
				case FRAMETYPE_ALARM2_AD2:
					frametype=FRAMETYPE_FRSKY_ALARM;
					alarmChannel = Channel.CHANNELTYPE_AD2;
					alarmNumber = 1;
					break;
				case FRAMETYPE_ALARM1_RSSI:
					frametype=FRAMETYPE_FRSKY_ALARM;
					alarmChannel = Channel.CHANNELTYPE_RSSI;
					alarmNumber = 0;
					break;
				case FRAMETYPE_ALARM2_RSSI:
					frametype=FRAMETYPE_FRSKY_ALARM;
					alarmChannel = Channel.CHANNELTYPE_RSSI;
					alarmNumber = 1;
					break;
				default:
					frametype=FRAMETYPE_UNDEFINED;
					
					Log.i(TAG,"Unknown frame:\n"+frameToHuman(frame));
					break;
			}
			if(frametype==FRAMETYPE_FRSKY_ALARM)
			{
				// Value of <AlarmChannel> alarm <AlarmNumber> is <greater> than <alarmthreshold>, and is at level <alarmlevel>
				_threshold = frame[2];
				_greaterthan = frame[3];
				String _greaterthanhuman;
				if(_greaterthan==1)
				{
					_greaterthanhuman="greater";
				}
				else
				{
					_greaterthanhuman="lower";
				}
				
				_level = frame[4];
				
				alarmLevel = _level;
				alarmThreshold=_threshold;
				alarmGreaterThan = _greaterthan;
				alarmGreaterThanString = _greaterthanhuman;
				Log.i(TAG,alarmChannel+" alarm "+alarmNumber+": Fires if value is "+_greaterthanhuman+" than "+_threshold+", and is at level "+_level);
				
				
			}
		}
	}
	
	
	
	private int[] frameDecode(int[] frame)
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
	
	public Frame()
	{
		this(new int[11]);
		//Log.i(TAG,"Constructor");
	}
	
	public static Frame InputRequestAll()
	{
		int[] buf = new int[11];
		buf[0] = 0x7e;
		buf[1] = 0xf8;	// Request All
		buf[10] = 0x7e;	// Request All
		return new Frame(buf);
	}
	
	public static Frame AlarmFrame(int AlarmType,int AlarmLevel,int AlarmThreshold, int AlarmGreaterThan)
	{
		int[] buf = new int[11];
		buf[0] = 0x7e;
		buf[1] = AlarmType;
		buf[2] = AlarmThreshold;
		buf[3] = AlarmGreaterThan;
		buf[4] = AlarmLevel;
		buf[10] = 0x7e;
		return new Frame(buf);
	}
	
	public static Frame FrameFromAnalog(int ad1,int ad2,int rssirx,int rssitx)
	{
		int[] inBuf = new int[4];
		int[] buf = new int[30];
		
		inBuf[0] = ad1;
		inBuf[1] = ad2;
		inBuf[2] = rssirx;
		inBuf[3] = rssitx*2 & 0xff;
		
		// Add the header
		buf[0] = 0x7e;
		buf[1] = 0xfe;

		// loop through the simulated values to see if we need to bytestuff the array
		int i = 2;
		for(int n=0;n<inBuf.length;n++)
		{
			if(inBuf[n]==0x7e)
			{
				buf[i]=0x7d;
				buf[i+1]=0x5e;
				i++;
			}
			else if(inBuf[n]==0x7d)
			{
				buf[i]=0x7d;
				buf[i+1]=0x5d;
				i++;
			}
			else
			{
				buf[i] = inBuf[n];
			}
			i++;
		}
		
		// add the last 4 0x00's
		for(int n=0;n<4;n++)
		{
			buf[i]=0x00;
			i++;
		}
		
		// add the ending 0x7e
		buf[i] = 0x7e;
		
		int[] outBuf = new int[i+1];
		
		for(int n=0;n<i+1;n++)
		{
			outBuf[n]=buf[n];
		}
		
		return new Frame(outBuf);
	}
	
	
	
	public static String frameToHuman(int[] frame)
	{
		StringBuffer buf = new StringBuffer();
		//Log.i(TAG,"Create human raedable string with "+frame.length+" bytes");
		int xor = 0x00;
		for(int n=0;n<frame.length;n++)
		{
			String hex ="";
			if(
					(frame[n]==0x7e) 		// delimiter
					&& (n>1)				// not first or second byte
					&& (n<frame.length-1) 	// not last byte
				)
			{
				hex = "7d 5e";
			}
			else if(
					(frame[n]==0x7d) 		// delimiter
					&& (n>1)				// not first or second byte
					&& (n<frame.length-1) 	// not last byte
				)
			{
				hex = "7d 5d";
			}
			else
			{
				hex = Integer.toHexString(frame[n]);
				if(hex.length()==1)
				{
					hex = "0"+hex;
				}
			}
			// Need to append in case it returns 0xf etc
			//if(hex.length()==1)
			//{
//				buf.append('0');
			//}
			buf.append(hex);
			if(n<frame.length-1)
			{
				buf.append(' ');
			}
		
		}
		String out = buf.toString();
		//Log.i(TAG,"String is then: "+out);
		return out;
	}
	
	public String toHuman()
	{
		return Frame.frameToHuman(_frame);
	}
	
	public int[] toInts()
	{
		return _frame;
	}
	
	public int[] toEncodedInts()
	{
		return _frameRaw;
	}
	
	public byte[] toRawBytes()
	{
		byte[] buf = new byte[_frameRaw.length];
		for(int n=0;n<_frameRaw.length;n++)
		{
			buf[n]=(byte) _frameRaw[n];
		}
		return buf;
	}
	
}