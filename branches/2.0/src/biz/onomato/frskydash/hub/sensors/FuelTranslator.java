package biz.onomato.frskydash.hub.sensors;

import biz.onomato.frskydash.hub.FrSkyHub;
import biz.onomato.frskydash.hub.SensorTypes;

public class FuelTranslator implements UserDataTranslator {

	@Override
	public double translateValue(SensorTypes type, int[] frame) {
		return FrSkyHub.getUnsignedLE16BitValue(frame);
	}

}
