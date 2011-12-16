package biz.onomato.frskydash;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBAdapterChannel {
	private static final boolean DEBUG=true;
	public static final String KEY_ROWID = "_id";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_LONGUNIT = "longunit";
    public static final String KEY_SHORTUNIT = "shortunit";
    public static final String KEY_OFFSET = "offset";
    public static final String KEY_FACTOR = "factor";
    public static final String KEY_PRECISION = "precision";
    public static final String KEY_MOVINGAVERAGE = "movingaverage";
    public static final String KEY_SOURCECHANNELID = "sourcechannelid";
    public static final String KEY_SILENT = "silent";
    public static final String KEY_MODELID = "modelid";
//    public static final String KEY_TYPE = "type";
    
    private static final String TAG = "DBAdapterChannel";
    
    private static final String DATABASE_NAME = "frsky";
    private static final String DATABASE_TABLE = "channels";
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE =
        "create table "
    	+ DATABASE_TABLE+ 		"("
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
        
    private final Context context; 
    
    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;

    public DBAdapterChannel(Context ctx) 
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
    public DBAdapterChannel open() throws SQLException 
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
        
        
        
        return db.insert(DATABASE_TABLE, null, initialValues);
    }

    //---deletes a particular title---
    public boolean deleteChannel(long rowId) 
    {
    	if(DEBUG)Log.d(TAG,"Deleting from the database");
        return db.delete(DATABASE_TABLE, KEY_ROWID + 
        		"=" + rowId, null) > 0;
    }

    //---retrieves all the titles---
    public Cursor getAllChannels() 
    {
    	if(DEBUG)Log.d(TAG,"Getting all channels from database");
        return db.query(DATABASE_TABLE, new String[] {
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

    //---retrieves a particular channel---
    public Cursor getChannel(long rowId) throws SQLException 
    {
    	if(DEBUG)Log.d(TAG,"Get one channel from the database");
        Cursor mCursor =
                db.query(true, DATABASE_TABLE, new String[] {
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

        
        return db.update(DATABASE_TABLE, args, 
                         KEY_ROWID + "=" + rowId, null) > 0;
    }
	
}
