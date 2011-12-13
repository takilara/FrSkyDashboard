package biz.onomato.frskydash;

import java.util.ArrayList;

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
	
	private int _type;
	private String _name;
	private long _id;
	private Context _context;
	
	private DBAdapterModel db;
	

	// Constructor
	public Model(Context context, String modelName,int modelType)
	{
		_context = context;
		// create if neccessary
		db = new DBAdapterModel(context);
		
		_id = -1; 
		setName(modelName);
		setType(modelType);
		_channels = new ArrayList<Channel>();
		//setId(-1);
	}
	
	public Model(Context context,String modelName)
	{
		this(context,modelName,MODEL_TYPE_UNKNOWN);
	}
	public Model(Context context)
	{
		this(context,"Model 1",MODEL_TYPE_UNKNOWN);
	}
	


	
	
	public int getType() {
		return _type;
	}


	public void setType(int modelType) {
		this._type = modelType;
	}

	// Should never set it, should get it from database
//	public void setId(int id)
//	{
//		_id = id;
//	}
	
	public long getId()
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
		}
	}
	
	// I need to be able to delete channels from this model
	public boolean removeChannel(Channel channel)
	{
		return _channels.remove(channel);
	}
	
	// I need to be able to set a given channel for this model
	public void setChannel(int id, Channel channel)
	{
		
		if(DEBUG) Log.d(TAG,"Replacing channel at id "+id);
		
		if(_channels.get(id)!=null)
		{
			if(DEBUG) Log.d(TAG,"Old channel existed");
			_channels.set(id, channel);
		}
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
		if(_id==-1)
		{
			if(DEBUG) Log.d(TAG,"Saving, using insert");
			db.open();
			long id = db.insertModel(_name);
			if(id==-1)
			{
				Log.e(TAG,"Insert Failed");
			}
			else
			{
				if(DEBUG) Log.d(TAG,"Insert ok, id:"+id);
				_id = id;
			}
			db.close();
			// Run insert
		}
		else
		{
			if(DEBUG) Log.d(TAG,"Saving, using update (id:"+_id+",name:"+_name+")");
			db.open();
			if(db.updateModel(_id, _name))
			{
				if(DEBUG)Log.d(TAG,"Update successful");
			}
			else
			{
				if(DEBUG)Log.e(TAG,"Update failed");
			}
			db.close();
			// run update
		}
	}
	
	// I need to be able to load settings from file or config storage
	public void loadSettings()
	{
		
	}
	
	public boolean loadFromSettings(long id)
	{
		// False if not found
		db.open();
		Cursor c = db.getModel(id);
		
		if(c.getCount()==0)
		{
			if(DEBUG) Log.w(TAG,"Model id "+id+" does not exist.");	
			_id= -1;
			db.close();
			return false;
		}
		else
		{
			if(DEBUG) Log.d(TAG,"Found the model");
			if(DEBUG) Log.d(TAG,c.getString(1));
			_id = c.getLong(0);
			_name = c.getString(1);
			db.close();
			return true;
		}
		
	}
	
	public boolean getFirstModel()
	{
		db.open();
		Cursor c = db.getAllModels();
		if(c.getCount()>0)
		{
			c.moveToFirst();
			loadFromSettings(c.getInt(0));
			db.close();
			return true;
		}
		else
		{
			db.close();
			return false;
		}
	}
	
	

	
	public static Model[] getAllModels(Context context)
	{
		if(DEBUG) Log.d(TAG,"Get all models");
		// Needs to create the model if it does not exists
		DBAdapterModel db = new DBAdapterModel(context);
		db.open();
		Cursor c = db.getAllModels();
		Model[] modelA = new Model[c.getCount()];
		if(c.getCount()>0)
		{
			
			
			for(int i=0;i<c.getCount();i++)
			{
				modelA[i] = new Model(context);
				long id = c.getLong(0);
				modelA[i].loadFromSettings(id);
			}
		}
		
		db.close();
		//m.loadFromSettings(id);
		
		// if not found, create it before returning it
		return modelA;
	}

}
