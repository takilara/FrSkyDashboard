package biz.onomato.frskydash;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBAdapterModel {
	private static final boolean DEBUG=true;
	public static final String KEY_ROWID = "_id";
    public static final String KEY_NAME = "name";
//    public static final String KEY_TYPE = "type";
    
    private static final String TAG = "DBAdapterModel";
    
    private static final String DATABASE_NAME = "frsky";
    private static final String DATABASE_TABLE = "models";
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE =
        "create table models (_id integer primary key autoincrement, "
        + "name text not null, "
        + "type text" 
        + ");";
        
    private final Context context; 
    
    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;

    public DBAdapterModel(Context ctx) 
    {
        this.context = ctx;
        DBHelper = new DatabaseHelper(context);
    }
	        
    private static class DatabaseHelper extends SQLiteOpenHelper 
    {
        DatabaseHelper(Context context) 
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) 
        {
        	if(DEBUG)Log.d(TAG,"Creating the database");
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
        {
        	if(DEBUG)Log.d(TAG,"Upgrade the database");
        	// Should perform alter table statements..
//            Log.w(TAG, "Upgrading database from version " + oldVersion 
//                    + " to "
//                    + newVersion + ", which will destroy all old data");
//            db.execSQL("DROP TABLE IF EXISTS titles");
//            onCreate(db);
        }
    }    
	    
    //---opens the database---
    public DBAdapterModel open() throws SQLException 
    {
    	if(DEBUG)Log.d(TAG,"Open the database");
        db = DBHelper.getWritableDatabase();
        return this;
    }

    //---closes the database---    
    public void close() 
    {
    	if(DEBUG)Log.d(TAG,"Close the database");
        DBHelper.close();
    }
    
    //---insert a title into the database---
    public long insertModel(String name) 
    {
    	if(DEBUG)Log.d(TAG,"Insert into the database");
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NAME, name);
        //initialValues.put(KEY_TITLE, title);
        
        return db.insert(DATABASE_TABLE, null, initialValues);
    }

    //---deletes a particular title---
    public boolean deleteModel(long rowId) 
    {
    	if(DEBUG)Log.d(TAG,"Deleting from the database");
        return db.delete(DATABASE_TABLE, KEY_ROWID + 
        		"=" + rowId, null) > 0;
    }

    //---retrieves all the titles---
    public Cursor getAllModels() 
    {
    	if(DEBUG)Log.d(TAG,"Getting all models from database");
        return db.query(DATABASE_TABLE, new String[] {
        		KEY_ROWID, 
        		KEY_NAME
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
                db.query(true, DATABASE_TABLE, new String[] {
                		KEY_ROWID,
                		KEY_NAME 
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
        return db.update(DATABASE_TABLE, args, 
                         KEY_ROWID + "=" + rowId, null) > 0;
    }
	
}
