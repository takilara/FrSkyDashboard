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
import biz.onomato.frskydash.hub.Hub;
import biz.onomato.frskydash.hub.SensorTypes;
import biz.onomato.frskydash.util.Logger;

public class FASTranslator implements UserDataTranslator {

	private static final String TAG = "FASVoltageTranslator";
	/**
	 * combined value
	 */
	private double voltage = 0.0;
	private double volt_bp = 0.0;
	private double volt_ap = 0;
	private double after_dec = 0.0;
	private double PRECISION_VOLTAGE = 10.0;
	private double PRECISION_CURRENT = 10.0;

	@Override
	public double translateValue(SensorTypes type, int[] frame) {
		//Logger.d(TAG,"Translate Frame: "+Arrays.toString(frame));
		//Logger.d(TAG,"fas100voltage at this time: "+fas100voltage);
		switch (type) {
		case fas_voltage_before:
			volt_bp = FrSkyHub.getUnsignedLE16BitValue(frame);
			break;
			
		case fas_voltage_after:
			volt_ap = FrSkyHub.getUnsignedLE16BitValue(frame)/PRECISION_VOLTAGE;
			voltage = (volt_bp+volt_ap)* (21.0/11.0); 
			return voltage;
		
		case fas_current:
			return FrSkyHub.getUnsignedLE16BitValue(frame)/PRECISION_CURRENT;
		default:
			break;
		}
		return Hub.UNDEFINED_VALUE;
	}

}
