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
	private int _greaterthan;
	private Channel _sourceChannel;
	private int _minThreshold=-1;
	private int _maxThreshold=-1;
	
	private static final int MINIMUM_THRESHOLD_RSSI=20;
	private static final int MAXIMUM_THRESHOLD_RSSI=110;
	private static final int MINIMUM_THRESHOLD_AD=1;
	private static final int MAXIMUM_THRESHOLD_AD=255;
	
	
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
		_sourceChannel = null;
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
	
	public String toString()
	{
		return String.format("Type: %s, Level: %s, Threshold: %s, Greaterthan: %s",_type,_level,_threshold,_greaterthan);
		
		
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
	
	public void setSourceChannel(Channel channel)
	{
		_sourceChannel = channel;
	}
	
	public Channel getSourceChannel()
	{
		return _sourceChannel;
	}
	
	public String getThresholdEng()
	{
		if(_sourceChannel==null)
		{
			return String.valueOf(_threshold);
		}
		else
		{
			Float f = _sourceChannel.getFactor();
			Float o = _sourceChannel.getOffset();
			Float val = (_threshold*f)+o;
			return String.format("%s %s", val,_sourceChannel.getShortUnit());
		}
	}
	
	public String[] getThresholds()
	{
		if(_sourceChannel==null)
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
				out[i] = String.valueOf((i*_sourceChannel.getFactor())+_sourceChannel.getOffset());
			}
			return out;
		}
	}
	
	

}
