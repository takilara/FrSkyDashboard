package biz.onomato.frskydash.hub.sensors;

import biz.onomato.frskydash.hub.FrSkyHub;
import biz.onomato.frskydash.hub.SensorTypes;

public class TempTranslator implements UserDataTranslator {

	@Override
	public double translateValue(SensorTypes type, int[] frame) {
		return FrSkyHub.getSignedLE16BitValue(frame);
	}

}
