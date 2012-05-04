package biz.onomato.frskydash.hub.sensors;

import java.util.Calendar;

import biz.onomato.frskydash.hub.FrSkyHub;
import biz.onomato.frskydash.hub.SensorTypes;
import biz.onomato.frskydash.util.Logger;

public class GPSTranslator implements UserDataTranslator {

	/**
	 * some values need to be combined, keep track of previous values for this
	 */
	private double altitude, speed, longitude, latitude, course;

	/**
	 * init data as today
	 */
	private Calendar date = Calendar.getInstance();

	@Override
	public double translateValue(SensorTypes type, int[] frame) {
		// calculate the correct way based on sensor type
		switch (type) {
		case gps_altitude_before:
			// combined value
			altitude = FrSkyHub.getAfter(altitude)
					+ FrSkyHub.getSignedLE16BitValue(frame);
			return altitude;
		case gps_altitude_after:
			// combined value
			altitude = FrSkyHub.getBefore(altitude)
					+ FrSkyHub.convertToAfter(FrSkyHub
							.getUnsignedLE16BitValue(frame));
			return altitude;
		case gps_speed_before:
			speed = FrSkyHub.getAfter(speed)
					+ FrSkyHub.getUnsignedLE16BitValue(frame);
			return speed;
		case gps_speed_after:
			speed = FrSkyHub.getBefore(speed)
					+ FrSkyHub.convertToAfter(FrSkyHub
							.getUnsignedLE16BitValue(frame));
			return speed;
		case longitude_before:
			longitude = FrSkyHub.getAfter(longitude)
					+ FrSkyHub.getUnsignedBE16BitValue(frame);
			return longitude;
		case longitude_after:
			longitude = FrSkyHub.getBefore(longitude)
					+ FrSkyHub.convertToAfter(FrSkyHub
							.getUnsignedBE16BitValue(frame));
			return longitude;
		case ew:
			return FrSkyHub.getUnsignedLE16BitValue(frame);
		case latitude_before:
			latitude = FrSkyHub.getAfter(latitude)
					+ FrSkyHub.getUnsignedBE16BitValue(frame);
			return latitude;
		case latitude_after:
			latitude = FrSkyHub.getBefore(latitude)
					+ FrSkyHub.convertToAfter(FrSkyHub
							.getUnsignedBE16BitValue(frame));
			return latitude;
		case ns:
			return FrSkyHub.getUnsignedLE16BitValue(frame);
		case course_before:
			// should be between 0 & 359.99 degrees??
			// FIXME seems to work but with much noise
			course = FrSkyHub.getAfter(course)
					+ FrSkyHub.getUnsignedLE16BitValue(frame);
			return course;
		case course_after:
			course = FrSkyHub.getBefore(course)
					+ FrSkyHub.convertToAfter(FrSkyHub
							.getUnsignedLE16BitValue(frame));
			return course;
		case day_month:
			int day = frame[2];
			int month = frame[3];
			date.set(Calendar.DAY_OF_MONTH, day);
			date.set(Calendar.MONTH, month - 1);
			// combine
			return date.getTimeInMillis();
		case year:
			int year = 2000 + frame[2];
			date.set(Calendar.YEAR, year);
			return date.getTimeInMillis();
		case hour_minute:
			int hour = frame[2];
			int minute = frame[3];
			date.set(Calendar.HOUR, hour);
			date.set(Calendar.MINUTE, minute);
			// combine
			return date.getTimeInMillis();
		case second:
			int second = frame[2];
			date.set(Calendar.SECOND, second);
			return date.getTimeInMillis();
		}
		// default value to return is zero
		Logger.e(this.getClass().toString(),
				"Invalid identifier for GPS data received, user data in frame: "
						+ frame);
		return 0;
	}

}
