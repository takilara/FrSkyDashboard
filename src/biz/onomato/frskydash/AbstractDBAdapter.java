package biz.onomato.frskydash;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public abstract class AbstractDBAdapter {
	protected static final boolean DEBUG=true;
	
//    public static final String KEY_TYPE = "type";
    
    private static final String TAG = "AbstractDBAdapter";
    
    private static final String DATABASE_NAME = "frsky";
    private static final int DATABASE_VERSION = 3;
    
    
    protected static final String DATABASE_TABLE_MODELS = "models";
    protected static final String KEY_ROWID = "_id";
    protected static final String KEY_NAME = "name";
    
    private static final String DATABASE_CREATE_MODELS =
        "create table models (_id integer primary key autoincrement, "
        + "name text not null, "
        + "type text" 
        + ");";
    
    protected static final String DATABASE_TABLE_CHANNELS = "channels";
    protected static final String KEY_DESCRIPTION = "description";
    protected static final String KEY_LONGUNIT = "longunit";
    protected static final String KEY_SHORTUNIT = "shortunit";
    protected static final String KEY_OFFSET = "offset";
    protected static final String KEY_FACTOR = "factor";
    protected static final String KEY_PRECISION = "precision";
    protected static final String KEY_MOVINGAVERAGE = "movingaverage";
    protected static final String KEY_SOURCECHANNELID = "sourcechannelid";
    protected static final String KEY_SILENT = "silent";
    protected static final String KEY_MODELID = "modelid";
    
    private static final String DATABASE_CREATE_CHANNELS =
            "create table "
        	+ DATABASE_TABLE_CHANNELS+ 		"("
            + KEY_ROWID+			" integer primary key autoincrement, "
            + KEY_DESCRIPTION+		" text, "
            + KEY_LONGUNIT+			" text, " 
            + KEY_SHORTUNIT+		" text, "
            + KEY_OFFSET+			" real, "
            + KEY_FACTOR+			" real, "
            + KEY_PRECISION+		" integer, "
            + KEY_MOVINGAVERAGE+ 	" integer, "
            + KEY_SOURCECHANNELID+ 	" integer, "
            + KEY_SILENT+			" integer, "
            + KEY_MODELID+ 			" integer DEFAULT -1"
            + ");";
    
        
    protected final Context context; 
    
    protected DatabaseHelper DBHelper;
    protected SQLiteDatabase db;

    public AbstractDBAdapter(Context ctx) 
    {
        this.context = ctx;
        DBHelper = new DatabaseHelper(context);
    }
	        
    protected static class DatabaseHelper extends SQLiteOpenHelper 
    {
        DatabaseHelper(Context context) 
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) 
        {
        	if(DEBUG)Log.d(TAG,"Creating the database");
            db.execSQL(DATABASE_CREATE_MODELS);
            db.execSQL(DATABASE_CREATE_CHANNELS);
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
        	
        	db.execSQL("DROP table IF EXISTS channels");
        	db.execSQL("DROP table IF EXISTS models");
            onCreate(db);
        	
        }
    }    
	    
    //---opens the database---
    public AbstractDBAdapter open() throws SQLException 
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
    

    public Cursor schema()
    {
    	try
    	{
    		Cursor cursor = DBHelper.getReadableDatabase().rawQuery("SELECT type,tbl_name,sql from sqlite_master;",null);
    		return cursor;
    	}
    	catch(Exception e)
    	{
    		Log.e(TAG,e.toString());
    		return null;
    	}
    	
    }
	
}
