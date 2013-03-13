package ee.kuli.emhi.ilm;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public class WeatherProviderMedium extends AppWidgetProvider {

	@Override
	public void onDisabled(Context context) {
		//Database.getInstance().deleteAll();
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		Database db = Database.getInstance();
		for(int i=0; i<appWidgetIds.length; i++) {
			int appWidgetId = appWidgetIds[i];
			db.deleteRow(appWidgetId);
		}
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,int[] appWidgetIds) {
		Intent service = new Intent(context, EmhiService.class);
		service.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
		context.startService(service);
	}
}
