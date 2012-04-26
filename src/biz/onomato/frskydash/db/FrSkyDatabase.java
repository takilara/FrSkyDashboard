package biz.onomato.frskydash.db;

import java.util.ArrayList;
import java.util.TreeMap;

import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.domain.Alarm;
import biz.onomato.frskydash.domain.Channel;
import biz.onomato.frskydash.domain.Model;
import biz.onomato.frskydash.util.Logger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class FrSkyDatabase extends AbstractDBAdapter {
    private static final String TAG = "Database";
    
    public FrSkyDatabase(Context context) 
    {
        super(context);
    }

    
    // ***********************************************************
    // ********************  CHANNELS  ***************************
    // ***********************************************************

    public ArrayList<Model> getModels()
    {
    	Logger.d(TAG,"Getting all models from database");
    	open();
    	Cursor cu = db.query(DATABASE_TABLE_MODELS, MODEL_COLUMNS, 
	            null, null, null, null, null);
    	
        ArrayList<Model> mList = new ArrayList<Model>();
        cu.moveToFirst();
        int len = cu.getCount();
        //while(!cu.isAfterLast())
        for(int i=0;i<len;i++)
		{
        	Logger.i(TAG,"Getting the "+i+"'th model, from position "+cu.getPosition());
        	Model m = getModel(cu);
        	Logger.i(TAG,"  This model is: "+m.getName());
			mList.add(m);
			cu.moveToNext();
		}
		cu.deactivate();
		close();
		
		//debug
		Logger.e(TAG,"Our Model list now contains: ");
		for(Model m : mList)
		{
			Logger.e(TAG,"  "+m.getName());
		}
		
		return mList;
    }
    
    public Model getModel(int modelId)
    {
    	open();

    	Cursor cu =
                db.query(true, DATABASE_TABLE_MODELS, MODEL_COLUMNS, 
                		KEY_ROWID + "=" + modelId, 
                		null,null,null,null,null);
        if (cu != null) {
//        	Log.d(TAG,"Found the model..");
//        	Log.d(TAG,"Count: "+mCursor.getCount());
            cu.moveToFirst();
        }
        Model m;
        if(cu.getCount()!=0)
        {	
	        m = getModel(cu);
	        
    	}
        else
        {
        	Logger.e(TAG,"Model with id: "+modelId+" was not found, try to get first model");
        	m = getModel();
        }
        cu.deactivate();
        close();
        return m;
    }
    
    public Model getModel()
    {
    	// Get the first model
    	Logger.i(TAG,"Try to get first model");
    	open();
    	Cursor cu =
                db.query(true, DATABASE_TABLE_MODELS, MODEL_COLUMNS,           		 
                		null,null,null,null,KEY_ROWID,"1");
    	
    	Model m;
    	cu.moveToFirst();
    	if(cu.getCount()!=0)
        {	
	        m = getModel(cu);
    	}
    	else
    	{
    		m = null;
    	}
    	cu.deactivate();
    	close();
    	return m;
    }
    
    
    
    public Model getModel(Cursor cu)
    {
    	Logger.i(TAG,"Pickup the model info from the cursor: "+cu.getColumnNames());
    	Model m = new Model();
    	//cu.moveToFirst();
    	Logger.i(TAG,"cursor id: "+cu.getInt(0));
    	
		m.setId(cu.getInt(cu.getColumnIndexOrThrow(KEY_ROWID)));
		m.setName(cu.getString(cu.getColumnIndexOrThrow(KEY_NAME)));
		m.setType(cu.getString(cu.getColumnIndexOrThrow(KEY_MODELTYPE)));

		// Add Channels to the model
		ArrayList<Channel> channelList = getChannelsForModel(m.getId());
		Logger.i(TAG,"Found "+channelList.size()+" channels for model with id "+m.getId());
		m.setChannels(channelList);
		
		// Add Alarms to the model
		TreeMap<Integer,Alarm> alarmMap = getAlarmsForModel(m.getId());
		m.setFrSkyAlarms(alarmMap);
		
		return m;
    }
    
    public void saveModel(Model model)
    {
    	boolean result = true;
    	if(model.getId()==-1)
    	{
    		int id = insertModel(model);
    		if(id!=-1)
    		{	
    			model.setId(id);
    		}
    		else
    		{
    			Logger.e(TAG,"Inserting the model failed");
    			result = false;
    		}
    	}
    	else
    	{
    		if(!updateModel(model))
    		{
    			Logger.e(TAG,"Updating the model failed");
    			result = false;
    		}
    	}
    	if(model.getId()!=-1) // Insert/update did not fail
    	{
    		// first, make sure the channels have the correct modelId
        	//Make sure all the models channels modelid is correct
        	int mId = model.getId();
        	if(mId!=-1)
        	{
        		for(Channel c : model.getChannels().values())
        		{
        			c.setModelId(mId);
        		}
        	}
    		
    		// Update the channels
    		Logger.i(TAG,"Saving channels");
    		
    		// no good
    		//deleteAllChannelsForModel(model);
    		for(Channel c:getChannelsForModel(model))
    		{
    			if(!model.getChannels().containsValue(c))
    			{
    				// Channel no longer in model, delete it
    				deleteChannel(c);
    			}
    		}
    		
    		// update or add channels
    		for(Channel ch:model.getChannels().values())
    		{
    			saveChannel(ch);
    		}
    		
    		// Update the alarms
    		Logger.i(TAG,"Saving alarms");
    		setAlarmsForModel(model);
    		
    	}
    }
    
    private boolean updateModel(Model model)
    {
    	Logger.d(TAG,"Update Model '"+model.getName()+"' in the database, at id "+model.getId());
    	open();
        ContentValues args = new ContentValues();
        args.put(KEY_NAME, model.getName());
        args.put(KEY_MODELTYPE, model.getType());
        boolean result = db.update(DATABASE_TABLE_MODELS, args, 
                KEY_ROWID + "=" + model.getId(), null) > 0;
        close();
        return result;
    }
    
    private int insertModel(Model model)
    {
    	Logger.d(TAG,"Insert Model '"+model.getName()+"' into the database");
    	open();
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NAME, model.getName());
        initialValues.put(KEY_MODELTYPE, model.getType());
        //initialValues.put(KEY_TITLE, title);
        
        int newId = (int) db.insert(DATABASE_TABLE_MODELS, null, initialValues);
        Logger.d(TAG," The id for '"+model.getName()+"' is "+newId);
        close();
        return newId;
    }
    
    public boolean deleteModel(int modelId) 
    {
    	Logger.d(TAG,"Deleting from the database");
    	
    	open();
    	boolean result = db.delete(DATABASE_TABLE_MODELS, KEY_ROWID + 
        		"=" + modelId, null) > 0;
        close();
    	return result;
        
    }
    
    
    
    // ***********************************************************
    // ********************  CHANNELS  ***************************
    // ***********************************************************
    public ArrayList<Channel> getChannelsForModel(int modelId)
    {
    	// Query for this modelid
    	open();
		Cursor cu = db.query(DATABASE_TABLE_CHANNELS, CHANNEL_COLUMNS, 
				KEY_MODELID + "=" + modelId,
	            null, null, null, null, null);

		
    	// loop getChannel(Cursor)
		ArrayList<Channel> mList = new ArrayList<Channel>();
		cu.moveToFirst();
        while(!cu.isAfterLast())
		{
        	Channel ch = getChannel(cu);
			mList.add(ch);
			cu.moveToNext();
		}
		cu.deactivate();
		close();
		return mList;
    }
    
    
    public ArrayList<Channel> getChannelsForModel(Model model)
    {
    	return getChannelsForModel(model.getId());
    }
    
   
    

    public ArrayList<Channel> getChannels()
    {
    	Logger.d(TAG,"Getting all channels from database");
    	open();
    	Cursor cu = db.query(DATABASE_TABLE_CHANNELS, CHANNEL_COLUMNS, 
	            null, null, null, null, null);
    	
        ArrayList<Channel> mList = new ArrayList<Channel>();
        while(!cu.isAfterLast())
		{
        	Channel ch = getChannel(cu);
			mList.add(ch);
			cu.moveToNext();
		}
		cu.deactivate();
		close();
		return mList;
    }
    
    public Channel getChannel(Cursor c)
    {
    	Channel ch = new Channel();
    	ch.setId(c.getInt(c.getColumnIndexOrThrow(KEY_ROWID)));
    	ch.setDescription(c.getString(c.getColumnIndexOrThrow(KEY_DESCRIPTION)));
    	ch.setLongUnit(c.getString(c.getColumnIndexOrThrow(KEY_LONGUNIT)));
		ch.setShortUnit(c.getString(c.getColumnIndexOrThrow(KEY_SHORTUNIT)));
		ch.setFactor(c.getFloat(c.getColumnIndexOrThrow(KEY_FACTOR)));
		ch.setOffset(c.getFloat(c.getColumnIndexOrThrow(KEY_OFFSET)));
		ch.setPrecision(c.getInt(c.getColumnIndexOrThrow(KEY_PRECISION)));
		ch.setMovingAverage(c.getInt(c.getColumnIndexOrThrow(KEY_MOVINGAVERAGE)));
		ch.setSilent(c.getInt(c.getColumnIndexOrThrow(KEY_SILENT))>0);
		ch.setModelId(c.getInt(c.getColumnIndexOrThrow(KEY_MODELID)));
		ch.setSourceChannel(c.getInt(c.getColumnIndexOrThrow(KEY_SOURCECHANNELID)));
		ch.setDirtyFlag(false);
		//db.close();
		
		Logger.d(TAG,"Loaded '"+ch.getDescription()+"' from database");
		Logger.d(TAG,"\tSilent:\t"+ch.getSilent());
		return ch;
    }
    
    public Channel getChannel(int channelId)
    {
    	Logger.d(TAG,"Get one channel from the database (channelid: "+channelId+")");
    	open();
        Cursor cu = db.query(true, DATABASE_TABLE_CHANNELS, CHANNEL_COLUMNS, 
                		KEY_ROWID + "=" + channelId, 
                		null, null,	null, null, null);
        if (cu != null) {
//        	Log.d(TAG,"Found the model..");
//        	Log.d(TAG,"Count: "+mCursor.getCount());
            cu.moveToFirst();
        }
        Channel ch = getChannel(cu);
        cu.deactivate();
        close();
        return ch;
    }
    
    public void saveChannel(Channel channel)
    {
    	boolean result = true;
    	if(channel.getId()==-1)
    	{
    		// save using insert
    		Logger.d(TAG,"Save channel using insert");
    		int id = insertChannel(channel);
    		if(id!=-1)
    		{
    			channel.setId(id);
    		}
    		else
    		{
    			Logger.e(TAG,"Inserting channel failed");
    			result = false;
    		}
    	}
    	else
    	{
    		// save using update
    		if(!updateChannel(channel))
    		{
    			Logger.e(TAG,"Channel Update failed");
    		}
    	}
    }
    
    private int insertChannel(Channel channel)
    {
    	Logger.d(TAG,"Insert Channel into the database");
    	open();
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_DESCRIPTION, channel.getDescription());
        initialValues.put(KEY_LONGUNIT, channel.getLongUnit());
        initialValues.put(KEY_SHORTUNIT, channel.getShortUnit());
        initialValues.put(KEY_OFFSET, channel.getOffset());
        initialValues.put(KEY_FACTOR, channel.getFactor());
        initialValues.put(KEY_PRECISION, channel.getPrecision());
        initialValues.put(KEY_MOVINGAVERAGE, channel.getMovingAverage());
        initialValues.put(KEY_MODELID, channel.getModelId());
        initialValues.put(KEY_SOURCECHANNELID, channel.getSourceChannelId());
        initialValues.put(KEY_SILENT, channel.getSilent());
        
        int id = (int) db.insert(DATABASE_TABLE_CHANNELS, null, initialValues);
        close();
        return id;
    }
    
    private boolean updateChannel(Channel channel)
    {
    	Logger.d(TAG,"Update one channel in the database");
    	open();
        ContentValues args = new ContentValues();
        int rowId = channel.getId();
        args.put(KEY_DESCRIPTION, channel.getDescription());
        args.put(KEY_LONGUNIT, channel.getLongUnit());
        args.put(KEY_SHORTUNIT, channel.getShortUnit());
        args.put(KEY_OFFSET, channel.getOffset());
        args.put(KEY_FACTOR, channel.getFactor());
        args.put(KEY_PRECISION, channel.getPrecision());
        args.put(KEY_MOVINGAVERAGE, channel.getMovingAverage());
        args.put(KEY_MODELID, channel.getModelId());
        args.put(KEY_SOURCECHANNELID, channel.getSourceChannelId());
        args.put(KEY_SILENT, channel.getSilent());
        
        boolean result = db.update(DATABASE_TABLE_CHANNELS, args, 
                KEY_ROWID + "=" + rowId, null) > 0;
        close();
        return result;
    }
    
    public boolean deleteChannel(int rowId) 
    {
    	Logger.d(TAG,"Deleting from the database");
    	open();
    	boolean result =db.delete(DATABASE_TABLE_CHANNELS, KEY_ROWID + 
        		"=" + rowId, null) > 0;
        close();
        return result;
    }
    
    public boolean deleteChannel(Channel channel) 
    {
    	Logger.d(TAG,"Deleting from the database");
        return deleteChannel(channel.getId());
    }
    
    public void deleteAllChannelsForModel(int modelId)
    {
    	Logger.d(TAG,"Deleting from the database where modelId="+modelId);
    	open();
    	db.delete(DATABASE_TABLE_CHANNELS,KEY_MODELID+"="+modelId,null);
    	close();
    }
    
    public void deleteAllChannelsForModel(Model model)
    {
    	Logger.d(TAG,"Deleting from the database where modelId="+model.getId());
    	open();
    	db.delete(DATABASE_TABLE_CHANNELS,KEY_MODELID+"="+model.getId(),null);
    	close();
    }
    
    
    // ***********************************************************
    // ********************  ALARMS  *****************************
    // ***********************************************************
    
    // Strategy:
    // Always get and change all alarms for a model at the same time
    // UNIQUE(modelId,FrSkyFrametype), so no real need for ~id
    
    public TreeMap<Integer,Alarm> getAlarmsForModel(Model model)
    {
    	return getAlarmsForModel(model.getId());
    }
    
    public TreeMap<Integer, Alarm> getAlarmsForModel(int modelId)
    {
    	// Query for this modelid
    	open();
		Cursor cu = db.query(DATABASE_TABLE_FRSKYALARMS, FRSKYALARM_COLUMNS, 
				KEY_MODELID + "=" + modelId,
	            null, null, null, null, null);

		
    	// loop getChannel(Cursor)
		
		Logger.d(TAG,"Loading alarms for modelid: "+modelId);
		Logger.d(TAG,"  found: "+cu.getCount()+" alarms");
		
		TreeMap<Integer,Alarm> mAlarms = new TreeMap<Integer,Alarm>();
		
		cu.moveToFirst();
		int len = cu.getCount();
        for(int i=0;i<len;i++)
		{
        	Alarm a = getAlarm(cu);
        	a.setModelId(modelId);
			mAlarms.put(a.getFrSkyFrameType(), a);
			cu.moveToNext();
		}
		cu.deactivate();
		close();
		return mAlarms;
    }
    
    public Alarm getAlarm(Cursor cursor)
    {
    	Alarm a = new Alarm(Alarm.ALARMTYPE_FRSKY);
    	a.setModelId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_MODELID)));
    	a.setFrSkyFrameType(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_FRAMETYPE)));
    	a.setThreshold(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_THRESHOLD)));
    	a.setGreaterThan(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_GREATERTHAN)));
    	a.setAlarmLevel(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ALARMLEVEL)));
    	a.setUnitChannel(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_UNITSOURCECHANNEL)));
		
    	Logger.d(TAG,"Loaded alarm '"+a+"' from database");
		return a;
    }
    
    public boolean setAlarmsForModel(Model model)
    {
    	return setAlarmsForModel(model.getId(),model.getFrSkyAlarms());
    }
    
    public boolean setAlarmsForModel(int modelId,TreeMap<Integer,Alarm> alarmMap)
    {
    	// delete all the existing alarms
    	deleteAlarmsForModel(modelId);
 	
    	for(Alarm a : alarmMap.values())
    	{
    		insertAlarm(a);
    	}
    	// insert the new alarms
    	
    	return false;
    }
    
    public int insertAlarm(Alarm alarm)
    {
    	Logger.d(TAG,"Insert Alarm into the database: (ModelId,Frskyframe) ("+alarm.getModelId()+","+alarm.getFrSkyFrameType()+")");
    	open();
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_MODELID, alarm.getModelId());
        initialValues.put(KEY_FRAMETYPE, alarm.getFrSkyFrameType());
        initialValues.put(KEY_THRESHOLD, alarm.getThreshold());
        initialValues.put(KEY_GREATERTHAN, alarm.getGreaterThan());
        initialValues.put(KEY_ALARMLEVEL, alarm.getAlarmLevel());
        initialValues.put(KEY_UNITSOURCECHANNEL, alarm.getUnitChannelId());
        
        int id = (int) db.insert(DATABASE_TABLE_FRSKYALARMS, null, initialValues);
        close();
        Logger.d(TAG,"  This alarm got the id: "+id);
        return id;
    }
    
    public void deleteAlarmsForModel(Model model)
    {
    	deleteAlarmsForModel(model.getId());
    }
    
    public void deleteAlarmsForModel(int modelId)
    {

    	open();
    	Logger.d(TAG,"Delete alarms for model "+modelId+" from database");
    	db.delete(DATABASE_TABLE_FRSKYALARMS,KEY_MODELID+"="+modelId,null);
    	close();
    }
    
}
