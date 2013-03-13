package ee.kuli.emhi.ilm;

import java.util.Hashtable;
import java.util.List;
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
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class WeatherMap extends MapActivity {
	private EmhiService s;
	private MapView map;
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
		map = (MapView) findViewById(R.id.mapview);
		map.setBuiltInZoomControls(true);
		List<Overlay> mapOverlays =  map.getOverlays();
		int lng = (int) (25.46631f * 1E6);
		int lat = (int) (58.75681f * 1E6);
		
		GeoPoint point = new GeoPoint(lat, lng); 
		MapController controller = map.getController();
		controller.setZoom(8);
		controller.setCenter(point);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		Drawable drawable = getResources().getDrawable(R.drawable.icon);
		itemizedoverlay = new WeatherOverlay(drawable, WeatherMap.this, map);
		mapOverlays.add(itemizedoverlay);
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
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
		showProgressDialog();
		doBindService();
	}
	
	class buildMarkers extends AsyncTask<String, Void, String> {
		private Hashtable<String, WidgetInstance> stations = null;
		
		@Override
		protected void onPreExecute() {
			itemizedoverlay.clear();
		}

		@Override
		protected String doInBackground(String... params) {
			stations = s.getStationPool();
			Set<Entry<String, WidgetInstance>> entries = stations.entrySet();
			WidgetInstance station = null;
			boolean map_fahrenheit = prefs.getBoolean("map_fahrenheit", false);
			boolean map_mmhg = prefs.getBoolean("map_mmhg", false);
	    	
			for(Entry<String, WidgetInstance> entry : entries) {
				station = entry.getValue();
				station.use_fahrenheit = map_fahrenheit;
				station.use_mmhg = map_mmhg;
				
				Location station_loc = ApplicationContext.getStationLocation(station.name);
				if ( station_loc != null ) {
					int lat = (int)(station_loc.getLatitude() * 1E6);    
					int lng = (int)(station_loc.getLongitude() * 1E6);
					GeoPoint point = new GeoPoint(lat, lng);
					WeatherOverlayItem overlayitem = new WeatherOverlayItem(point, station.name, "", station);
					itemizedoverlay.addOverlay(overlayitem);
				} else {
					Log.i("emhi-widget", "Oops, no station location acquired");
				}
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			if(progressDialog != null) {
				progressDialog.dismiss();
			}
			if(stations.size() == 0) {
				noConnectionDialog();
			} else {
				itemizedoverlay.populateNow();	
				map.invalidate();
				if(itemizedoverlay.size() == 0) {
					Toast.makeText(WeatherMap.this, getString(R.string.geocoding_failed), Toast.LENGTH_SHORT).show();
				}
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
