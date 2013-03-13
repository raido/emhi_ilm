package ee.kuli.emhi.ilm;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.readystatesoftware.mapviewballoons.BalloonItemizedOverlay;
import com.readystatesoftware.mapviewballoons.BalloonOverlayView;

public class WeatherOverlay extends BalloonItemizedOverlay<WeatherOverlayItem> {
	private ArrayList<WeatherOverlayItem> mOverlays = new ArrayList<WeatherOverlayItem>();
	private BalloonOverlayView<WeatherOverlayItem> custom_overlay = null;
	private Context mContext;
	
	public WeatherOverlay(Drawable defaultMarker, MapView mapView) {
		super(boundCenterBottom(defaultMarker), mapView);
	}
	
	public WeatherOverlay(Drawable defaultMarker, Context context, MapView mapView) {
		super(defaultMarker, mapView);
		mContext = context;
	}

	@Override
	protected WeatherOverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}
	
	public void addOverlay(WeatherOverlayItem overlay) {
	    mOverlays.add(overlay);
	}
	
	public void populateNow() {
	    populate();
	}
	
	public void clear() {
		mOverlays.clear();
	}

	@Override
	protected BalloonOverlayView<WeatherOverlayItem> createBalloonOverlayView() {
		if ( custom_overlay == null ) {
			custom_overlay = new WeatherBalloonOverlayView<WeatherOverlayItem>(mContext, getBalloonBottomOffset());
		}
		return custom_overlay;
	}

	@Override
	protected boolean onBalloonTap(int index, WeatherOverlayItem item) {
		this.hideBalloon();
		return super.onBalloonTap(index, item);
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		if (shadow == false) {
            //cycle through all overlays
            for (int index = 0; index < mOverlays.size(); index++)
            {
            	WeatherOverlayItem item = mOverlays.get(index);
            	WidgetInstance station = item.getStation();

                // Converts lat/lng-Point to coordinates on the screen
                GeoPoint point = item.getPoint();
                Point ptScreenCoord = new Point() ;
                mapView.getProjection().toPixels(point, ptScreenCoord);

                //Paint
                Paint paint = new Paint();
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setAntiAlias(true);
                
                final float densityMultiplier = mContext.getResources().getDisplayMetrics().density;
                final float scaledPx = 18 * densityMultiplier;
                
                paint.setTextSize(scaledPx);
                paint.setARGB(200, 0, 0, 0); // alpha, r, g, b (Black, semi see-through)
                paint.setShadowLayer(1f, 1f, 1f, 0xFFB5D408);
                //show text to the right of the icon
                canvas.drawText(station.getTemperature(), ptScreenCoord.x, ptScreenCoord.y, paint);
            }
        }
	}

}
