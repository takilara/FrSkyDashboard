package biz.onomato.frskydash;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

public class FrskyDatabase extends SQLiteOpenHelper {
	static final String TAG="FrSkyDatabase";
		
	static final String DB_NAME="frsky";
	static final String TBL_CHANNEL_CONFIG="channelconfig";
	static final String COL_ID="channelId";
	static final String COL_NAME="channelName";
	static final String COL_DESCRIPTION="channelDescription";
	
	public FrskyDatabase(Context context) {
		super(context, DB_NAME, null, 6);
		Log.i(TAG,"Constructor");
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		Log.i(TAG,"Create the database File");
		String q = "CREATE TABLE "+TBL_CHANNEL_CONFIG+" ("+COL_ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+COL_NAME+" text,"+COL_DESCRIPTION+" text)";
		db.execSQL(q);

		// Add AD1 and AD2 entries
		insertDefaults(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		Log.i(TAG,"Upgrade database");
		
		Log.i(TAG,"Drop Tables");
		db.execSQL("DROP TABLE IF EXISTS "+TBL_CHANNEL_CONFIG);
		
		onCreate(db);
	}		

	//public Channel getChannel(int id)
	public void getChannel(int id)
	{
		
		SQLiteDatabase db=this.getReadableDatabase();
		String [] columns=new String[]{COL_ID,COL_NAME,COL_DESCRIPTION};
		Cursor c=db.query(TBL_CHANNEL_CONFIG, columns, COL_ID+"="+id,null,null,null,null );
		c.moveToFirst();
		
		String name = c.getString(c.getColumnIndex(COL_NAME));
		Log.i(TAG,"Fetched channel '"+name+"' from database");
		
		
	}
	
//	public void createDatabase()
//	{
//		Log.i(TAG,"Try to create the database");
//		this.getReadableDatabase();
//	}
	
	public void insertDefaults(SQLiteDatabase db)
	{
		Log.i(TAG,"Insert Default Values");
		
		ContentValues cv=new ContentValues();
		cv.put(COL_ID, 0);
		cv.put(COL_NAME, "AD1");
		cv.put(COL_DESCRIPTION, "Analog channel one");
		
		cv.put(COL_ID, 1);
		cv.put(COL_NAME, "AD2");
		cv.put(COL_DESCRIPTION, "Analog channel two");
		
		db.insert(TBL_CHANNEL_CONFIG, COL_ID, cv);
		db.close();
	}
}
