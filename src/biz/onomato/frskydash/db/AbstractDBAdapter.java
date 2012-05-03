package biz.onomato.frskydash.db;


import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import biz.onomato.frskydash.util.Logger;

/**
 * general methods for DB management. Holds the SQL statements and methods for
 * managing connections
 * 
 */
public abstract class AbstractDBAdapter {
	
	//protected static final boolean DEBUG=true;
	
//    public static final String KEY_TYPE = "type";
    
	/**
	 * tag for debug messages
	 */
    private static final String TAG = "AbstractDBAdapter";
    
    private static final String DATABASE_NAME = "frsky";
    private static final int DATABASE_VERSION = 4;
    
    protected static final String DATABASE_TABLE_MODELS = "models";
    protected static final String KEY_ROWID = "_id";
    protected static final String KEY_NAME = "name";
    protected static final String KEY_MODELTYPE = "type";
    
    protected static final String DATABASE_TABLE_FRSKYALARMS = "alarmsfrsky";
    //protected static final String KEY_ROWID = "_id";
    protected static final String KEY_MODELID = "modelid";
    protected static final String KEY_FRAMETYPE = "frskyframetype";
    protected static final String KEY_THRESHOLD = "threshold";
    protected static final String KEY_GREATERTHAN = "greaterthan";
    protected static final String KEY_ALARMLEVEL = "alarmlevel";
    protected static final String KEY_UNITSOURCECHANNEL = "unitsourcechannel";
    
    //
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
    //protected static final String KEY_MODELID = "modelid";
    
    protected boolean _open=false;
    protected boolean _prevOpen=false;  
    
    private static final String DATABASE_CREATE_MODELS =
            "create table "
    		+ DATABASE_TABLE_MODELS+ "("
    		+ KEY_ROWID+			" integer primary key autoincrement, "
            + KEY_NAME+ 			" text not null, "
            + KEY_MODELTYPE+		" text"
            
            + ");";
    
	protected String[] MODEL_COLUMNS = {
			KEY_ROWID,
    		KEY_NAME,
    		KEY_MODELTYPE};
	
	private static final String DATABASE_CREATE_FRSKYALARMS =
            "create table "
    		+ DATABASE_TABLE_FRSKYALARMS+ "("
    		+ KEY_ROWID+			" integer primary key autoincrement, "
            + KEY_MODELID+ 			" integer, "
            + KEY_FRAMETYPE+		" integer, " 
            + KEY_THRESHOLD+		" integer, "
            + KEY_GREATERTHAN+		" integer, "
            + KEY_ALARMLEVEL+		" integer, "
            + KEY_UNITSOURCECHANNEL+" integer, "
            + "UNIQUE ("+KEY_MODELID+","+KEY_FRAMETYPE+") ON CONFLICT REPLACE"
            + ");";
	
	protected String[] FRSKYALARM_COLUMNS = {
			KEY_ROWID,
			KEY_MODELID,
			KEY_FRAMETYPE,
			KEY_THRESHOLD,
			KEY_GREATERTHAN,
			KEY_ALARMLEVEL,
			KEY_UNITSOURCECHANNEL};
    
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
    
	protected String[] CHANNEL_COLUMNS = new String[] {
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
    		KEY_MODELID};

    protected final Context context; 
    
    protected static DatabaseHelper DBHelper;
    
    protected static SQLiteDatabase db;

    /**
     * def ctor
     * 
     * @param ctx
     */
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
        	Logger.d(TAG,"Creating the database");
            db.execSQL(DATABASE_CREATE_MODELS);
            db.execSQL(DATABASE_CREATE_CHANNELS);
            db.execSQL(DATABASE_CREATE_FRSKYALARMS);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
        {
        	Logger.d(TAG,"Upgrade the database");
        	// Should perform alter table statements..
//            Log.w(TAG, "Upgrading database from version " + oldVersion 
//                    + " to "
//                    + newVersion + ", which will destroy all old data");
//            db.execSQL("DROP TABLE IF EXISTS titles");
//            onCreate(db);
        	
        	db.execSQL("DROP table IF EXISTS "+DATABASE_TABLE_CHANNELS);
        	db.execSQL("DROP table IF EXISTS "+DATABASE_TABLE_MODELS);
        	db.execSQL("DROP table IF EXISTS "+DATABASE_TABLE_FRSKYALARMS);
            onCreate(db);
        	
        }
    }    
	    
    /**
     * open database connection 
     * 
     * @return
     * @throws SQLException
     */
    public AbstractDBAdapter open() throws SQLException 
    {
//    	_prevOpen = _open;
//    	if(!_open)
//    	{
    		Logger.d(TAG,"Open the database:"+this.getClass().getName());
        	db = DBHelper.getWritableDatabase();
        	_open = true;
//    	}
//    	else
//    	{
//    		if(DEBUG)Log.d(TAG,"Database was already open");
//    	}
        return this;
    }

    /**
     * close database connection    
     */
    public void close() 
    {
//    	if(_prevOpen)
//    	{
    		Logger.d(TAG,"Close the database:"+this.getClass().getName());
    		db.close();
    		_open = false;
        	DBHelper.close();
//    	}
//    	else
//    	{
//    		if(DEBUG)Log.d(TAG,"Database was already open, leave it open");
//    	}
    }
    
	/**
	 * verify if the database is open or not
	 * 
	 * @return
	 */
    public boolean isOpen()
    {
    	return _open;
    }

    /**
     * get database schema, returns null on exceptions
     * 
     * @return
     */
    public Cursor schema()
    {
    	Logger.d(TAG,"DATABASE SCHEMA");
    	try
    	{
    		Cursor cursor = DBHelper.getReadableDatabase().rawQuery("SELECT type,tbl_name,sql from sqlite_master;",null);
    		return cursor;
    	}
    	catch(Exception e)
    	{
    		Logger.e(TAG,e.toString());
    		return null;
    	}
    	
    }
	
}
