package biz.onomato.frskydash;

import android.os.AsyncTask;
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
	
	WriteRaw rawTask;
	WriteCsv csvTask;
	WriteHuman humanTask;
	
	private OutputStream _streamCsv = null;
	private OutputStream _streamRaw = null;
	private OutputStream _streamHuman = null;
	
	
	private final Calendar time = Calendar.getInstance();
	 
	
	public Logger(Context Context, boolean LogRaw,boolean LogCsv,boolean LogHuman)
	{
		_logRaw = LogRaw;
		_logCsv = LogCsv;
		_logHuman = LogHuman;
		_context = Context;
		//_path = _context.getExternalFilesDir(null); 
		Log.i(TAG,"STorage dir: "+_path);
		//_path = "";
//		Date myDate = new Date();
//		Time myTime = new Time();
//		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmm");
		_prefix = makePrefix();  

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
	}
	
	public String makePrefix()
	{
		Date myDate = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmmss");
		return formatter.format(myDate);  
	}
	
	public void setPrefix(String Prefix)
	{
		_prefix = Prefix;
	}

	private void openFiles()
	{
		String prefix = makePrefix();
		openRawFile(prefix);
		openHumanFile(prefix);
		openCsvFile(prefix);
	}
	
	private void closeStream(OutputStream stream)
	{
		try 
		{
			stream.close();
			//stream=null;
		} 
		catch (Exception e)	{}
		stream=null;
	}
	
	private FileOutputStream openStream(File filename)
	{
		FileOutputStream tFile;
		try {
			tFile = new FileOutputStream(filename);
			return tFile;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Log.e(TAG,e.getMessage());
			return null;
		}
	}
	
	
	private void openCsvFile(String filename)
	{
		closeStream(_streamCsv);
		_fileCsv = null;
		if(mExternalStorageWriteable)
		{
			
			if(_logCsv)
			{
				_fileCsv = new File(_context.getExternalFilesDir(null), filename+".csv");
				if(!_fileCsv.exists())
					try {
						_fileCsv.createNewFile();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						Log.e(TAG,e1.getMessage());
					}
				_streamCsv = openStream(_fileCsv);
			}
		}	
	}
	
	private void openRawFile(String filename)
	{
		closeStream(_streamRaw);
		_fileRaw = null;
		
		if(mExternalStorageWriteable)
		{
			if(_logRaw)
			{
				_fileRaw = new File(_context.getExternalFilesDir(null), filename+".raw");
				if(!_fileRaw.exists())
					try {
						_fileRaw.createNewFile();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						Log.e(TAG,e1.getMessage());
					}
				_streamRaw = openStream(_fileRaw);
			}
		}
	}
	
	private void openHumanFile(String filename)
	{
		closeStream(_streamHuman);
		_fileHuman = null;
		if(mExternalStorageWriteable)
		{
			if(_logHuman)
			{
				_fileHuman = new File(_context.getExternalFilesDir(null), filename+".asc");
				if(!_fileHuman.exists())
					try {
						_fileHuman.createNewFile();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						Log.e(TAG,e1.getMessage());
					}
				_streamHuman = openStream(_fileHuman);
			}
		}
	}
	
	public void log(Frame f)
	{
		if(mExternalStorageWriteable)
		{
			if(_logCsv){
				csvTask = new WriteCsv();
				csvTask.execute(f);
			}
			if(_logRaw){ 
				rawTask = new WriteRaw();
				rawTask.execute(f);
			}
			if(_logHuman){
				humanTask = new WriteHuman();
				humanTask.execute(f);
			}
		}	
	}
	
	
	public void stop()
	{
		Log.i(TAG,"Stopping any running loggers");
		// Cancel (wait for) any pending writes
		try {rawTask.cancel(false);} catch (Exception e){}
		try {humanTask.cancel(false);} catch (Exception e){}
		try {csvTask.cancel(false);} catch (Exception e){}
		
		// close any open streams
		closeStream(_streamRaw);
		closeStream(_streamHuman);
		closeStream(_streamCsv);
		
		_fileRaw = null;
		_fileHuman = null;
		_fileCsv = null;
	}
	
	
	private class WriteRaw extends AsyncTask<Frame, Void, Integer> {
		public boolean done=false;
		protected Integer doInBackground(Frame... frames) {
			int bytes = 0;
			if(_logRaw)
			{
				if(_fileRaw==null || !_fileRaw.canWrite())
				{
						Log.d(TAG,"NOT Allowed to write to file, make new file/stream");
						openRawFile(makePrefix());
				}
				int count = frames.length;
				for(int n=0;n<count;n++)
				{
					Frame f = frames[n];
					try 
					{
						_streamRaw.write(f.toRawBytes());
						bytes += f.toRawBytes().length;
					}
					catch (IOException e)
					{
						Log.w(TAG, "failure to write");
					}
				}
			}
			done = true;
			return bytes;
		}


	     protected void onPostExecute(Integer result) {
	         //Log.i(TAG,"Written "+result+" bytes to file");
	     }
	 }
	
	private class WriteHuman extends AsyncTask<Frame, Void, Void> {
		public boolean done=false;
		protected Void doInBackground(Frame... frames) {
			if(_logHuman)
			{
				if(_fileHuman==null || !_fileHuman.canWrite())
				{
						Log.d(TAG,"NOT Allowed to write to file, make new file/stream");
						openHumanFile(makePrefix());
				}
				int count = frames.length;
				for(int n=0;n<count;n++)
				{
					Frame f = frames[n];
					try 
					{
						_streamHuman.write((System.currentTimeMillis()+"\t").getBytes());
						_streamHuman.write((f.toHuman()+"\n").getBytes());
					}
					catch (IOException e)
					{
						Log.w(TAG, "failure to write");
					}
				}
			
			}
			done = true;
			return null;
		}
	 }
	
	private class WriteCsv extends AsyncTask<Frame, Void, Void> {
		public boolean done=false;
		protected Void doInBackground(Frame... frames) {
			// not yet implemented
			done = true;
			return null;
		}
	 }
	
	public void setLogToRaw(boolean logToRaw)
	{
		_logRaw = logToRaw;
	}
	
	public void setLogToHuman(boolean logToHuman)
	{
		_logHuman = logToHuman;
	}
	
	public void setLogToCsv(boolean logToCsv)
	{
		_logCsv = logToCsv;
	}

}
