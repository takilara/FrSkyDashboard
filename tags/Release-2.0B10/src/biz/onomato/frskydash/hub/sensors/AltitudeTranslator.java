package biz.onomato.frskydash.hub.sensors;

import biz.onomato.frskydash.hub.FrSkyHub;
import biz.onomato.frskydash.hub.SensorTypes;

public class AltitudeTranslator implements UserDataTranslator {

	/**
	 * combined value
	 */
	private double altitude = 0.0;

	@Override
	public double translateValue(SensorTypes type, int[] frame) {
		switch (type) {
		case altitude_before:
			altitude = FrSkyHub.getAfter(altitude)
					+ FrSkyHub.getSignedLE16BitValue(frame);
			return altitude;
		case altitude_after:
			altitude = FrSkyHub.getBefore(altitude)
					+ FrSkyHub.convertToAfter(FrSkyHub
							.getUnsignedLE16BitValue(frame));
			return altitude;
		}
		return altitude;
	}

}
