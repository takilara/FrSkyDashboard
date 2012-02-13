package biz.onomato.frskydash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.app.Service;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class Simulator {
    private Handler simHandler;
    private Runnable runnableSimulator;
    
    public int _ad1,_ad2,_rssirx,_rssitx;
    private int[] _simFrame;
    public boolean running;
    
    public boolean noise = false;
    
    //private FrSkyServer context;
    
    private FrSkyServer server;
    
    
    private static final String TAG="Simulator Class";
    
	public Simulator(Service srv)
	{
		//context = (FrSkyServer) cnt;
		server = (FrSkyServer) srv;
		if(FrSkyServer.D)Log.i(TAG,"constructor");
		running = false;
		_ad1 = 0;
		_ad2 = 0;
		_rssirx = 0;
		_rssitx = 0;
		
		//globals = ((MyApp) getApplicationContext());	
		
		simHandler = new Handler();
		
		runnableSimulator = new Runnable() {
			//@Override
			public void run()
			{
				_ad1 +=1;
				if(_ad1>=255) {_ad1=0;}
				
				_ad2 -=1;
				if(_ad2<=0) {_ad2=255;}
				
				//Log.i("SIM","Automatic post new frame");
				
				//_simFrame = genFrame(_ad1,_ad2,_rssirx,_rssitx);
				Frame f = Frame.FrameFromAnalog(_ad1,_ad2,_rssirx,_rssitx);
				//deliberately break the frame

				_simFrame = f.toInts();

				// Corrupt data
				if(noise)
					{
					if(Math.random()*100>90)
					{
						int brokenBytePos = (int) (Math.random()*Frame.SIZE_TELEMETRY_FRAME);
						int newInt = (int) (Math.random()*255);
						//Log.w(TAG,"Modifying frame, setting pos: "+brokenBytePos+" to "+newInt);
						_simFrame[brokenBytePos]=newInt;
					}
					// Bad frame size
					if(Math.random()*100>90)
					{
						ArrayList<Integer> l = new ArrayList<Integer>();
						for(int v=0;v<_simFrame.length;v++)
						{
							l.add(_simFrame[v]);
						}
						
						int brokenBytePos = (int) (Math.random()*Frame.SIZE_TELEMETRY_FRAME);
						int newInt = (int) (Math.random()*255);
						if(Math.random()*100>50)
						{
							l.add(brokenBytePos,newInt);
							//Log.w(TAG,"Modifying frame, adding a byte at pos "+brokenBytePos);
						}
						else
						{
							l.remove(brokenBytePos);
							//Log.w(TAG,"Modifying frame, removing a byte at pos "+brokenBytePos);
						}
						
						_simFrame = new int[l.size()];
						for(int v=0;v<_simFrame.length;v++)
						{
							_simFrame[v]=(Integer) l.get(v);
						}
						
					}
					f = new Frame(_simFrame);
				}

				//context.parseFrame(_simFrame);
				server.parseFrame(f);
				//outFrame_tv.setText(globals.frameToHuman(simFrame));
				//globals.parseFrame(simFrame);
				
				simHandler.removeCallbacks(runnableSimulator);
				simHandler.postDelayed(runnableSimulator, 10);
			}
		};
		
	}
	
	public int[] getFrame()
	{
		return _simFrame;
	}
	
	public void start(){
		if(FrSkyServer.D)Log.i(TAG,"Starting sim thread");
		simHandler.removeCallbacks(runnableSimulator);
		simHandler.postDelayed(runnableSimulator, 30);
		running = true;
		
	}
	
	public void stop(){
		if(FrSkyServer.D)Log.i(TAG,"Stopping sim thread");
		try
		{
			simHandler.removeCallbacks(runnableSimulator);
		}
		catch (Exception e)
		{
			
		}
		running = false;
	}

	public void reset()
	{
		stop();
		_ad1=0;
		_ad2=0;
		_rssitx=0;
		_rssirx=0;
	}
	
	public int[] genFrame(int ad1,int ad2,int rssirx,int rssitx)
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
				if(FrSkyServer.D)Log.i(TAG,"Bytestuffing 7E to 7d 5e");
			}
			else if (inBuf[n]==0x7d)
			{
				buf[i]=0x7d;
				buf[i+1]=0x5d;
				i++;
				if(FrSkyServer.D)Log.i(TAG,"Bytestuffing 7D to 7d 5d");
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
		
		return outBuf;
	}
}
