package ee.kuli.emhi.ilm;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;

public class WeatherOverlay extends ItemizedOverlay<WeatherOverlayItem> {
	private ArrayList<WeatherOverlayItem> mOverlays = new ArrayList<WeatherOverlayItem>();
	private Context mContext;

	public WeatherOverlay(Drawable defaultMarker, MapView mapView) {
		super(boundCenterBottom(defaultMarker));
	}

	public WeatherOverlay(Drawable defaultMarker, Context context,
			MapView mapView) {
		super(defaultMarker);
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
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		if (shadow == false) {
			// cycle through all overlays
			for (int index = 0; index < mOverlays.size(); index++) {
				WeatherOverlayItem item = mOverlays.get(index);
				WidgetInstance station = item.getStation();

				// Converts lat/lng-Point to coordinates on the screen
				GeoPoint point = item.getPoint();
				Point ptScreenCoord = new Point();
				mapView.getProjection().toPixels(point, ptScreenCoord);

				// Paint
				Paint paint = new Paint();
				paint.setTextAlign(Paint.Align.CENTER);
				paint.setAntiAlias(true);

				final float densityMultiplier = mContext.getResources()
						.getDisplayMetrics().density;
				final float scaledPx = 18 * densityMultiplier;

				paint.setTextSize(scaledPx);
				paint.setARGB(200, 0, 0, 0); // alpha, r, g, b (Black, semi
												// see-through)
				paint.setShadowLayer(1f, 1f, 1f, 0xFFB5D408);
				// show text to the right of the icon
				canvas.drawText(station.getTemperature(), ptScreenCoord.x,
						ptScreenCoord.y, paint);
			}
		}
	}

	@Override
	protected boolean onTap(int index) {
		WeatherOverlayItem item = mOverlays.get(index);
		if (item != null) {
			AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
			WidgetInstance station = item.getStation();

			dialog.setTitle(item.getTitle());

			int icon = station.getWeatherIcon();

			LayoutInflater inf = (LayoutInflater) mContext
					.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
			LinearLayout l = (LinearLayout) inf.inflate(R.layout.map_popup,
					null);
			if (icon == 0) {
				icon = R.drawable.weather_placeholder;
			}

			ImageView iconView = (ImageView) l.findViewById(R.id.map_popup_img);
			iconView.setImageResource(icon);

			TextView temp = (TextView) l.findViewById(R.id.map_popup_temp);
			temp.setText(station.getTemperature());

			TextView wind = (TextView) l.findViewById(R.id.map_popup_wind);
			wind.setText(station.getWindSpeed());

			TextView feels_like = (TextView) l
					.findViewById(R.id.map_popup_feels_like);
			feels_like.setText(station.getFeelsLikeTemperature());

			int windDegrees = station.getWindDirection();

			Bitmap bmpOriginal = BitmapFactory.decodeResource(
					mContext.getResources(), R.drawable.wind);
			Bitmap bmResult = Bitmap.createBitmap(bmpOriginal.getWidth(),
					bmpOriginal.getHeight(), Bitmap.Config.ARGB_8888);
			Canvas tempCanvas = new Canvas(bmResult);
			tempCanvas.rotate(windDegrees, bmpOriginal.getWidth() / 2,
					bmpOriginal.getHeight() / 2);
			tempCanvas.drawBitmap(bmpOriginal, 0, 0, null);

			ImageView wind_icon = (ImageView) l
					.findViewById(R.id.map_wind_icon);
			wind_icon.setImageBitmap(bmResult);

			TextView humidity = (TextView) l
					.findViewById(R.id.map_popup_humidity);
			humidity.setText(station.getHumidity());

			TextView airpressure = (TextView) l
					.findViewById(R.id.map_popup_airpressure);
			airpressure.setText(station.getAirpressure());

			// dialog.setMessage(item.getSnippet());
			dialog.setView(l);
			dialog.setNeutralButton(mContext.getString(R.string.close),
					new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			dialog.show();
			return true;
		}
		return false;
	}

}
