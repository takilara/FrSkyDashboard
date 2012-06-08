package biz.onomato.frskydash.db;

import java.util.ArrayList;
import java.util.TreeMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import biz.onomato.frskydash.domain.Alarm;
import biz.onomato.frskydash.domain.Channel;
import biz.onomato.frskydash.domain.Model;
import biz.onomato.frskydash.util.Logger;

/**
 * <p>
 * All database access should go through this class. This clas provides methods
 * for retrieving {@link Model}s with their {@link Channel}s and {@link Alarm}s
 * and all required CRUD operations on these objects.
 * </p>
 * 
 * <p>
 * From outside the saveX methods should be used for both new entries (inserts)
 * and updates.</p>
 * 
 */
public class FrSkyDatabase extends AbstractDBAdapter {
   
	/**
	 * identifier for logging
	 */
	private static final String TAG = "Database";
    
	/**
	 * default ctor, needs a context
	 * 
	 * @param context
	 *            the context for this database
	 */
    public FrSkyDatabase(Context context) 
    {
        super(context);
    }

    // ***********************************************************
    // ********************  MODELS    ***************************
    // ***********************************************************

    /**
     * retrieve a list of all available models
     * 
     */
    public ArrayList<Model> getModels()
    {
    	// debug information
    	Logger.d(TAG,"Getting all models from database");
    	// open the DB connection
    	open();
    	//retrieve a cursor for all models
    	Cursor cursor = db.query(DATABASE_TABLE_MODELS, MODEL_COLUMNS, 
	            null, null, null, null, null);
    	// prepare collection of proper size
        ArrayList<Model> mList = new ArrayList<Model>(cursor.getCount());
        //go to first position
        if( cursor.moveToFirst()){
	        //int len = cursor.getCount();
	        //while(!cu.isAfterLast())
	        //for(int i=0;i<len;i++)
			do{
	        	Logger.i(TAG,"Getting the model from position "+cursor.getPosition());
	        	Model m = getModel(cursor);
	        	Logger.i(TAG,"  This model is: "+m.getName());
				mList.add(m);
				//cursor.moveToNext();
			} while(cursor.moveToNext());
        } else {
        	Logger.d(TAG,"No models found in database");
        }
		//close cursor & DB connection
		cursor.deactivate();
		close();
		//debug
		Logger.e(TAG,"Our Model list now contains: ");
		// log al collected models , already done on getting model
//		for(Model m : mList) {
//			Logger.e(TAG,"  "+m.getName());
//		}
		// return the collection
		return mList;
    }
    
    /**
     * Retrieve details of a Model for the given ID
     * 
     * @param modelId
     * @return
     */
    public Model getModel(int modelId)
    {
    	// open connection
    	open();
    	// get cursor
    	Cursor cu =
                db.query(true, DATABASE_TABLE_MODELS, MODEL_COLUMNS, 
                		KEY_ROWID + "=" + modelId, 
                		null,null,null,null,null);
        //if (cu != null) {
//        	Log.d(TAG,"Found the model..");
//        	Log.d(TAG,"Count: "+mCursor.getCount());
        //    cu.moveToFirst();
        //}
    	// init model as null
        Model m = null;
        if( cu.moveToFirst() )
        {	
	        m = getModel(cu);
    	}
        // if no model available by selection on ID rely on getting first model (can also be null?)
        else
        {
        	Logger.e(TAG,"Model with id: "+modelId+" was not found, try to get first model");
        	m = getModel();
        }
        // close
        cu.deactivate();
        close();
        return m;
    }
    
    /**
     * helper to get the first model from database
     * 
     * @return the first model from database if available, if none available this method will return null
     */
    private Model getModel()
    {
    	// Get the first model
    	Logger.i(TAG,"Try to get first model");
    	// open db connection
    	open();
    	// retrieve the first model 
    	Cursor cu =
                db.query(true, DATABASE_TABLE_MODELS, MODEL_COLUMNS,           		 
                		null,null,null,null,KEY_ROWID,"1");
    	// init model as null so we can return if nothing available
    	Model m = null;
    	// go to first element (moveToFirst will return false if nothing available)
    	if( cu != null && cu.moveToFirst() ) {	
	        m = getModel(cu);
    	}
    	// close cursor
    	cu.deactivate();
    	// and connection
    	close();
    	// then return the found model
    	return m;
    }

    /**
     * helper to get model from a cursor. For this no query is executed, everything is fetched from the cursor
     * 
     * FIXME make sure to use complete sql statement so we don't have to fetch channels and alarms separately
     * 
     * @param cu
     * @return
     */
    private Model getModel(Cursor cu)
    {
    	//debug information
    	Logger.i(TAG,"Pickup the model info from the cursor: "+cu.getColumnNames());
    	// init this model as an empty model so we always have something to return
    	Model m = new Model();
    	//cu.moveToFirst();
    	Logger.i(TAG,"cursor id: "+cu.getInt(0));
    	// complete model based on information from give cursor
		m.setId(cu.getInt(cu.getColumnIndexOrThrow(KEY_ROWID)));
		m.setName(cu.getString(cu.getColumnIndexOrThrow(KEY_NAME)));
		m.setType(cu.getString(cu.getColumnIndexOrThrow(KEY_MODELTYPE)));
		// Add Channels to the model
		//FIXME update query and get this info from the cursor instead
		ArrayList<Channel> channelList = getChannelsForModel(m.getId());
		Logger.i(TAG,"Found "+channelList.size()+" channels for model with id "+m.getId());
		m.setChannels(channelList);
		// Add Alarms to the model
		//FIXME update query and get this info form the cursor instead
		TreeMap<Integer,Alarm> alarmMap = getAlarmsForModel(m.getId());
		m.setFrSkyAlarms(alarmMap);
		// return this information
		return m;
    }
    
	/**
	 * save a model. This is either an update or insert based on the
	 * availability in the backend
	 * 
	 * @param model
	 */
    public void saveModel(Model model)
    {
    	// if the model is -1 then this is a non saved object that needs to be inserted in the backend
    	if(model.getId()==-1)
    	{
    		// insert and retrieve the generated model id, if this is still negative than insert failed
    		if( insertModel(model) == -1)
    		{	
    			//TODO why not return an exception instead of null? Than we can react on that exception? Same for other save methods 
    			Logger.e(TAG,"Inserting the model failed");
    			return;
    		}
    	}
    	// otherwise this is an existing model so we can perform an update instead
    	else if(!updateModel(model))
    	{
    		Logger.e(TAG,"Updating the model failed");
    		return;
    	}
    	// at this point Insert/update did not fail
		// first, make sure the channels have the correct modelId
		// Make sure all the models channels modelid is correct. These have to
		// be updated because they could be added while the model wasn't saved
		// yet and therefor didn't have a proper ID from database yet
		int mId = model.getId();
        // iterate all channels and set the model id
        		for(Channel c : model.getChannels().values())
        		{
        			c.setModelId(mId);
        		}
        	//}
    		
    		// Update the channels
    		Logger.i(TAG,"Saving channels");
    		
    		// no good
    		//deleteAllChannelsForModel(model);
    		
    		// iterate channels for a model in database, all these channels that are no longer found in the Model in memory are deleted channels
    		// TODO review this
    		for(Channel c:getChannelsForModel(model))
    		{
    			Logger.d(TAG,"Checking model for database channel '"+c.getDescription()+"'");
    			if(!model.getChannels().containsValue(c))
    			{
    				// Channel no longer in model, delete it
    				Logger.d(TAG,c.getDescription()+" not in the model, delete it");
    				deleteChannel(c);
    			}
    		}
    		// FIXME is it needed to save them here? Is setAlarmsForModel not already saving these??
    		// update or add channels based on the channels on the model in memory
    		for(Channel ch:model.getChannels().values())
    		{
    			saveChannel(ch);
    		}
    		
    		// Update the alarms
    		Logger.i(TAG,"Saving alarms");
    		setAlarmsForModel(model);
    }

    /**
     * helper for updating an existing model
     * 
     * @param model
     * @return
     */
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
    
	/**
	 * insert a model into database. By doing so an id is generated and set to
	 * the model reference.
	 * 
	 * @param model
	 * @return
	 */
    private int insertModel(Model model)
    {
    	// debug information
    	Logger.d(TAG,"Insert Model '"+model.getName()+"' into the database");
    	// open connection
    	open();
    	// create content
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NAME, model.getName());
        initialValues.put(KEY_MODELTYPE, model.getType());
        //initialValues.put(KEY_TITLE, title);
        // insert and retrieve the generated id
        int newId = (int) db.insert(DATABASE_TABLE_MODELS, null, initialValues);
        // log information
        Logger.d(TAG," The id for '"+model.getName()+"' is "+newId);
        // update model object
        model.setId(newId);
        //FIXME is there a better way to get the last id first from db? Done this in PL/SQL but don't now for 
        model.setName("Model "+newId);
        ContentValues cv = new ContentValues();
        cv.put(KEY_NAME, model.getName());
        db.update(DATABASE_TABLE_MODELS, cv, KEY_ROWID + "=?", new String[]{String.valueOf(model.getId())});
        // close connection
        close();
        // return the new id
        return newId;
    }
    
    /**
     * delete a model from backend
     * 
     * @param modelId
     * @return
     */
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

    /**
     * get all channels for the given model
     * 
     */
    public ArrayList<Channel> getChannelsForModel(int modelId)
    {
    	// Query for this modelid
    	open();
		Cursor cu = db.query(DATABASE_TABLE_CHANNELS, CHANNEL_COLUMNS, 
				KEY_MODELID + "=" + modelId,
	            null, null, null, null, null);
    	// loop getChannel(Cursor)
		// create collection of the right size
		ArrayList<Channel> mList = new ArrayList<Channel>(cu.getCount());
		cu.moveToFirst();
        while(!cu.isAfterLast())
		{
        	Channel ch = getChannel(cu);
			mList.add(ch);
			cu.moveToNext();
		}
        //close all
		cu.deactivate();
		close();
		return mList;
    }
    
	/**
	 * wrapper method to retrieve all channels for a model
	 * 
	 * @param model
	 * @return
	 */
    public ArrayList<Channel> getChannelsForModel(Model model)
    {
    	return getChannelsForModel(model.getId());
    }
    
    /**
     * retrieve all available channels from backend
     * 
     * @return
     */
    public ArrayList<Channel> getChannels()
    {
    	Logger.d(TAG,"Getting all channels from database");
    	open();
    	Cursor cu = db.query(DATABASE_TABLE_CHANNELS, CHANNEL_COLUMNS, 
	            null, null, null, null, null);
    	// collection of right size
        ArrayList<Channel> mList = new ArrayList<Channel>(cu.getCount());
        // iterate
        while(!cu.isAfterLast())
		{
        	Channel ch = getChannel(cu);
			mList.add(ch);
			cu.moveToNext();
		}
        // close
		cu.deactivate();
		close();
		return mList;
    }
    
    /**
     * helper to create a channel from cursor
     * 
     * @param c
     * @return
     */
    private Channel getChannel(Cursor c)
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
		
		Logger.d(TAG,"Loaded '"+ch.getDescription()+"' from database");
		Logger.d(TAG,"\tSilent:\t"+ch.getSilent());
		return ch;
    }
    
	/**
	 * get a channel from backend based on ID. Will return null if the channel
	 * doesn't exist
	 * 
	 * @param channelId
	 * @return
	 */
    public Channel getChannel(int channelId)
    {
    	Logger.d(TAG,"Get one channel from the database (channelid: "+channelId+")");
    	open();
        Cursor cu = db.query(true, DATABASE_TABLE_CHANNELS, CHANNEL_COLUMNS, 
                		KEY_ROWID + "=" + channelId, 
                		null, null,	null, null, null);
        // init as null reference
        Channel ch = null;
        // try to retrieve object data
        if (cu != null && cu.moveToFirst()) {
//        	Log.d(TAG,"Found the model..");
//        	Log.d(TAG,"Count: "+mCursor.getCount());
//            cu.moveToFirst();
        	ch = getChannel(cu);
        }
        // close
        cu.deactivate();
        close();
        return ch;
    }
    
	/**
	 * save (insert or update) a channel object
	 * 
	 * @param channel
	 */
    public void saveChannel(Channel channel)
    {
    	// if channel id is -1 then this is a new channel that needs insert
    	if(channel.getId()==-1)
    	{
    		// save using insert
    		Logger.d(TAG,"Save channel using insert");
    		if( insertChannel(channel) == -1)
    		{
    			Logger.e(TAG,"Inserting channel failed");
    			return;
    		}
    	}
    	// save using update
    	else if(!updateChannel(channel))
    		{
    			Logger.e(TAG,"Channel Update failed");
    		}
    }
    
    /**
     * insert of a channel object
     * 
     * @param channel
     * @return
     */
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
        // update channel id
        channel.setId(id);
        
        close();
        return id;
    }
    
    /**
     * update channel object in database
     * 
     * @param channel
     * @return
     */
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
    
    /**
     * delete a channel from db
     * 
     * @param rowId
     * @return
     */
    public boolean deleteChannel(int rowId) 
    {
    	Logger.d(TAG,"Deleting from the database");
    	open();
    	boolean result =db.delete(DATABASE_TABLE_CHANNELS, KEY_ROWID + 
        		"=" + rowId, null) > 0;
        close();
        return result;
    }
    
    /**
     * delete a channel from db
     * 
     * @param channel
     * @return
     */
    public boolean deleteChannel(Channel channel) 
    {
    	Logger.d(TAG,"Deleting from the database");
        return deleteChannel(channel.getId());
    }
    
	/**
	 * delete all channels for a model
	 * 
	 * @param modelId
	 */
    public void deleteAllChannelsForModel(int modelId)
    {
    	Logger.d(TAG,"Deleting from the database where modelId="+modelId);
    	open();
    	db.delete(DATABASE_TABLE_CHANNELS,KEY_MODELID+"="+modelId,null);
    	close();
    }
    
    /**
     * delete all channels for given model 
     * 
     * @param model
     */
    public void deleteAllChannelsForModel(Model model)
    {
    	deleteAlarmsForModel(model.getId());
    }
    
    
    // ***********************************************************
    // ********************  ALARMS  *****************************
    // ***********************************************************
    
    // Strategy:
    // Always get and change all alarms for a model at the same time
    // UNIQUE(modelId,FrSkyFrametype), so no real need for ~id
    
    /**
     * get all alarms for a given model
     * 
     */
    public TreeMap<Integer,Alarm> getAlarmsForModel(Model model)
    {
    	return getAlarmsForModel(model.getId());
    }
    
    /**
     * get all alarms for given model 
     * 
     * @param modelId
     * @return
     */
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
		// init collection
		TreeMap<Integer,Alarm> mAlarms = new TreeMap<Integer,Alarm>();
		// go to first element
		if (cu.moveToFirst())
			do {
				Alarm a = getAlarm(cu);
				a.setModelId(modelId);
				mAlarms.put(a.getFrSkyFrameType(), a);
			} while (cu.moveToNext());
		// clean up
		cu.deactivate();
		close();
		// return 
		return mAlarms;
    }
    
    /**
     * get information for a single alarm
     * 
     * @param cursor
     * @return
     */
    private Alarm getAlarm(Cursor cursor)
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
    
    /**
     * update alarms for a model
     * 
     * @param model
     * @return
     */
    public boolean setAlarmsForModel(Model model)
    {
    	return setAlarmsForModel(model.getId(),model.getFrSkyAlarms());
    }
    
    /**
     * update alarms for a model
     * 
     * @param modelId
     * @param alarmMap
     * @return
     */
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
    
    /**
     * insert a single alarm
     * 
     * @param alarm
     * @return
     */
    private int insertAlarm(Alarm alarm)
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
    
    /**
     * delete all alarms for a model
     * 
     * @param model
     */
    public void deleteAlarmsForModel(Model model)
    {
    	deleteAlarmsForModel(model.getId());
    }
    
    /**
     * delete all alarms for model
     * 
     * @param modelId
     */
    public void deleteAlarmsForModel(int modelId)
    {
    	open();
    	Logger.d(TAG,"Delete alarms for model "+modelId+" from database");
    	db.delete(DATABASE_TABLE_FRSKYALARMS,KEY_MODELID+"="+modelId,null);
    	close();
    }
    
}
