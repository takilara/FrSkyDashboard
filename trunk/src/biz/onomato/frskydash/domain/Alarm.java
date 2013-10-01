/*
 * Copyright 2011-2013, Espen Solbu
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

/**
 * @author Espen Solbu
 *
 */
public abstract class Alarm {
	public static final int ALARM_LEVEL_NORMAL = 0;
	public static final int ALARM_LEVEL_MEDIUM = 1;
	public static final int ALARM_LEVEL_CRITICAL = 2;
	
	/**
	 * tag for debug messages
	 */
	protected static final String TAG = "Alarm";
	protected String mDescription ="";
	protected Channel mSourceChannel;
	protected int mAlarmLevel = ALARM_LEVEL_NORMAL;
	
	public Alarm(String description)
	{
		mDescription = description;
	}
	
	/**
	 * Setup the source channel
	 * @param channel Channel that this alarm will react to
	 */
	public void setSourceChannel(Channel channel)
	{
		mSourceChannel = channel;
		mSourceChannel.addAlarm(this);
	}
	/**
	 * Call this to get alarm level, must be overridden to give actual alarm
	 * @return the alarm level
	 */
	public int analyze()
	{
		mAlarmLevel = ALARM_LEVEL_NORMAL;
		return mAlarmLevel;
	}
	
	/** 
	 * Get current Alarm Level
	 * @return current Alarm level
	 */
	public int getAlarmLevel()
	{
		return mAlarmLevel;
	}
}
