package ee.kuli.emhi.ilm;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

public class Database {

	private static final String DATABASE_NAME = "emhi_weather.db";
	private static final int DATABASE_VERSION = 5;
	private static final String TABLE_NAME = "emhi_widgets";
	private static final String GEOCODE_CACHE_TABLE = "geocode_cache"; //All versions below 5 have this, need to keep for deleting

	private SQLiteDatabase db;
	
	private SQLiteStatement insertStmt, deleteStmt, updateLastStmt, updateName, updateFeelsLike, updateGPS, updateTemp, updatePressure, updateGlazeWarn;
	//Widget instances SQL statements
	private static final String INSERT = "insert into " 
		+ TABLE_NAME + "(widget_id, station_name, use_gps, use_fahrenheit, glaze_warnings, use_mmhg, feels_like) values (?, ?, ?, ?, ?, ?, ?)";
	private static final String DELETE = "delete from "+TABLE_NAME+" WHERE widget_id = ?";
	private static final String UPDATE_LAST = "update "+TABLE_NAME+" set last_temp = ?, last_phenomenon = ?, " +
									"last_windspeed = ?, last_windspeed_max = ?, last_winddirection = ?, last_humidity = ?, last_airpressure = ?, last_update_time = ? WHERE widget_id = ?";
	private static final String UPDATE_NAME = "update "+TABLE_NAME+" set station_name = ? WHERE widget_id = ?";
	private static final String UPDATE_GPS = "update "+TABLE_NAME+" set use_gps = ? WHERE widget_id = ?";
	private static final String UPDATE_TEMPUNIT = "update "+TABLE_NAME+" set use_fahrenheit = ? WHERE widget_id = ?";
	private static final String UPDATE_PRESSUREUNIT = "update "+TABLE_NAME+" set use_mmhg = ? WHERE widget_id = ?";
	private static final String UPDATE_GLAZEWARN = "update "+TABLE_NAME+" set glaze_warnings = ? WHERE widget_id = ?";
	private static final String UPDATE_FEELS_LIKE = "update "+TABLE_NAME+" set feels_like = ? WHERE widget_id = ?";
	
	private static Database instance = null;
	
	private Database() {
		final DatabaseHelper db_helper = DatabaseHelper.getInstance(ApplicationContext.getContext());
		this.db = db_helper.getWritableDatabase();
		this.insertStmt = this.db.compileStatement(INSERT);
		this.deleteStmt = this.db.compileStatement(DELETE);
		this.updateLastStmt = this.db.compileStatement(UPDATE_LAST); 
		this.updateName = this.db.compileStatement(UPDATE_NAME);
		
		this.updateGlazeWarn = db.compileStatement(UPDATE_GLAZEWARN);
		this.updateGPS = db.compileStatement(UPDATE_GPS);
		this.updatePressure = db.compileStatement(UPDATE_PRESSUREUNIT);
		this.updateTemp = db.compileStatement(UPDATE_TEMPUNIT);
		this.updateFeelsLike = db.compileStatement(UPDATE_FEELS_LIKE);
	}
	
	public static Database getInstance() {
		if (instance == null) {
			instance = new Database();
		}
		return instance;
	}
	
	public boolean deleteRow(int appWidgetId) {
		try {
			this.deleteStmt.bindLong(1, appWidgetId);
			this.deleteStmt.execute();
			return true;
		} catch (SQLException e) {
			return false;
		}
	}

	public void addWidgetInstance(int appWidgetId, String station_name, boolean use_gps, boolean use_fahrenheit, boolean glaze_warning, boolean use_mmhg, boolean feels_like) {
		this.insertStmt.bindLong(1, appWidgetId);
		this.insertStmt.bindString(2, station_name);
		this.insertStmt.bindLong(3, use_gps ? 1 : 0 );
		this.insertStmt.bindLong(4, use_fahrenheit ? 1 : 0);
		this.insertStmt.bindLong(5, glaze_warning ? 1 : 0);
		this.insertStmt.bindLong(6, use_mmhg ? 1 : 0);
		this.insertStmt.bindLong(7, feels_like ? 1 : 0);
		this.insertStmt.executeInsert();
	}
	


	public void deleteAll() {
		this.db.delete(TABLE_NAME, null, null);
	}
	
	public boolean isConfigured(int appWidgetId) {
		boolean result = false;
		Cursor cursor = this.db.rawQuery("SELECT id FROM "+TABLE_NAME+" WHERE widget_id = '"+appWidgetId+"';", null);
		if(cursor.moveToFirst()) {
			result = true;
		}
		cursor.close();
		return result;
	}
	
	public synchronized void updateName(int appWidgetId, String new_name) {
		this.updateName.bindString(1, new_name);
		this.updateName.bindLong(2, appWidgetId);
		this.updateName.execute();
	}
	
	public synchronized void updateLast(int appWidgetId, WidgetInstance widget) {
		try {
			this.updateLastStmt.bindString(1, widget.getRawTemperature().toString());
			this.updateLastStmt.bindString(2, widget.getPhenomenon());
			this.updateLastStmt.bindString(3, widget.getRawWindSpeed().toString());
			this.updateLastStmt.bindString(4, widget.getRawWindSpeedMax().toString());
			this.updateLastStmt.bindString(5, widget.getWindDirection()+"");
			this.updateLastStmt.bindString(6, widget.getRawHumidity());
			this.updateLastStmt.bindString(7, widget.getRawAirpressure());
			this.updateLastStmt.bindLong(8, widget.getRawUpdateTime());
			this.updateLastStmt.bindLong(9, appWidgetId);
			this.updateLastStmt.executeInsert();
		} catch (SQLiteConstraintException e) {
			
		}
	}

	public synchronized WidgetInstance getStation(int appWidgetId) {
		Cursor cursor = this.db.rawQuery("SELECT station_name, use_gps, use_fahrenheit, last_temp, last_phenomenon, glaze_warnings, last_windspeed, last_windspeed_max" +
				", last_winddirection, last_humidity, last_airpressure, last_update_time, use_mmhg, feels_like FROM "+TABLE_NAME+" WHERE widget_id = '"+appWidgetId+"';", null);
		WidgetInstance station = null;
		if(cursor.moveToFirst()) {
			station = new WidgetInstance();
			station.name = cursor.getString(0);
			station.setUseGPS(cursor.getInt(1));
			station.setTemperatureUnit(cursor.getInt(2));
			station.airtemp = cursor.getString(3);
			station.phenomenon = cursor.getString(4);
			station.glaze_warnings = cursor.getInt(5);
			station.windSpeed = cursor.getString(6);
			station.windSpeedMax = cursor.getString(7);
			station.winddirection = cursor.getString(8);
			station.humidity = cursor.getString(9);
			station.airpressure = cursor.getString(10);
			station.last_update = cursor.getInt(11);
			station.setAirPressureUnit(cursor.getInt(12));
			station.setFeelsLike(cursor.getInt(13));
		}
		cursor.close();
		return station;
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		
		private static DatabaseHelper instance;
		
		public static synchronized DatabaseHelper getInstance(Context context) {
			if (instance == null) {
				instance = new DatabaseHelper(context);
			}
			return instance;
		}

		private DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_NAME + "(id INTEGER PRIMARY KEY, widget_id INTEGER KEY, station_name TEXT, use_gps INTEGER DEFAULT 0, " +
						"use_fahrenheit INTEGER DEFAULT 0, glaze_warnings INTEGER DEFAULT 0, use_mmhg INTEGER DEFAULT 0, last_temp TEXT DEFAULT '0.0', last_phenomenon TEXT DEFAULT '', last_humidity TEXT, last_windspeed TEXT, last_windspeed_max TEXT" +
						", last_winddirection TEXT, last_airpressure TEXT, last_update_time INTEGER, feels_like INTEGER DEFAULT 0)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if(oldVersion == 1 && newVersion == 2) { 
				db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD last_airpressure TEXT");
				db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD last_update_time INTEGER");
			}
			if(oldVersion == 2 && newVersion == 3) {
				db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD use_mmhg INTEGER DEFAULT 0");
			}
			if(oldVersion == 1 && newVersion == 3) {
				db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD last_airpressure TEXT");
				db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD last_update_time INTEGER");
				db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD use_mmhg INTEGER DEFAULT 0");
			}

			if(oldVersion == 3 && newVersion == 4) {
				db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD feels_like INTEGER DEFAULT 0");
			}
			if(oldVersion == 2 && newVersion == 4) {
				db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD feels_like INTEGER DEFAULT 0");
				db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD use_mmhg INTEGER DEFAULT 0");
			}
			if(oldVersion == 1 && newVersion == 4) {
				db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD last_airpressure TEXT");
				db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD last_update_time INTEGER");
				db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD use_mmhg INTEGER DEFAULT 0");
				db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD feels_like INTEGER DEFAULT 0");
			}
			//Drop geocode table, if DB version below 5
			if(oldVersion < 5) {
				db.execSQL("DROP TABLE " + GEOCODE_CACHE_TABLE);
			}
			
		}
	}

	public synchronized void setUseGps(int appWidgetId, boolean checked) {
		this.updateGPS.bindLong(1, checked ? 1 :0);
		this.updateGPS.bindLong(2, appWidgetId);
		this.updateGPS.execute();
	}

	public synchronized void setUseFahrenheit(int appWidgetId, boolean checked) {
		this.updateTemp.bindLong(1, checked ? 1 :0);
		this.updateTemp.bindLong(2, appWidgetId);
		this.updateTemp.execute();
	}

	public synchronized void setGlazeWarnings(int appWidgetId, boolean checked) {
		this.updateGlazeWarn.bindLong(1, checked ? 1 :0);
		this.updateGlazeWarn.bindLong(2, appWidgetId);
		this.updateGlazeWarn.execute();
	}

	public synchronized void setUseMMHG(int appWidgetId, boolean checked) {
		this.updatePressure.bindLong(1, checked ? 1 :0);
		this.updatePressure.bindLong(2, appWidgetId);
		this.updatePressure.execute();
	}
	
	public void setFeelsLike(int appWidgetId, boolean checked) {
		this.updateFeelsLike.bindLong(1, checked ? 1 :0);
		this.updateFeelsLike.bindLong(2, appWidgetId);
		this.updateFeelsLike.execute();
	}
	
	public int[] getAppWidgetIdsForUpdate() {
		Cursor cursor = this.db.rawQuery("SELECT widget_id FROM "+TABLE_NAME+" WHERE (strftime('%s','now') - last_update_time) >= 4500;", null);
		int[] appWidgetIds = new int[cursor.getCount()];
		int i = 0;
		while(cursor.moveToNext()) {
			appWidgetIds[i++] = cursor.getInt(0);
		}
		cursor.close();
		return appWidgetIds;
	}

	public int[] getAppWidgetIds() {
		Cursor cursor = this.db.rawQuery("SELECT widget_id FROM "+TABLE_NAME+";", null);
		int[] appWidgetIds = new int[cursor.getCount()];
		int i = 0;
		while(cursor.moveToNext()) {
			appWidgetIds[i++] = cursor.getInt(0);
		}
		cursor.close();
		return appWidgetIds;
	}
}