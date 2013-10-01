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

package biz.onomato.frskydash.hub;

import java.util.HashMap;
import java.util.Map;

import android.util.Log;

public class HubFactory {

	private static HubFactory instance;

	private Map<String, String> mapping = new HashMap<String, String>();

	private Map<String, String> inverse = new HashMap<String, String>();

	{
		// init the mapping collection
		mapping.put("None", null);
		mapping.put("FrSkyHub", "biz.onomato.frskydash.hub.FrSkyHub");
		// and create inverse
		for (String key : mapping.keySet())
			inverse.put(mapping.get(key), key);
	}

	private HubFactory() {
	}

	public static HubFactory getInstance() {
		if (instance == null)
			instance = new HubFactory();
		return instance;
	}

	/**
	 * this will return the class name for a hub based on the name of the hub.
	 * Null is covered.
	 * 
	 * @param hubName
	 * @return
	 */
	public String getHubClassFromName(String hubName) {
		return hubName == null ? null : mapping.get(hubName);
	}

	/**
	 * this will return the hub name for a hub based on the class name of that
	 * hub. Null is covered
	 * 
	 * @param className
	 * @return
	 */
	public String getHubNameFromClass(String className) {
		return className == null ? "None" : inverse.get(className);
	}

	/**
	 * get a list of
	 * 
	 * @return
	 */
	public String[] getHubNames() {
		return (String[]) mapping.keySet().toArray(new String[mapping.size()]);
	}

	/**
	 * init a hub based on the class name
	 * 
	 * @param className
	 * @return
	 */
	public Hub initHubFromClassName(String className) {
		try {
			return ((Hub) Class.forName(className).newInstance())
					.initializeChannels();
		} catch (Exception e) {
			// and log this info
			Log.e(getClass().getName(), "Create hub from class " + className + " failed"); //, e);
			// reset to null
			return null;
		}
	}
}
