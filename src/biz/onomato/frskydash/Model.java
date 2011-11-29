package biz.onomato.frskydash;

import java.util.ArrayList;

import android.util.Log;

public class Model {
	
	private static final String TAG="ModelClass";
	
	public static final int MODEL_TYPE_HELICOPTER=0;
	public static final int MODEL_TYPE_FIXEDWING=1;
	public static final int MODEL_TYPE_CAR=2;
	public static final int MODEL_TYPE_BOAT=3;
	public static final int MODEL_TYPE_MULTIROTOR=4;
	public static final int MODEL_TYPE_UNKNOWN=-1;
	
	private static final boolean DEBUG=true;
	
	private ArrayList<Channel> _channels;
	
	private int _type;
	private String _name;
	

	// Constructor
	public Model(String modelName,int modelType)
	{
		setName(modelName);
		setType(modelType);
		_channels = new ArrayList<Channel>();
	}
	
	public Model(String modelName)
	{
		this(modelName,MODEL_TYPE_UNKNOWN);
	}
	public Model()
	{
		this("Model 1",MODEL_TYPE_UNKNOWN);
	}
	


	
	
	public int getType() {
		return _type;
	}


	public void setType(int modelType) {
		this._type = modelType;
	}


	public String getName() {
		return _name;
	}


	public void setName(String modelName) {
		this._name = modelName;
	}


	// I need to be able to add channels to this model
	public void addChannel(Channel channel)
	{
		if(!_channels.contains(channel))
		{
			_channels.add(channel);
		}
	}
	
	// I need to be able to delete channels from this model
	public boolean removeChannel(Channel channel)
	{
		return _channels.remove(channel);
	}
	
	// I need to be able to return list of channels from this model
	public Channel[] getChannels()
	{
		Channel[] outChannels = new Channel[_channels.size()];
		int i=0;
		for(Channel ch:_channels)
		{
			outChannels[i]=ch;
			i++;
		}
		return outChannels;
	}
	
	// I need to be able to add alarms to this model
	public void addAlarm(Alarm alarm)
	{
		
	}
	
	
	// I need to be able to delete alarms from this model
	public void deleteAlarm(Alarm alarm)
	{
		
	}
	
	// I need to be able to save settings to file or config storage
	public void saveSettings()
	{
		
	}
	
	// I need to be able to load settings from file or config storage
	public void loadSettings()
	{
		
	}
	
	public boolean loadFromSettings(String modelName)
	{
		// False if not found
		
		// True if found
		return true;
	}
	
	public static Model createFromSettings(String modelName)
	{
		if(DEBUG) Log.d(TAG,"Load settings for '"+modelName+"' from settingsstore");
		Model m = new Model(modelName);
		m.loadFromSettings(modelName);
		
		// if not found, create it before returning it
		return m;
	}

}
