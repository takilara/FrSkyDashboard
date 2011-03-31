package biz.onomato.frskydash;

import android.util.Log;

public class Frame {
	private static final String TAG="Frame";
	private int[] _frame;
	
	public Frame(int[] frame)
	{
		//Log.i(TAG,"Constructor");
		_frame = frame;
	}
	
	
	public Frame()
	{
		this(new int[11]);
		//Log.i(TAG,"Constructor");
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
	
	public String toHuman()
	{
		return Frame.frameToHuman(_frame);
	}
	
	public int[] toInts()
	{
		return _frame;
	}
	
}
