package biz.onomato.frskydash.hub.sensors;

import biz.onomato.frskydash.hub.SensorTypes;

/**
 * Interface as a contract to all User data translation logic. All sensor (and
 * in the future custom user data) should translate their values to human
 * readable formats using this interface.
 * 
 * @author hcpl
 * 
 */
public interface UserDataTranslator {

	/**
	 * retrieve the user data as a double based on what is available in the
	 * frame.
	 * 
	 * TODO see if we can use Frame object instead
	 * 
	 * @param frame
	 * @return
	 */
	public double translateValue(SensorTypes type, int[] frame);

}
