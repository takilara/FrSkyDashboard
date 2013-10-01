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

/**
 * 
 */
package biz.onomato.frskydash.hub;

import java.util.Collections;
import java.util.TreeMap;

import biz.onomato.frskydash.domain.Channel;
import biz.onomato.frskydash.domain.Frame;

/**
 * Used to provide (and enforce) basic hub functionality, All hub's must inherit
 * this class
 * 
 * @author eso
 * 
 */
public abstract class Hub {

	/**
	 * Translators must pass this if unable to create full value
	 */
	public final static double UNDEFINED_VALUE=-999.25;
	
	/**
	 * Treemap to hold the Hubs channels
	 */
	private TreeMap<Integer, Channel> mChannelMap;

	/**
	 * a human readable name for this hub class
	 */
	protected String name;

	/**
	 * init channels map
	 */
	private void initializeMap() {
		mChannelMap = new TreeMap<Integer, Channel>(Collections.reverseOrder());
	}

	/**
	 * Get all sourceChannels from the hub
	 * 
	 * @return a TreeMap containing all the hub's channels
	 */
	public TreeMap<Integer, Channel> getChannels() {
		if (mChannelMap == null) {
			initializeMap();
		}
		return mChannelMap;
	}

	/**
	 * Get a channels from the hub
	 * 
	 * @return a channel with the given id
	 */
	public Channel getChannel(int id) {
		return mChannelMap.get(id);
	}

	/**
	 * Method to get the ID of the hub, must be unique
	 * 
	 * @return
	 */
	public abstract int getId();

	/**
	 * Method that should take the userbyte part of a frame and "handle" it
	 * 
	 * @param ints Incoming Userbytes
	 * @param frame Frame containing the Userbytes
	 */
	public abstract void addUserBytes(int[] ints,Frame frame);

	/**
	 * Method that should take the userbyte part of a frame and "handle" it
	 * 
	 * @param ints
	 */
	public abstract void addUserBytes(byte[] bytes);

	/**
	 * A hub must initialize its channels, what channels need to be initialised
	 * depend on the actual implementation fo the hub.
	 * 
	 * @return this method will return itself so that we can chain this process
	 */
	public abstract Hub initializeChannels();

	/**
	 * Method for the hub to add channels to its channellist
	 * 
	 * @param channel
	 */
	protected void addChannel(Channel channel) {
		if (mChannelMap == null) {
			initializeMap();
		}
		mChannelMap.put(channel.getId(), channel);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String toHuman(int[] ints)
	{
		StringBuilder  buf = new StringBuilder(64);
		//for(int n=0;n<_frameRaw.length;n++)
		if(ints!=null)
		{
			for(int b:ints)
			{
				if(b<0x10)
				{
					buf.append("0");
				}
				
				buf.append(Integer.toHexString(b));
					buf.append(' ');
		
			}
		}
		return buf.toString();
	}		
}