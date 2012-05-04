package biz.onomato.frskydash.sim;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * <p>
 * Should create sensor hub data frames for simulation. Since this data is
 * spread over several frames we'll have to keep track of at least the previous
 * frame that was handled.
 * </p>
 * 
 * <p>
 * Bytes at index 4 to 9 (incl) are available for sensor hub data in a frame
 * starting/ending with 0x7D and with prim (index 1) byte 0xFD.
 * </p>
 * 
 * <p>
 * For more information on frame format visit <a
 * href="http://projects.onomato.biz/projects/android-dash/wiki/FrameFormat"
 * >Frame Format page on wiki</a>.
 * </p>
 * 
 * @author hcpl
 * 
 */
public class SensorHubDataGenerator {

	// TODO not tested with signed information

	/**
	 * the current sensorHubDataFrame we are working on. If this one isn't empty
	 * then we need to continue working with it
	 */
	private static ArrayList<Integer> sensorHubDataFrame = new ArrayList<Integer>();

	/**
	 * the index we were at for this current {@link #sensorHubDataFrame}.
	 */
	private static int currentIndex = -1;

	private static final int INDEX_START = 4;
	private static final int INDEX_STOP = 9;

	/**
	 * create valid frame with simulated sensor hub data.
	 * 
	 */
	public static int[] generateSimulatedSensorHubData() {
		// create base structure of frame first
		int[] frame = new int[11];
		frame[0] = 0x7D;
		frame[1] = 0xFD;
		frame[10] = 0x7D;
		// based on this information we can complete the received frame with
		// simulated sensor hub data
		int i = INDEX_START;
		for (; i <= INDEX_STOP; i++) {
			// see if we are still working on a sensor hub data frame or need to
			// create a new one
			if (currentIndex < 0 || currentIndex >= sensorHubDataFrame.size()) {
				// no more generated sensor hub data available, need to create a
				// new one first
				createSensorHubData();
				// set index at beginning
				currentIndex = 0;
			}
			// FIXME need to check for byte stuffing being required or not!!
			// add this information byte by byte
			frame[i] = sensorHubDataFrame.get(currentIndex++);
		}
		// we also need to update the number of valid bytes in the main
		// frame. This is based on the index we used for counting on top here,
		// if we were at the end of a hub frame the counter would have stopped
		// so we now how many bytes we actually have set on the frame
		frame[2] = i - INDEX_START;
		return frame;
	}

	private static int alt = 100;
	private static final int MAX_ALT = 250;
	private static int alt_after = 0;
	private static final int MAX_ALT_AFTER = 99;
	private static int temp1 = 10;
	private static final int MAX_TEMP1 = 25;
	private static int temp2 = 10;
	private static final int MAX_TEMP2 = 25;
	private static final int MIN_RPM = 250;
	private static final int MAX_RPM = 2500;
	private static int rpm = MIN_RPM;

	/**
	 * helper to create some simulated sensor hub data frame. See
	 * http://www.frsky-rc.com/uploadfile/201111/20111124233818462.pdf for more
	 * information on valid frames
	 */
	private static void createSensorHubData() {
		// TODO add more dataIds here
		sensorHubDataFrame.add(0x5E); // start/stop byte
		sensorHubDataFrame.add(0x10); // dataID vario before ,
		sensorHubDataFrame.add(generateSignedLE16Bit(alt)[0]); // value vario
		sensorHubDataFrame.add(generateSignedLE16Bit(alt)[1]); // value vario
		alt = (alt >= MAX_ALT ? 0 : alt + 1); // update value

		sensorHubDataFrame.add(0x5E); // start/stop byte
		sensorHubDataFrame.add(0x21); // dataID vario after ,
		sensorHubDataFrame.add(generateUnsignedLE16Bit(alt_after)[0]); // value
		sensorHubDataFrame.add(generateUnsignedLE16Bit(alt_after++)[1]); // value
		alt_after = (alt_after >= MAX_ALT_AFTER ? 0 : alt_after + 1);

		sensorHubDataFrame.add(0x5E); // start/stop byte
		sensorHubDataFrame.add(0x02); // dataId temp1
		sensorHubDataFrame.add(generateSignedLE16Bit(temp1)[0]);
		sensorHubDataFrame.add(generateSignedLE16Bit(temp1)[1]);
		temp1 = temp1 >= MAX_TEMP1 ? 0 : temp1 + 1;

		sensorHubDataFrame.add(0x5E); // start/stop byte
		sensorHubDataFrame.add(0x05); // dataId temp1
		sensorHubDataFrame.add(generateSignedLE16Bit(temp2)[0]);
		sensorHubDataFrame.add(generateSignedLE16Bit(temp2)[1]);
		temp2 = temp2 >= MAX_TEMP2 ? 0 : temp2 + 1;

		sensorHubDataFrame.add(0x5E); // start/stop byte
		sensorHubDataFrame.add(0x06); // voltage sensor
		sensorHubDataFrame.add(generateVoltByte(0, 3.80)[0]);
		sensorHubDataFrame.add(generateVoltByte(0, 3.80)[1]);

		sensorHubDataFrame.add(0x5E); // start/stop byte
		sensorHubDataFrame.add(0x06); // voltage sensor
		sensorHubDataFrame.add(generateVoltByte(1, 3.81)[0]);
		sensorHubDataFrame.add(generateVoltByte(1, 3.81)[1]);

		sensorHubDataFrame.add(0x5E); // start/stop byte
		sensorHubDataFrame.add(0x06); // voltage sensor
		sensorHubDataFrame.add(generateVoltByte(2, 3.82)[0]);
		sensorHubDataFrame.add(generateVoltByte(2, 3.82)[1]);

		sensorHubDataFrame.add(0x5E); // start/stop byte
		sensorHubDataFrame.add(0x03); // rpm sensor
		sensorHubDataFrame.add(generateUnsignedLE16Bit(rpm/60)[0]);
		sensorHubDataFrame.add(generateUnsignedLE16Bit(rpm/60)[1]);
		rpm = rpm >= MAX_RPM ? 0 : rpm + 100;

		sensorHubDataFrame.add(0x5E); // start/stop byte
		// ...
	}

	// TODO now values are calc 2x, use cache instead
	private static HashMap<Integer, HashMap<Double, int[]>> cachedVoltageValues = new HashMap<Integer, HashMap<Double, int[]>>();

	// TODO make constants for dataIDs or add this to enum ChannelTypes

	private static int[] generateSignedLE16Bit(int value) {
		// return 0xFFFF - ( (frame[2] & 0xFF) + ( (frame[3] & 0xFF) * 0x100) );
		// return 0xFFFF - getUnsigned16BitValue(frame);
		return new int[] { (value & 0x00FF), (value >> 8) };
	}

	private static int[] generateUnsignedLE16Bit(int value) {
		// return getSigned16BitValue(frame) & 0xffff;
		// ((frame[2] & 0xFF) + ((frame[3] & 0xFF) * 0x100));
		return new int[] { (value & 0x00FF), (value >> 8) };
	}

	private static int[] generateUnsignedBE16Bit(int value) {
		// return frame[3] + frame[2] * 100;
		int[] bytes = new int[2];
		bytes[0] = value / 100;
		bytes[1] = value - bytes[0];
		return bytes;
	}

	private static int[] generateVoltByte(int cell, double voltage) {
		// voltage = ((frame[2] << 8 & 0xFFF) + frame[3]) / 500
		// int[] frame = new int[] { 0x5E, 0x06, 0, 0, 0x5E };
		int[] bytes = new int[2];
		// calculate what the integer voltage value was
		int volt = (int) (voltage * 500);
		// based on that update frame
		// bytes[1] = volt - ((volt >> 8) << 8);
		bytes[1] = volt & 0xFF; // mask bits instead
		bytes[0] = (cell << 4) + (volt >> 8);
		return bytes;
	}
}
