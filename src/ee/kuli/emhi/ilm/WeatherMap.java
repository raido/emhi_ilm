package ee.kuli.emhi.ilm;

import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Set;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.maps.GeoPoint;

public class WeatherMap extends FragmentActivity {
	private EmhiService s;
	private GoogleMap map;
	private ProgressDialog progressDialog;
	private SharedPreferences prefs;
	
	private WeatherOverlay itemizedoverlay;
	
	private static final int MENU_REFRESH_ACTION = Menu.FIRST;
	private static final int MENU_SETTINGS_ACTION = Menu.FIRST + 1;
	
	/**
	 * EmhiService connection
	 */
	
	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			s = ((EmhiService.MyBinder) binder).getService();
			new buildMarkers().execute();
		}

		public void onServiceDisconnected(ComponentName className) {
			s = null;
		}
	};

	
	/**
	 * EmhiService binder method
	 */

	private void doBindService() {
		Intent i = new Intent(this, EmhiService.class);
		bindService(i, mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.map);
		map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		
		/*map = (MapView) findViewById(R.id.map);
		
		
		map.setBuiltInZoomControls(true);
		List<Overlay> mapOverlays =  map.getOverlays();
		int lng = (int) (25.46631f * 1E6);
		int lat = (int) (58.75681f * 1E6);
		
		GeoPoint point = new GeoPoint(lat, lng); 
		MapController controller = map.getController();
		controller.setZoom(8);
		controller.setCenter(point);
		
		Drawable drawable = getResources().getDrawable(R.drawable.icon);
		itemizedoverlay = new WeatherOverlay(drawable, WeatherMap.this, map);
		mapOverlays.add(itemizedoverlay);*/
	}

	@Override
	protected void onPause() {
		super.onPause();
		unbindService(mConnection);
		if(progressDialog != null) {
			progressDialog.dismiss();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		int play_services_status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if ( play_services_status == ConnectionResult.SUCCESS ) {
			showProgressDialog();
			doBindService();
		} else {
			GooglePlayServicesUtil.getErrorDialog(play_services_status, this, 0); //Todo handle request code
		}
	}
	
	class buildMarkers extends AsyncTask<String, Void, Hashtable<String, WidgetInstance>> {
		private Hashtable<String, WidgetInstance> stations = null;
		
		@Override
		protected void onPreExecute() {
			//itemizedoverlay.clear();
		}

		@Override
		protected Hashtable<String, WidgetInstance> doInBackground(String... params) {
			return s.getStationPool();
		}
		
		@Override
		protected void onPostExecute(Hashtable<String, WidgetInstance> result) {
			
			Set<Entry<String, WidgetInstance>> entries = result.entrySet();
			WidgetInstance station = null;
			boolean map_fahrenheit = prefs.getBoolean("map_fahrenheit", false);
			boolean map_mmhg = prefs.getBoolean("map_mmhg", false);
	    	
			for(Entry<String, WidgetInstance> entry : entries) {
				station = entry.getValue();
				station.use_fahrenheit = map_fahrenheit;
				station.use_mmhg = map_mmhg;
				
				Location station_loc = ApplicationContext.getStationLocation(station.name);
				if ( station_loc != null ) {
					LatLng latlng = new LatLng(station_loc.getLatitude(), station_loc.getLongitude());
					
					Bitmap.Config conf = Bitmap.Config.ARGB_8888; 
					Bitmap bmp = Bitmap.createBitmap(75, 50, conf); 
					Canvas canvas = new Canvas(bmp);
					
					Paint paint = new Paint();
					paint.setTextAlign(Paint.Align.CENTER);
					paint.setAntiAlias(true);

					final float densityMultiplier = getResources()
							.getDisplayMetrics().density;
					final float scaledPx = 18 * densityMultiplier;

					paint.setTextSize(scaledPx);
					paint.setARGB(200, 0, 0, 0); // alpha, r, g, b (Black, semi
													// see-through)
					paint.setShadowLayer(1f, 1f, 1f, 0xFFB5D408);
					// show text to the right of the icon
					canvas.drawText(station.getTemperature(), bmp.getWidth() / 2, 50, paint);
					
					map.addMarker(new MarkerOptions()
                    .position(latlng)
                    .title(station.name)
                    .snippet("Population: 4,137,400")
                    .icon(BitmapDescriptorFactory.fromBitmap(bmp)));
				} else {
					Log.i("emhi-widget", "Oops, no station location acquired");
				}
			}
			
			
			if(progressDialog != null) {
				progressDialog.dismiss();
			}
		}
		
	}
	
	private void showProgressDialog() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setMessage(getText(R.string.loading));
			progressDialog.setOnCancelListener(new OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					finish();
				}
			});
		}
		if(!isFinishing()) {
			progressDialog.show();
		}
	}
	
	/**
	 * Display dialog if no coarse location is enabled
	 */
	
	private void noConnectionDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getText(R.string.check_connectivity))
               .setCancelable(false)
               .setPositiveButton(getText(R.string.close), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        finish();
                   }
               }).setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.check_connectivity);
        AlertDialog alert = builder.create();
        alert.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_REFRESH_ACTION, 0, getString(R.string.refreshBtn)).setIcon(android.R.drawable.ic_popup_sync);
		menu.add(0, MENU_SETTINGS_ACTION, 0, getString(R.string.configBtn)).setIcon(android.R.drawable.ic_menu_preferences);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case MENU_REFRESH_ACTION:
				showProgressDialog();
				new buildMarkers().execute();
				break;
			case MENU_SETTINGS_ACTION:
				startActivity(new Intent(this, MapPreferences.class));
				break;
		}
		return super.onOptionsItemSelected(item);
	}

}
