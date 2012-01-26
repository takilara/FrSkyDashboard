package biz.onomato.frskydash;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class DB extends AbstractDBAdapter {
    private static final String TAG = "DBAdapterChannel";
    
    public DB(Context context) 
    {
        super(context);
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
        	Log.e(TAG,"Model with id: "+modelId+" was not found");
        	m = getModel();
        }
        cu.deactivate();
        close();
        return m;
    }
    
    public Model getModel()
    {
    	// Get the first model
    	Cursor cu =
                db.query(true, DATABASE_TABLE_MODELS, MODEL_COLUMNS,           		 
                		null,null,null,null,KEY_ROWID,"1");
    	
    	Model m;
    	if(cu.getCount()!=0)
        {	
	        m = getModel(cu);
    	}
    	else
    	{
    		m = null;
    	}
    	return m;
    }
    
    
    public Model getModel(Cursor cu)
    {
    	Model m = new Model(context);
    	
		m.setId(cu.getInt(cu.getColumnIndexOrThrow(DBAdapterChannel.KEY_ROWID)));
		m.setName(cu.getString(cu.getColumnIndexOrThrow(DBAdapterChannel.KEY_NAME)));
		m.setType(cu.getString(cu.getColumnIndexOrThrow(DBAdapterChannel.KEY_MODELTYPE)));
		
		m.setChannels(getChannelsForModel(m));
		return m;
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
    	if(DEBUG)Log.d(TAG,"Getting all channels from database");
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
    	Channel ch = new Channel(context);
    	ch.setId(c.getInt(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_ROWID)));
    	ch.setDescription(c.getString(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_DESCRIPTION)));
    	ch.setLongUnit(c.getString(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_LONGUNIT)));
		ch.setShortUnit(c.getString(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_SHORTUNIT)));
		ch.setFactor(c.getFloat(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_FACTOR)));
		ch.setOffset(c.getFloat(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_OFFSET)));
		ch.setPrecision(c.getInt(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_PRECISION)));
		ch.setMovingAverage(c.getInt(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_MOVINGAVERAGE)));
		ch.setSilent(c.getInt(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_SILENT))>0);
		ch.setModelId(c.getLong(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_MODELID)));
		ch.listenTo(c.getInt(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_SOURCECHANNELID)));
		ch.setDirtyFlag(false);
		//db.close();
		
		if(DEBUG) Log.d(TAG,"Loaded '"+ch.getDescription()+"' from database");
		if(DEBUG) Log.d(TAG,"\tSilent:\t"+ch.getSilent());
		return ch;
    }
    
    public Channel getChannel(Long channelId)
    {
    	if(DEBUG)Log.d(TAG,"Get one channel from the database");
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
    
    public long insertChannel(Channel channel)
    {
    	if(DEBUG)Log.d(TAG,"Insert into the database");
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
        close();
        return db.insert(DATABASE_TABLE_CHANNELS, null, initialValues);
    }
    
    public boolean updateChannel(Channel channel)
    {
    	if(DEBUG)Log.d(TAG,"Update one channel in the database");
    	open();
        ContentValues args = new ContentValues();
        long rowId = channel.getId();
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
        close();
        return db.update(DATABASE_TABLE_CHANNELS, args, 
                         KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    public boolean deleteChannel(long rowId) 
    {
    	if(DEBUG)Log.d(TAG,"Deleting from the database");
    	open();
    	boolean result =db.delete(DATABASE_TABLE_CHANNELS, KEY_ROWID + 
        		"=" + rowId, null) > 0;
        close();
        return result;
    }
    
    public boolean deleteChannel(Channel channel) 
    {
    	if(DEBUG)Log.d(TAG,"Deleting from the database");
        return deleteChannel(channel.getId());
    }
    
    public void deleteAllChannelsForModel(long modelId)
    {
    	if(DEBUG)Log.d(TAG,"Deleting from the database where modelId="+modelId);
    	open();
    	db.delete(DATABASE_TABLE_CHANNELS,KEY_MODELID+"="+modelId,null);
    	close();
    }
    
    public void deleteAllChannelsForModel(Model model)
    {
    	if(DEBUG)Log.d(TAG,"Deleting from the database where modelId="+model.getId());
    	open();
    	db.delete(DATABASE_TABLE_CHANNELS,KEY_MODELID+"="+model.getId(),null);
    	close();
    }
    
}
