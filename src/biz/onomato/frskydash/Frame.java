package biz.onomato.frskydash;

import java.util.Date;

import android.util.Log;

public class Frame {
	private static final String TAG="Frame";
	
	public static final int FRAMETYPE_UNDEFINED=-1;
	public static final int FRAMETYPE_CORRUPT=-2;
	public static final int FRAMETYPE_FRSKY_ALARM=1;
	public static final int FRAMETYPE_ANALOG=0xfe;
	public static final int FRAMETYPE_INPUT_REQUEST_ALARMS_AD=0xf8;
	public static final int FRAMETYPE_INPUT_REQUEST_ALARMS_RSSI=0xf1;
	public static final int FRAMETYPE_ALARM1_RSSI=0xf7;
	public static final int FRAMETYPE_ALARM2_RSSI=0xf6;
	public static final int FRAMETYPE_ALARM1_AD1=0xfc;
	public static final int FRAMETYPE_ALARM2_AD1=0xfb;
	public static final int FRAMETYPE_ALARM1_AD2=0xfa;
	public static final int FRAMETYPE_ALARM2_AD2=0xf9;

	/**
	 * User data frame type. Second byte in frames for user data (telemetry
	 * sensor hub data) are indicated with 0xFD at index 1 (pos 2)
	 */
	public static final int FRAMETYPE_USER_DATA = 0xFD;
	
	// these need to correspond real deal
	//public static final int ALARMLEVEL_OFF=0;
	//public static final int ALARMLEVEL_LOW=1;
	//public static final int ALARMLEVEL_MID=2;
	//public static final int ALARMLEVEL_HIGH=3;
	
	public int frametype;
	public int frameHeaderByte;
	private int[] _frame;
	private int[] _frameRaw;
	
	public int alarmChannel;
	public int alarmNumber;
	public int alarmLevel;
	public int alarmThreshold;
	public int alarmGreaterThan;
	public String alarmGreaterThanString;
	public String humanFrame;
	public Date timestamp;
	
	public int ad1,ad2,rssirx,rssitx = 0;

	//hcpl: some parameters for the new byte per byte reading of frames from input stream
	/**
	 * delimiter byte for telemetry frames (start & stop byte)
	 */
	public static final int START_STOP_TELEMETRY_FRAME = 0x7E;
	
	/**
	 * xor byte for telemetry frames
	 */
	public static final int XOR_TELEMETRY_FRAME = 0x20;
	
	/**
	 * byt stuffing indicator for telementry frames
	 */
	public static final int STUFFING_TELEMETRY_FRAME = 0x7D;
	
	/**
	 * size for telemetry frames
	 */
	public static final int SIZE_TELEMETRY_FRAME = 11;
	
	/**
	 * def ctor using an array of integers as the 11 bytes for a single frame
	 * 
	 * @param frame
	 */
	public Frame(int[] frame)
	{
		//hcpl: reviewed frame length validation
		//eso: unsupported frames should be dealt with by FrSkyServer
//		if (frame.length != Frame.SIZE_TELEMETRY_FRAME
//				|| frame[0] != Frame.START_STOP_TELEMETRY_FRAME
//				|| frame[Frame.SIZE_TELEMETRY_FRAME - 1] != Frame.START_STOP_TELEMETRY_FRAME) {
//			//drop frame and log information
//			Log.d(TAG, "Invalid frame format received: "+Arrays.toString(frame));
//			Log.d(TAG, "Invalid frame format received(hex): "+Frame.frameToHuman(frame));
//			return;
//		}
		
		//if((frame.length>10) && (frame.length<30))
		//{
			timestamp = new Date();
			//Log.i(TAG,"Constructor");
			_frameRaw = frame;
			// fix bytestuffing
		// FIXME this is now done on SerialService level but it needs to be
		// reviewed!
			// eso: temporary fix to allow frames "that are still longer than 11" (from simulator) to get destuffed
			if(frame.length>11)
			{
				frame = frameDecode(frame);
			}
			

			
			_frame = frame;
			
			humanFrame = Frame.frameToHuman(frame);
			
			int _threshold;
			int _greaterthan;
			int _level;
			
			if(frame.length!=Frame.SIZE_TELEMETRY_FRAME)
			{
				frametype=FRAMETYPE_CORRUPT;
				Log.w(TAG,"Found corrupt frame");
			}
			else
			{
			
				frameHeaderByte =frame[1]; 
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
					case FRAMETYPE_INPUT_REQUEST_ALARMS_AD:
						frametype=FRAMETYPE_INPUT_REQUEST_ALARMS_AD;
						break;
					case FRAMETYPE_INPUT_REQUEST_ALARMS_RSSI:
						frametype=FRAMETYPE_INPUT_REQUEST_ALARMS_RSSI;
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
					case FRAMETYPE_USER_DATA:
						//hcpl handle sensor hub information
						frametype=FRAMETYPE_USER_DATA;
						//parsing is done in parseFrame method using Frame object and 
						break;
					default:
						frametype=FRAMETYPE_UNDEFINED;
						
						if(FrSkyServer.D)Log.i(TAG,"Unknown frame:\n"+frameToHuman(frame));
						break;
				}
				if(frametype==FRAMETYPE_FRSKY_ALARM)
				{
					// Value of <AlarmChannel> alarm <AlarmNumber> is <greater> than <alarmthreshold>, and is at level <alarmlevel>
					//Log.w(TAG,"Incoming frame is alarmframe");
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
					//Log.i(TAG,alarmChannel+" alarm "+alarmNumber+": Fires if value is "+_greaterthanhuman+" than "+_threshold+", and is at level "+_level);
					
					
				}
			}
		//}
	}
	
	// hcpl sensor hub data frame parameters
	/**
	 * size for user data frames
	 */
	public static final int SIZE_HUB_FRAME = 5;
	
	/**
	 * delimiter byte for user data frames
	 */
	public static final int START_STOP_HUB_FRAME = 0x5E;
	
	/**
	 * stuffing indicator for the user data frames
	 */
	public static final int STUFFING_HUB_FRAME = 0x5D;
	
	/**
	 * first byte after stuffing indicator should be XORed with this value
	 */
	public static final int XOR_HUB_FRAME = 0x60;

	/**
	 * Handle byte stuffing in case frame has more then 11 bytes
	 * 
	 * eso: Method is needed to parse Simulator frames, as they also are bytestuffed, and not handled by {@link BluetoothSerialService}
	 * Method would probably throw exception if a frame is still longer than 11 bytes after decoding..
	 * 
	 * hcpl: reviewed
	 * 
	 * @param frame
	 *            integer array
	 * @return the frame with stuffed bytes replaced so length matches 11 bytes.
	 *             
	 *             
	 */
	private int[] frameDecode(int[] frame)
	{
		// only for frames longer than the required 11 bits
		if (frame.length == Frame.SIZE_TELEMETRY_FRAME)
			return frame;
		// otherwise we need to handle the bytestuffing here
		// a new frame in proper size for the decode bytes
		int[] decodedFrame = new int[frame.length];
		// set delimiters in decodedFrame, these are fixed
		decodedFrame[0] = frame[0];
		decodedFrame[Frame.SIZE_TELEMETRY_FRAME - 1] = frame[frame.length - 1];
		// index for decoded frame, start at 1
		int di = 1;
		// current byte
		int b = 0;
		// flag for XOR operation
		boolean xor = false;
		// iterate byte per byte of original frame, only count without start and
		// stop bytes already transferred
		for (int i = 1; i < frame.length - 1; i++) {
			// watch that we stay within the expected frame size, don't count
			// the start and stop bytes
			if (di >= Frame.SIZE_TELEMETRY_FRAME - 1) {
				// inform about this issues
//				if(FrSkyServer.D)Log.e(TAG, "Invalid frame length, can't decode frame: " + frameToHuman(frame));
//				// no need to continue here, this will return a wrong formatted
//				// frame now... Maybe best to replace the stop bit with this out
//				// of range byte so we can detect this problem later on
//				decodedFrame[di] = b;
//				break;
				// eso: keep adding bytes, but set frametype to CORRUPT
				
			}
			// get current byte
			b = frame[i];
			// check if this is a stuffed byte
			if (b == Frame.STUFFING_TELEMETRY_FRAME) {
				// set xor flag
				xor = true;
				// drop this byte
				continue;
			}
			// check the xor flag
			if (xor) {
				// copy the byte after xor operation
				decodedFrame[di++] = b ^ Frame.XOR_TELEMETRY_FRAME;
				// always disable xor at this point
				xor = false;
			}
			// otherwis regular copy of byte
			else {
				decodedFrame[di++] = b;
			}
		}
		// return result
		return decodedFrame;
		
		// hcpl: original code
//			int[] outFrameInitial = new int[frame.length];
//			
//			
//			outFrameInitial[0] = frame[0];
//			outFrameInitial[1] = frame[1];
//			int xor = 0x00;
//			int i = 2;
//			
//			for(int n=2;n<frame.length;n++)
//			{
//				if(frame[n]!=0x7d)
//				{
//					outFrameInitial[i] = frame[n]^xor;
//					i++;
//					xor = 0x00;
//				}
//				else
//				{
//					xor = 0x20;
//				}
//			}
//			
//			// make new array with the new length
//			int[] outFrame = new int[i];
//			for(int n=0;n<i;n++)
//			{
//				outFrame[n]=outFrameInitial[n];
//			}
//			
//			Log.i("FRAME decode","Pre:  "+frameToHuman(frame));
//			Log.i("FRAME decode","Post: "+frameToHuman(outFrame));
//			
//			return outFrame;
//		}
//		else
//		{
//			return frame;
//		}	
	}
	
	/**
	 * this ctor will only create an empty Frame object with 11 frames with no
	 * data.
	 */
	public Frame()
	{
		this(new int[11]);
		//Log.i(TAG,"Constructor");
	}
	
	public static Frame InputRequestADAlarms()
	{
		int[] buf = new int[11];
		buf[0] = START_STOP_TELEMETRY_FRAME;
		buf[1] = FRAMETYPE_INPUT_REQUEST_ALARMS_AD;
		buf[10] = START_STOP_TELEMETRY_FRAME;
		return new Frame(buf);
	}
	public static Frame InputRequestRSSIAlarms()
	{
		int[] buf = new int[11];
		buf[0] = START_STOP_TELEMETRY_FRAME;
		buf[1] = FRAMETYPE_INPUT_REQUEST_ALARMS_RSSI;
		buf[10] = START_STOP_TELEMETRY_FRAME;
		return new Frame(buf);
	}

	
	
	public static Frame AlarmFrame(int AlarmType,int AlarmLevel,int AlarmThreshold, int AlarmGreaterThan)
	{
		int[] buf = new int[11];
		buf[0] = START_STOP_TELEMETRY_FRAME;
		buf[1] = AlarmType;
		buf[2] = AlarmThreshold;
		buf[3] = AlarmGreaterThan;
		buf[4] = AlarmLevel;
		buf[10] = START_STOP_TELEMETRY_FRAME;
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
		buf[0] = START_STOP_TELEMETRY_FRAME;
		buf[1] = 0xfe;

		// loop through the simulated values to see if we need to bytestuff the array
		int i = 2;
		for(int n=0;n<inBuf.length;n++)
		{
			if(inBuf[n]==START_STOP_TELEMETRY_FRAME)
			{
				buf[i]=STUFFING_TELEMETRY_FRAME;
				buf[i+1]=0x5e;
				i++;
			}
			else if(inBuf[n]==STUFFING_TELEMETRY_FRAME)
			{
				buf[i]=STUFFING_TELEMETRY_FRAME;
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
		buf[i] = START_STOP_TELEMETRY_FRAME;
		
		int[] outBuf = new int[i+1];
		
		for(int n=0;n<i+1;n++)
		{
			outBuf[n]=buf[n];
		}
		
		return new Frame(outBuf);
	}
	
	
	
	public static String frameToHuman(int[] frame)
	{
		//Date startTime = new Date();
		StringBuffer buf = new StringBuffer();
		
		//Log.i(TAG,"Create human raedable string with "+frame.length+" bytes");
		//int xor = 0x00;
		for(int n=0;n<frame.length;n++)
		{
			String hex ="";
			if(
					(frame[n]==START_STOP_TELEMETRY_FRAME) 		// delimiter
					&& (n>1)				// not first or second byte
					&& (n<frame.length-1) 	// not last byte
				)
			{
				hex = "7d 5e";
			}
			else if(
					(frame[n]==STUFFING_TELEMETRY_FRAME) 		// delimiter
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

		//Log.i(TAG,"String is then: "+out);
		
		//Date endTime = new Date();
		//long duration = endTime.getTime()-startTime.getTime();
		//Log.d(TAG,"Constructing human frame took: "+duration+" ms");
		return buf.toString();
	}
	
	public String toHuman()
	{
		return frameToHuman(_frame);
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
