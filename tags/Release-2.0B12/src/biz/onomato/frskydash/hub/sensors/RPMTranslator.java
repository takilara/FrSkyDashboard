package biz.onomato.frskydash.hub.sensors;

import biz.onomato.frskydash.hub.FrSkyHub;
import biz.onomato.frskydash.hub.SensorTypes;

public class RPMTranslator implements UserDataTranslator {

	@Override
	public double translateValue(SensorTypes type, int[] frame) {
		// actual RPM value is Frame1*60
		// also needs to be divided by the number of blades of the prop!
		return FrSkyHub.getUnsignedLE16BitValue(frame) * 60;
	}

}
