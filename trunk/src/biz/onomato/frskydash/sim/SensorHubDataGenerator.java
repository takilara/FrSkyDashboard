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

	/**
	 * helper to create some simulated sensor hub data frame. See
	 * http://www.frsky-rc.com/uploadfile/201111/20111124233818462.pdf for more
	 * information on valid frames
	 */
	private static void createSensorHubData() {
		// TODO add some randomness and more dataIds here
		// sensorHubDataFrame.add(0x5E); // start/stop byte
		// sensorHubDataFrame.add(0x10); // dataID vario before ,
		// sensorHubDataFrame.add(generateSignedLE16Bit(200)[0]); // value vario
		// sensorHubDataFrame.add(generateSignedLE16Bit(200)[1]); // value vario
		//
		// sensorHubDataFrame.add(0x5E); // start/stop byte
		// sensorHubDataFrame.add(0x21); // dataID vario after ,
		// sensorHubDataFrame.add(generateUnSignedLE16Bit(10)[0]); // value
		// vario
		// sensorHubDataFrame.add(generateUnSignedLE16Bit(10)[1]); // value
		// vario
		//
		// sensorHubDataFrame.add(0x5E); // start/stop byte
		// sensorHubDataFrame.add(0x02); // dataId temp1
		// sensorHubDataFrame.add(generateSignedLE16Bit(18)[0]); // value temp1
		// sensorHubDataFrame.add(generateSignedLE16Bit(18)[1]); // value temp1

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
		// ...
	}

	// TODO now values are calc 2x, use cache instead
	private static HashMap<Integer, HashMap<Double, int[]>> cachedVoltageValues = new HashMap<Integer, HashMap<Double, int[]>>();

	// TODO make constants for dataIDs or add this to enum ChannelTypes

	private static void generateSignedLE16Bit() {
		// return (short) getUnsignedLE16BitValue(frame);
		// TODO
	}

	private static void generateUnsignedLE16Bit() {
		// return ((frame[2] & 0xFF) + ((frame[3] & 0xFF) * 0x100));
		// TODO
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
		bytes[1] = volt - ((volt >> 8) << 8);
		bytes[0] = (cell << 4) + (volt >> 8);
		return bytes;
	}
}
