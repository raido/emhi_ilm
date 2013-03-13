package ee.kuli.emhi.ilm;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class WeatherOverlayItem extends OverlayItem {
	private WidgetInstance station;

	public WeatherOverlayItem(GeoPoint point, String title, String snippet, WidgetInstance mStation) {
		super(point, title, snippet);
		station = mStation;
	}
	
	public WidgetInstance getStation() {
		return this.station;
	}
}
