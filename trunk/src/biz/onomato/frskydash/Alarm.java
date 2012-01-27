package biz.onomato.frskydash;

import android.util.Log;
import android.widget.ArrayAdapter;

public class Alarm {

	private static final String TAG = "Alarm";
	public static final int ALARMTYPE_UNDEFINED=-1;
	public static final int ALARMTYPE_FRSKY=1;
	
	public static final int ALARMLEVEL_OFF=0;
	public static final int ALARMLEVEL_LOW=1;
	public static final int ALARMLEVEL_MID=2;
	public static final int ALARMLEVEL_HIGH=3;
	public static final int GREATERTHAN=1;
	public static final int LESSERTHAN=0;
	
	private int _threshold;
	private int _type;
	private int _frSkyFrameType;
	private int _level;
	private int _modelId=-1;
	private int _greaterthan;
	//private Channel _sourceChannel;

	
	private int _minThreshold=-1;
	private int _maxThreshold=-1;
	
	private int _sourceChannelId=-1;
	private String _sourceChannelUnit="";
	private float _sourceChannelOffset=0;
	private float _sourceChannelFactor=1;
	
	private static final int MINIMUM_THRESHOLD_RSSI=20;
	private static final int MAXIMUM_THRESHOLD_RSSI=110;
	private static final int MINIMUM_THRESHOLD_AD=1;
	private static final int MAXIMUM_THRESHOLD_AD=255;
	
	public Alarm(int alarmtype)
	{
		_type = alarmtype;
	}
	
	public Alarm(int alarmtype,int alarmlevel,int alarmgreaterthan,int alarmthreshold)
	{
		_type=alarmtype;
		_level = alarmlevel;
		_greaterthan = alarmgreaterthan;
		_threshold = alarmthreshold;
		_frSkyFrameType= -1;
		Log.i(TAG,"Created Alarm: "+toString());
	}
	
	public Alarm(Frame frame)
	{
		//getCurrentModel().setFrSkyAlarm(f.alarmNumber, f.alarmThreshold, f.alarmGreaterThan, f.alarmLevel);
		_type = ALARMTYPE_FRSKY;
		_level = frame.alarmLevel;
		_greaterthan = frame.alarmGreaterThan;
		_threshold = frame.alarmThreshold;
		_frSkyFrameType = frame.frameHeaderByte;
		
		if((_frSkyFrameType==Frame.FRAMETYPE_ALARM1_AD1) || 
				(_frSkyFrameType==Frame.FRAMETYPE_ALARM2_AD1) || 
				(_frSkyFrameType==Frame.FRAMETYPE_ALARM1_AD2) ||
				(_frSkyFrameType==Frame.FRAMETYPE_ALARM2_AD2))
		{
			_minThreshold = MINIMUM_THRESHOLD_AD;
			_maxThreshold = MAXIMUM_THRESHOLD_AD;
		}
		else if((_frSkyFrameType==Frame.FRAMETYPE_ALARM1_RSSI) || 
				(_frSkyFrameType==Frame.FRAMETYPE_ALARM2_RSSI))
		{
			_minThreshold = MINIMUM_THRESHOLD_RSSI;
			_maxThreshold = MAXIMUM_THRESHOLD_RSSI;
		}
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
		return String.format("Type: %s, Level: %s, Threshold: %s, Greaterthan: %s",_type,_level,_threshold,_greaterthan);
		
		
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
		setModelId(model.getId());
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
	}
	public int getFrSkyFrameType()
	{
		return _frSkyFrameType;
	}
	
	public int getSourceChannel()
	{
		return _sourceChannelId;
	}
	
	public void setSourceChannel(int channelId)
	{
		Channel ch = FrSkyServer.database.getChannel(channelId);
		setSourceChannel(ch);
	}
	
	
	
	
	public void setSourceChannel(Channel channel)
	{
		_sourceChannelId = channel.getId();
		_sourceChannelFactor = channel.getFactor();
		_sourceChannelOffset = channel.getOffset();
		_sourceChannelUnit = channel.getShortUnit();
	}
	
//	public Channel getSourceChannel()
//	{
//		return _sourceChannel;
//	}
	
	
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
	
	public String getThresholdEng()
	{
		if(_sourceChannelId==-1)
		{
			return String.valueOf(_threshold);
		}
		else
		{
			Float val = (_threshold*_sourceChannelFactor)+_sourceChannelOffset;
			return String.format("%s %s", val,_sourceChannelUnit);
		}
	}
	
	public String[] getThresholds()
	{
		if(_sourceChannelId==-1)
		{
			String[] out = new String[_maxThreshold-_minThreshold];
			for(int i=_minThreshold;i<_maxThreshold;i++)
			{
				out[i] = String.valueOf(i);
			}
			return out;
		}
		else
		{
			String[] out = new String[_maxThreshold-_minThreshold];
			for(int i=_minThreshold;i<_maxThreshold;i++)
			{
				out[i] = String.format("%s %s",((i*_sourceChannelFactor)+_sourceChannelOffset),_sourceChannelUnit);
			}
			return out;
		}
	}
	
	

}
