package biz.onomato.frskydash;

import android.os.Environment;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.content.Context;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Logger {
	private static final String TAG="Logger";
	private boolean _logRaw;
	private boolean _logCsv;
	private boolean _logHuman;
	private String _prefix;
	private String _path;
	private Context _context;
	boolean mExternalStorageAvailable = false;
	boolean mExternalStorageWriteable = false;
	private File _fileCsv = null;
	private File _fileHuman = null;
	private File _fileRaw = null;
	
	private OutputStream _streamCsv = null;
	private OutputStream _streamRaw = null;
	private OutputStream _streamHuman = null;
	
	
	private final Calendar time = Calendar.getInstance();
	 
	
	public Logger(Context Context, boolean LogRaw,boolean LogCsv,boolean LogHuman)
	{
		_logRaw = LogRaw;
		_logCsv = LogCsv;
		_logHuman = LogHuman;
		//_path = "/Android/data/biz.onomato.frskydash/files/log/";
		_path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Android/data/biz.onomato.frskydash/files/log/";
		Log.i(TAG,"STorage dir: "+_path);
		//_path = "";
		Date myDate = new Date();
		Time myTime = new Time();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmm");
		_prefix = formatter.format(myDate);  

		_logCsv = false; // not yet implemented
		

		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		
		if(mExternalStorageWriteable)
		{
			openFiles();
		}
		else
		{
			Log.e(TAG, "SD card not available or writeable");
		}
	}
	
	public void setPrefix(String Prefix)
	{
		_prefix = Prefix;
	}

	private void openFiles()
	{
		if(mExternalStorageWriteable)
		{
			if(_logCsv)	openCsvFile(_path+_prefix);
			if(_logRaw)	openRawFile(_path+_prefix);
			if(_logHuman) openHumanFile(_path+_prefix);
		}
	}
	
	private void openCsvFile(String filename)
	{
		String extension = "CSV";
		String fName = filename+"."+extension;
		Log.i(TAG,"Open "+fName+" for writing");
		//String path = _context.getExternalFilesDir(null).getAbsolutePath();
		//Log.i(TAG,"using path: "+path);
		//_fileCsv = new File(_context.getExternalFilesDir(null), fName);
		
		try {
			_streamCsv = new FileOutputStream(_fileCsv);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Log.e(TAG,e.getMessage());
		}
	}
	private void openRawFile(String filename)
	{
		String extension = "RAW";
		String fName = filename+"."+extension;
		Log.i(TAG,"Open "+fName+" for writing");
		
		_fileRaw = new File(_path, fName);
		if(!_fileRaw.exists())
			try {
				_fileRaw.createNewFile();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				Log.e(TAG,e1.getMessage());
			}
		Log.i(TAG,"Raw opened");
		
		try {
			_streamRaw = new FileOutputStream(_fileRaw);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Log.e(TAG,e.getMessage());
		}
		
		
	}
	private void openHumanFile(String filename)
	{
		String extension = "ASC";
		String fName = filename+"."+extension;
		_fileHuman = new File(_path, fName);
		
		if(!_fileHuman.exists())
			try {
				_fileHuman.createNewFile();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				Log.e(TAG,e1.getMessage());
			}
		
		try {
			_streamHuman = new FileOutputStream(_fileHuman);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Log.e(TAG,e.getMessage());
			
		}
		Log.i(TAG,"Open "+fName+" for writing");
	}
	
	public void log(Frame f)
	{
		if(mExternalStorageWriteable)
		{
			if(_logCsv)	writeCsv(f);
			if(_logRaw)	writeRaw(f);
			if(_logHuman) writeHuman(f);
		}	
	}
	
	private void writeCsv(Frame f)
	{
		
	}
	private void writeRaw(Frame f)
	{
		Log.i(TAG,"Log '"+f.toInts()+"' to file");
	}
	private void writeHuman(Frame f)
	{
		Log.i(TAG,"Log '"+f.toHuman()+"' to file");
		if(_fileHuman!=null)
		{
//			try 
//			{
//				_streamHuman.write(f.toHuman().getBytes());
//			}
//			catch (IOException e)
//			{
//				Log.w(TAG, "failure to write");
//			}
		}
	}
	
	public void stop()
	{
//		try
//		{
//			_streamHuman.close();
//			_streamRaw.close();
//			_streamCsv.close();
//		}
//		catch (IOException e)
//		{
//			Log.e(TAG,e.getMessage());
//		}
	}
	
}
