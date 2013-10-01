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

import biz.onomato.frskydash.hub.FrSkyHub;
import biz.onomato.frskydash.hub.SensorTypes;

public class VoltTranslator implements UserDataTranslator {

//	/**
//	 * Combined value, array of cell voltages
//	 */
//	private double[] voltages = new double[6];

	@Override
	public double translateValue(SensorTypes type, int[] frame) {
//		// first 4 bit is battery cell number
//		// last 12 bit refer to voltage range 0-2100 corresponding 0-4.2V
//		int cell = FrSkyHub.getBatteryCell(frame);
//		// if (cell < 1 || cell > 6) { CHECK RANGE OF CELL
//		if (cell < 0 || cell > 5) {
//			Logger.d(this.getClass().toString(),
//					"failed to handle cell nr out of range: " + cell);
//			return 0;
//		}
//		voltages[cell] = FrSkyHub.getCellVoltage(frame);
//		return voltages;
		// TODO we will need some extra logic on the FrSkyHubSide for this...
		return FrSkyHub.getCellVoltage(frame);

	}

}
