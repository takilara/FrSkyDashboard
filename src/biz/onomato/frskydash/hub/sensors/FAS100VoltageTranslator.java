package biz.onomato.frskydash.hub.sensors;

import biz.onomato.frskydash.hub.FrSkyHub;
import biz.onomato.frskydash.hub.SensorTypes;

public class FAS100VoltageTranslator implements UserDataTranslator {

	/**
	 * combined value
	 */
	private double fas100voltage = 0.0;

	@Override
	public double translateValue(SensorTypes type, int[] frame) {
		switch (type) {
		case fas100_voltage_before:
			fas100voltage = FrSkyHub.getAfter(fas100voltage)
					+ FrSkyHub.getSignedLE16BitValue(frame);
			return fas100voltage;
		case fas100_voltage_after:
			fas100voltage = FrSkyHub.getBefore(fas100voltage)
					+ FrSkyHub.convertToAfter(FrSkyHub
							.getUnsignedLE16BitValue(frame));
			return fas100voltage;
		}
		return fas100voltage;
	}

}
