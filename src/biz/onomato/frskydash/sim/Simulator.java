/*
 * Copyright 2011-2013, Espen Solbu, Hans Cappelle
 * 
 * This file is part of FrSky Dashboard.
 *
 *  FrSky Dashboard is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FrSky Dashboard is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FrSky Dashboard.  If not, see <http://www.gnu.org/licenses/>.
 */

package biz.onomato.frskydash.sim;

import java.util.ArrayList;

import android.app.Service;
import android.os.Handler;
import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.domain.Frame;
import biz.onomato.frskydash.util.Logger;

/**
 * Simulator for faking streams so we can test application without actual
 * connection
 * 
 */
public class Simulator {
	
    private Handler simHandler;
    private Runnable runnableSimulator;
    
    public int _ad1,_ad2,_rssirx,_rssitx;
    private int[] _simFrame;
    
    /**
     * if simulator is currently running or not
     */
    public boolean running;
    
    /**
     * if this simulator should create error frames or not
     */
    private boolean noise = false;
    
	/**
	 * If this is enabled the simulator will also create sensor hub data frames
	 */
    private boolean sensorData = false;
        
	/**
     * a reference to the server (service running in the background)
     */
    private FrSkyServer server;
    
    /**
     * tag used for logging
     */
    private static final String TAG="Simulator Class";

    /**
     * default ctor
     * 
     * @param srv The FrSkyServer
     */
	public Simulator(Service srv)
	{
		server = (FrSkyServer) srv;
		Logger.i(TAG,"constructor");
		
		// init
		running = false;
		_ad1 = 0;
		_ad2 = 0;
		_rssirx = 0;
		_rssitx = 0;
		
		simHandler = new Handler();
		
		//create runnable to iterate fake values
		runnableSimulator = new Runnable() {
			//@Override
			public void run()
			{
				// init
				Frame f = null;
				
				// create either sensor hub data or other data on a 50/50 basis
				// if sensor data generation is enabled
				if (sensorData && Math.random() > 0.5) {
					// generate sensor hub data
					_simFrame = SensorHubDataGenerator
							.generateSimulatedSensorHubData();
					f = new Frame(_simFrame);
				}
				// original frsky data sim
				else {
					// increase A1 value for simulator use only
					_ad1 = _ad1 >= 255 ? 0 : _ad1 + 1;
					// decrease A2 value for simulator use only
					_ad2 = _ad2 <= 0 ? _ad2 = 255 : _ad2 - 1;

					// Log.i("SIM","Automatic post new frame");

					// _simFrame = genFrame(_ad1,_ad2,_rssirx,_rssitx);
					f = Frame.FrameFromAnalog(_ad1, _ad2, _rssirx,
							_rssitx);

					// convert
					_simFrame = f.toInts();
				}

				//deliberately break the frame
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
		Logger.i(TAG,"Starting sim thread");
		simHandler.removeCallbacks(runnableSimulator);
		simHandler.postDelayed(runnableSimulator, 30);
		running = true;
		
	}
	
	public void stop(){
		Logger.i(TAG,"Stopping sim thread");
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
	
	/**
	 * update if this simulator needs to generate sensor hub frame data as well
	 * 
	 * @param sensorData
	 */
	public void setSensorData(boolean sensorData) {
		this.sensorData = sensorData;
	}
	
    public boolean isSensorData() {
		return sensorData;
	}

	/**
	 * update if simulator should create noise also when generating frames. If
	 * noise is set a percent (10%?) of the generated frames will contain
	 * errors.
	 * 
	 * @param noise
	 */
	public void setNoise(boolean noise) {
		this.noise = noise;
	}

	/**
	 * 
	 * FIXME is this a duplicate? If so remove for code clean up
	 * 
	 * @deprecated don't think this is used anymore. See
	 *             {@link Frame#FrameFromAnalog(int, int, int, int)} instead.
	 */
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
				Logger.i(TAG,"Bytestuffing 7E to 7d 5e");
			}
			else if (inBuf[n]==0x7d)
			{
				buf[i]=0x7d;
				buf[i+1]=0x5d;
				i++;
				Logger.i(TAG,"Bytestuffing 7D to 7d 5d");
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
