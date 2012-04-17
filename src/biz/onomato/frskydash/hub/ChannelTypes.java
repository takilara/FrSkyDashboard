package biz.onomato.frskydash.hub;

/**
 * possible channels for sensor hub data
 * 
 */
public enum ChannelTypes {

	undefined, gps_altitude_before, gps_altitude_after, temp1, rpm, fuel, temp2, volt, altitude_before, altitude_after, gps_speed_before, gps_speed_after, longitude_before, longitude_after, ew, latitude_before, latitude_after, ns, course_before, course_after, day, month, year, hour, minute, second, acc_x, acc_y, acc_z,
	// for the new volt sensor we need a voltage value per cell (up to 6 cell)
	volt_1, volt_2, volt_3, volt_4, volt_5, volt_6;

}