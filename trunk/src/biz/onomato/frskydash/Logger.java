package biz.onomato.frskydash;

import android.util.Log;

public class Logger {
	private static final String TAG="Logger";
	private boolean _logRaw;
	private boolean _logCsv;
	private boolean _logHuman;
	private String _prefix;
	 
	
	public Logger(boolean LogRaw,boolean LogCsv,boolean LogHuman)
	{
		_logRaw = LogRaw;
		_logCsv = LogCsv;
		_logHuman = LogHuman;
		_prefix ="FrSkyLog";
		_logCsv = false; // not yet implemented
	}
	
	public void setPrefix(String Prefix)
	{
		_prefix = Prefix;
	}

	private void openFiles()
	{
		
		if(_logCsv)	openCsvFile(_prefix);
		if(_logRaw)	openRawFile(_prefix);
		if(_logHuman) openHumanFile(_prefix);
	}
	
	private void openCsvFile(String filename)
	{
		String extension = "CSV";
	}
	private void openRawFile(String filename)
	{
		String extension = "RAW";
	}
	private void openHumanFile(String filename)
	{
		String extension = "ASC";	
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
