package biz.onomato.frskydash;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

public class DBAdapterModel extends AbstractDBAdapter {

	private static final String TAG = "DBAdapterModel";
	
	public DBAdapterModel(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

    //---insert a title into the database---
    public long insertModel(String name) 
    {
    	if(DEBUG)Log.d(TAG,"Insert into the database");
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NAME, name);
        //initialValues.put(KEY_TITLE, title);
       
        return db.insert(DATABASE_TABLE_MODELS, null, initialValues);
    }

    //---deletes a particular title---
    public boolean deleteModel(long rowId) 
    {
    	if(DEBUG)Log.d(TAG,"Deleting from the database");
    	
    	return db.delete(DATABASE_TABLE_MODELS, KEY_ROWID + 
        		"=" + rowId, null) > 0;
        
    }

    //---retrieves all the titles---
    public Cursor getAllModels() 
    {
    	if(DEBUG)Log.d(TAG,"Getting all models from database");

    	return db.query(DATABASE_TABLE_MODELS, new String[] {
        		KEY_ROWID, 
        		KEY_NAME,
        		KEY_MODELTYPE
                }, 
                null, 
                null, 
                null, 
                null, 
                null);

    	
    }

    //---retrieves a particular model---
    public Cursor getModel(long rowId) throws SQLException 
    {
    	if(DEBUG)Log.d(TAG,"Get one model from the database");
    	
        Cursor mCursor =
                db.query(true, DATABASE_TABLE_MODELS, new String[] {
                		KEY_ROWID,
                		KEY_NAME,
                		KEY_MODELTYPE
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
    public boolean updateModel(long rowId, String name) 
    {
    	if(DEBUG)Log.d(TAG,"Update one model in the database");
    	
        ContentValues args = new ContentValues();
        args.put(KEY_NAME, name);
        return db.update(DATABASE_TABLE_MODELS, args, 
                KEY_ROWID + "=" + rowId, null) > 0;
        
        
    }
    
    

	
}
