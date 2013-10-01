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

package biz.onomato.frskydash.hub.sensors;

import biz.onomato.frskydash.hub.SensorTypes;

/**
 * Interface as a contract to all User data translation logic. All sensor (and
 * in the future custom user data) should translate their values to human
 * readable formats using this interface.
 * 
 * @author hcpl
 * 
 */
public interface UserDataTranslator {

	/**
	 * retrieve the user data as a double based on what is available in the
	 * frame.
	 * 
	 * TODO see if we can use Frame object instead
	 * 
	 * @param frame
	 * @return
	 */
	public double translateValue(SensorTypes type, int[] frame);

}
