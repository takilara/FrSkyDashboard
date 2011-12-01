package biz.onomato.frskydash;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBAdapterModel {
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
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
        {
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
        db = DBHelper.getWritableDatabase();
        return this;
    }

    //---closes the database---    
    public void close() 
    {
        DBHelper.close();
    }
    
    //---insert a title into the database---
    public long insertTitle(String name, String title, String publisher) 
    {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NAME, name);
        //initialValues.put(KEY_TITLE, title);
        
        return db.insert(DATABASE_TABLE, null, initialValues);
    }

    //---deletes a particular title---
    public boolean deleteTitle(long rowId) 
    {
        return db.delete(DATABASE_TABLE, KEY_ROWID + 
        		"=" + rowId, null) > 0;
    }

    //---retrieves all the titles---
    public Cursor getAllTitles() 
    {
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
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    //---updates a title---
    public boolean updateModel(long rowId, String name) 
    {
        ContentValues args = new ContentValues();
        args.put(KEY_NAME, name);
        return db.update(DATABASE_TABLE, args, 
                         KEY_ROWID + "=" + rowId, null) > 0;
    }
	
}
