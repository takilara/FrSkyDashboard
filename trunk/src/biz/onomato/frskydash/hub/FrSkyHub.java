package biz.onomato.frskydash.hub;

import java.util.Arrays;

import android.util.Log;
import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.Frame;

/**
 * responsible for parsing FrSky Hub Sensor Data
 * 
 */
public class FrSkyHub {

	/**
	 * singleton instance
	 */
	private static FrSkyHub instance = null;

	/**
	 * the current user frame we are working on. This is used to pass data
	 * between incompletes frames.
	 */
	private static int[] hubFrame = new int[Frame.SIZE_HUB_FRAME];

	/**
	 * index of the current user frame. If set to -1 no user frame is under
	 * construction.
	 */
	private static int currentHubFrameIndex = -1;

	/**
	 * if on previous byte the XOR byte was found or not
	 */
	private static boolean hubXOR = false;

	/**
	 * def ctor, singleton use {@link #getInstance()} instead
	 */
	private FrSkyHub() {

	}

	public static FrSkyHub getInstance() {
		if (instance == null)
			instance = new FrSkyHub();
		return instance;
	}

	/**
	 * Extract user data bytes from telemetry data frame. This subframe is
	 * delimited by the 0x5E byte, included in the frame argument for this
	 * method. A single call can contain incomplete user data frames so need to
	 * keep track of previous frame also.
	 * 
	 * @param frame
	 */
	public void extractUserDataBytes(Frame frame) {
		// init
		int b;
		int[] ints = frame.toInts();
		// iterate elements in frame
		// for (int b : frame) {
		// don't handle all the bytes, skip header (0), prim(1), size(2),
		// unused(3) and end but(10)
		for (int i = 4; i < Frame.SIZE_TELEMETRY_FRAME - 1; i++) {
			b = ints[i];
			// handle byte stuffing first
			if (b == Frame.STUFFING_HUB_FRAME) {
				hubXOR = true;
				// drop this byte
				continue;
			}
			if (hubXOR) {
				b ^= Frame.XOR_HUB_FRAME;
				// don't unset the xor flag yet since we'll have to check on
				// this for start/stop bit detection
				// xor = false;
				Log.d(FrSkyServer.TAG,
						"XOR operation, unstuffed to " + Integer.toHexString(b));
			}
			// if we encounter a start byte we need to indicate we're in a
			// frame or if at the end handle the frame and continue
			if (b == Frame.START_STOP_HUB_FRAME && !hubXOR) {
				// if currentFrameIndex is not set we have to start a new
				// frame here
				if (currentHubFrameIndex < 0) {
					// init current frame index at beginning
					currentHubFrameIndex = 0;
					// and copy this first byte in the frame
					hubFrame[currentHubFrameIndex++] = b;
				}
				// otherwise we were already collecting a frame so this
				// indicates we are at the end now. At this point a frame is
				// available that we can send over.
				else if (currentHubFrameIndex == Frame.SIZE_HUB_FRAME - 1) {
					// just complete the frame we were collecting
					hubFrame[currentHubFrameIndex] = b;
					// this way the length is confirmed
					handleHubDataFrame(hubFrame);
					// once information is handled we can reset the frame
					hubFrame = new int[Frame.SIZE_HUB_FRAME];
					currentHubFrameIndex = 0;
					// unlike the telemetry frames where 126 is on each side of
					// the frame (126, x, y, 126, 126, x, y, 126) the 94 bit of
					// the hub frame is only a single bit in between (94, x, y,
					// 94, x, y, 94) so we need to reuse that last 94 bit
					// encountered as the beginning of our next frame
					hubFrame[currentHubFrameIndex++] = b;
				}
				// if for some reason we got 2 0x5e bytes after each other
				// or the size of the frame was different we can't do
				// anything with the previous collected information. We can
				// log a debug message and drop the frame to start over
				// again.
				else {
					// log debug info here
					Log.d(FrSkyServer.TAG,
							"Start/stop byte at wrong position: 0x"
									+ Integer.toHexString(b)
									+ " frame so far: "
									+ Arrays.toString(hubFrame));
					currentHubFrameIndex = 0;
					hubFrame = new int[Frame.SIZE_HUB_FRAME];
					hubFrame[currentHubFrameIndex++] = b;
				}
			}
			// otherwise we are handling a valid byte that has to be put in
			// the frame we are collecting. But only when we are currently
			// working on a frame!
			else if (currentHubFrameIndex >= 0
					&& currentHubFrameIndex < Frame.SIZE_HUB_FRAME - 1) {
				hubFrame[currentHubFrameIndex++] = b;
			}
			// finally it's possible that we receive bytes without being in
			// a frame, just discard them for now
			else {
				// log debug info here
				Log.d(FrSkyServer.TAG,
						"Received data outside frame, dropped byte: 0x"
								+ Integer.toHexString(b));
			}
			// make sure to unset the xor flag at this point
			hubXOR = false;
		}
	}

	/**
	 * once we extracted the user data frames these can be handled by checking
	 * their data ID
	 * 
	 * @param frame
	 */
	private void handleHubDataFrame(int[] frame) {
		// some validation first
		// all frames are delimited by the 0x5e start and end byte and should be
		// 5 bytes long. We can validate this before doing any parsing
		if (frame.length != Frame.SIZE_HUB_FRAME
				|| frame[0] != Frame.START_STOP_HUB_FRAME
				|| frame[Frame.SIZE_HUB_FRAME - 1] != Frame.START_STOP_HUB_FRAME) {
			// log exception here
			Log.d(FrSkyServer.TAG,
					"Wrong hub frame format: " + Arrays.toString(frame));
			return;
		}
		// check data ID and update correct channel
		// FIXME CHECK HOW TO PARSE DATA, NOT ALWAYS FRAME IDX 2 USED, UNITS,
		// ...?
		switch (frame[1]) {
		case 0x01:
			updateChannel(Channels.gps_altitude_before, frame[2]);
			break;
		case 0x01 + 8:
			updateChannel(Channels.gps_altitude_after, frame[2]);
			break;
		case 0x02:
			updateChannel(Channels.temp1, frame[2]);
			break;
		case 0x03:
			updateChannel(Channels.rpm, frame[2] * 60);
			break;
		case 0x04:
			updateChannel(Channels.fuel, frame[2]);
			break;
		case 0x05:
			updateChannel(Channels.temp2, frame[2]);
		case 0x06:
			// FIXME cell & voltage in this one value
			updateChannel(Channels.volt, frame[2]);
			break;
		case 0x10:
			updateChannel(Channels.altitude, frame[2]);
			break;
		case 0x11:
			updateChannel(Channels.gps_speed_before, frame[2]);
			break;
		case 0x11 + 8:
			updateChannel(Channels.gps_speed_after, frame[2]);
			break;
		case 0x12:
			updateChannel(Channels.longitude_before, frame[2]);
			break;
		case 0x12 + 8:
			updateChannel(Channels.longitude_after, frame[2]);
			break;
		case 0x1A + 8:
			updateChannel(Channels.ew, frame[2]);
			break;
		case 0x13:
			updateChannel(Channels.latitude_before, frame[2]);
			break;
		case 0x13 + 8:
			updateChannel(Channels.latitude_after, frame[2]);
			break;
		case 0x1B + 8:
			updateChannel(Channels.ns, frame[2]);
			break;
		case 0x14:
			updateChannel(Channels.course_before, frame[2]);
			break;
		case 0x14 + 8:
			updateChannel(Channels.course_after, frame[2]);
			break;
		case 0x15:
			updateChannel(Channels.day, frame[2]);
			updateChannel(Channels.month, frame[3]);
			break;
		case 0x16:
			updateChannel(Channels.year, 2000 + frame[2]);
			break;
		case 0x17:
			updateChannel(Channels.hour, frame[2]);
			updateChannel(Channels.minute, frame[3]);
			break;
		case 0x18:
			updateChannel(Channels.second, frame[2]);
			break;
		case 0x24:
			updateChannel(Channels.acc_x, frame[2] / 1000);
			break;
		case 0x25:
			updateChannel(Channels.acc_y, frame[2] / 1000);
			break;
		case 0x26:
			updateChannel(Channels.acc_z, frame[2] / 1000);
			break;
		default:
			Log.d(FrSkyServer.TAG,
					"Unknown sensor type for frame: " + Arrays.toString(frame));
		}
	}

	/**
	 * update a single channel with a single value
	 * 
	 * @param channel
	 * @param value
	 */
	private void updateChannel(Channels channel, int value) {
		// TODO create a channel here for the correct type of information and
		// broadcast channel so GUI can update this value
		Log.d(FrSkyServer.TAG, "Data received for channel: " + channel
				+ ", value: " + value);
	}

	/**
	 * possible channels for sensor hub data
	 * 
	 */
	public enum Channels {
		undefined, gps_altitude_before, gps_altitude_after, temp1, rpm, fuel, temp2, volt, altitude, gps_speed_before, gps_speed_after, longitude_before, longitude_after, ew, latitude_before, latitude_after, ns, course_before, course_after, day, month, year, hour, minute, second, acc_x, acc_y, acc_z
	}

}
