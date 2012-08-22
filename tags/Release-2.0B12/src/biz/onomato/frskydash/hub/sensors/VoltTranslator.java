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
