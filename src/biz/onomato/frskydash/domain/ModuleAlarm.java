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

package biz.onomato.frskydash.domain;

import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.util.Logger;

import com.google.gson.annotations.Expose;

public class ModuleAlarm {

	private static final String TAG = "Alarm";
	
	public static final int ALARMTYPE_UNDEFINED=-1;
	
	public static final int ALARMTYPE_FRSKY=1;
	
	public static final int ALARMLEVEL_OFF=0;
	public static final int ALARMLEVEL_LOW=1;
	public static final int ALARMLEVEL_MID=2;
	public static final int ALARMLEVEL_HIGH=3;
	public static final int GREATERTHAN=1;
	public static final int LESSERTHAN=0;
	
	@Expose
	private int _threshold;
	
	@Expose
	private int _type;
	
	@Expose
	private int _frSkyFrameType;
	
	@Expose
	private int _level;
	
	private int _modelId=-1;
	
	@Expose
	private int _greaterthan;
	
	@Expose
	private String _name="";

	@Expose
	private int _minThreshold=-1;
	
	@Expose
	private int _maxThreshold=-1;
	
	@Expose
	private int _unitChannelId=-1;
	
	@Expose
	private int _sourceChannelId = -1;
	
	@Expose
	private String _unitChannelUnit="";
	
	@Expose
	private float _unitChannelOffset=0;
	
	@Expose
	private float _unitChannelFactor=1;
	
	@Expose
	private int _unitChannelPrecision=0;
	
	private static final int MINIMUM_THRESHOLD_RSSI=20;
	private static final int MAXIMUM_THRESHOLD_RSSI=110;
	private static final int MINIMUM_THRESHOLD_AD=1;
	private static final int MAXIMUM_THRESHOLD_AD=255;
	
	
	public ModuleAlarm(int alarmtype)
	{
		_type = alarmtype;
	}
	
	public ModuleAlarm(int alarmtype,int alarmlevel,int alarmgreaterthan,int alarmthreshold)
	{
		_type=alarmtype;
		_level = alarmlevel;
		_greaterthan = alarmgreaterthan;
		_threshold = alarmthreshold;
		_frSkyFrameType= -1;
		Logger.i(TAG,"Created Alarm: "+toString());
	}
	
	public ModuleAlarm(Frame frame)
	{
		//getCurrentModel().setFrSkyAlarm(f.alarmNumber, f.alarmThreshold, f.alarmGreaterThan, f.alarmLevel);
		_type = ALARMTYPE_FRSKY;
		_level = frame.alarmLevel;
		_greaterthan = frame.alarmGreaterThan;
		_threshold = frame.alarmThreshold;
		//_frSkyFrameType = frame.frameHeaderByte;
		
		setFrSkyFrameType(frame.frameHeaderByte);
		
		
	}
	
	public Frame toFrame()
	{
		Frame frame = Frame.AlarmFrame(
				_frSkyFrameType, 
				_level, 
				_threshold, 
				_greaterthan);
		return frame;
	}
	
	
	public String toString()
	{
		//return String.format("Type: %s, Level: %s, Threshold: %s, Greaterthan: %s",_type,_level,_threshold,_greaterthan);
		String out = _name+": ";

		switch(_level)
		{
			case ALARMLEVEL_OFF:
				out += "off ";
				break;
			case ALARMLEVEL_LOW:
				out += "low ";
				break;
			case ALARMLEVEL_MID:
				out += "medium ";
				break;
			case ALARMLEVEL_HIGH:
				out += "high ";
				break;
		}
		out +="when value is ";
		
		if(_greaterthan==GREATERTHAN)
		{
			out += "greater than "+_threshold;
		}
		else
		{
			out += "lower than "+_threshold;
		}
		if(_unitChannelId!=-1)
		{
			out += " ("+getThresholdEng()+")";
		}
		return out;
	}
	
	public String getName()
	{
		return _name;
	}

	
	public void setGreaterThan(int greaterThan)
	{
		_greaterthan = greaterThan;
	}
	public int getGreaterThan()
	{
		return _greaterthan;
	}
	
	public void setAlarmLevel(int alarmLevel)
	{
		_level = alarmLevel;
	}
	public int getAlarmLevel()
	{
		return _level;
	}
	
	public void setModelId(Model model)
	{
		if(model!=null)
		{
			setModelId(model.getId());
		}
		else
		{
			setModelId(-1);
		}
	}
	
	public void setModelId(int modelId)
	{
		_modelId = modelId;
	}
	
	public int getModelId()
	{
		return _modelId;
	}
	
	
	public int getAlarmType()
	{
		return _type;
	}
	
	public void setFrSkyFrameType(int frameType)
	{
		_frSkyFrameType = frameType;
		switch(_frSkyFrameType)
		{
			case Frame.FRAMETYPE_ALARM1_AD1:
				_minThreshold = MINIMUM_THRESHOLD_AD;
				_maxThreshold = MAXIMUM_THRESHOLD_AD;
				_name = "AD1 - Alarm 1";
				_sourceChannelId = FrSkyServer.CHANNEL_ID_AD1;
				break;
			case Frame.FRAMETYPE_ALARM2_AD1:
				_minThreshold = MINIMUM_THRESHOLD_AD;
				_maxThreshold = MAXIMUM_THRESHOLD_AD;
				_name = "AD1 - Alarm 2";
				_sourceChannelId = FrSkyServer.CHANNEL_ID_AD1;
				break;
			case Frame.FRAMETYPE_ALARM1_AD2:
				_minThreshold = MINIMUM_THRESHOLD_AD;
				_maxThreshold = MAXIMUM_THRESHOLD_AD;
				_name = "AD2 - Alarm 1";
				_sourceChannelId = FrSkyServer.CHANNEL_ID_AD2;
				break;
			case Frame.FRAMETYPE_ALARM2_AD2:
				_minThreshold = MINIMUM_THRESHOLD_AD;
				_maxThreshold = MAXIMUM_THRESHOLD_AD;
				_name = "AD2 - Alarm 2";
				_sourceChannelId = FrSkyServer.CHANNEL_ID_AD2;
				break;
			case Frame.FRAMETYPE_ALARM1_RSSI:
				_minThreshold = MINIMUM_THRESHOLD_RSSI;
				_maxThreshold = MAXIMUM_THRESHOLD_RSSI;
				_name = "RSSI - Alarm 1";
				_sourceChannelId = FrSkyServer.CHANNEL_ID_RSSIRX;
				break;
			case Frame.FRAMETYPE_ALARM2_RSSI:
				_minThreshold = MINIMUM_THRESHOLD_RSSI;
				_maxThreshold = MAXIMUM_THRESHOLD_RSSI;
				_name = "RSSI - Alarm 2";
				_sourceChannelId = FrSkyServer.CHANNEL_ID_RSSIRX;
				break;
		}
	}
	
	public int getFrSkyFrameType()
	{
		return _frSkyFrameType;
	}
	
	public int getUnitChannelId()
	{
		return _unitChannelId;
	}
	
	public int getSourceChannelId()
	{
		return _sourceChannelId;
	}
	
	public void setUnitChannel(int channelId)
	{
		if(channelId>=0)
		{
			//Channel ch = FrSkyServer.database.getChannel(channelId);
			//setUnitChannel(ch);
			_unitChannelId = channelId;
			//setUnitChannel(FrSkyServer.modelMap.get(_modelId).getChannels().get(channelId));
		}
		else
		{
			switch(_frSkyFrameType)	//only RSSI alarms should have default (and locked) unitchannel
			{
				case Frame.FRAMETYPE_ALARM1_AD1:
					break;
				case Frame.FRAMETYPE_ALARM2_AD1:
					break;
				case Frame.FRAMETYPE_ALARM1_AD2:
					break;
				case Frame.FRAMETYPE_ALARM2_AD2:
					break;
				case Frame.FRAMETYPE_ALARM1_RSSI:
					setUnitChannel(FrSkyServer.getSourceChannel(FrSkyServer.CHANNEL_ID_RSSIRX));
					break;
				case Frame.FRAMETYPE_ALARM2_RSSI:
					setUnitChannel(FrSkyServer.getSourceChannel(FrSkyServer.CHANNEL_ID_RSSIRX));
					break;
			}
		}
	}
	
	
	
	
	public void setUnitChannel(Channel channel)
	{
		_unitChannelId = channel.getId();
		_unitChannelFactor = channel.getFactor();
		_unitChannelOffset = channel.getOffset();
		_unitChannelUnit = channel.getShortUnit();
		//_unitChannelDescription = channel.getDescription();
		_unitChannelPrecision = channel.getPrecision();
	}
	
	
	public void setThreshold(int threshold)
	{
		_threshold = threshold;
		if(_threshold<0) _threshold=0;
		if(_threshold>255) _threshold=255;
	}
	
	public int getThreshold()
	{
		return _threshold;
	}
	
	public int getMinThreshold()
	{
		return _minThreshold;
	}
	
	public int getMaxThreshold()
	{
		return _maxThreshold;
	}
	

	public String getThresholdEng()
	{
		if(_unitChannelId==-1)
		{
			return String.valueOf(_threshold);
		}
		else
		{
			Float val = (_threshold*_unitChannelFactor)+_unitChannelOffset;
			return String.format("%."+_unitChannelPrecision+"f %s", val,_unitChannelUnit);
		}
	}
	
	
	
	public String[] getThresholds()
	{
		Logger.i(TAG,"get thresholds: ");
		Logger.i(TAG,"sourcechannel: "+_unitChannelId);
		if(_unitChannelId==-1)
		{
			String[] out = new String[_maxThreshold-_minThreshold];
			for(int i=0;i<_maxThreshold-_minThreshold;i++)
			{
				out[i] = String.valueOf(i+_minThreshold);
			}
			Logger.i(TAG,"Thresholds: "+out.toString());
			return out;
		}
		else
		{
			String[] out = new String[_maxThreshold-_minThreshold];
			for(int i=0;i<_maxThreshold-_minThreshold;i++)
			{
				out[i] = String.format("%s %s",(((i+_minThreshold)*_unitChannelFactor)+_unitChannelOffset),_unitChannelUnit);
			}
			Logger.i(TAG,"Thresholds: "+out.toString());
			return out;
		}
	}

	
	
	@Override 
	public boolean equals(Object o) {
		if (this == o) {
			   return true;
			}

		    // Return false if the other object has the wrong type.
		    // This type may be an interface depending on the interface's specification.
		    if (!(o instanceof ModuleAlarm)) {
		       return false;
		    }
			ModuleAlarm alarm = (ModuleAlarm) o;
			if(
					(this._frSkyFrameType==alarm.getFrSkyFrameType()) &&
					(this._greaterthan==alarm.getGreaterThan()) &&
					(this._level==alarm.getAlarmLevel()) &&
					(this._threshold==alarm.getThreshold())
					)
			{
				return true;
			}
			else
			{
				return false;
			}
			
	}

}
