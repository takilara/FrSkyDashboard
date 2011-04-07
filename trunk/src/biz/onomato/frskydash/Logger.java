package biz.onomato.frskydash;

import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;

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
	private final Calendar time = Calendar.getInstance();
	 
	
	public Logger(boolean LogRaw,boolean LogCsv,boolean LogHuman)
	{
		_logRaw = LogRaw;
		_logCsv = LogCsv;
		_logHuman = LogHuman;
		_path = "/Android/data/biz.onomato.frskydash/files/log/";
		Date myDate = new Date();
		Time myTime = new Time();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HH:mm");
		_prefix = formatter.format(myDate);  

		_logCsv = false; // not yet implemented
		
		openFiles();
	}
	
	public void setPrefix(String Prefix)
	{
		_prefix = Prefix;
	}

	private void openFiles()
	{
		
		if(_logCsv)	openCsvFile(_path+_prefix);
		if(_logRaw)	openRawFile(_path+_prefix);
		if(_logHuman) openHumanFile(_path+_prefix);
	}
	
	private void openCsvFile(String filename)
	{
		String extension = "CSV";
		String fName = filename+"."+extension;
		Log.i(TAG,"Open "+fName+" for writing");
	}
	private void openRawFile(String filename)
	{
		String extension = "RAW";
		String fName = filename+"."+extension;
		Log.i(TAG,"Open "+fName+" for writing");
	}
	private void openHumanFile(String filename)
	{
		String extension = "ASC";
		String fName = filename+"."+extension;
		Log.i(TAG,"Open "+fName+" for writing");
	}
	
	public void log(Frame f)
	{
		if(_logCsv)	writeCsv(f);
		if(_logRaw)	writeRaw(f);
		if(_logHuman) writeHuman(f);
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
	}
	
}
