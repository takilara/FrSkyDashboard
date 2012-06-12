package biz.onomato.frskydash.domain;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.MyStack;
import biz.onomato.frskydash.util.Logger;

/**
 * A Channel is a value that can be updated through the Telemetry protocol. The
 * intention is to use this in a generic way for both Analog values (AD1 and
 * AD2), signal values (RSSI) and hub data.<br>
 * <br>
 * Channels support averaging, and scaling
 * <ul>
 * <li>Term "raw" is used for incoming values
 * <li>Term "value" is used for calculated values (values calculated to
 * engineering units)
 * </ul>
 * 
 * @see setRaw setRaw() to update a channel
 * @see getRaw getRaw() to get the incoming value (averaged or not)
 * @see getValue getValue() to get the calculated value (average or not)
 * 
 * @author Espen Solbu
 * 
 */
public class Channel implements Comparator<Channel> {

	/**
	 * tag for debug messages
	 */
	private static final String TAG = "Channel";

	// TODO change towards enum, also I saw this before in the Model class for
	// instance
	public static final int CHANNELTYPE_AD1 = 0;
	public static final int CHANNELTYPE_AD2 = 1;
	public static final int CHANNELTYPE_RSSI = 2;
	
	

	/**
	 * Broadcast message used for capturing and sending value updates
	 */
	public static final String MESSAGE_CHANNEL_UPDATED = "biz.onomato.frskydash.update.channel.";

	// TODO: Refactor to use Locale.US's delimiter
	public static final String delim = ";";

	/**
	 * indicates if this channel is currently listening or not. This is an
	 * important property since this will be used to unregister in case of
	 * channel updates etc.
	 */
	private boolean listening = false;

	private boolean _closed = false;

	// /**
	// * the actual values
	// */
	// public double raw,rawAvg;
	// public double eng,engAvg;
	//

	/**
	 * Holds the last incoming raw value
	 */
	private double _raw;

	/**
	 * Holds the average of the incoming values
	 */
	private double _avg;

	/**
	 * Holds the average outgoing converted value
	 */
	private double _val;

	public double rounder;
	private String _description;
	private int _sourceChannelId;
	private float _offset;
	private float _factor;
	private int _precision;
	private NumberFormat decFormat;
	// private Context _context;
	private int _textViewId = -1;

	//private IntentFilter mIntentFilter;
	private IntentFilter mIntentFilterCommands;

	private String _shortUnit;
	private String _longUnit;
	private int _movingAverage;
	public boolean _silent;
	public Date timestamp;

	// public Alarm[] alarms;
	// public int alarmCount = 0;
	/**
	 * identifiers
	 */
	private int _modelId = -1;
	private int _channelId = -1;

	/**
	 * if this object is dirty or not
	 */
	private boolean _dirty = false;

	// private static DBAdapterChannel db;

	// private ArrayList <OnChannelListener> _listeners;

	// public static final Channel AD1 = new
	// Channel("ad1","FrSky AD1",(float)0,(float)1,"","");
	// public static final Channel AD2 = new
	// Channel("ad2","FrSky AD2",(float)0,(float)1,"","");

	/**
	 * keep a stack of values for moving average etc.
	 */
	private MyStack _stack;

	// SharedPreferences _settings;
	// SharedPreferences.Editor editor;

	/**
	 * Def ctor
	 */
	public Channel() {
		this("description", (float) 0, (float) 1, "Symbol", "UnitName");
	}

	/**
	 * ctor using the most common properties for a channel object
	 * 
	 * @param description
	 * @param offset
	 * @param factor
	 * @param unit
	 * @param longUnit
	 */
	public Channel(String description, float offset, float factor, String unit,
			String longUnit) {
		// instanciate listeners list
		// _listeners = new ArrayList<OnChannelListener> ();
		_sourceChannelId = -1;
		rounder = 1;
		_silent = false;
		_precision = 2;

		// _name = name;
		_description = description;
		_offset = offset;
		_factor = factor;
		_shortUnit = unit;
		_longUnit = longUnit;
		// _mc = new MathContext(2);
		// _context = context;
		// _context = FrSkyServer.getContext();
		setMovingAverage(0);

		setDirtyFlag(true);

		// _movingAverage = 10;
		// _raw=-1;
		// _val=-1;
		// _avg=0;
		// _stack = new MyStack(10);
		//
		reset();

		// FRSKY channels only for now
		// alarms = new Alarm[2];

		// db = new DBAdapterChannel(context);

		// TODO: listen for FrSkyServer.BROADCAST_CHANNEL_COMMAND_RESET_CHANNELS
		mIntentFilterCommands = new IntentFilter();
		mIntentFilterCommands
				.addAction(FrSkyServer.BROADCAST_CHANNEL_COMMAND_RESET_CHANNELS);
	}

	// ==========================================================================================
	// ==== PROPERTIES =====
	// ==========================================================================================

	public void setTextViewId(int id) {
		_textViewId = id;
	}

	public int getTextViewId() {
		return _textViewId;
	}

	public void setDirtyFlag(boolean dirty) {
		_dirty = dirty;
	}

	public boolean getDirtyFlag() {
		return _dirty;
	}

	public void setId(int channelId) {
		_channelId = channelId;
		setDirtyFlag(true);
	}

	public int getId() {
		return _channelId;
	}

	public void setModelId(Model model) {
		_modelId = model.getId();
		setDirtyFlag(true);
	}

	public void setModelId(int modelId) {
		_modelId = modelId;
		setDirtyFlag(true);
	}

	public int getModelId() {
		return _modelId;
	}

	public int getSourceChannelId() {
		return _sourceChannelId;
	}

	public int getMovingAverage() {
		return _movingAverage;
	}

	public void setMovingAverage(int Size) {
		if (Size < 1) {
			Size = 1;
		}
		_stack = new MyStack(Size);
		_movingAverage = Size;
		setDirtyFlag(true);
	}

	public String getDescription() {
		return _description;
	}

	public void setDescription(String d) {
		_description = d;
		setDirtyFlag(true);
	}


	public String getLongUnit() {
		if (_longUnit == null) {
			return "";
		} else {
			return _longUnit;
		}
	}

	public void setLongUnit(String unit) {
		_longUnit = unit;
		setDirtyFlag(true);
	}

	public String getShortUnit() {
		return _shortUnit;
	}

	public void setShortUnit(String unit) {
		_shortUnit = unit;
		setDirtyFlag(true);
	}

	public int getPrecision() {
		return _precision;
	}

	public void setPrecision(int precision) {
		String df = "0";
		if (precision > 0) {
			df = df + ".";
			_precision = precision;
			for(int n=0;n<_precision;n++)
			{
				df = df+"0";
			}
		} else {
			_precision = 0;
		}
		rounder = 1;
		for (int i = 0; i < _precision; i++) {
			rounder = rounder * 10;
		}
		
		
		decFormat = new DecimalFormat(df);
		setDirtyFlag(true);
	}

	public float getOffset() {
		return _offset;
	}

	public void setOffset(float o) {
		_offset = o;
		setDirtyFlag(true);
	}

	public float getFactor() {
		return _factor;
	}

	public void setFactor(float f) {
		_factor = f;
		setDirtyFlag(true);
	}

	public boolean getSpeechEnabled() {
		return !_silent;
	}

	public boolean getSilent() {
		return _silent;
	}

	public void setSilent(boolean setSilent) {
		_silent = setSilent;
		setDirtyFlag(true);
	}

	public void setSpeechEnabled(boolean speech) {
		_silent = !speech;
		setDirtyFlag(true);
	}


	// ==========================================================================================
	// ==== CHANNEL METHODS =====
	// ==========================================================================================

	public double setRaw(int value) {
		return setRaw((double) value);
	}

	/**
	 * set new raw value for this channel. This will update stack for average
	 * and other calculations and also send a broadcast.
	 * 
	 * @param value
	 * @return
	 */
	public double setRaw(double value) {
		timestamp = new Date();
		_avg = _stack.push(value);

		_raw = value;
		_val = convert(_avg);

		// // Update public properties
		// raw = _raw;
		// rawAvg = _avg;
		// eng = convert(_raw);
		// engAvg = _val;

		if (_channelId != -1) {
			// Interface based communication
			updateDerivedChannels();
			
			// Broadcast based communication
			//broadcastUpdate();
		}
		return _val;
	}

	
	/**
	 * Retrieve the last calculated value (not an average).
	 * 
	 * @return
	 */
	public double getValue() {
		return getValue(false);
	}

	/**
	 * Retrieve the calculated value for this channel
	 * 
	 * @param average
	 *            pass true if you want to retrieve the average raw value
	 * @return
	 */
	public double getValue(boolean average) {
		// tVal = Math.round(_val*100f)/100f;
		return average ? convert(_avg) : convert(_raw);
	}

	/**
	 * Retrieve the last raw value for this channel (not an average)
	 * 
	 * @return
	 */
	public double getRaw() {
		return getRaw(false);
	}

	/**
	 * Retrieve the raw value for this channel
	 * 
	 * @param average
	 *            pass true if you want to retrieve the average value
	 * @return
	 */
	public double getRaw(boolean average) {
		return average ? _avg : _raw;
	}

	/**
	 * 
	 * @return The description of the Channel
	 * 
	 *         eso: This is used when the channel are added to adapters for the
	 *         spinners
	 */
	@Override
	public String toString() {
		return getDescription();
		// return "Channel [_raw=" + _raw + ", _val=" + _val + ", _avg=" + _avg
		// + ", _description=" + _description + ", _movingAverage="
		// + _movingAverage + ", _modelId=" + _modelId + ", _channelId="
		// + _channelId + "]";
	}

	
	
	/**
	 * Used to return the calculated value (average) as a nicely formatted
	 * string
	 * 
	 * @return the formatted string
	 */
	public String toValueString() {
		// TODO DecimalFormat is probably faster
		//return String.format("%." + _precision + "f", _val);
		return decFormat.format(_val);
	}

	/**
	 * Used to return a nicely formatted string for an arbitrary input value
	 * 
	 * @param inputValue
	 *            a raw value you want to retrieve the string for
	 * @return the formatted string
	 */
	public String toValueString(int inputValue) {
		// TODO DecimalFormat is probably faster
		//return String.format("%." + _precision + "f", convert(inputValue));
		return decFormat.format(convert(inputValue));
		
	}

	// ==========================================================================================
	// ==== INTER CHANNEL COMMUNICATION =====
	// ==========================================================================================

	/**
	 * Tell this channel to get its updates from another channel
	 * @param channel the channel to receive updates from
	 */
//	public void listenTo(Channel channel)
//	{
//		if(_sourceChannelId!=-1) 	// Used to listen to a channel
//		{
//			FrSkyServer.getChannel(_sourceChannelId).dropDerivedChannel(this);
//		}
//		
//		if(channel!=null) // only listen to new channel if not null
//		{
//			// channel already listening on a channel
//			channel.addDerivedChannel(this);
//		}
//	}
	
	/**
	 * Array to hold derived Channels,
	 * Note, this was made an Array instead of arrayList, as iterating needs high performance
	 */
	private Channel[] mDerivedChannelsA = new Channel[0];
	private ArrayList<Channel> mDerivedChannelsL = new ArrayList<Channel>();
	private boolean mUseList=true;
	
	/**
	 * Add a channel that wants to get updates from this channel
	 * 
	 * @param channel
	 */
	public void addDerivedChannel(Channel channel)
	{
		if(!mDerivedChannelsL.contains(channel))
		{
			mDerivedChannelsL.add(channel);
		}
		mDerivedChannelsA = mDerivedChannelsL.toArray(new Channel[mDerivedChannelsL.size()]);
	}
	
	/**
	 * Drop a channel from the update list
	 * 
	 * @param channel
	 */
	public void dropDerivedChannel(Channel channel)
	{
		
		mDerivedChannelsL.remove(channel);
		mDerivedChannelsA = mDerivedChannelsL.toArray(new Channel[mDerivedChannelsL.size()]);
		
	}
	
	/**
	 * Update all the derived Channels
	 */
	private void updateDerivedChannels()
	{
		for(Channel c : mDerivedChannelsA)
		{
			c.setRaw(_val);
		}
	}
	
	
	

	
	

	/**
	 * Set the channel this channel listens to
	 * @param channel source channel
	 */
	public void setSourceChannel(Channel channel) {
		if(_sourceChannelId!=-1)
		{
			Logger.w(TAG,_description+": Try to drop me from channel with id: "+_sourceChannelId);
			FrSkyServer.getChannel(_sourceChannelId).dropDerivedChannel(this);
		}
		if(channel!=null)
		{
			_sourceChannelId = channel.getId();
			channel.addDerivedChannel(this);
		}
		else
		{
			_sourceChannelId = -1;
		}
	}

	/**
	 * @deprecated {@link #setSourceChannel(Channel)} preferred
	 * @param channelId
	 */
	public void setSourceChannel(int channelId) {
		setSourceChannel(FrSkyServer.getChannel(channelId));
	}

	// broadcast based communication
	/**
	 * Used to receive commands from Server
	 */
	public void registerListenerForServerCommands() {
		FrSkyServer.getContext().registerReceiver(mCommandReceiver,
				mIntentFilterCommands); // Used to receive messages from Server
		Logger.d(TAG, "Channel "+this.toString()+" registered for commands. Channel Object ID: "+this.hashCode());
	}
	
	/**
	 * Used to enable reception of updates from source channel
	 */
	public void registerListenerForChannelUpdates() {
		if(_sourceChannelId!=-1) 	// Used to listen to a channel
		{
			Logger.d(TAG, _description + " Registering listener");
			if(listening)
			{
				//FrSkyServer.getChannel(_sourceChannelId).dropDerivedChannel(this);
			}
		
			try
			{
				FrSkyServer.getChannel(_sourceChannelId).addDerivedChannel(this);
				listening = true;
			}
			catch (Exception e)
			{
				
			}
			
		}
		else
		{
			unregisterListenerForChannelUpdates();
		}
		setDirtyFlag(true);
	}


	/**
	 * Used to unregister self from updates from the source channel
	 */
	public void unregisterListenerForChannelUpdates() {
		if(_sourceChannelId!=-1)
		{
			Logger.d(TAG, "Stopped listening");
			FrSkyServer.getChannel(_sourceChannelId).dropDerivedChannel(this);
		}
	}

	public void unregisterListenerForServerCommands() {
		Logger.d(TAG, _description + ": Removing Server Commands broadcast listener");
		try {
			FrSkyServer.getContext().unregisterReceiver(mCommandReceiver);
			Logger.d(TAG, "Channel "+this.toString()+" unregistered for commands. Channel Object ID: "+this.hashCode());
		} catch (Exception e) {
			Logger.e(TAG, "Channel "+this.toString()+" unregistered for commands FAILED. Channel Object ID: "+this.hashCode());
		}
	}


	/**
	 * Used to prepare Channel for deletion
	 */
	public void close() {
		// remove the channel update messages
		Logger.i(TAG, "Try to close Channel "+this.toString());
		if(_sourceChannelId!=-1)
		{
			FrSkyServer.getChannel(_sourceChannelId).dropDerivedChannel(this);
		}
		unregisterListenerForChannelUpdates();
		unregisterListenerForServerCommands();
		_closed = true;
	}


	private BroadcastReceiver mCommandReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO: Currently only supports reset command
			Logger.i(TAG, getDescription() + " (id: " + _channelId
					+ ", ModelId: " + _modelId + "): Received RESET broadcast");
			reset();
		}
	};


	// ==========================================================================================
	// ==== UTILITY METHODS =====
	// ==========================================================================================

	/**
	 * Used to concatenate different parameters of the channel to a string that
	 * can be used for TextToSpeech. Will convert the value of the channel to
	 * Locale.US as that is currently the hardcoded locale for the TextToSpeech
	 * engine.
	 * 
	 * @return the string to be passed to the speaker
	 * @author eso
	 */
	public String toVoiceString() {
		// DecimalFormatSymbols dsDefault = new DecimalFormatSymbols();
		// char decDefault = dsDefault.getDecimalSeparator();
		//
		// DecimalFormatSymbols dsUS = new DecimalFormatSymbols(Locale.US);
		// char decUS = dsUS.getDecimalSeparator();
		//
		// if(FrSkyServer.D)Log.d(TAG,"Local decimal symbol: '"+decDefault+"', US decimal symbol: '"+decUS+"'");
		// toValueString should perform replace of Locale's decimal point with
		// Locale.US decimal point
		// TODO DecimalFormat probably faster
		return getDescription() + ": "
				+ decFormat.format(_val)
				//+ String.format(Locale.US, "%." + _precision + "f", _val)
				+ getLongUnit();
		// return getDescription()+": "+toValueString()+" "+getLongUnit();
	}

	/**
	 * reset this channel
	 */
	public void reset() {
		// debug logging
		Logger.d(TAG, _description + ": Resetting self");
		// first update value the proper way so it is broadcasted and displayed
		// on the screen
		// setRaw(-1);
		// next manually ensure all values are reset for later use
		_raw = -1;
		_val = -1;
		_avg = 0;
		// raw = _raw;
		// rawAvg = _avg;
		// eng = _val;
		_stack = new MyStack(_movingAverage);
		//setSourceChannel(-1); // force unregister receiver
		setDirtyFlag(true);
	}

	/**
	 * Converts a raw value to engineering units, and performs "rounding" on it.
	 * 
	 * @param inputValue
	 *            the raw value to convert
	 * @return the value in engineering units
	 * @author eso
	 */
	private double convert(double inputValue) {
		double o = (inputValue * _factor) + _offset;
		// Log.d(TAG,_name+" convert from inputvalue ("+inputValue+") to outputvalue ("+o+")");
		return Math.round(o * rounder) / rounder;
	}

	// TODO: Deprecate
	// public void addListener(OnChannelListener channel)
	// {
	// _listeners.add(channel);
	// setDirtyFlag(true);
	// }

	// public String toCsv()
	// {
	// ///TODO: stop using String.format, should not be neccessary as already
	// uses convert
	//
	// StringBuilder sb = new StringBuilder();
	//
	//
	// sb.append(String.format("%."+_precision+"f",convert(_raw))+delim);
	// sb.append(String.format("%."+_precision+"f",convert(_avg))+delim);
	// return sb.toString();
	// }
	public String toCsvHeader() {
		StringBuilder sb = new StringBuilder();
		// TODO decimalFormat probably faster
		sb.append(String.format("%s (%s)", _description, _longUnit) + delim);
		sb.append(String.format("%s (Averaged) (%s)", _description, _longUnit)
				+ delim);
		return sb.toString();
	}

	// ==========================================================================================
	// ==== COMPAREABLE =====
	// ==========================================================================================

	@Override
	public int compare(Channel lhs, Channel rhs) {
		// TODO Auto-generated method stub

		if (lhs.getId() == rhs.getId())
			return 0;
		if (lhs.getId() < rhs.getId())
			return -1;
		else
			return 1;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		// Return false if the other object has the wrong type.
		// This type may be an interface depending on the interface's
		// specification.
		if (!(o instanceof Channel)) {
			return false;
		}
		Channel channel = (Channel) o;
		if (this.getId() == channel.getId()) {
			return true;
		} else {
			return false;
		}

	}
}