package biz.onomato.frskydash;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBAdapterChannel extends AbstractDBAdapter {
    private static final String TAG = "DBAdapterChannel";
    
    public DBAdapterChannel(Context context) 
    {
        super(context);
    }
	        

	    

    
    //---insert a title into the database---
    //public long insertChannel(String description) 
    public long insertChannel(Channel channel)
    {
    	if(DEBUG)Log.d(TAG,"Insert into the database");
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
        
        
        
        return db.insert(DATABASE_TABLE_CHANNELS, null, initialValues);
    }

    //---deletes a particular title---
    public boolean deleteChannel(long rowId) 
    {
    	if(DEBUG)Log.d(TAG,"Deleting from the database");
        return db.delete(DATABASE_TABLE_CHANNELS, KEY_ROWID + 
        		"=" + rowId, null) > 0;
    }
    
    public void deleteAllChannelsForModel(long modelId)
    {
    	if(DEBUG)Log.d(TAG,"Deleting from the database where modelId="+modelId);
    	db.delete(DATABASE_TABLE_CHANNELS,KEY_MODELID+"="+modelId,null);
    }

    //---retrieves all the titles---
    public Cursor getAllChannels() 
    {
    	if(DEBUG)Log.d(TAG,"Getting all channels from database");
        return db.query(DATABASE_TABLE_CHANNELS, new String[] {
        		KEY_ROWID, 
        		KEY_DESCRIPTION,
        		KEY_LONGUNIT,
        		KEY_SHORTUNIT,
        		KEY_OFFSET,
        		KEY_FACTOR,
        		KEY_PRECISION,
        		KEY_MOVINGAVERAGE,
        		KEY_SOURCECHANNELID,
        		KEY_SILENT,
        		KEY_MODELID
                }, 
                null, 
                null, 
                null, 
                null, 
                null);
    }
    
    public Cursor getAllChannelsForModel(long modelId) 
    {
    	if(DEBUG)Log.d(TAG,"Getting all channels for a model from database");
        return db.query(DATABASE_TABLE_CHANNELS, new String[] {
        		KEY_ROWID, 
        		KEY_DESCRIPTION,
        		KEY_LONGUNIT,
        		KEY_SHORTUNIT,
        		KEY_OFFSET,
        		KEY_FACTOR,
        		KEY_PRECISION,
        		KEY_MOVINGAVERAGE,
        		KEY_SOURCECHANNELID,
        		KEY_SILENT,
        		KEY_MODELID
                }, 
                KEY_MODELID + "=" + modelId,
                null, 
                null, 
                null, 
                null, 
                null);
    }

    //---retrieves a particular channel---
    public Cursor getChannel(long rowId) throws SQLException 
    {
    	if(DEBUG)Log.d(TAG,"Get one channel from the database");
        Cursor mCursor =
                db.query(true, DATABASE_TABLE_CHANNELS, new String[] {
                		KEY_ROWID,
                		KEY_DESCRIPTION,
                		KEY_LONGUNIT,
                		KEY_SHORTUNIT,
                		KEY_OFFSET,
                		KEY_FACTOR,
                		KEY_PRECISION,
                		KEY_MOVINGAVERAGE,
                		KEY_SOURCECHANNELID,
                		KEY_SILENT,
                		KEY_MODELID
                		}, 
                		KEY_ROWID + "=" + rowId, 
                		null,
                		null, 
                		null, 
                		null, 
                		null);
        if (mCursor != null) {
//        	Log.d(TAG,"Found the model..");
//        	Log.d(TAG,"Count: "+mCursor.getCount());
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    //---updates a title---
    public boolean updateChannel(Channel channel) 
    {
    	if(DEBUG)Log.d(TAG,"Update one channel in the database");
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
        
        return db.update(DATABASE_TABLE_CHANNELS, args, 
                         KEY_ROWID + "=" + rowId, null) > 0;
    }
	
//    @Override
//    public AbstractDBAdapter open() throws SQLException 
//    {
//    	
//    	if(DEBUG)Log.d(TAG,"Open the database");
//        return super.open();
//    }
//
//    //---closes the database---
//    @Override
//    public void close() 
//    {
//    	if(DEBUG)Log.d(TAG,"Close the database");
//        DBHelper.close();
//    }
    
}
