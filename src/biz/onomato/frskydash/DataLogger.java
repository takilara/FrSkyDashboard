package biz.onomato.frskydash;

import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.content.Context;
import biz.onomato.frskydash.domain.Channel;
import biz.onomato.frskydash.domain.Frame;
import biz.onomato.frskydash.domain.Model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DataLogger {
	private static final String TAG="Logger";
	private boolean _logRaw;
	private boolean _logCsv;
	private boolean _logHuman;
	private String _prefix;
	private String _path;
	private String _headerString;
	private Context _context;

	private Handler writerHandler;
    private Runnable runnableWriter;
	
	boolean mExternalStorageAvailable = false;
	boolean mExternalStorageWriteable = false;
	private File _fileCsv = null;
	private File _fileHuman = null;
	private File _fileRaw = null;
	
	private String startDateS;
	private Date startDateCsv;		// Used for duration and metadata in CSV
	private Date startDateAsc;		// Used for duration in Human file
	private Date startDate;			// Used for filename
	private StringBuilder csvBuffer;
	private int csvBufferLen;
	private static final int CSV_BUFFER_LENGTH=30;
	public static final String crlf="\r\n";
	
	//private BlockingQueue<Frame> frameBuffer;
	private BlockingQueue<Frame> frameBufferAsc;
	private BlockingQueue<Frame> frameBufferRaw;
	private BlockingQueue<String> channelCsvBuffer;
	
	//WriteRaw rawTask;
	//WriteCsv csvTask;
	//WriteHuman humanTask;
	
	private OutputStream _streamCsv = null;
	private OutputStream _streamRaw = null;
	private OutputStream _streamHuman = null;
	
	private Model _model;
	
	private final Calendar time = Calendar.getInstance();
	 
	
	public DataLogger(Context Context, Model model, boolean LogRaw,boolean LogCsv,boolean LogHuman)
	{
		//_channelList = new ArrayList<Channel>();
		_model = model;
		_logRaw = LogRaw;
		_logCsv = LogCsv;
		_logHuman = LogHuman;
		_context = Context;
		//_path = _context.getExternalFilesDir(null); 
		if(FrSkyServer.D)Log.i(TAG,"STorage dir: "+_path);
		
		_prefix = makePrefix();  

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
			
		frameBufferAsc = new LinkedBlockingQueue <Frame>();
		frameBufferRaw = new LinkedBlockingQueue <Frame>();
		channelCsvBuffer = new LinkedBlockingQueue <String>();

		HumanLogger humanLogger = new HumanLogger(frameBufferAsc);
		new Thread(humanLogger).start();
		
		RawLogger rawLogger = new RawLogger(frameBufferRaw);
		new Thread(rawLogger).start();
		
		CsvLogger csvLogger = new CsvLogger(channelCsvBuffer);
		new Thread(csvLogger).start();
	}
	
	public void setModel(Model model)
	{
		stop();
		_model = model;
	}

	
	public String makePrefix()
	{
		Date myDate = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmmss");
		startDate = myDate;
		startDateS = formatter.format(startDate);
		return _model.getName()+"_"+startDateS;  
	}
	
	public void setPrefix(String Prefix)
	{
		_prefix = Prefix;
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
	
	//public void setCsvHeader(Channel... channels)
	public void setCsvHeader()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("\"Time since start (ms)\""+Channel.delim);
		
		
		
		int len = _model.getChannels().size();
		
		
		//for(Channel ch : channels)
		//for(int i=0;i<len;i++)
		for(Channel c : _model.getChannels().values())
		{
			
			if(!c.getLongUnit().equals(""))
			{
				sb.append("\""+c.getDescription()+" ("+c.getLongUnit()+")\""+Channel.delim);
			}
			else
			{
				sb.append("\""+c.getDescription()+"\""+Channel.delim);
			}
			//sb.append("\""+channels.get(i).getDescription()+" ("+channels.get(i).getLongUnit()+") (Avg)\""+Channel.delim);
			
		}
		_headerString = sb.toString(); 
	}
	
	
	// ALWAYS logs average, user need to add non averaged channel if he wants this..
	/**
	 * Trigger a CSV write action
	 * 
	 */

	public void logCsv()
	{
		if(mExternalStorageWriteable)
		{
			if(_logCsv){
				int len = _model.getChannels().size();
				StringBuilder sb = new StringBuilder();
				//for(int i=0;i<len;i++)
				for(Channel c : _model.getChannels().values())
				{
					// behave differently if integer vs double
					if(c.getPrecision()>0)
					{
						sb.append(c.getValue(true));
					}
					else
					{
						sb.append((int)c.getValue(true));
					}
					sb.append(Channel.delim);
				}
				sb.append(crlf);

				channelCsvBuffer.add(sb.toString());
			}
		}
	}
	
	
	/**
	 * Log the frame to raw and ascii files
	 * 
	 * @param f frame to log
	 */
	public void logFrame(Frame f)
	{
		if(mExternalStorageWriteable)
		{
			// Add frame to buffers
			if(_logRaw){ 
				frameBufferRaw.add(f);
			}
			if(_logHuman){
				frameBufferAsc.add(f);
			}
		}	
	}
	
	
	
	
	public void stop()
	{
		if(FrSkyServer.D)Log.i(TAG,"Stopping any running loggers");
		// Cancel (wait for) any pending writes
//		try {rawTask.cancel(false);} catch (Exception e){}
//		try {humanTask.cancel(false);} catch (Exception e){}
//		try {csvTask.cancel(false);} catch (Exception e){}
		
		// close any open streams
		closeStream(_streamRaw);
		closeStream(_streamHuman);
		closeStream(_streamCsv);
		
		_fileRaw = null;
		_fileHuman = null;
		_fileCsv = null;
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

	
	class HumanLogger implements Runnable 
	{
		private final BlockingQueue queue;
		private static final String TAG="HumanLogger";
		
		HumanLogger(BlockingQueue q) { queue = q; }
		public void run() 
		{
			try 
			{
				while (true) { consume((Frame) queue.take()); }
			} 
			catch (InterruptedException ex) 
			{
			}
		}
		   
		private void openFile(String filename,Date frameTimeStamp)
		{
			closeStream(_streamHuman);
			_fileHuman = null;
			if(mExternalStorageWriteable)
			{
				if(_logHuman)
				{
					_fileHuman = new File(_context.getExternalFilesDir(null), filename+".asc");
					if(!_fileHuman.exists())
						try {_fileHuman.createNewFile();} catch (IOException e1) {Log.e(TAG,e1.getMessage());}
					startDateAsc = new Date();
					// assume first frame here has first date...
					startDateAsc = frameTimeStamp; 
					_streamHuman = openStream(_fileHuman);
					
				}
			}
		}
		   
		void consume(Frame f) {
			if(_fileHuman==null || !_fileHuman.canWrite())
			{
				if(FrSkyServer.D)Log.d(TAG,"NOT Allowed to write to file, make new file/stream (ASC)");
				openFile(makePrefix(),f.timestamp);
			}
		   
			if(_fileHuman!=null && _fileHuman.canWrite())
			{
				long timeElapsed = f.timestamp.getTime()-startDateAsc.getTime();
				try 
				{
					_streamHuman.write((timeElapsed+"\t").getBytes());
					//_streamHuman.write((f.toHuman()+crlf).getBytes());
					_streamHuman.write((f.humanFrame+crlf).getBytes());
				}
				catch (IOException e)
				{
					if(FrSkyServer.D)Log.w(TAG, "failure to write");
				}
			}
		}
	}
	
	class RawLogger implements Runnable 
	{
		private final BlockingQueue queue;
		private static final String TAG="RawLogger";
		
		RawLogger(BlockingQueue q) { queue = q; }
		public void run() 
		{
			try 
			{
				while (true) { consume((Frame) queue.take()); }
			} 
			catch (InterruptedException ex) 
			{
			}
		}
		   
		private void openFile(String filename)
		{
			closeStream(_streamRaw);
			_fileRaw = null;
			if(mExternalStorageWriteable)
			{
				if(_logRaw)
				{
					_fileRaw = new File(_context.getExternalFilesDir(null), filename+".raw");
					if(!_fileRaw.exists())
						try {_fileRaw.createNewFile();} catch (IOException e1) {Log.e(TAG,e1.getMessage());}
					_streamRaw = openStream(_fileRaw);
				}
			}
		}
		   
		void consume(Frame f) {
			if(_fileRaw==null || !_fileRaw.canWrite())
			{
				if(FrSkyServer.D)Log.d(TAG,"NOT Allowed to write to file, make new file/stream");
				openFile(makePrefix());
			}
		   
			if(_fileRaw!=null && _fileRaw.canWrite())
			{
				try 
				{
					_streamRaw.write(f.toRawBytes());
				}
				catch (IOException e)
				{
					if(FrSkyServer.D)Log.w(TAG, "failure to write");
				}
			}
		}
	}
	
	
	class CsvLogger implements Runnable 
	{
		private final BlockingQueue queue;
		private static final String TAG="CsvLogger";
		
		CsvLogger(BlockingQueue q) { queue = q; }
		public void run() 
		{
			try 
			{
				while (true) { consume((String) queue.take()); }
			} 
			catch (InterruptedException ex) 
			{
			}
		}
		   
		private void openFile(String filename)
		{
			closeStream(_streamCsv);
			_fileCsv = null;
			if(mExternalStorageWriteable)
			{
				if(_logCsv)
				{
					
					_fileCsv = new File(_context.getExternalFilesDir(null), filename+".csv");
					if(!_fileCsv.exists())
						try {_fileCsv.createNewFile();} catch (IOException e1) {Log.e(TAG,e1.getMessage());}
					_streamCsv = openStream(_fileCsv);
					
					startDateCsv = new Date();
					
					///TODO: Add metadata
					StringBuilder sb = new StringBuilder();
					// Comments
					
					
					sb.append("// Model: "+_model.getName()+""+DataLogger.crlf);
					
					
					// Add Channel data:
					for(Channel c : _model.getChannels().values())
					{
						sb.append("// Channel '"+c.getDescription()+"', Averaged over "+c.getMovingAverage()+" sample(s), shown with a precision of "+c.getPrecision()+" decimals");
						sb.append(DataLogger.crlf);
					}
					
					sb.append("// Log Started: ");
					SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
					sb.append(formatter.format(startDate));
					sb.append(DataLogger.crlf);
					sb.append(DataLogger.crlf);
					//sb.append("// Model: "+model.getName()+"\""+Channel.delim);
					setCsvHeader();
					sb.append(_headerString);
					sb.append(crlf);
					try {
						_streamCsv.write(sb.toString().getBytes());				
					}
					catch (Exception e)
					{}
					
					
					///TODO: Add header
				}
			}
		}
		   
		void consume(String line) {
			if(_fileCsv==null || !_fileCsv.canWrite())
			{
				if(FrSkyServer.D)Log.d(TAG,"NOT Allowed to write to file, make new file/stream");
				openFile(makePrefix());
			}
		   
			if(_fileCsv!=null && _fileCsv.canWrite())
			{
				Date nowDate = new Date();
				
				long timeElapsed;
				timeElapsed = nowDate.getTime()-startDateCsv.getTime();
				
				try 
				{
					/// TODO: validate this..
					_streamCsv.write((timeElapsed+Channel.delim+line).getBytes());
				}
				catch (IOException e)
				{
					if(FrSkyServer.D)Log.w(TAG, "failure to write");
				}
			}
		}
	}

	/**
	 * retrieve the path where logging is done by default
	 * 
	 * @return
	 */
	public static File getLoggingPath(Context context) {
		return context.getExternalFilesDir(null);
	}
	
}
