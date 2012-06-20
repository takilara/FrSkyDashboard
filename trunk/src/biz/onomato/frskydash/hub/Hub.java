/**
 * 
 */
package biz.onomato.frskydash.hub;

import java.util.Collections;
import java.util.TreeMap;

import biz.onomato.frskydash.domain.Channel;

/**
 * Used to provide (and enforce) basic hub functionality,
 * All hub's must inherit this class
 * @author eso
 *
 */
public abstract class Hub {
	
	/**
	 * Treemap to hold the Hubs channels
	 */
	private TreeMap<Integer, Channel> mChannelMap;

	public final String TAG="Hub";

	private void initializeMap()
	{
		mChannelMap = new TreeMap<Integer, Channel>(Collections.reverseOrder());
	}
	
	/**
	 * Get all sourceChannels from the hub
	 * @return a TreeMap containing all the hub's channels
	 */
	public TreeMap<Integer, Channel> getChannels() {
		if(mChannelMap==null)
		{
			initializeMap();
		}
		return mChannelMap;
	}
	
	/**
	 * Get a channels from the hub
	 * @return a channel with the given id
	 */
	public Channel getChannel(int id) {
		return mChannelMap.get(id);
	}

	/**
	 * Method to get the ID of the hub, must be unique
	 * @return
	 */
	public abstract int getId();
	
	/**
	 * Method that should take the userbyte part of a frame and "handle" it
	 * @param ints
	 */
	public abstract void addUserBytes(int[] ints);
	
	/**
	 * Method that should take the userbyte part of a frame and "handle" it
	 * @param ints
	 */
	public abstract void addUserBytes(byte[] bytes);
	
	/**
	 * A hub must initialize its channels
	 */
	protected abstract void initializeChannels();
	
	/**
	 * Method for the hub to add channels to its channellist
	 * @param channel
	 */
	protected void addChannel(Channel channel)
	{
		if(mChannelMap==null)
		{
			initializeMap();
		}
		mChannelMap.put(channel.getId(), channel);
	}
	
}
