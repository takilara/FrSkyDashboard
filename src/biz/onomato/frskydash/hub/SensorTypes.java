package biz.onomato.frskydash.hub;

/**
 * possible channels for sensor hub data
 * 
 */
public enum SensorTypes {

	undefined, gps_altitude_before, gps_altitude_after, temp1, rpm, fuel, temp2, volt, altitude_before, altitude_after, gps_speed_before, gps_speed_after, longitude_before, longitude_after, ew, latitude_before, latitude_after, ns, course_before, course_after,

	/**
	 * @deprecated use day_month instead
	 */
	day,
	/**
	 * @deprecated use day_month instead
	 */
	month, year,
	/**
	 * @deprecated use hour_minute instead
	 */
	hour,
	/**
	 * @deprecated use hour_minute instead
	 */
	minute, second, acc_x, acc_y, acc_z,

	// for the new volt sensor we need a voltage value per cell (up to 6 cell)
	CELL_0, CELL_1, CELL_2, CELL_3, CELL_4, CELL_5, // , volt_6;

	// concatenated value for time & date values
	day_month, hour_minute, date

}
