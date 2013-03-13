package ee.kuli.emhi.ilm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.xml.sax.Attributes;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

public class EmhiService extends Service {

	private final Hashtable<Integer, UpdateThread> threadPool = new Hashtable<Integer, UpdateThread>();
	private final Hashtable<String, WidgetInstance> stationPool = new Hashtable<String, WidgetInstance>();
	public static final String MANUAL_UPDATE = "manual-widget-update";
	private final IBinder serviceBind = new MyBinder();
	private CountDownLatch latch = new CountDownLatch(2);
	private CountDownLatch locationLatch = new CountDownLatch(1);
	private LocationManager locationManager;
	
	private Location new_location = null;
	
	private boolean manual_update = false;
	private final int TECHNICAL_PROBLEMS = 1;
	private WeatherLocationListener locListener = null;
	
	
	private Handler messageHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case TECHNICAL_PROBLEMS:
				Toast.makeText(ApplicationContext.getContext(), getText(R.string.app_name)+" - "+getText(R.string.technical_difficulties), Toast.LENGTH_LONG).show();
				break;
			default:
				super.handleMessage(msg);
			}
		}
		
	};
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		sound_vibrated_added = false;
	}

	/**
	 * Asynchronous XML retrieval
	 * 
	 * @author raido
	 *
	 */
	
	class prefetchXML extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			prefetchStationsInfo();
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			latch.countDown();
		}

		@Override
		protected void onPreExecute() {
			latch.countDown();
		}
		
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent != null) {
			if ( locationManager == null ) {
				locationManager = (LocationManager) getSystemService(Service.LOCATION_SERVICE);
			}
			if ( locationManager != null ) {
				String provider = getLocationProvider();
				if ( provider != null ) {
					 locListener = new WeatherLocationListener() {

						@Override
						public void onLocationChanged(Location location) {
							locationLatch.countDown();
							new_location = location;
							locationManager.removeUpdates(this);
						}
						
					};
					locationManager.requestLocationUpdates(provider, 0, 0, locListener);
				} else {
					locationLatch.countDown();
				}
			}
			updateWidgetInstances(intent);
		}
		return START_REDELIVER_INTENT;
	}
	
	public void startXMLPrefetch() {
		if(latch.getCount() > 1) {
			new prefetchXML().execute();
		}
	}
	
	/**
	 * This method inits widget instances update
	 * 
	 * @param intent
	 */
	
	public void updateWidgetInstances(Intent intent) {
		final Bundle extras = intent.getExtras();
		if (extras != null) {
			manual_update = extras.getBoolean(MANUAL_UPDATE);
			startXMLPrefetch();
			final int[] appWidgetIds = extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS);
			UpdateThread thread = null;
			Database db = Database.getInstance();
			for (Integer appWidgetId : appWidgetIds) {
				if (!threadPool.containsKey(appWidgetId) && db.isConfigured(appWidgetId)) { // If thread not running
					thread = new UpdateThread(appWidgetId);
					threadPool.put(appWidgetId, thread);
					thread.execute();
				}
			}
		}
	}

	/**
	 * UpdateThread class used for each widget instance update
	 * 
	 * @author Raido Kuli
	 */
	
	class UpdateThread extends AsyncTask<String, Void, String> {
		private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
		private RemoteViews views = null;

		public UpdateThread(int _appWidgetId) {
			appWidgetId = _appWidgetId;
		}

		@Override
		protected void onPreExecute() {
			if ( appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID ) {
				views = getRemoteViews(appWidgetId);
			}
			if ( views != null ) {
				final AppWidgetManager manager = AppWidgetManager.getInstance(getApplicationContext());	
				// Set refresh display
				if ( views.getLayoutId() != R.layout.tiny_widget) {
					views.setTextViewText(R.id.station_name, getString(R.string.reloading));
				}
				manager.updateAppWidget(appWidgetId, views);
			}
		}

		@Override
		protected String doInBackground(String... params) {
			
			//If we didnt get RemoteViews for some reason, bail
			if(views != null) {
		
				WidgetInstance widget = Database.getInstance().getStation(appWidgetId);
				if ( widget != null && widget.use_gps ) {
					try {
						locationLatch.await();
					} catch (InterruptedException e) { }
					
					if (new_location == null) {
						String provider = getLocationProvider();
						if ( provider != null ) {
							new_location = locationManager.getLastKnownLocation(provider);
						}
					} 
				}
				widget = null;
				
				try {
					latch.await();
				} catch(InterruptedException e) {

				}
				//Init update, after getStationsList latch count down reaches zero
				updateWidgetInstance(appWidgetId, new_location);
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			//Remove thread from the pool
			threadPool.remove(appWidgetId);
			
			//If no threads left, shutdown the service
			if (threadPool.size() == 0) {
				stopSelf();
				if (locListener != null ) {
					locationManager.removeUpdates(locListener);
				}
			}
		}
	}
	
	/**
	 * Update widget instance RemoteViews
	 * 
	 * @param appWidgetId
	 * @param stations
	 * @param new_location 
	 * @param db_instance
	 */
	
	public void updateWidgetInstance(int appWidgetId, Location new_location) {
		final Database db = Database.getInstance();
		WidgetInstance widget = db.getStation(appWidgetId);
		if(widget != null) {	
			final Context context = ApplicationContext.getContext();
			final RemoteViews views = getRemoteViews(appWidgetId);
			final AppWidgetManager manager = AppWidgetManager.getInstance(context);
			
			if(widget.use_gps && new_location != null) {
				String new_station = GeocodingUtils.getClosestStation(new_location);
				if(new_station != null) {
					widget.name = new_station;
				}
			}
			
			Double last_update_airtemp = widget.getRawTemperature();
			widget = updateWeatherInfo(widget); //if not found  for example connection issue, returns old station object
			
			views.setImageViewResource(R.id.bg_image, getBackground());
			if ( !getWidgetProvider(appWidgetId).equals(".WeatherProviderTiny") ) {
				views.setTextViewText(R.id.station_name, widget.name);
			}
			int icon = widget.getWeatherIcon();
			
			if ( views.getLayoutId() == R.layout.tiny_widget && icon == 0) {
				views.setTextViewText(R.id.temperature_center, widget.getTemperature());
				views.setViewVisibility(R.id.temperature, View.GONE);
				views.setViewVisibility(R.id.temperature_center, View.VISIBLE);
			} else {
				if ( views.getLayoutId() == R.layout.tiny_widget) {
					views.setViewVisibility(R.id.temperature, View.VISIBLE);
					views.setViewVisibility(R.id.temperature_center, View.GONE);
				}
				views.setTextViewText(R.id.temperature, widget.getTemperature());
			}
			
			
			if (icon == 0) {
				views.setViewVisibility(R.id.weather_icon, View.INVISIBLE);
			} else {	
				views.setImageViewResource(R.id.weather_icon, icon);
				views.setViewVisibility(R.id.weather_icon, View.VISIBLE);
			}
			
			if(getWidgetProvider(appWidgetId).equals(".WeatherProviderMedium")) {
				int windDegrees = widget.getWindDirection();
				
				Bitmap bmpOriginal = BitmapFactory.decodeResource(context.getResources(), R.drawable.wind);
				Bitmap bmResult = Bitmap.createBitmap(bmpOriginal.getWidth(), bmpOriginal.getHeight(), Bitmap.Config.ARGB_8888);
				Canvas tempCanvas = new Canvas(bmResult); 
				tempCanvas.rotate(windDegrees, bmpOriginal.getWidth()/2, bmpOriginal.getHeight()/2);
				tempCanvas.drawBitmap(bmpOriginal, 0, 0, null);
				views.setImageViewBitmap(R.id.wind_icon, bmResult);
				
				views.setTextViewText(R.id.wind_speed, widget.getWindSpeed());
				views.setTextViewText(R.id.humidity, widget.getHumidity());
				views.setTextViewText(R.id.airpressure, widget.getAirpressure());
			}
			 
			if(widget.use_gps) {
				views.setViewVisibility(R.id.gps_icon, View.VISIBLE);
			} else {
				views.setViewVisibility(R.id.gps_icon, View.GONE);
			}
			if(widget.glaze_warnings == 1) {
				views.setViewVisibility(R.id.glaze_icon, View.VISIBLE);
			} else {
				views.setViewVisibility(R.id.glaze_icon, View.GONE);
			}
			
			if (views.getLayoutId() != R.layout.tiny_widget) {
				if(widget.getFeelsLike()) {
					views.setTextViewText(R.id.feels_like_temp, widget.getFeelsLikeTemperature());
					views.setViewVisibility(R.id.feels_like_temp, View.VISIBLE);
				} else {
					views.setViewVisibility(R.id.feels_like_temp, View.GONE);
				}
			}
			
			views.setTextViewText(R.id.last_update_time, widget.getUpdateTime());
	
			//Update last values in DB
			db.updateLast(appWidgetId, widget);
			db.updateName(appWidgetId, widget.name);
			
			Intent intent = new Intent(context, EmhiService.class);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { appWidgetId });
			intent.putExtra(EmhiService.MANUAL_UPDATE, true);
			
	        PendingIntent pendingIntent = PendingIntent.getService(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.widget_content_wrapper, pendingIntent);
			
			Intent configIntent = new Intent(context, PreferencesActivity.class);
			configIntent.setAction(ApplicationContext.RECONFIGURE_WIDGET_ACTION);
			configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			
			PendingIntent pendingIntentConf = PendingIntent.getActivity(context, appWidgetId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.configureBtn, pendingIntentConf);	
			
			Double new_airtemp = widget.getRawTemperature();
			if(widget.glaze_warnings == 1) {
				if(last_update_airtemp > 0.5 && new_airtemp <= 0.5) { //If last update airtemp was above zero degrees and new airtemp is not then display
					glazeNotification(appWidgetId, widget, pendingIntent);
				}
			} 
			// Push update for this widget to the home screen
			manager.updateAppWidget(appWidgetId, views);
		}
	}
	
	public static boolean sound_vibrated_added = false;
	
	/**
	 * Display notification for temperature below zero
	 */

	public static void glazeNotification(int appWidgetId, WidgetInstance station, PendingIntent pendingIntent) {
		final Context context = ApplicationContext.getContext();
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
		
		CharSequence tickerText = station.name+" - "+context.getText(R.string.glaze_warning_short);
		
		Notification notification = new Notification(R.drawable.icon_snowflake, tickerText, System.currentTimeMillis());
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		if (!sound_vibrated_added ) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			notification.sound = Uri.parse(prefs.getString("ringtone", ""));
			if(prefs.getBoolean("vibrate", false)) {
				notification.defaults |= Notification.DEFAULT_VIBRATE;
			}
			sound_vibrated_added = true;
		}
		
		CharSequence contentTitle = station.name + " \u00BB "+station.getTemperature()+" \u00AB";
		CharSequence contentText = context.getText(R.string.glaze_warning_text)+" "+station.getTemperature();
		notification.setLatestEventInfo(context, contentTitle, contentText, pendingIntent);
		//push notificaton
		mNotificationManager.notify(appWidgetId, notification);
	}
	
	public static String getWidgetProvider(int appWidgetId) {
		if(appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
			final AppWidgetManager manager = AppWidgetManager.getInstance(ApplicationContext.getContext());
			final AppWidgetProviderInfo managerInfo = manager.getAppWidgetInfo(appWidgetId);
			if(managerInfo != null) {
				ComponentName widgetProvider = managerInfo.provider;
				return widgetProvider.getShortClassName();
			}
		}
		return null;
	}
	
	public static RemoteViews getRemoteViews(int appWidgetId) {
		RemoteViews views = null;
		String widgetProvider = getWidgetProvider(appWidgetId);
		
		if(!StringUtils.isEmpty(widgetProvider)) {
			if(widgetProvider.equals(".WeatherProvider")) {
				views = new RemoteViews(ApplicationContext.getContext().getPackageName(), R.layout.small_widget);
			} else if(widgetProvider.equals(".WeatherProviderTiny")) { 
				views = new RemoteViews(ApplicationContext.getContext().getPackageName(), R.layout.tiny_widget);
			} else {
				views = new RemoteViews(ApplicationContext.getContext().getPackageName(), R.layout.widget);
			}
		}
		return views;
	}
	
	public static int getBackground() {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationContext.getContext());
		//Set background according to settings
		String background_num = prefs.getString("background_transparency", "25");
		if ( background_num.equals("100") ) {
			return R.drawable.widget_bg_0;
		} else { 
			if ( background_num.equals("75") ) {
				return R.drawable.widget_bg_25;
			}
			if ( background_num.equals("50") ) {
				return R.drawable.widget_bg_50;
			}
			if ( background_num.equals("25") ) {
				return R.drawable.widget_bg_75;
			}
			if ( background_num.equals("0") ) {
				return R.drawable.widget_bg;
			}
		}
		return R.drawable.widget_bg;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return serviceBind;
	}
	
	public WidgetInstance updateWeatherInfo(WidgetInstance station_instance) {

		String key = station_instance.name;
		if (stationPool.containsKey(key)) {
			station_instance.airtemp = stationPool.get(key).airtemp;
			station_instance.phenomenon = stationPool.get(key).phenomenon;
			station_instance.windSpeed = stationPool.get(key).windSpeed;
			station_instance.windSpeedMax = stationPool.get(key).windSpeedMax;
			station_instance.winddirection = stationPool.get(key).winddirection;
			station_instance.humidity = stationPool.get(key).humidity;
			station_instance.airpressure = stationPool.get(key).airpressure;
			station_instance.last_update = stationPool.get(key).last_update;
		}
		//Fallback to previous data
		return station_instance;
	}

	public void prefetchStationsInfo() {
		try {
			final RootElement root = new RootElement("observations");
			final Element station = root.getChild("station"); 
			final WidgetInstance stat = new WidgetInstance();
			final int seconds = (int) (System.currentTimeMillis() / 1000L);
			
			root.setStartElementListener(new StartElementListener() {		
				@Override
				public void start(Attributes attributes) {
					stat.last_update = seconds;
				}
			});

			station.setEndElementListener(new EndElementListener() {
				public void end() {
					if (!stat.name.equalsIgnoreCase("PŠrnu sadam")) { //Exclude PŠrnu Sadam station
						stationPool.put(stat.name, stat.getInstance()); //Add to hashtable
					}
				}
			});
			station.getChild("name").setEndTextElementListener(
					new EndTextElementListener() {
						public void end(String body) {
							stat.name = body;
						} 
					});
			
			station.getChild("airpressure").setEndTextElementListener(
					new EndTextElementListener() {
						public void end(String body) {
							stat.airpressure = body;
						} 
					});

			station.getChild("airtemperature").setEndTextElementListener(
					new EndTextElementListener() {
						public void end(String body) {
							stat.airtemp = body;
						}
					});
			
			station.getChild("relativehumidity").setEndTextElementListener(
					new EndTextElementListener() {
						public void end(String body) {
							stat.humidity = body;
						}
					});
			station.getChild("windspeed").setEndTextElementListener(
					new EndTextElementListener() {
						public void end(String body) {
							stat.windSpeed = body;
						}
					});
			station.getChild("winddirection").setEndTextElementListener(
					new EndTextElementListener() {
						public void end(String body) {
							stat.winddirection = body;
						}
					});
			station.getChild("windspeedmax").setEndTextElementListener(
					new EndTextElementListener() {
						public void end(String body) {
							stat.windSpeedMax = body;
						}
					});

			station.getChild("phenomenon").setEndTextElementListener(
					new EndTextElementListener() {
						public void end(String body) {
							stat.phenomenon = body;
						}
					});
			Xml.parse(getXMLFeed(), root.getContentHandler());
			
		} catch (Exception e) {
			//Could not parse
			if (manual_update) {
				messageHandler.sendEmptyMessage(TECHNICAL_PROBLEMS);
			}
		}
	}

	
	private String getXMLFeed() {
		try {
			File cache_file = getFileStreamPath("emhi.cache"); 
			if(cache_file.exists()) {
				int last_modified = (int) ( cache_file.lastModified() / 1000L );
				int current_time = (int) ( System.currentTimeMillis() / 1000L );
				
				if (current_time - last_modified < 600 ) { //Cache is valid for 10 minutes
					StringBuilder cached_xml = new StringBuilder();
					int ch;
					FileInputStream fis = openFileInput("emhi.cache");
					while( (ch = fis.read()) != -1) {
				        cached_xml.append((char)ch);
					}
					return cached_xml.toString();
				}
			}
			HttpParams httpParameters = new BasicHttpParams();
			// Set the timeout in milliseconds until a connection is established.
			int timeoutConnection = 15000;
			HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);			
			// Set the default socket timeout (SO_TIMEOUT) 
			// in milliseconds which is the timeout for waiting for data.
			int timeoutSocket = 15000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
			HttpClient httpclient = new DefaultHttpClient(httpParameters);
			
			HttpGet httpget = new HttpGet(new URI("http://www.emhi.ee/ilma_andmed/xml/observations.php"));; 
			// Execute HTTP Get Request
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			//Cache response
			String response = httpclient.execute(httpget, responseHandler);
			//Persistent cache
			FileOutputStream fos = openFileOutput("emhi.cache", Context.MODE_PRIVATE);
			fos.write(response.getBytes("ISO-8859-1"));
			fos.close();
			
			return response;
		} catch (Exception e) {
			//Something went wrong, we dont care
		}
		return null;
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
	
	
	public Hashtable<String, WidgetInstance> getStationPool() {
		startXMLPrefetch(); //This is needed for reconfiguration
		try {
			latch.await();
		} catch (InterruptedException e) { }
		latch = new CountDownLatch(2);
		return stationPool;
	}
	
	/**
	 * EmhiService binder
	 * 
	 * @author Raido Kuli
	 *
	 */
	
	public class MyBinder extends Binder {
		EmhiService getService() {
			return EmhiService.this;
		}
	}
	
}