/*
 * Copyright 2011-2013, Espen Solbu, Hans Cappelle
 * 
 * This file is part of FrSky Dashboard.
 *
 *  FrSky Dashboard is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FrSky Dashboard is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FrSky Dashboard.  If not, see <http://www.gnu.org/licenses/>.
 */

package biz.onomato.frskydash.domain;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import android.content.IntentFilter;
import android.graphics.Color;
import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.MyStack;
import biz.onomato.frskydash.util.Logger;

import com.google.gson.annotations.Expose;

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

	private static final int DEFAULT_DIRTY_ID = -1;

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
	 * a default value for the channel description. If this value is set on
	 * insert at db we know we are not dealing with user data
	 */
	public static final String DEFAULT_CHANNEL_DESCRIPTION = "New Channel";
	
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
	
	private int mTextColor = Color.WHITE;

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
	
	@Expose
	private String _description;
	
	@Expose
	private int _sourceChannelId;
	
	//private Channel _sourceChannel = null;
	
	@Expose
	private float _offset;
	
	@Expose
	private float _factor;
	
	@Expose
	private int _precision;
	
	private NumberFormat decFormat;

	private int _textViewId = DEFAULT_DIRTY_ID;

	//private IntentFilter mIntentFilter;
	private IntentFilter mIntentFilterCommands;

	@Expose
	private String _shortUnit;
	
	@Expose
	private String _longUnit;
	
	@Expose
	private int _movingAverage;
	
	public boolean _silent;
	
	public Date timestamp;
	
	private Alarm mAlarm; //FIXME: Replace with alarmlist

	// public Alarm[] alarms;
	// public int alarmCount = 0;
	
	/**
	 * identifier for the model this channel belongs to 
	 */
	private int _modelId = DEFAULT_DIRTY_ID;
	
	/**
	 * Identifier for this channel
	 */
	private int _channelId = DEFAULT_DIRTY_ID;

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

	/**
	 * Def ctor, this will create a channel with some default description and
	 * values. Note that the default channel description wil be updated on
	 * saving
	 */
	public Channel() {
		this(DEFAULT_CHANNEL_DESCRIPTION, (float) 0, (float) 1, "Symbol", "UnitName");
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
		_sourceChannelId = DEFAULT_DIRTY_ID;
		rounder = 1;
		_silent = false;
		_precision = 2;

		// _name = name;
		_description = description;
		_offset = offset;
		_factor = factor;
		_shortUnit = unit;
		_longUnit = longUnit;

		setMovingAverage(0);

		setDirtyFlag(true);

		reset();
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

	/**
	 * wrapper to set int values (will just forward after casting to double
	 * 
	 * @param value
	 * @return
	 */
	public double setRaw(int value) {
		return setRaw((double) value);
	}

	/**
	 * set new raw value for this channel. This will update stack for average
	 * and other calculations and also update all of the derived channels so
	 * that on next gui update these new values are fetched instead.
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

		if (_channelId != DEFAULT_DIRTY_ID) {
			// Interface based communication
			updateDerivedChannels();
			
			// Broadcast based communication NO MORE IN USE
			//broadcastUpdate();
		}
		
		// Update alarms
		if(mAlarm!=null)
		{
			testAlarms();
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
	 *            pass true if you want to retrieve the average calculated value
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
		// FIXED don't update this as long as there is no adapter since this will be used to display in lists
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
		// DONE DecimalFormat is faster
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
		// DONE DecimalFormat is faster
		//return String.format("%." + _precision + "f", convert(inputValue));
		return decFormat.format(convert(inputValue));
		
	}

	// ==========================================================================================
	// ==== INTER CHANNEL COMMUNICATION =====
	// ==========================================================================================


	
	/**
	 * Array to hold derived Channels,
	 * Note, this was made an Array instead of arrayList, as iterating needs high performance
	 */
	private Channel[] mDerivedChannelsA = new Channel[0];
	private ArrayList<Channel> mDerivedChannelsL = new ArrayList<Channel>();
	//private boolean mUseList=true;
	
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
		if(_sourceChannelId!=DEFAULT_DIRTY_ID)
		{
			//Logger.d(TAG,_description+": Try to drop me from channel with id: "+_sourceChannelId);
			if(getSourceChannel()!=null) {	
				getSourceChannel().dropDerivedChannel(this);
			}
			//FrSkyServer.getChannel(_sourceChannelId).dropDerivedChannel(this);
		}
		if(channel!=null)	// Null if not found
		{
			_sourceChannelId = channel.getId();
			if(channel.getId()!=-1)	// -1 if source set to "None" 
			{
				channel.addDerivedChannel(this);
			}
		}
		else
		{
			_sourceChannelId = DEFAULT_DIRTY_ID;
		}
	}

	/**
	 * @deprecated {@link #setSourceChannel(Channel)} preferred
	 * @param channelId
	 */
	public void setSourceChannelId(int channelId) {
		_sourceChannelId = channelId;
	}
	
	public Channel getSourceChannel()
	{
		if(_sourceChannelId!=DEFAULT_DIRTY_ID)
		{
			if(FrSkyServer.modelMap.get(_modelId).getChannels().containsKey(_sourceChannelId))
			{
				// Check if sourcechannel exist in our own models channels
				Logger.d(TAG, "Channel is in model");
				return FrSkyServer.modelMap.get(_modelId).getChannels().get(_sourceChannelId);
			}
			
			if(FrSkyServer.modelMap.get(_modelId).getHub()!=null)
			{
				if(FrSkyServer.modelMap.get(_modelId).getHub().getChannels().containsKey(_sourceChannelId))
				{
					// If not, check currentmodel's hub channels
					Logger.d(TAG, "Channel is in model's hub");
					return FrSkyServer.modelMap.get(_modelId).getHub().getChannels().get(_sourceChannelId);
				}
			}
			if(FrSkyServer.getSourceChannels().containsKey(_sourceChannelId))
			{
				// Last resort, check server channels
				Logger.d(TAG, "Channel is in server");
				return FrSkyServer.getChannel(_sourceChannelId);
			}
			
			
			Logger.e(TAG, "Unable to find channel with id: "+_sourceChannelId);
			Logger.e(TAG, "The requesting channel is: "+toString());
			Logger.e(TAG, "Tested towards channels in model: "+_modelId);
			return null;
			
			
		}
		else
		{
			return null;
		}
	}

	
	/**
	 * Used to enable reception of updates from source channel
	 */
	public void registerListenerForChannelUpdates() {
		if(_sourceChannelId!=DEFAULT_DIRTY_ID) 	// Used to listen to a channel
		{
			Logger.d(TAG, _description + " Registering listener, sourceId: "+_sourceChannelId);
			if(listening)
			{
				//FrSkyServer.getChannel(_sourceChannelId).dropDerivedChannel(this);
			}
			
			if(getSourceChannel()!=null)	// just in case the system is unable to find the channel
			{
				getSourceChannel().addDerivedChannel(this);
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
		if(_sourceChannelId!=DEFAULT_DIRTY_ID)
		{
			Logger.d(TAG, "Stopped listening");
			//FrSkyServer.getChannel(_sourceChannelId).dropDerivedChannel(this);
			getSourceChannel().dropDerivedChannel(this);
		}
	}



	/**
	 * Used to prepare Channel for deletion
	 */
	public void close() {
		// remove the channel update messages
		Logger.i(TAG, "Try to close Channel "+this.toString());
		if(_sourceChannelId!=DEFAULT_DIRTY_ID)
		{
			//FrSkyServer.getChannel(_sourceChannelId).dropDerivedChannel(this);
			getSourceChannel().dropDerivedChannel(this);
		}
		unregisterListenerForChannelUpdates();
//		unregisterListenerForServerCommands();
		_closed = true;
	}




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
		// DONE DecimalFormat is faster
		// 20120702 eso: decFormat uses "." regardless of Localization, this seem to cause problems with localized phones, reverting to 
		// use String.format as performance requirement is not high regarding this method
		return getDescription() + ": "
				//+ decFormat.format(_val)
				+ String.format(Locale.US, "%." + _precision + "f", _val)
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
		_raw = DEFAULT_DIRTY_ID;
		_val = DEFAULT_DIRTY_ID;
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
		if (lhs.getId() == rhs.getId())
			return 0;
		if (lhs.getId() < rhs.getId())
			return DEFAULT_DIRTY_ID;
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
	
	public boolean addAlarm(Alarm alarm)
	{
		mAlarm = alarm;
		return true;
	}
	
	public void testAlarms()
	{
		//Logger.w(TAG, "Testing the alarms for channel "+_description);
		//Logger.w(TAG, "Alarm Level = "+mAlarm.analyze());
		switch (mAlarm.analyze())
		{
			case Alarm.ALARM_LEVEL_CRITICAL:
				mTextColor = Color.rgb(255, 0, 0);
				break;
			case Alarm.ALARM_LEVEL_MEDIUM:
				mTextColor = Color.rgb(255, 113, 0);
				break;
			case Alarm.ALARM_LEVEL_NORMAL:
			default:
				mTextColor = Color.GREEN;
				break;
		}
	}
	
	public int getColor()
	{
		return mTextColor;
	}
}
