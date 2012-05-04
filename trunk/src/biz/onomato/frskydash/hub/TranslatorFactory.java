package biz.onomato.frskydash.hub;

import java.util.HashMap;

import biz.onomato.frskydash.hub.sensors.AcceleratorTranslator;
import biz.onomato.frskydash.hub.sensors.AltitudeTranslator;
import biz.onomato.frskydash.hub.sensors.FuelTranslator;
import biz.onomato.frskydash.hub.sensors.GPSTranslator;
import biz.onomato.frskydash.hub.sensors.RPMTranslator;
import biz.onomato.frskydash.hub.sensors.TempTranslator;
import biz.onomato.frskydash.hub.sensors.UserDataTranslator;
import biz.onomato.frskydash.hub.sensors.VoltTranslator;

/**
 * Use this factory to get the right translator based on user data frame
 * 
 * @author hcpl
 * 
 */
public class TranslatorFactory {

	/**
	 * singleton instance
	 */
	private static TranslatorFactory instance = null;

	/**
	 * collection of available data ids mapped to their sensor type
	 */
	private final HashMap<Integer, SensorTypes> dataIDs = new HashMap<Integer, SensorTypes>();

	/**
	 * collection of sensor types mapped to data translators
	 */
	private HashMap<SensorTypes, UserDataTranslator> dataTranslators = new HashMap<SensorTypes, UserDataTranslator>();

	/**
	 * singleton, use {@link #getInstance()} instead
	 */
	private TranslatorFactory() {
		// create the fixed mapping, in later releases we could also load
		// custom mappings based on user configuration
		initTranslators();
		// we also need a mapping for the data IDs
		initDataIDs();
	}

	/**
	 * retrieve singleton instance
	 * 
	 * @return
	 */
	public static TranslatorFactory getInstance() {
		if (instance == null)
			instance = new TranslatorFactory();
		return instance;
	}

	/**
	 * helper to initialise mapping of translators
	 */
	private void initTranslators() {
		GPSTranslator gpsTranslator = new GPSTranslator();
		dataTranslators.put(SensorTypes.gps_altitude_before, gpsTranslator);
		dataTranslators.put(SensorTypes.gps_altitude_after, gpsTranslator);
		dataTranslators.put(SensorTypes.temp1, new TempTranslator());
		dataTranslators.put(SensorTypes.rpm, new RPMTranslator());
		dataTranslators.put(SensorTypes.fuel, new FuelTranslator());
		dataTranslators.put(SensorTypes.temp2, new TempTranslator());
		dataTranslators.put(SensorTypes.volt, new VoltTranslator());
		AltitudeTranslator altTranslator = new AltitudeTranslator();
		dataTranslators.put(SensorTypes.altitude_before, altTranslator);
		dataTranslators.put(SensorTypes.altitude_after, altTranslator);
		dataTranslators.put(SensorTypes.gps_speed_before, gpsTranslator);
		dataTranslators.put(SensorTypes.gps_speed_after, gpsTranslator);
		dataTranslators.put(SensorTypes.longitude_before, gpsTranslator);
		dataTranslators.put(SensorTypes.longitude_after, gpsTranslator);
		dataTranslators.put(SensorTypes.ew, gpsTranslator);
		dataTranslators.put(SensorTypes.latitude_before, gpsTranslator);
		dataTranslators.put(SensorTypes.latitude_after, gpsTranslator);
		dataTranslators.put(SensorTypes.ns, gpsTranslator);
		dataTranslators.put(SensorTypes.course_before, gpsTranslator);
		dataTranslators.put(SensorTypes.course_after, gpsTranslator);
		dataTranslators.put(SensorTypes.day_month, gpsTranslator);
		dataTranslators.put(SensorTypes.year, gpsTranslator);
		dataTranslators.put(SensorTypes.hour_minute, gpsTranslator);
		dataTranslators.put(SensorTypes.second, gpsTranslator);
		AcceleratorTranslator accTranslator = new AcceleratorTranslator();
		dataTranslators.put(SensorTypes.acc_x, accTranslator);
		dataTranslators.put(SensorTypes.acc_y, accTranslator);
		dataTranslators.put(SensorTypes.acc_z, accTranslator);
	}

	/**
	 * helper to initialise mapping of data IDs
	 */
	private void initDataIDs() {
		// put data
		dataIDs.put(0x01, SensorTypes.gps_altitude_before);
		dataIDs.put(0x01 + 8, SensorTypes.gps_altitude_after);
		dataIDs.put(0x02, SensorTypes.temp1);
		dataIDs.put(0x03, SensorTypes.rpm);
		dataIDs.put(0x04, SensorTypes.fuel);
		dataIDs.put(0x05, SensorTypes.temp2);
		dataIDs.put(0x06, SensorTypes.volt);
		dataIDs.put(0x10, SensorTypes.altitude_before);
		dataIDs.put(0x21, SensorTypes.altitude_after);
		dataIDs.put(0x11, SensorTypes.gps_speed_before);
		dataIDs.put(0x11 + 8, SensorTypes.gps_speed_after);
		dataIDs.put(0x12, SensorTypes.longitude_before);
		dataIDs.put(0x12 + 8, SensorTypes.longitude_after);
		dataIDs.put(0x1A + 8, SensorTypes.ew);
		dataIDs.put(0x13, SensorTypes.latitude_before);
		dataIDs.put(0x13 + 8, SensorTypes.latitude_after);
		dataIDs.put(0x1B + 8, SensorTypes.ns);
		dataIDs.put(0x14, SensorTypes.course_before);
		dataIDs.put(0x14 + 8, SensorTypes.course_after);
		dataIDs.put(0x15, SensorTypes.day_month);
		dataIDs.put(0x16, SensorTypes.year);
		dataIDs.put(0x17, SensorTypes.hour_minute);
		dataIDs.put(0x18, SensorTypes.second);
		dataIDs.put(0x24, SensorTypes.acc_x);
		dataIDs.put(0x25, SensorTypes.acc_y);
		dataIDs.put(0x26, SensorTypes.acc_z);
	}

	/**
	 * retrieve the translator based on the data available in the user data
	 * frame
	 * 
	 * TODO see if we can use the Frame object instead
	 * 
	 * @param sensorType
	 * @return
	 */
	public UserDataTranslator getTranslatorForFrame(SensorTypes sensorType) {
		return dataTranslators.get(sensorType);
	}

	/**
	 * resolve the sensor type based on the given frame information
	 * 
	 * @param frame
	 * @return
	 */
	public SensorTypes getSensorType(int[] frame) {
		return dataIDs.get(frame[1]);
	}

}
