package ee.kuli.emhi.ilm;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.text.format.Time;

/**
 * This is a weather station class
 * 
 * Each instance is for praticular weather station, holds data like name, airtemp, phenomenon, wmocode 
 * and widget instance settings like use_fahrenheiet and use_gps.
 * 
 * @author Raido Kuli
 */

public class WidgetInstance {
	private final Context context = ApplicationContext.getContext();
	public boolean use_fahrenheit = false, use_gps = false, use_mmhg = false, feels_like = false;
	public String name;
	public String airpressure = null;
	public String airtemp = "0.0";
	public String phenomenon = "";
	public String humidity = "0";
	public String windSpeed = "0";
	public String windSpeedMax = "0";
	public String winddirection = "0";
	public int glaze_warnings = 0;
	public int last_update = 0;
	private Bitmap bitmap;
	
	
	public int getRawUpdateTime() {
		return last_update;
	}
	
	public String getUpdateTime() {		
		SimpleDateFormat date_format = new SimpleDateFormat("HH:mm");
		return date_format.format(last_update * 1000L);
	}
	
	public String getRawAirpressure() {
		if(!StringUtils.isEmpty(airpressure)) {
			return Math.round(Double.parseDouble(airpressure))+"";
		}
		return "";
	}
	
	public String getAirpressure() {
		String airpressure = getRawAirpressure();
		if(!StringUtils.isEmpty(airpressure)) {
			Locale locale = Locale.getDefault();
			String lang = locale.getLanguage();
			if(use_mmhg) {
				return Math.round((Double.parseDouble(airpressure) / 1.3332239 )) + ( lang.equals("ru") ? " мм рт.ст":" mmHg");
			} else {
				return airpressure + ( lang.equals("ru") ? " гПа":" hPa");
			}
		}
		if(use_mmhg) {
			return context.getString(R.string.airpressure_unknown_mmhg);
		}
		return context.getString(R.string.airpressure_unknown);
	}
	
	public String getPhenomenon() {
		if(!StringUtils.isEmpty(phenomenon)) {
			return phenomenon;
		}
		return "";
	}
	
	public String getRawHumidity() {
		if(!StringUtils.isEmpty(humidity)) {
			return humidity;
		}
		return "";
	}
	
	public Double getRawTemperature() {
		if(!StringUtils.isEmpty(airtemp)) {
			return Double.parseDouble(airtemp);
		}
		return -99D;
	}
	
	public int getWindDirection() {
		if(StringUtils.isEmpty(winddirection)) {
			winddirection = "0";	
		}
		return Integer.parseInt(winddirection);
	}
	
	/**
	 * Set GPS flag, whether to use or not
	 * 
	 * @param value integer 1 = true, 0 = false
	 */
	
	public void setUseGPS(int value) {
		if(value == 1) {
			use_gps = true;
		} else {
			use_gps = false;
		}
	}
	
	/**
	 * Set feels like temperature flag
	 */
	
	public void setFeelsLike(int value) {
		if(value == 1) {
			feels_like = true;
		} else {
			feels_like = false;
		}
	}
	
	/**
	 * Get feels like pref
	 */
	
	public boolean getFeelsLike() {
		return feels_like;
	}
	
	/**
	 * Set temperature unit C or F
	 * 
	 * @param unit integer 1 = Fahrenheit, 0 = Celsius
	 */
	
	public void setTemperatureUnit(int unit) {
		if(unit == 1) {
			use_fahrenheit = true;
		} else {
			use_fahrenheit = false;
		}
	}
	
	/**
	 * Get instance of the same station, returns new instance with all the original data
	 * 
	 * @return {@link WidgetInstance}
	 */
	
	public WidgetInstance getInstance() {
		final WidgetInstance copy = new WidgetInstance();
		copy.name = name;
		copy.airtemp = airtemp;
		copy.phenomenon = phenomenon;
		copy.glaze_warnings = glaze_warnings;
		copy.humidity = humidity;
		copy.winddirection = winddirection;
		copy.windSpeed = windSpeed;
		copy.windSpeedMax = windSpeedMax;
		copy.airpressure = airpressure;
		copy.last_update = last_update;
		return copy;
	}
	
	public Long getRawWindSpeed() {
		if ( !StringUtils.isEmpty(windSpeed) ) {
			return Math.round(Double.parseDouble(windSpeed));
		}
		return -1L;
	}
	
	public Long getRawWindSpeedMax() {
		if(!StringUtils.isEmpty(windSpeedMax)) {
			return Math.round(Double.parseDouble(windSpeedMax));
		}
		return -1L;
	}
	
	public String getWindSpeed() {
		Long windSpeed = getRawWindSpeed();
		Long windSpeedMax = getRawWindSpeedMax();
		
		if ( windSpeed > -1 && windSpeedMax > -1) {
			Locale locale = Locale.getDefault();
			String lang = locale.getLanguage();
			return windSpeed+" ("+windSpeedMax+") " + ( lang.equals("ru") ? "м/сек" : "m/s");
		}
		return context.getString(R.string.windspeed_unknown);
	}
	
	/**
	 * Get air temperature format for Celsius or Fahrenheit
	 * 
	 * @return formatted air temperature
	 */
	
	public String getTemperature() {
		Double raw_temp = getRawTemperature();
		if (raw_temp > -99) {
			DecimalFormat oneDigit = new DecimalFormat("#,##0.0");
			String temp;
			if (use_fahrenheit) {
				// Convert to Fahrenheit before display
				temp = oneDigit.format(((9 * raw_temp) / 5) + 32) +"\u00B0";
			} else {
				temp = oneDigit.format(raw_temp) + "\u00B0";
			}
			return temp;
		}
		return context.getString(R.string.temperature_unknown);
	}
	
	public String getFeelsLikeTemperature() {
		Double raw_temp = getRawTemperature();
		if (raw_temp > -99) {
			DecimalFormat oneDigit = new DecimalFormat("#,##0.0");
			String temp;
			double wind = ( ( getRawWindSpeed() * 3600 ) / 1000);			
			if( wind >= 4.8 && raw_temp < 10 ) {
				raw_temp = ( 13.12 + 0.6215 * raw_temp - 11.37 * Math.pow(wind,0.16) + 0.3965 * raw_temp * Math.pow( wind, 0.16 ) );
			}
			if (use_fahrenheit) {
				// Convert to Fahrenheit before display
				temp = oneDigit.format(((9 * raw_temp) / 5) + 32) +"\u00B0";
			} else {
				temp = oneDigit.format(raw_temp) + "\u00B0";
			}
			return temp;
		}
		return context.getString(R.string.temperature_unknown);
	}
	
	/**
	 * Select an icon to describe the given {@link ForecastsColumns#CONDITIONS}
	 * string. Uses a descending importance scale that matches keywords against
	 * the described conditions.
	 * 
	 * @param daytime
	 *            If true, return daylight-specific icons when available,
	 *            otherwise assume night icons.
	 */
	public int getWeatherIcon() {
		int icon = 0;
		String phenomenon = getPhenomenon();
		if(!StringUtils.isEmpty(phenomenon)) {
			final boolean isDayTime = isDayTime();
			if (stringContains(phenomenon, getArray(R.array.iconStorm, context))) {
				icon = R.drawable.weather_storm;
				//Snow icons, light, moderate and heavy
			} else if (stringContains(phenomenon, getArray(R.array.iconLightSnow, context))) {
				icon = isDayTime ? R.drawable.weather_light_snow : R.drawable.weather_light_snow_night;
			} else if (stringContains(phenomenon, getArray(R.array.iconModerateSnow, context))) {
				icon = R.drawable.weather_heavy_snow;
			} else if (stringContains(phenomenon, getArray(R.array.iconHeavySnow, context))) {
				icon = R.drawable.weather_heavy_snow;
				//Rain icons light, moderate and heavy
			} else if (stringContains(phenomenon, getArray(R.array.iconLightRain, context))) {
				icon = isDayTime ? R.drawable.weather_light_showers : R.drawable.weather_light_showers_night;
			} else if (stringContains(phenomenon, getArray(R.array.iconModerateRain, context))) {
					icon = R.drawable.weather_heavy_showers;
			} else if (stringContains(phenomenon, getArray(R.array.iconHeavyRain, context))) {
				icon = R.drawable.weather_heavy_showers;
				//Sleet icons
			} else if (stringContains(phenomenon, getArray(R.array.iconSleet, context))) {
				icon = R.drawable.weather_sleet;
				//Light sleet icons
			} else if (stringContains(phenomenon, getArray(R.array.iconLightSleet, context))) {
				icon = R.drawable.weather_sleet;
				//Fog icons
			} else if (stringContains(phenomenon, getArray(R.array.iconFog, context))) {
				icon = R.drawable.weather_fog;
				//Few clouds
			} else if (stringContains(phenomenon, getArray(R.array.iconFewClouds, context))) {
				icon = isDayTime ? R.drawable.weather_few_clouds : R.drawable.weather_few_clouds_night;
				//Variable clouds
			} else if (stringContains(phenomenon, getArray(R.array.iconVariableClouds, context))) {
				icon = isDayTime ? R.drawable.weather_variable_clouds : R.drawable.weather_variable_clouds_night;
				//Clouds
			} else if (stringContains(phenomenon, getArray(R.array.iconClouds, context))) {
				icon = R.drawable.weather_clouds;
				//Glaze icon
			} else if (phenomenon.equals("Glaze")) {
				icon = R.drawable.weather_glaze;
				//Clouds icons
			} else if (stringContains(phenomenon, getArray(R.array.iconClear, context))) {
				icon = isDayTime ? R.drawable.weather_clear : R.drawable.weather_clear_night;
			}
		}
		return icon;
	}
	
	/**
	 * Helper function, to get array from values/*
	 * 
	 * @param id - resource array id
	 * @param context
	 * @return string array
	 */
	
	private static String[] getArray(final int id, final Context context) {
		return context.getResources().getStringArray(id);
	}

	/**
	 * Search the subject string for the given words using a case-sensitive
	 * search. This is usually faster than a {@link Pattern} regular expression
	 * because we don't have JIT.
	 */
	
	private static boolean stringContains(String subject, String[] searchWords) {
		for (String word : searchWords) {
			if (subject.equals(word)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Is it day or night ?
	 * This method calculates sunrise and sunset, based on current location and timezone
	 * Original code taken from Android Grass live wallpaper
	 * 
	 * @return boolean - true if day, false otherwise
	 */
	
	private boolean isDayTime() {
		final Location location = getLastLocation();
		final Calendar now = Calendar.getInstance();
		final Date time = now.getTime();
		
		if(location != null) {
			final String timeZone = Time.getCurrentTimezone();
	
			final SunCalculator calculator = new SunCalculator(location, timeZone);
	
			final double sunrise = calculator.computeSunriseTime(SunCalculator.ZENITH_OFFICIAL, now);
	
			final double sunset = calculator.computeSunsetTime(SunCalculator.ZENITH_OFFICIAL, now);
			
			final Date sunset_date = now.getTime();
			sunset_date.setHours(SunCalculator.timeToHours(sunset));
			sunset_date.setMinutes(SunCalculator.timeToMinutes(sunset));
			
			final Date sunrise_date = now.getTime();
			sunrise_date.setHours(SunCalculator.timeToHours(sunrise));
			sunrise_date.setMinutes(SunCalculator.timeToMinutes(sunrise));
			
			return (time.after(sunrise_date) && time.before(sunset_date));
		} else {
			return (time.getHours() >= 6 && time.getHours() <= 18);
		}
	}
	
	/**
	 * Get last known network location
	 * @return Location
	 */
	
	public Location getLastLocation() {
		final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		LocationProvider provider = locationManager.getProvider(LocationManager.NETWORK_PROVIDER);
		if (provider != null) {
			final Location network_location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			return network_location;
		}
		return null;
	}

	public String getHumidity() {
		String humidity = getRawHumidity();
		if(!StringUtils.isEmpty(humidity)) {
			return humidity+"%";
		}
		return context.getString(R.string.humidity_unknown);
	}

	public void setAirPressureUnit(int value) {
		if(value == 1) {
			use_mmhg = true;
		} else {
			use_mmhg = false;
		}
	}

	public void setBitmap(Bitmap bmp) {
		this.bitmap = bmp;
	}
	
	public Bitmap getBitmap() {
		return this.bitmap;
	}
}
