package biz.onomato.frskydash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.util.Log;

public class Model {
	
	private static final String TAG="ModelClass";
	
	public static final int MODEL_TYPE_HELICOPTER=0;
	public static final int MODEL_TYPE_FIXEDWING=1;
	public static final int MODEL_TYPE_CAR=2;
	public static final int MODEL_TYPE_BOAT=3;
	public static final int MODEL_TYPE_MULTIROTOR=4;
	public static final int MODEL_TYPE_UNKNOWN=-1;
	 
	
	
	
	//private ArrayList<Channel> _channels;
	private TreeMap<Integer,Channel> channelMap;
	private String[] _modelTypes;
	
	private String _type;
	private String _name;
	private int _id;
	//private Context _context;
	//public Alarm[] alarms;
	TreeMap<Integer,Alarm> frSkyAlarms;
	public int alarmCount = 0;
	
	public boolean dirty=false;

	
	
	//private static DBAdapterModel db;
	

	// Constructor
	public Model(String modelName,String modelType)
	{
		//_context = context;
		_modelTypes = FrSkyServer.getContext().getResources().getStringArray(R.array.model_types);
		// create if neccessary
		//db = new DBAdapterModel(context);
		
		// FRSKY channels only for now
		//alarms = new Alarm[6];
		frSkyAlarms = new TreeMap<Integer, Alarm>();
		channelMap = new TreeMap<Integer,Channel>();
		
		
		// populate FrSky Alarms with defaults
		//initiateFrSkyAlarms();
		
		
		_id = -1; 
		setName(modelName);
		if(modelType.equals(""))
		{
			setType(_modelTypes[0]);
		}
		else
		{
			setType(modelType);
		}
		//_channels = new ArrayList<Channel>();
		//setId(-1);
	}
	

	
	public Model(String modelName)
	{
		
		this(modelName,"");
	}
	public Model()
	{
		this("Model 1","");
	}
	

	/**
	 * Should be called when you want to "release" a model
	 * 
	 * Stops any channel listeners
	 * Removes channel references
	 * Removes alarm references 
	 */
	public void close()
	{
		if(FrSkyServer.D)Log.d(TAG,_name+": Resetting myself and all my components");
		for(Channel c: getChannels().values())
		{
			c.reset();
			c= null;
		}
		channelMap.clear();
	}
	
	
	public String getType() {
		return _type;
	}


	public void setType(String modelType) {
		if(FrSkyServer.D)Log.d(TAG,"Setting model type to: "+modelType);
		this._type = modelType;
	}


	public void setId(int id)
	{
		_id = id;
	}
	
	public int getId()
	{
		return _id;
	}

	public String getName() {
		return _name;
	}


	public void setName(String modelName) {
		this._name = modelName;
	}

	public void initializeDefaultChannels()
	{
		Channel ad1raw = new Channel();
		ad1raw.setDescription("AD1 raw");
		ad1raw.setModelId(_id);
		ad1raw.setSourceChannel(FrSkyServer.CHANNEL_ID_AD1);
		ad1raw.setId(-1);
		ad1raw.setSilent(true);
		ad1raw.setLongUnit("");
		ad1raw.setShortUnit("");
		ad1raw.setPrecision(0);
		ad1raw.setMovingAverage(0);
		// save to force id update
		//FrSkyServer.database.saveChannel(ad1raw);
		addChannel(ad1raw);
		
		Channel ad2raw = new Channel();
		ad2raw.setDescription("AD2 raw");
		ad2raw.setModelId(_id);
		ad2raw.setSourceChannel(FrSkyServer.CHANNEL_ID_AD2);
		ad2raw.setId(-1);
		ad2raw.setSilent(true);
		ad2raw.setLongUnit("");
		ad2raw.setShortUnit("");
		ad2raw.setPrecision(0);
		ad2raw.setMovingAverage(0);
		// save to force id update		
		//FrSkyServer.database.saveChannel(ad2raw);
		addChannel(ad2raw);
		dirty=true;
	}

	// I need to be able to add channels to this model
	public void addChannel(Channel channel)
	{
		if(channel.getId()==-1)
		{
			FrSkyServer.saveChannel(channel);
		}
		channelMap.put(channel.getId(), channel);
	}
	
	// I need to be able to delete channels from this model
	public boolean removeChannel(Channel channel)
	{
		channel.unregisterListener();
		channelMap.remove(channel.getId());
		return true;
		//return _channels.remove(channel);
	}
	
	// I need to be able to set a given channel for this model
	//public void setChannel(int id, Channel channel)
	public void setChannel(Channel channel)
	{
		addChannel(channel);
	}
	
	public void setChannels(ArrayList<Channel> channels)
	{
		for(Channel c : channels)
		{
			addChannel(c);
		}
	}
	
	public void setChannels(TreeMap<Integer,Channel> channels)
	{
		channelMap = channels;
	}
	
	// I need to be able to return list of channels from this model
	//public ArrayList<Channel> getChannels()
	public TreeMap<Integer,Channel> getChannels()
	{
		return channelMap;
		//return _channels;
	}
	
	// I need to be able to add alarms to this model
	public void addAlarm(Alarm alarm)
	{
		//if(DEBUG)Log.i(TAG,"Adding alarm: "+alarm);
		if(alarm.getAlarmType()==Alarm.ALARMTYPE_FRSKY)
		{
			//--> add to frSkyAlarms
			//if(DEBUG)Log.i(TAG,"FrSky alarm of type: "+alarm.getFrSkyFrameType());
			frSkyAlarms.put(alarm.getFrSkyFrameType(), alarm);
		}
		else
		{
			Log.e(TAG,"Unhandled Alarm Type");
			
		}

	}
	public TreeMap<Integer,Alarm> getFrSkyAlarms()
	{
		return frSkyAlarms;
	}
	
	public void setFrSkyAlarms(TreeMap<Integer,Alarm> alarmMap)
	{
		if(alarmMap.size()>0)
		{
			for(Alarm a:alarmMap.values())
			{
				a.setModelId(_id);
				addAlarm(a);
			//	alarmCount += 1;
			}
		}
		else
		{
			//initiateFrSkyAlarms();
		}
	}
	
	
	// I need to be able to delete alarms from this model
	public void deleteAlarm(Alarm alarm)
	{
		
	}
	
	
	
	// I need to be able to load settings from file or config storage
	public void loadSettings()
	{
		
	}
	
	public ArrayList<Channel> getAllowedSourceChannels()
	{
		return getAllowedSourceChannels(null);
	}
	public ArrayList<Channel> getAllowedSourceChannels(Alarm alarm)
	{
		ArrayList<Channel> sourceChannels = new ArrayList<Channel>();
		if(alarm==null)
		{
			
			
			// Add all server channels
			for(Channel c : FrSkyServer.getSourceChannels().values())
			{
				//sourceChannels[n] = c;
				sourceChannels.add(c);
			}
			
			// Add channels from this model
		
			//for(Channel c : FrSkyServer.database.getChannelsForModel(getId())) 	// Gets from database		
			for(Channel c : channelMap.values())											// Gets from instance
			{
					sourceChannels.add(c);
			}
		}
		else
		{
			// Add the None channel
			
			
			// Add the single server channel
			// eso: no point..
			if(alarm.getUnitChannelId()==FrSkyServer.CHANNEL_ID_RSSIRX)
			{
				sourceChannels.add(FrSkyServer.getSourceChannel(FrSkyServer.CHANNEL_ID_RSSIRX));
			}
			else
			{
				sourceChannels.add(FrSkyServer.getSourceChannel(FrSkyServer.CHANNEL_ID_NONE));
				// Add any model channels that has this source Channel
				for(Channel c : channelMap.values())											// Gets from instance
				{
					if(c.getSourceChannelId()==alarm.getSourceChannelId())
						sourceChannels.add(c);
				}
			}
			
		}
		return sourceChannels;
	}

	/**
	 * Remove listeners for all channels in the model
	 */
	public void unregisterListeners()
	{
		for(Channel c: getChannels().values())
		{
			c.unregisterListener();
		}
	}
	
	/**
	 * Make the channels of this model start listening
	 */
	public void registerListeners()
	{
		if(FrSkyServer.D)Log.d(TAG,_name+": Registrering listeners");
		for(Channel c: getChannels().values())
		{
			c.registerListener();
		}
	}
	
	@Override
	public String toString() {
		return "Model [_type=" + _type + ", _name=" + _name + ", _id=" + _id
				+ "]";
	}

}
