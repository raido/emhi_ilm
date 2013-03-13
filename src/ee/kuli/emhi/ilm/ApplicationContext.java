package ee.kuli.emhi.ilm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Application;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.location.Location;

public class ApplicationContext extends Application {
	private static ApplicationContext instance;
	private static final HashMap<String, Location> stations = new HashMap<String, Location>();
	private static final ArrayList<String> station_list = new ArrayList<String>();
	
	public static final String RECONFIGURE_WIDGET_ACTION = "widgetReConf";

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;

		try {
			XmlResourceParser raw = this.getResources().getXml(R.xml.stations);
			raw.next();
			int eventType = raw.getEventType();
			
			String current_tag = "";
			String current_station = "";
			double lat = 0D;
			double lng = 0D;
			
			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG) {
					current_tag = raw.getName();
				} else if (eventType == XmlPullParser.END_TAG) {
					if (raw.getName().equals("station")) {
						Location loc = new Location("");
						loc.setLatitude(lat);
						loc.setLongitude(lng);
						//Finally add station
						stations.put(current_station, loc);
						station_list.add(current_station);
					}
				} else if (eventType == XmlPullParser.TEXT) {
					if ( current_tag.equals("name") ) {
						current_station = raw.getText();
					}
					if ( current_tag.equals("lat") ) {
						lat = Double.parseDouble(raw.getText());
					}
					if ( current_tag.equals("lng") ) {
						lng = Double.parseDouble(raw.getText());
					}
				}
				eventType = raw.next();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static Context getContext() {
		return instance;
	}
	
	public static ArrayList<String> getStationList() {
		Collections.sort(station_list);
		return station_list;
	}
	
	public static Location getStationLocation(String name) {
		return stations.get(name);
	}

}
