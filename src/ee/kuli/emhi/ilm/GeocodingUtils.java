package ee.kuli.emhi.ilm;

import java.util.ArrayList;

import android.location.Location;

public class GeocodingUtils {
	
	
	/**
	 * Calculate distances between stations, return the closest station name
	 * 
	 * @param context
	 * @param stations
	 * @param location
	 * @param gpsStationProgress 
	 * @return String closest station name
	 */
	
	public static String getClosestStation(final Location location) {
		int last_distance = 0;
		String new_station = null;
		ArrayList<String> stations = ApplicationContext.getStationList();
		
		if(location != null && stations.size() > 1) {
			for(String station : stations) {
				Location station_location = ApplicationContext.getStationLocation(station);
				if(station_location != null) {
					int distanceTo = (int) location.distanceTo(station_location);
					if(last_distance == 0 || last_distance > distanceTo) {
						last_distance = distanceTo;
						new_station = station;
					}
				}
			}
		}
		return new_station;
	}	
}
