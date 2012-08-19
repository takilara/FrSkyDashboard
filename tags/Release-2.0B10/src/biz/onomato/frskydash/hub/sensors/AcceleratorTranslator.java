package biz.onomato.frskydash.hub.sensors;

import biz.onomato.frskydash.hub.FrSkyHub;
import biz.onomato.frskydash.hub.SensorTypes;

public class AcceleratorTranslator implements UserDataTranslator {

	@Override
	public double translateValue(SensorTypes type, int[] frame) {
		// actual 3-axis value is Frame1/1000
		return FrSkyHub.getSignedLE16BitValue(frame) / 1000;
	}

}
