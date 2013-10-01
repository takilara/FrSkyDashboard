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

import java.util.Arrays;

import biz.onomato.frskydash.hub.FrSkyHub;
import biz.onomato.frskydash.hub.SensorTypes;
import biz.onomato.frskydash.util.Logger;

public class OpenXVarioTranslator implements UserDataTranslator {

	private static final String TAG = "OpenXVarioTranslator";
	private double PRECISION_FVAS_VOLTAGE = 10.0;
	
	/**
	 * combined value
	 */
	private double voltage = 0.0;
	private double gps_distance,gps_distance_tmp = 0.0;

	@Override
	public double translateValue(SensorTypes type, int[] frame) {
		//Logger.d(TAG,"Translate Frame: "+Arrays.toString(frame));
		//Logger.d(TAG,"fas100voltage at this time: "+fas100voltage);
		switch (type) {
		case openxvario_vfas_voltage:
			voltage = FrSkyHub.getSignedLE16BitValue(frame)/PRECISION_FVAS_VOLTAGE; // representing mV
			return voltage;
		case openxvario_gps_distance:
			gps_distance = FrSkyHub.getSignedLE16BitValue(frame); // Precision is "unknown" as this changes depending on value....
			return gps_distance;	
			
		}
		return FrSkyHub.UNDEFINED_VALUE;
	}

}
