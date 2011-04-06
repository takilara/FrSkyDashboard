package biz.onomato.frskydash;

import android.util.Log;

public class Frame {
	private static final String TAG="Frame";
	
	public static final int FRAMETYPE_UNDEFINED=-1;
	public static final int FRAMETYPE_ANALOG=0;
	public static final int FRAMETYPE_INPUT_REQUEST_ALL=1;
	public int frametype;
	private int[] _frame;
	
	public int ad1,ad2,rssirx,rssitx = 0;
	
	public Frame(int[] frame)
	{
		if((frame.length>10) && (frame.length<30))
		{
			//Log.i(TAG,"Constructor");
			
			// fix bytestuffing
			if(frame.length>11)
			{
				frame = frameDecode(frame);
			}
			
			_frame = frame;
			
			switch(frame[1])
			{
				// Analog values
				case 0xfe:
					frametype=FRAMETYPE_ANALOG;
					ad1 = frame[2];
					ad2 = frame[3];
					rssirx = frame[4];
					rssitx = (int) frame[5]/2;
					break;
				case 0xf8:
					frametype=FRAMETYPE_INPUT_REQUEST_ALL;
					break;
				default:
					frametype=FRAMETYPE_UNDEFINED;
					
					Log.i(TAG,"Unknown frame:\n"+frameToHuman(frame));
					break;
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
	
}
