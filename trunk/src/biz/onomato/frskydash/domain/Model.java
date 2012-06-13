package biz.onomato.frskydash.domain;

import java.util.ArrayList;
import java.util.TreeMap;

import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.R;
import biz.onomato.frskydash.util.Logger;

/**
 * A Model as it is configured by the user. A Model should be of a certain type
 * (car, airplane, helicopter, quad, ...) and have a certain name so the user
 * can recognize it.
 * 
 * For the frsky telemetry part a Model also has a collection of {@link Channel}
 * objects that are listening for incoming values. And a collection of
 * {@link Alarm} configurations that can be set to the FrSky Module.
 * 
 * @author Espen Solbu
 */
public class Model {
	
	private static final String TAG="ModelClass";
	
	// TODO create enum for this instead
	private String[] _modelTypes;
	
	public static final int MODEL_TYPE_HELICOPTER=0;
	public static final int MODEL_TYPE_FIXEDWING=1;
	public static final int MODEL_TYPE_CAR=2;
	public static final int MODEL_TYPE_BOAT=3;
	public static final int MODEL_TYPE_MULTIROTOR=4;
	public static final int MODEL_TYPE_UNKNOWN=-1;
	
	//private ArrayList<Channel> _channels;
	/**
	 * collection of channels for this model
	 */
	private TreeMap<Integer,Channel> channelMap;
	
	/**
	 * type of a model 
	 */
	private String _type;
	
	/**
	 * name of a model
	 */
	private String _name;
	
	/**
	 * ID of a model
	 */
	private int _id;
	
	//private Context _context;
	//public Alarm[] alarms;
	
	/**
	 * collection of alarms for this model
	 */
	public TreeMap<Integer,Alarm> frSkyAlarms;
	
	/**
	 * ? number of alarms counted for this Model ? 
	 */
	public int alarmCount = 0;
	
	/**
	 * indicates if this Model needs to be saved or not
	 */
	public boolean dirty = false;

	//private static DBAdapterModel db;

	/**
	 * Default Constructor
	 * 
	 * @param modelName
	 *            String name for the model
	 * @param modelType
	 *            String type for this model
	 */
	public Model(String modelName,String modelType)
	{
		//_context = context;
		// let user select from a fixed set of modeltypes
		// TODO make an Enum for this instead
		_modelTypes = FrSkyServer.getContext().getResources().getStringArray(R.array.model_types);
		// create if neccessary
		//db = new DBAdapterModel(context);
		
		// FRSKY channels only for now
		//alarms = new Alarm[6];
		frSkyAlarms = new TreeMap<Integer, Alarm>();
		
		channelMap = new TreeMap<Integer,Channel>();
		
		// populate FrSky Alarms with defaults
		initializeFrSkyAlarms();
		
		_id = -1; 
		setName(modelName);
		//set some default modeltype if none was set by the user
		setType(modelType.equals("") ? _modelTypes[0] : modelType);
		//_channels = new ArrayList<Channel>();
		//setId(-1);
	}

	/**
	 * Ctor
	 * 
	 * @param modelName
	 */
	public Model(String modelName) {
		this(modelName, "");
	}

	/**
	 * ctor
	 */
	public Model() {
		this("Model 1", "");
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
		Logger.d(TAG,_name+": Resetting myself and all my components");
		for(Channel c: getChannels().values())
		{
			c.reset();
			c = null;
		}
		channelMap.clear();
	}
	
	
	/**
	 * retrieve the type of this model
	 * 
	 * @return
	 */
	public String getType() {
		return _type;
	}

	/**
	 * set the type for this model
	 * 
	 * @param modelType
	 */
	public void setType(String modelType) {
		Logger.d(TAG,"Setting model type to: "+modelType);
		this._type = modelType;
	}

	/**
	 * update ID for this model
	 * 
	 * @param id
	 */
	public void setId(int id)
	{
		_id = id;
	}
	
	/**
	 * retrieve ID for this model
	 * 
	 * @return
	 */
	public int getId()
	{
		return _id;
	}

	/**
	 * get the name of this model
	 * 
	 * @return
	 */
	public String getName() {
		return _name;
	}

	/**
	 * set the name of this model 
	 * 
	 * @param modelName
	 */
	public void setName(String modelName) {
		this._name = modelName;
	}

	/**
	 * By default a Model has 2 channels, AD1 and AD2. Use this method to
	 * initialize these.
	 */
	public void initializeDefaultChannels() {
		// AD 1
		addChannel(createChannel("AD1 raw", _id, FrSkyServer.CHANNEL_ID_AD1));
		// AD 2
		addChannel(createChannel("AD2 raw", _id, FrSkyServer.CHANNEL_ID_AD2));
		// indicate model is dirty, needs saving
		dirty=true;
	}
	
	/**
	 * helper to create a single channel with mostly default values
	 * 
	 * @return
	 */
	private Channel createChannel(String description, int id, int sourceChannel){
		Channel channel = new Channel();
		channel.setDescription(description);
		channel.setModelId(id);
		channel.setSourceChannel(sourceChannel);
		channel.setId(-1);
		channel.setSilent(true);
		channel.setLongUnit("");
		channel.setShortUnit("");
		channel.setPrecision(0);
		channel.setMovingAverage(0);
		return channel;
	}

	/**
	 * I need to be able to add channels to this model
	 * @param channel
	 */
	public void addChannel(Channel channel)
	{
		if(channel.getId()==-1)
		{
			//FIXME don't perform saves to db in the domain object!
			FrSkyServer.saveChannel(channel);
		}
		channelMap.put(channel.getId(), channel);
	}
	
	/**
	 * I need to be able to delete channels from this model
	 * @param channel
	 * @return
	 */
	public boolean removeChannel(Channel channel)
	{
		//channel.unregisterListener();
		
		//channelMap.remove(channel.getId());
		return removeChannel(channel.getId());
		
		//return _channels.remove(channel);
	}
	public boolean removeChannel(int channelId)
	{
		//channel.unregisterListener();
		channelMap.get(channelId).close();
		channelMap.remove(channelId);
		return true;
		//return _channels.remove(channel);
	}
	
	/**
	 * I need to be able to set a given channel for this model
	 * @param channel
	 */
	//public void setChannel(int id, Channel channel)
	public void setChannel(Channel channel)
	{
		addChannel(channel);
	}
	
	/**
	 * Update collection of channels for this model 
	 * 
	 * @param channels
	 */
	public void setChannels(ArrayList<Channel> channels)
	{
		for(Channel c : channels)
		{
			addChannel(c);
		}
	}
	
	/**
	 * Update collection of channels for this model 
	 * 
	 * @param channels
	 */
	public void setChannels(TreeMap<Integer,Channel> channels)
	{
		channelMap = channels;
	}
	
	/**
	 *  I need to be able to return list of channels from this model
	 * @return
	 */
	//public ArrayList<Channel> getChannels()
	public TreeMap<Integer,Channel> getChannels()
	{
		return channelMap;
		//return _channels;
	}
	
	/**
	 * I need to be able to add alarms to this model. This will check the
	 * alarmtype and store in the correct collection reference. For the moment
	 * we only support frsky alarms but eventually this system could be expanded
	 * to support other types of alarms defined by the user. Thing of sensor
	 * values and so on.
	 * 
	 * @param alarm
	 */
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
			Logger.e(TAG,"Unhandled Alarm Type");
			
		}

	}
	
	/**
	 * get the {@link Alarm}s set to this model
	 * 
	 * @return
	 */
	public TreeMap<Integer,Alarm> getFrSkyAlarms()
	{
		return frSkyAlarms;
	}
	
	/**
	 * update the {@link Alarm}s for this model. By doing so all modelId
	 * references on the alarms will be set to this model and the alarms
	 * collection will be attached to this model instance
	 * 
	 * @param alarmMap
	 */
	public void setFrSkyAlarms(TreeMap<Integer,Alarm> alarmMap)
	{
		Logger.w(TAG,"Adding Alarms to me ["+_name+"]");
		// this check isn't really needed
	//	if(alarmMap.size()>0)
	//	{
			for(Alarm a:alarmMap.values()){
				a.setModelId(_id);
				addAlarm(a);
			//	alarmCount += 1;
			}
		//}
		//else
		//{
			//initiateFrSkyAlarms();
		//}
	}
	
	
	/**
	 * TODO I need to be able to delete alarms from this model
	 * @param alarm
	 */
	public void deleteAlarm(Alarm alarm)
	{
		//TODO
	}
	
	/**
	 * TODO I need to be able to load settings from file or config storage
	 */
	public void loadSettings()
	{
		//TODO
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
	 * Used to create initial alarms for a model.<br>
	 * This consists of the following alarms:<br>
	 * <ul>
	 * <li>AD1 Alarm 1 and 2
	 * <li>AD2 Alarm 1 and 2
	 * <li>RSSI Alarm 1 and 2 <i>(Note, RSSI alarms are undocumented)</i>
	 * </ul>
	 * 
	 * FIXME: Get the proper default values => hcpl: I believe the default values are 72<br>
	 * FIXED: consider if this should be moved to Model
	 */
	public void initializeFrSkyAlarms()
	{
		// no need to keep an intermediate map here. I will be using all new
		// object references so we don't work on the same objects
		Frame alarm1RSSIFrame = Frame.AlarmFrame(
				Frame.FRAMETYPE_ALARM1_RSSI, 
				Alarm.ALARMLEVEL_LOW, 
				45, 
				Alarm.LESSERTHAN);
		Alarm alarm1RSSI = new Alarm(alarm1RSSIFrame);
		alarm1RSSI.setUnitChannel(FrSkyServer.getSourceChannel(FrSkyServer.CHANNEL_ID_RSSIRX));
		alarm1RSSI.setModelId(this._id);
		addAlarm(alarm1RSSI);
		
		Frame alarm2RSSIFrame = Frame.AlarmFrame(
				Frame.FRAMETYPE_ALARM2_RSSI, 
				Alarm.ALARMLEVEL_MID, 
				42, 
				Alarm.LESSERTHAN);
		Alarm alarm2RSSI = new Alarm(alarm2RSSIFrame);
		alarm2RSSI.setUnitChannel(FrSkyServer.getSourceChannel(FrSkyServer.CHANNEL_ID_RSSIRX));
		alarm2RSSI.setModelId(this._id);
		addAlarm(alarm2RSSI);
		
		Frame alarm1AD1Frame = Frame.AlarmFrame(
				Frame.FRAMETYPE_ALARM1_AD1, 
				Alarm.ALARMLEVEL_OFF, 
				200, 
				Alarm.LESSERTHAN);
		Alarm alarm1AD1 = new Alarm(alarm1AD1Frame);
		alarm1AD1.setUnitChannel(FrSkyServer.getSourceChannel(FrSkyServer.CHANNEL_ID_AD1));
		alarm1AD1.setModelId(this._id);
		addAlarm(alarm1AD1);
		
		Frame alarm2AD1Frame = Frame.AlarmFrame(
				Frame.FRAMETYPE_ALARM2_AD1, 
				Alarm.ALARMLEVEL_OFF, 
				200, 
				Alarm.LESSERTHAN);
		Alarm alarm2AD1 = new Alarm(alarm2AD1Frame );
		alarm2AD1.setUnitChannel(FrSkyServer.getSourceChannel(FrSkyServer.CHANNEL_ID_AD1));
		alarm2AD1.setModelId(this._id);
		addAlarm(alarm2AD1);
		
		Frame alarm1AD2Frame = Frame.AlarmFrame(
				Frame.FRAMETYPE_ALARM1_AD2, 
				Alarm.ALARMLEVEL_OFF, 
				200, 
				Alarm.LESSERTHAN);
		Alarm alarm1AD2 = new Alarm(alarm1AD2Frame);
		alarm1AD2.setUnitChannel(FrSkyServer.getSourceChannel(FrSkyServer.CHANNEL_ID_AD2));
		alarm1AD2.setModelId(this._id);
		addAlarm(alarm1AD2);
		
		Frame alarm2AD2Frame = Frame.AlarmFrame(
				Frame.FRAMETYPE_ALARM2_AD2, 
				Alarm.ALARMLEVEL_OFF, 
				200, 
				Alarm.LESSERTHAN);
		Alarm alarm2AD2 = new Alarm(alarm2AD2Frame);
		alarm2AD2.setUnitChannel(FrSkyServer.getSourceChannel(FrSkyServer.CHANNEL_ID_AD2));
		alarm2AD2.setModelId(this._id);
		addAlarm(alarm2AD2);
	}

	/**
	 * Remove listeners for all channels in the model
	 */
	public void unregisterListeners()
	{
		for(Channel c: getChannels().values())
		{
			c.unregisterListenerForChannelUpdates();
			c.unregisterListenerForServerCommands();
		}
	}
	
	/**
	 * Make the channels of this model start listening
	 */
	public void registerListeners()
	{
		Logger.d(TAG,_name+": Registering listeners");
		for(Channel c: getChannels().values())
		{
			c.registerListenerForChannelUpdates();
			c.registerListenerForServerCommands();
		}
	}
	
	@Override
	public String toString() {
		return "Model [_type=" + _type + ", _name=" + _name + ", _id=" + _id
				+ "]";
	}

}
