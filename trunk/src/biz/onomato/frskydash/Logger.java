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
import java.util.ArrayList;
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
	
	private String startDateS;
	private Date startDate;
	
	WriteRaw rawTask;
	WriteCsv csvTask;
	WriteHuman humanTask;
	
	private OutputStream _streamCsv = null;
	private OutputStream _streamRaw = null;
	private OutputStream _streamHuman = null;
	
	private ArrayList<Channel> _channelList;
	
	
	private final Calendar time = Calendar.getInstance();
	 
	
	public Logger(Context Context, boolean LogRaw,boolean LogCsv,boolean LogHuman)
	{
		_channelList = new ArrayList<Channel>();
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

		//_logCsv = false; // not yet implemented, override to false
		

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
	
	public void addChannel(Channel channel)
	{
		_channelList.add(channel);
	}
	
	public String makePrefix()
	{
		Date myDate = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmmss");
		startDate = myDate;
		startDateS = formatter.format(startDate);
		return startDateS;  
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
				///TODO: write header
				//String[] headerA = new String[13];
				
				StringBuilder sb = new StringBuilder();
				// Comments
				sb.append("// Log Started: ");
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
				sb.append(formatter.format(startDate));
				sb.append(Channel.crlf);
				sb.append("Time since Start (ms)");
				for (Channel ch : _channelList)
				{
					sb.append(Channel.delim);
					sb.append(ch.toCsvHeader());
				}
				sb.append(Channel.crlf);
				try {
					_streamCsv.write(sb.toString().getBytes());				
				}
				catch (Exception e)
				{}
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
				Log.i(TAG,_context.getExternalFilesDir(null)+ filename+".raw");	
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
				// Make sure file is there before spawning any writer threads
				if(_fileCsv==null || !_fileCsv.canWrite())
				{
						Log.d(TAG,"NOT Allowed to write to file, make new file/stream (CSV)");
						openCsvFile(makePrefix());
				}
				csvTask = new WriteCsv();
				csvTask.execute(f);
			}
			if(_logRaw){ 
				// Make sure file is there before spawning any writer threads
				if(_fileRaw==null || !_fileRaw.canWrite())
				{
						Log.d(TAG,"NOT Allowed to write to file, make new file/stream (RAW)");
						openRawFile(makePrefix());
				}
				rawTask = new WriteRaw();
				rawTask.execute(f);
			}
			if(_logHuman){
				// Make sure file is there before spawning any writer threads
				if(_fileHuman==null || !_fileHuman.canWrite())
				{
						Log.d(TAG,"NOT Allowed to write to file, make new file/stream (ASC)");
						openHumanFile(makePrefix());
				}
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
			if(_logCsv)
			{
				StringBuilder sb = new StringBuilder();
				Date nowDate = new Date();
				
				long timeElapsed;
				timeElapsed = nowDate.getTime()-startDate.getTime();
				sb.append(timeElapsed);
				for (Channel ch : _channelList)
				{
					sb.append(Channel.delim);
					sb.append(ch.toCsv());
				}
				sb.append(Channel.crlf);
				try {
					_streamCsv.write(sb.toString().getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
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
