package biz.onomato.frskydash.hub.sensors;

import java.util.Arrays;

import biz.onomato.frskydash.hub.FrSkyHub;
import biz.onomato.frskydash.hub.SensorTypes;
import biz.onomato.frskydash.util.Logger;

public class FAS100VoltageTranslator implements UserDataTranslator {

	private static final String TAG = "FASVoltageTranslator";
	/**
	 * combined value
	 */
	private double fas100voltage = 0.0;
	private double before = 0.0;
	private int after = 0;
	private double after_dec = 0.0;

	@Override
	public double translateValue(SensorTypes type, int[] frame) {
		//Logger.d(TAG,"Translate Frame: "+Arrays.toString(frame));
		//Logger.d(TAG,"fas100voltage at this time: "+fas100voltage);
		switch (type) {
		case fas100_voltage_before:
			before = FrSkyHub.getUnsignedLE16BitValue(frame);
			fas100voltage = FrSkyHub.getAfter(fas100voltage)
					+ before;
			//Logger.d(TAG,"Got before: "+before);
			
			//Logger.d(TAG, "Combined to: "+fas100voltage);
			return fas100voltage;
		case fas100_voltage_after:
			after = FrSkyHub.getUnsignedLE16BitValue(frame);
			after_dec = FrSkyHub.convertToAfter(after);
			
			fas100voltage = FrSkyHub.getBefore(fas100voltage)
					+ after_dec;
			
			//Logger.d(TAG,"Got after: "+after);
			//Logger.d(TAG,"After shifted to: "+after_dec);
			//Logger.d(TAG, "Combined to: "+fas100voltage);
			return fas100voltage;
		}
		return fas100voltage;
	}

}
