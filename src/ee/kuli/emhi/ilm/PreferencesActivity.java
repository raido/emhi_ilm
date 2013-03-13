package ee.kuli.emhi.ilm;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.widget.RemoteViews;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener,OnPreferenceClickListener {

	private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private boolean WIDGET_RECONFIGURE = false;
	private ProgressDialog gpsStationProgress = null;
	
	private ListPreference stations_list, background;
	private CheckBoxPreference use_gps, use_fahrenheit, glaze_warning, use_mmhg, feels_like;
	private Preference send_email, view_tutorial;
	
	private LocationManager locationManager;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		addPreferencesFromResource(R.xml.prefs);
		
		stations_list = (ListPreference) findPreference("station");
		background = (ListPreference) findPreference("background_transparency");
		
		use_gps = (CheckBoxPreference) findPreference("use_gps");
		use_fahrenheit = (CheckBoxPreference) findPreference("use_fahrenheit");
		glaze_warning = (CheckBoxPreference) findPreference("glaze_warning");
		use_mmhg = (CheckBoxPreference) findPreference("use_mmhg");
		feels_like = (CheckBoxPreference) findPreference("display_windchill");
		send_email = (Preference) findPreference("send_email");
		send_email.setOnPreferenceClickListener(this);
		view_tutorial = (Preference) findPreference("view_tutorial");
		view_tutorial.setOnPreferenceClickListener(this);
		
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
			appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			if(intent.getAction() != null && intent.getAction().equals(ApplicationContext.RECONFIGURE_WIDGET_ACTION)) {
				WIDGET_RECONFIGURE = true;
				restoreInstanceReconfigure(false);
			}
			if ( appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				RemoteViews views = EmhiService.getRemoteViews(appWidgetId);
				if ( views != null ) {
					if ( views.getLayoutId() == R.layout.tiny_widget) {
						use_mmhg.setEnabled(false);
						feels_like.setEnabled(false);
					}
					if (views.getLayoutId() == R.layout.small_widget) {
						use_mmhg.setEnabled(false);
					}
				}
			}
		}
		
		setConfigResult(Activity.RESULT_CANCELED);
	}
	
	public void restoreInstanceReconfigure(boolean setNewName) {
		final Database db = Database.getInstance();
		WidgetInstance widget = db.getStation(appWidgetId);
		
		if(widget != null) {
			if(setNewName) {
				stations_list.setValue(widget.name);
			} else {
				use_gps.setChecked(widget.use_gps);
				use_fahrenheit.setChecked(widget.use_fahrenheit);
				use_mmhg.setChecked(widget.use_mmhg);
				feels_like.setChecked(widget.getFeelsLike());
				glaze_warning.setChecked(widget.glaze_warnings == 1 ? true : false);
			}
		}
		if(widget != null) {
			use_gps.setChecked(false);
			use_gps.setChecked(widget.use_gps);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(gpsStationProgress == null) {
			gpsStationProgress = new ProgressDialog(this);
			gpsStationProgress.setMessage(getText(R.string.gps_station_search));
			gpsStationProgress.setOnCancelListener(new OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					use_gps.setChecked(false);
				}
			});
		}
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
		
		ArrayList<String> stations = ApplicationContext.getStationList();
		CharSequence[] cs = stations.toArray(new CharSequence[stations.size()]);
		
		stations_list.setEntries(cs);
		stations_list.setEntryValues(cs);
		
		if(stations.size() <= 1) {
	        if(!isFinishing()) {
	        	noConnectivityDialog();
	        }
		} else {
			if(WIDGET_RECONFIGURE) { //If reconfiguring old  instance name 
				restoreInstanceReconfigure(true);
			}
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if(!prefs.getBoolean("tutorial_done", false)) {
			Intent tutorial = new Intent(getApplicationContext(), WeatherTutorial.class);
			startActivity(tutorial);
		}
	}


	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if(gpsStationProgress != null) {
			gpsStationProgress.dismiss();
			gpsStationProgress = null;
		}
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.remove("station");
		editor.remove("use_gps");
		editor.remove("use_fahrenheit");
		editor.remove("glaze_warning");
		editor.remove("use_mmhg");
		editor.remove("display_windchill");
		editor.commit();
	}
	
	
	/**
	 * This method is for Android 1.6 support
	 */
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR) {
			onBackPressed();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		if(stations_list.getValue() != null && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
			final Database db = Database.getInstance();
			if(!WIDGET_RECONFIGURE) {
				db.addWidgetInstance(appWidgetId, stations_list.getValue(), use_gps.isChecked(), use_fahrenheit.isChecked(), glaze_warning.isChecked(), use_mmhg.isChecked(), feels_like.isChecked());
			} else {
				WidgetInstance widget = db.getStation(appWidgetId);
				if(widget != null) {
					widget.name = stations_list.getValue();
					db.updateLast(appWidgetId, widget);
					db.updateName(appWidgetId, widget.name);
					//Update new settings
					db.setUseGps(appWidgetId, use_gps.isChecked());
					db.setUseFahrenheit(appWidgetId, use_fahrenheit.isChecked());
					db.setGlazeWarnings(appWidgetId, glaze_warning.isChecked());
					db.setUseMMHG(appWidgetId, use_mmhg.isChecked());
					db.setFeelsLike(appWidgetId, feels_like.isChecked());
				}
			}
			setConfigResult(Activity.RESULT_OK);
		}
		Intent i = new Intent(this, EmhiService.class);
		i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { appWidgetId });
		startService(i);
		finish();
	}

	/**
	 * Activity setResult wrapper method
	 * 
	 * @param result
	 */
	
	public void setConfigResult(int result) {
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(result, resultValue);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		
		if(key.equals(background.getKey())) {
			int[] appWidgetIds = Database.getInstance().getAppWidgetIds();
			if ( appWidgetIds.length > 0 ) {
				RemoteViews views = null;
				AppWidgetManager manager = AppWidgetManager.getInstance(getApplicationContext());
				for(int appWidgetId : appWidgetIds) {
					views = EmhiService.getRemoteViews(appWidgetId);
					if ( views != null ) {
						views.setImageViewResource(R.id.bg_image, EmhiService.getBackground());
						manager.updateAppWidget(appWidgetId, views);
					}
				}
			}
		}
		
		if(key.equals(use_gps.getKey())) {
			String provider = getLocationProvider();
			
			if(provider == null && use_gps.isChecked()) {
				use_gps.setChecked(false);
		        if(!isFinishing()) {
		        	noCoarseLocationDialog();
		        }
			}
			if(use_gps.isChecked() && provider != null) {
				stations_list.setEnabled(false);
				use_gps.setSummaryOn(R.string.gps_station_search);
				if(gpsStationProgress != null) {
					gpsStationProgress.show();
				}
				WeatherLocationListener locationListener = new WeatherLocationListener() {

					@Override
					public void onLocationChanged(Location location) {
						new findStationNearBy().execute(location);
						locationManager.removeUpdates(this);
					}
					
				};
				locationManager.requestLocationUpdates(provider, 0, 0, locationListener);
			} else {
				stations_list.setEnabled(true);
			}
		}
		
		if(key.equals("glaze_warning")) {
			if(glaze_warning.isChecked()) {
				PendingIntent pendingIntent = PendingIntent.getActivity(this, appWidgetId, new Intent(), PendingIntent.FLAG_ONE_SHOT);
				WidgetInstance demoNotification = new WidgetInstance();
				demoNotification.name = "Tallinn";
				demoNotification.airtemp = "-0.5";
				EmhiService.glazeNotification(appWidgetId, demoNotification, pendingIntent);
				EmhiService.sound_vibrated_added = false;
			}
		}
	}
	
	/**
	 * Get location provider for station search
	 * @return String provider
	 */
	
	private String getLocationProvider() {
		Criteria location_criteria = new Criteria();
		location_criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		return locationManager.getBestProvider(location_criteria, true);
	}
	
	/**
	 * Find nearby station using current location
	 * @param location
	 * @param gpsStationProgress2 
	 */
	
	class findStationNearBy extends AsyncTask<Location, Void, String> {	
			@Override
			protected String doInBackground(Location... params) {
				return GeocodingUtils.getClosestStation(params[0]);
			}

			@Override
			protected void onPostExecute(String station) {
				use_gps.setSummaryOn(getText(R.string.nearest_weather_station)+" - "+station);
				stations_list.setValue(station);
				if(gpsStationProgress != null) {
					gpsStationProgress.dismiss();
				}
			}
	}
	
	/**
	 * Display dialog if no coarse location is enabled
	 */
	
	private void noCoarseLocationDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getText(R.string.enable_coarse_location))
               .setCancelable(false)
               .setPositiveButton(getText(R.string.yes), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                         //Sent user to GPS settings screen
                         dialog.dismiss();
                         startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                   }
               })
               .setNegativeButton(getText(R.string.no), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                   }
               });
        AlertDialog alert = builder.create();
        alert.show();
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if(preference.getKey().equals(send_email.getKey())) {
			String[] toEmails = { "raido357@gmail.com" };
			Intent emailiIntent = new Intent(android.content.Intent.ACTION_SEND);
			emailiIntent.putExtra(android.content.Intent.EXTRA_EMAIL, toEmails);
			emailiIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getText(R.string.feedback)+" - "+getText(R.string.app_name));
			emailiIntent.setType("message/rfc822");
			startActivity(Intent.createChooser(emailiIntent, getText(R.string.choose_email_app)));
			finish();
			return true;
		}
		
		if(preference.getKey().equals(view_tutorial.getKey())) {
			Intent tutorial = new Intent(ApplicationContext.getContext(), WeatherTutorial.class);
			startActivity(tutorial);
			return true;
		}
		return false;
	}
	
	/**
	 * Display dialog if no coarse location is enabled
	 */
	
	private void noConnectivityDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getText(R.string.check_connectivity))
               .setCancelable(false)
               .setPositiveButton(getText(R.string.close), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                 		Intent i = new Intent(ApplicationContext.getContext(), EmhiService.class);
                		stopService(i);
                        finish();
                   }
               }).setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.check_connectivity);
        AlertDialog alert = builder.create();
        alert.show();
	}

}
