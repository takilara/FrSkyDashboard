package biz.onomato.frskydash.hub;

import java.util.Arrays;
import java.util.Collections;
import java.util.TreeMap;

import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.domain.Channel;
import biz.onomato.frskydash.domain.Frame;
import biz.onomato.frskydash.util.Logger;

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
	 * is bound to a single server instance
	 */
	private static FrSkyServer server;

	/**
	 * def ctor, singleton use {@link #getInstance()} instead
	 */
	
	
	
	
	private FrSkyHub() {

	}

	public static FrSkyHub getInstance(/* FrSkyServer forServer */) {
		if (instance == null) {
			instance = new FrSkyHub();
			// server = forServer;
			// not a good idea since parameter will be ignored if instance
			// already existed, moved to method where needed
			
			
			/**
			 * eso: Prototype Channel code
			 */
			setupFixedChannels();
		}
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
	public void extractUserDataBytes(FrSkyServer paramServer, Frame frame) {
		// init
		// FIXME can't this be implemented in another way so we don't have to
		// pass the server instance?
		server = paramServer;
		int b;
		int[] ints = frame.toInts();
		// iterate elements in frame
		// for (int b : frame) {
		// don't handle all the bytes, skip header (0), prim(1), size(2),
		// unused(3) and end but(10)
		// the byte at index 2 indicated how many bytes are valid in this frame.
		// Make sure to only take in account these valid bytes starting to count
		// from byte at index 4
		// index => byte description
		// 0 => frame start byte
		// 1 => type of frame byte (analog, signal, user data, ...)
		// 2 => length of valid bytes
		// 3 => discard this byte
		// 4 => first user data byte
		// ...
		// 10 => stop byte frame
		int nrOfValidBytesInFrame = ints[2];
		// for (int i = 4; i < Frame.SIZE_TELEMETRY_FRAME - 1; i++) {
		for (int i = 4; i < 4 + nrOfValidBytesInFrame; i++) {
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
				Logger.d(FrSkyServer.TAG, "XOR operation, unstuffed to "
						+ Integer.toHexString(b));
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
					Logger.d(
							FrSkyServer.TAG,
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
				Logger.d(FrSkyServer.TAG,
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
			Logger.d(FrSkyServer.TAG,
					"Wrong hub frame format: " + Arrays.toString(frame));
			return;
		}
		// check data ID and update correct channel
		switch (frame[1]) {
		case 0x01:
			updateChannel(ChannelTypes.gps_altitude_before,
					getUnsigned16BitValue(frame));
			break;
		case 0x01 + 8:
			updateChannel(ChannelTypes.gps_altitude_after,
					getUnsigned16BitValue(frame));
			break;
		case 0x02:
			updateChannel(ChannelTypes.temp1, getUnsigned16BitValue(frame));
			break;
		case 0x03:
			// actual RPM value is Frame1*60
			updateChannel(ChannelTypes.rpm, getUnsigned16BitValue(frame) * 60);
			break;
		case 0x04:
			updateChannel(ChannelTypes.fuel, getUnsigned16BitValue(frame));
			break;
		case 0x05:
			updateChannel(ChannelTypes.temp2, getUnsigned16BitValue(frame));
		case 0x06:
			// first 4 bit is battery cell number
			// last 12 bit refer to voltage range 0-2100 corresponding 0-4.2V
			int cell = getBatteryCell(frame);
			// if (cell < 1 || cell > 6) {
			if (cell < 0 || cell > 5) {
				Logger.d(this.getClass().toString(),
						"failed to handle cell nr out of range: " + cell);
				break;
			}
			double value = getCellVoltage(frame);
			// get the right channeltype based on cell nr
			String channelType = "volt_" + cell;
			// and update
			updateChannel(ChannelTypes.valueOf(channelType), value);
			break;
		case 0x10:
			updateChannel(ChannelTypes.altitude_before,
					getUnsigned16BitValue(frame));
			break;
		case 0x21:
			updateChannel(ChannelTypes.altitude_after,
					getUnsigned16BitValue(frame));
			break;
		case 0x11:
			updateChannel(ChannelTypes.gps_speed_before,
					getUnsigned16BitValue(frame));
			break;
		case 0x11 + 8:
			updateChannel(ChannelTypes.gps_speed_after,
					getUnsigned16BitValue(frame));
			break;
		case 0x12:
			updateChannel(ChannelTypes.longitude_before,
					getUnsigned16BitValue(frame));
			break;
		case 0x12 + 8:
			updateChannel(ChannelTypes.longitude_after,
					getUnsigned16BitValue(frame));
			break;
		case 0x1A + 8:
			updateChannel(ChannelTypes.ew, frame[2]);
			break;
		case 0x13:
			updateChannel(ChannelTypes.latitude_before,
					getUnsigned16BitValue(frame));
			break;
		case 0x13 + 8:
			updateChannel(ChannelTypes.latitude_after,
					getUnsigned16BitValue(frame));
			break;
		case 0x1B + 8:
			updateChannel(ChannelTypes.ns, frame[2]);
			break;
		case 0x14:
			updateChannel(ChannelTypes.course_before,
					getUnsigned16BitValue(frame));
			break;
		case 0x14 + 8:
			updateChannel(ChannelTypes.course_after,
					getUnsigned16BitValue(frame));
			break;
		case 0x15:
			updateChannel(ChannelTypes.day, frame[2]);
			updateChannel(ChannelTypes.month, frame[3]);
			break;
		case 0x16:
			updateChannel(ChannelTypes.year, 2000 + frame[2]);
			break;
		case 0x17:
			updateChannel(ChannelTypes.hour, frame[2]);
			updateChannel(ChannelTypes.minute, frame[3]);
			break;
		case 0x18:
			updateChannel(ChannelTypes.second, frame[2]);
			break;
		case 0x24:
			// actual 3-axis value is Frame1/1000
			updateChannel(ChannelTypes.acc_x, getSigned16BitValue(frame) / 1000);
			break;
		case 0x25:
			// actual 3-axis value is Frame1/1000
			updateChannel(ChannelTypes.acc_y, getSigned16BitValue(frame) / 1000);
			break;
		case 0x26:
			// actual 3-axis value is Frame1/1000
			updateChannel(ChannelTypes.acc_z, getSigned16BitValue(frame) / 1000);
			break;
		default:
			// TODO add voltage and current sensor data
			Logger.d(FrSkyServer.TAG, "Unknown sensor type for frame: "
					+ Arrays.toString(frame));
		}
	}

	private int getSigned16BitValue(int[] frame) {
		// ByteBuffer bb = ByteBuffer.allocate(2);
		// bb.order(ByteOrder.LITTLE_ENDIAN);
		// bb.put((byte)frame[3]);
		// bb.put((byte)frame[2]);
		// short shortVal = bb.getShort(0);
		// return shortVal;
		// return 0xFFFF - ( (frame[2] & 0xFF) + ( (frame[3] & 0xFF) * 0x100) );
		return 0xFFFF - getUnsigned16BitValue(frame);
	}

	private int getUnsigned16BitValue(int[] frame) {
		// return getSigned16BitValue(frame) & 0xffff;
		return ((frame[2] & 0xFF) + ((frame[3] & 0xFF) * 0x100));
	}

	// private int getBCDValue(int[] frame){
	// return 0;
	// }

	/**
	 * helper to retrieve the number of the cell from frame
	 * 
	 * @param frame
	 * @return
	 */
	private int getBatteryCell(int[] frame) {
		// only need the first 4 bits to get cell number
		int cellNumber = frame[2] >> 4;
		return cellNumber;
	}

	/**
	 * helper to retrieve voltage of a cell from frame
	 * 
	 * @param frame
	 * @return
	 */
	private double getCellVoltage(int[] frame) {
		// the last 12 bits are a value between 0-2100 representing voltage
		// 0-4.2v (so /500)
		double voltage = (double) ((frame[2] << 8 & 0xFFF) + frame[3]) / 500;
		return voltage;
	}

	/**
	 * update a single channel with a single value
	 * 
	 * @param channel
	 * @param value
	 */
	private void updateChannel(ChannelTypes channel, double value) {
		// TODO create a channel here for the correct type of information and
		// broadcast channel so GUI can update this value
		Logger.d(FrSkyServer.TAG, "Data received for channel: " + channel
				+ ", value: " + value);
		
		
		/** eso, prototype Channel support
		 * Update a proper channel rather than broadcast. 
		 * Allow broadcasts until Channels are fully implemented
		*/
		switch (channel){
		case rpm:
			_sourceChannelMap.get(CHANNEL_ID_RPM).setRaw(value);
			break;
		case temp1:
			_sourceChannelMap.get(CHANNEL_ID_TEMP1).setRaw(value);
			break;
		case altitude_before:
			///TODO: Proper construction of resulting altitude double needs to be done in handleHubDataFrame or extractUserDataBytes
			/**
			 * Temporarily construct double. 
			 * TODO: Do this properly elsewhere
			 */
			
			double alt = value+(alt_after/100);
			
			_sourceChannelMap.get(CHANNEL_ID_TEMP1).setRaw(alt);
			break;			
		case altitude_after:
			// bad method of allowing construction of altitude double
			alt_after = value;
		}
		
		// let server update this information
		server.broadcastChannelData(channel, value);
	}
	
	
	/**
	 * Prototype Channel support
	 * eso
	 */

	/**
	 * Unique ID for the HUB
	 */
	public static final int HUB_ID = -1000;
	
	/**
	 * Unique ID's for the Hubs channels
	 */
    public static final int CHANNEL_ID_ALTITUDE = 	0		+ HUB_ID;
    public static final int CHANNEL_ID_RPM = 		1		+ HUB_ID;
    public static final int CHANNEL_ID_TEMP1 = 		2		+ HUB_ID;
    public static final int CHANNEL_ID_TEMP2 = 		3		+ HUB_ID;

    /**
     * Poor solution for holding decimal part of altitude
     * 
     */
    public static double alt_after = 0;
    
    /**
     * Treemap to hold the Hubs channels
     */
    private static TreeMap<Integer,Channel> _sourceChannelMap;
	
    /**
	 * 
	 * @return a ServerChannel corresponding to the given id
	 */
	public static Channel getSourceChannel(int id)
	{
		return _sourceChannelMap.get(id);
	}
	
	/**
	 * 
	 * @return all the ServerChannels in a TreeMap (key is the id of the Channel) 
	 */
	public static TreeMap<Integer,Channel> getSourceChannels()
	{
		return _sourceChannelMap;
	}
	
	/**
	 * Create the _sourceChannelMap
	 * Populate it with our channels
	 */
	public static void setupFixedChannels()
	{
		_sourceChannelMap = new TreeMap<Integer,Channel>(Collections.reverseOrder());
		
		//Sets up the hardcoded channels (Altitude,RPM)
		///TODO: Add all channels here, these two are added for reference
		///TODO: Figure out how to deal with race conditions on "split numbers"
		Channel altitude =  new Channel("Hub: Altitude", 0, 1, "", "");
		altitude.setId(CHANNEL_ID_ALTITUDE);
		altitude.setPrecision(0);
		altitude.setSilent(true);
		_sourceChannelMap.put(CHANNEL_ID_ALTITUDE, altitude);
		
		Channel rpm =  new Channel("Hub: RPM (pulses)", 0, 1, "", "");
		rpm.setId(CHANNEL_ID_RPM);
		rpm.setPrecision(0);
		rpm.setSilent(true);
		_sourceChannelMap.put(CHANNEL_ID_RPM, rpm);
		
		Channel temp1 =  new Channel("Hub: Temp 1", 0, 1, "", "");
		temp1.setId(CHANNEL_ID_TEMP1);
		temp1.setPrecision(0);
		temp1.setSilent(true);
		_sourceChannelMap.put(CHANNEL_ID_TEMP1, temp1);
	}

}
