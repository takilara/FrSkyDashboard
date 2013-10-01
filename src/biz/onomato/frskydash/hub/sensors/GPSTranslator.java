/*
 * Copyright 2011-2013, Espen Solbu, Hans Cappelle
 * 
 * This file is part of FrSky Dashboard.
 *
 *  FrSky Dashboard is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FrSky Dashboard is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FrSky Dashboard.  If not, see <http://www.gnu.org/licenses/>.
 */

package biz.onomato.frskydash.hub.sensors;

import java.util.Arrays;
import java.util.Calendar;

import biz.onomato.frskydash.hub.FrSkyHub;
import biz.onomato.frskydash.hub.Hub;
import biz.onomato.frskydash.hub.SensorTypes;
import biz.onomato.frskydash.util.Logger;

public class GPSTranslator implements UserDataTranslator {
	private static final String TAG = "GPSTranslator";
	/**
	 * some values need to be combined, keep track of previous values for this
	 */
	private double altitude, speed, longitude, latitude, course;
	private double long_bp,long_ap,long_tmp;
	private double lat_bp,lat_ap,lat_tmp;
	private double alt_bp, alt_ap;
	private double speed_bp, speed_ap;
	private double course_bp,course_ap;
	private int day,month,hour,minute,second;
	private int year = 2000;
	
	private double PRECISION_SPEED = 100.0;
	private double PRECISION_ALTITUDE = 10.0;
	private double PRECISION_COURSE = 100.0;
	

	/**
	 * init data as today
	 */
	private Calendar date = Calendar.getInstance();

	@Override
	public double translateValue(SensorTypes type, int[] frame) {
		// calculate the correct way based on sensor type
		// latitude (in decimal form) = floor(lat_bp/100)*1000000 + (((lat_bp%100)*10000)+lat_ap)*5/3
		switch (type) {
		case gps_altitude_before:
			// combined value
			alt_bp = FrSkyHub.getSignedLE16BitValue(frame);
			break;
		case gps_altitude_after:
			// combined value
			alt_ap = FrSkyHub.getUnsignedLE16BitValue(frame)/PRECISION_ALTITUDE;
			altitude = alt_bp+alt_ap;
			return altitude;
		case gps_speed_before:
			speed_bp = FrSkyHub.getUnsignedLE16BitValue(frame);
			break;
		case gps_speed_after:
			speed_ap = FrSkyHub.getUnsignedLE16BitValue(frame)/PRECISION_SPEED;
			speed = speed_bp+speed_ap; 
			return speed;
		case gps_longitude_before:
			//TODO: Do we need to look at NS/EW as well = (for sign?)
			long_bp = Math.floor(FrSkyHub.getSignedLE16BitValue(frame)/100.0)*1000000.0;
			//Logger.w(TAG, "bp: "+long_bp);
			long_tmp = (FrSkyHub.getSignedLE16BitValue(frame)%100.0)*10000.0;
			//Logger.w(TAG, "tmp: "+long_tmp);
			break;
		case gps_longitude_after:
			long_ap = FrSkyHub.getUnsignedLE16BitValue(frame);
			//Logger.w(TAG, "ap: "+long_ap);
			longitude = (long_bp + (long_tmp+long_ap)*5/3)/1000000.0;
			//Logger.w(TAG, "longditude: "+longitude);
			break;
			//return longitude;
		case gps_longitude_ew:
			int ew = FrSkyHub.getUnsignedLE16BitValue(frame);
			//Logger.w(TAG, "Got E/W: "+ew);
			switch(ew){
				case 69: 
					//Logger.w(TAG, "Got: E");
					// need to endure longditude positive
					if(longitude<0) longitude *= (-1);
					break;
				case 87:
					//Logger.w(TAG, "Got: W");
					// need to ensure longditude negative
					if(longitude>0) longitude *= (-1);
					break;
				default:
					Logger.e(TAG, "Got unknown direction: "+ew);
					break;
			}
			//return ew;
			return longitude;
		case gps_latitude_ns:
			int ns = FrSkyHub.getUnsignedLE16BitValue(frame);
			//Logger.w(TAG, "Got E/W: "+ew);
			switch(ns){
				case 78: 
					//Logger.w(TAG, "Got: N");
					// need to ensure latitude positive
					if(latitude<0) latitude *= (-1);
					break;
				case 83:
					//Logger.w(TAG, "Got: S");
					// need to endure latitude negative
					if(latitude>0) latitude *= (-1);
					break;
				default:
					Logger.e(TAG, "Got unknown direction: "+ns);
					break;
			}
			//return ns;
			return latitude;
		case gps_latitude_before:
			lat_bp = Math.floor(FrSkyHub.getSignedLE16BitValue(frame)/100.0)*1000000.0;
			lat_tmp = (FrSkyHub.getSignedLE16BitValue(frame)%100.0)*10000.0;
			break;
		case gps_latitude_after:
			lat_ap = FrSkyHub.getUnsignedLE16BitValue(frame);
			latitude = (lat_bp + (lat_tmp+lat_ap)*5/3)/1000000.0;
			//return latitude;
			break;
		
		case gps_course_before:
			course_bp = FrSkyHub.getUnsignedLE16BitValue(frame);
			break;
		case gps_course_after:
			course_ap = FrSkyHub.getUnsignedLE16BitValue(frame)/PRECISION_COURSE;
			course = course_bp+course_ap;
			return course;
		case gps_day_month:
			day = frame[2];
			month = frame[3];
			//Logger.e(TAG, "Day: "+day);
			//Logger.e(TAG, "month: "+month);
			//date.set(Calendar.DAY_OF_MONTH, day);
			//date.set(Calendar.MONTH, month - 1);
			// combine
			break;
			//return date.getTimeInMillis();
		case gps_year:
			year = 2000 + frame[2];
			//Logger.e(TAG, "Year: "+year);
			break;
			//date.set(Calendar.YEAR, year);
			//return date.getTimeInMillis();
		case gps_hour_minute:
			hour = frame[2];
			minute = frame[3];
			//Logger.e(TAG, "Hour: "+hour);
			//Logger.e(TAG, "Minute: "+minute);
			break;
			//date.set(Calendar.HOUR, hour);
			//date.set(Calendar.MINUTE, minute);

			//return date.getTimeInMillis();
		case gps_second:
			second = frame[2];
			//Logger.e(TAG, "Second: "+second);
//			// combine
//			Logger.e(TAG, "---");
//			Logger.e(TAG, "Year: "+year);
//			Logger.e(TAG, "month: "+month);
//			Logger.e(TAG, "Day: "+day);
//			Logger.e(TAG, "Hour: "+hour);
//			Logger.e(TAG, "Minute: "+minute);
//			Logger.e(TAG, "Second: "+second);
			
			
			date.set(Calendar.YEAR, year);
			date.set(Calendar.MONTH, month);
			date.set(Calendar.DAY_OF_MONTH, day);
			date.set(Calendar.HOUR_OF_DAY, hour);
			date.set(Calendar.MINUTE, minute);
			date.set(Calendar.SECOND, second);
			//Logger.e(TAG, "Time in Seconds: "+date.getTimeInMillis()/1000.0);
			//Logger.e(TAG, "Year: "+year+", Month: "+month+", Day: "+day+", Hour: "+hour+", Minutes: "+minute+", Seconds: "+second);
			return date.getTimeInMillis()/1000.0;
			//return date.
			
		}
		
		return Hub.UNDEFINED_VALUE;
	}

}
