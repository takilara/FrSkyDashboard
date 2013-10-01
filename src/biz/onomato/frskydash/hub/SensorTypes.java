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

package biz.onomato.frskydash.hub;

/**
 * possible channels for sensor hub data
 * 
 */
public enum SensorTypes {

	undefined, gps_altitude_before, gps_altitude_after, temp1, rpm, fuel, temp2, volt, altitude_before, altitude_after, gps_speed_before, gps_speed_after, gps_longitude_before, gps_longitude_after, gps_longitude_ew, gps_latitude_before, gps_latitude_after, gps_latitude_ns, gps_course_before, gps_course_after,vertical_speed,

	/**
	 * @deprecated use day_month instead
	 */
	day,
	/**
	 * @deprecated use day_month instead
	 */
	month, gps_year,
	/**
	 * @deprecated use hour_minute instead
	 */
	hour,
	/**
	 * @deprecated use hour_minute instead
	 */
	minute, gps_second, acc_x, acc_y, acc_z,

	// for the new volt sensor we need a voltage value per cell (up to 6 cell)
	CELL_0, CELL_1, CELL_2, CELL_3, CELL_4, CELL_5, // , volt_6;

	// concatenated value for time & date values
	gps_day_month, gps_hour_minute, gps_date,
	
	// FAS-100
	fas_current,fas_voltage_before,fas_voltage_after,
	
	// OpenXVario
	openxvario_vfas_voltage,openxvario_gps_distance,
	
}
