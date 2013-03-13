/***
 * Copyright (c) 2011 readyState Software Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package ee.kuli.emhi.ilm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.maps.OverlayItem;
import com.readystatesoftware.mapviewballoons.BalloonOverlayView;

public class WeatherBalloonOverlayView<Item extends OverlayItem> extends BalloonOverlayView<WeatherOverlayItem> {

	private TextView title;
	private TextView temp;
	private TextView wind;
	private TextView airpressure;
	private TextView humidity;
	private ImageView image;
	
	public WeatherBalloonOverlayView(Context context, int balloonBottomOffset) {
		super(context, balloonBottomOffset);
	}
	
	@Override
	protected void setupView(Context context, final ViewGroup parent) {
		
		// inflate our custom layout into parent
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.map_popup, parent);
		
		// setup our fields
		title = (TextView) v.findViewById(R.id.balloon_item_title);
		temp = (TextView) v.findViewById(R.id.map_popup_temp);
		
		airpressure = (TextView) v.findViewById(R.id.map_popup_airpressure);
		humidity = (TextView) v.findViewById(R.id.map_popup_humidity);
		
		wind = (TextView) v.findViewById(R.id.map_popup_wind);
		
		image = (ImageView) v.findViewById(R.id.balloon_item_image);

	}

	@Override
	protected void setBalloonData(WeatherOverlayItem item, ViewGroup parent) {
		
		WidgetInstance station = item.getStation();
		// map our custom item data to fields
		title.setText(item.getTitle());
		temp.setText(station.getTemperature()+" / "+station.getFeelsLikeTemperature());
		
		humidity.setText(station.getHumidity());
		airpressure.setText(station.getAirpressure());
		wind.setText(station.getWindSpeed());
		
		int windDegrees = station.getWindDirection();
		
		Bitmap bmpOriginal = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.wind);
		Bitmap bmResult = Bitmap.createBitmap(bmpOriginal.getWidth(), bmpOriginal.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas tempCanvas = new Canvas(bmResult); 
		tempCanvas.rotate(windDegrees, bmpOriginal.getWidth()/2, bmpOriginal.getHeight()/2);
		tempCanvas.drawBitmap(bmpOriginal, 0, 0, null);
		
		BitmapDrawable result = new BitmapDrawable(getContext().getResources(), bmResult);
		wind.setCompoundDrawablesWithIntrinsicBounds(null, null, result, null);
		
		// get remote image from network.
		// bitmap results would normally be cached, but this is good enough for demo purpose.
		int icon = station.getWeatherIcon();
		if ( icon > 0 ) {
			image.setImageResource( icon );
		} else {
			image.setImageResource(R.drawable.weather_placeholder);
		}
	}
}
