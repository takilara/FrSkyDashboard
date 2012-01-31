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
	
	private static final boolean DEBUG=true;
	
	private ArrayList<Channel> _channels;
	private String[] _modelTypes;
	
	private String _type;
	private String _name;
	private int _id;
	private Context _context;
	//public Alarm[] alarms;
	TreeMap<Integer,Alarm> frSkyAlarms;
	public int alarmCount = 0;

	
	
	//private static DBAdapterModel db;
	

	// Constructor
	public Model(Context context, String modelName,String modelType)
	{
		_context = context;
		_modelTypes = _context.getResources().getStringArray(R.array.model_types);
		// create if neccessary
		//db = new DBAdapterModel(context);
		
		// FRSKY channels only for now
		//alarms = new Alarm[6];
		frSkyAlarms = new TreeMap<Integer, Alarm>();
		
		
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
		_channels = new ArrayList<Channel>();
		//setId(-1);
	}
	

	
	public Model(Context context,String modelName)
	{
		
		this(context,modelName,"");
	}
	public Model(Context context)
	{
		this(context,"Model 1","");
	}
	

//	public void initiateFrSkyAlarms()
//	{
//		//TODO: Need defaults
//		Frame alarmFrame = Frame.AlarmFrame(
//				Frame.FRAMETYPE_ALARM1_RSSI, 
//				Alarm.ALARMLEVEL_LOW, 
//				45, 
//				Alarm.LESSERTHAN);
//		//Alarm a = new Alarm(alarmFrame);
//		
//		addAlarm(new Alarm(alarmFrame));
//		
//		alarmFrame = Frame.AlarmFrame(
//				Frame.FRAMETYPE_ALARM2_RSSI, 
//				Alarm.ALARMLEVEL_MID, 
//				42, 
//				Alarm.LESSERTHAN);
//		addAlarm(new Alarm(alarmFrame));
//		
//		alarmFrame = Frame.AlarmFrame(
//				Frame.FRAMETYPE_ALARM1_AD1, 
//				Alarm.ALARMLEVEL_MID, 
//				42, 
//				Alarm.LESSERTHAN);
//		addAlarm(new Alarm(alarmFrame));
//		
//		alarmFrame = Frame.AlarmFrame(
//				Frame.FRAMETYPE_ALARM2_AD1, 
//				Alarm.ALARMLEVEL_MID, 
//				42, 
//				Alarm.LESSERTHAN);
//		addAlarm(new Alarm(alarmFrame));
//		
//		alarmFrame = Frame.AlarmFrame(
//				Frame.FRAMETYPE_ALARM1_AD2, 
//				Alarm.ALARMLEVEL_MID, 
//				42, 
//				Alarm.LESSERTHAN);
//		addAlarm(new Alarm(alarmFrame));
//		
//		alarmFrame = Frame.AlarmFrame(
//				Frame.FRAMETYPE_ALARM2_AD2, 
//				Alarm.ALARMLEVEL_MID, 
//				42, 
//				Alarm.LESSERTHAN);
//		addAlarm(new Alarm(alarmFrame));
//		
//	}
	
	
	public String getType() {
		return _type;
	}


	public void setType(String modelType) {
		if(DEBUG)Log.d(TAG,"Setting model type to: "+modelType);
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


	// I need to be able to add channels to this model
	public void addChannel(Channel channel)
	{
		if(!_channels.contains(channel))
		{
			_channels.add(channel);
			channel.setModelId(_id);
		}
	}
	
	// I need to be able to delete channels from this model
	public boolean removeChannel(Channel channel)
	{
		//channel.deleteFromDatabase();
		//setDirtyFlag(true);
		return _channels.remove(channel);
	}
	
	// I need to be able to set a given channel for this model
	public void setChannel(int id, Channel channel)
	{
		
		if(id<_channels.size())
		{
			if(DEBUG) Log.d(TAG,"Old channel existed, replacing");
			_channels.set(id, channel);
		}
		else
		{
			if(DEBUG) Log.d(TAG,"Old did not exist, adding");
			_channels.add(channel);
		}
		
	}
	
	public void setChannels(ArrayList<Channel> channels)
	{
		for(Channel ch : channels)
		{
//			if(_channels.get(ch.getId())!=null)	// channel exists, update it
//			{
//				_channels.set((int)ch.getId(), ch);
//			}
//			else
//			{
				_channels.add(ch);
//			}
		}
	}
	
	// I need to be able to return list of channels from this model
	public ArrayList<Channel> getChannels()
	{
		//Channel[] outChannels = new Channel[_channels.size()];
		//int i=0;
		//TODO for each is ineficcient for arraylists!
//		int n=0;
//		
//		for(Channel ch:_channels)
//		{
//			outChannels[i]=ch;
//			i++;
//		}
		//return outChannels;
		//if(DEBUG)Log.d(TAG,"return all channels: "+_channels);
//		return _channels.toArray(Channel[]);
		return _channels;
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
				addAlarm(a);
			//	alarmCount += 1;
			}
		}
		else
		{
			//initiateFrSkyAlarms();
		}
	}
	
	//TODO: Should compare the alarms before setting them
//	public void setFrSkyAlarm(int number,int threshold,int greaterthan,int level)
//	{
//		
//		alarms[number] = new Alarm(Alarm.ALARMTYPE_FRSKY,level,greaterthan,threshold);
//		alarmCount += 1;
//		//setDirtyFlag(true);
//	}

	// I need to be able to delete alarms from this model
	public void deleteAlarm(Alarm alarm)
	{
		
	}
	
	// I need to be able to save settings to file or config storage
//	public void saveToDatabase()
//	{
//		if(_id==-1)
//		{
//			if(DEBUG) Log.d(TAG,"Saving, using insert");
//			db.open();
//			long id = db.insertModel(_name,_type);
//			if(id==-1)
//			{
//				Log.e(TAG,"Insert Failed");
//			}
//			else
//			{
//				if(DEBUG) Log.d(TAG,"Insert ok, id:"+id);
//				_id = id;
//			}
//			db.close();
//			// Run insert
//		}
//		else
//		{
//			if(DEBUG) Log.d(TAG,"Saving, using update (id:"+_id+",name:"+_name+")");
//			db.open();
//			if(db.updateModel(_id, _name,_type))
//			{
//				if(DEBUG)Log.d(TAG,"Update successful");
//			}
//			else
//			{
//				if(DEBUG)Log.e(TAG,"Update failed");
//			}
//			db.close();
//			// run update
//		}
//		
//		
//		// Save the channels (using this models id)
//		for(Channel ch :getChannels())
//		{
//			if(DEBUG) Log.i(TAG,"Save channel "+ch.getDescription()+" (Dirty: "+ch.getDirtyFlag()+")");
//			ch.setModelId(_id);
//			ch.saveToDatabase();
//			
//		}
//	}
	
	// I need to be able to load settings from file or config storage
	public void loadSettings()
	{
		
	}
	
//	public boolean loadFromDatabase(long id)
//	{
//		// False if not found
//		db.open();
//		Cursor c = db.getModel(id);
//		//startManagingCursor(c);
//		
//		if(c.getCount()==0)
//		{
//			if(DEBUG) Log.w(TAG,"Model id "+id+" does not exist.");	
//			_id= -1;
//			c.deactivate();
//			db.close();
//			
//			// no channels
//			return false;
//		}
//		else
//		{
//			if(DEBUG) Log.d(TAG,"Found the model");
//			if(DEBUG) Log.d(TAG,c.getString(1));
//			_id = c.getLong(0);
//			_name = c.getString(1);
//			_type = c.getString(2);
//			
//			// update alarms with the proper ones
//			// TODO: change this from adding defaults to add something from database
//			initiateFrSkyAlarms();
//			
//			c.deactivate();
//			db.close();
//			
//			// load channels
//			
//			for(Channel ch : Channel.getChannelsForModel(_context,this))
//			{
//				if(DEBUG)Log.d(TAG,"Found and adding channel "+ch.getDescription()+" to "+_name);
//				addChannel(ch);
//			}
//			return true;
//		}
//		
//		
//	}
	
//	public boolean getFirstModel()
//	{
//		db.open();
//		Cursor c = db.getAllModels();
//		if(c.getCount()>0)
//		{
//			c.moveToFirst();
//			loadFromDatabase(c.getInt(0));
//			c.deactivate();
//			db.close();
//			return true;
//		}
//		else
//		{
//			c.deactivate();
//			db.close();
//			return false;
//		}
//	}
	
	
	
	
//	public static Model[] getAllModels(Context context)
//	{
//		if(DEBUG) Log.d(TAG,"Get all models");
//		// Needs to create the model if it does not exists
//	//	AbstractDBAdapter db = new AbstractDBAdapter(context);
//		db.open();
//		
//		Cursor c = db.getAllModels();
//		Model[] modelA = new Model[c.getCount()];
//		if(c.getCount()>0)
//		{
//			
//			
//			for(int i=0;i<c.getCount();i++)
//			{
//				modelA[i] = new Model(context);
//				long id = c.getLong(0);
//				modelA[i].loadFromDatabase(id);
//			}
//		}
//		c.deactivate();
//		db.close();
//		//m.loadFromSettings(id);
//		
//		// if not found, create it before returning it
//		return modelA;
//	}

}
