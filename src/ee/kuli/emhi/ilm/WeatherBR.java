package ee.kuli.emhi.ilm;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;

public class WeatherBR extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle extras = intent.getExtras();
		if ( extras != null ) {
			boolean noConnectivity = extras.getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY);
			if ( !noConnectivity ) {
				Database db = Database.getInstance();
				int[] appWidgetIds = db.getAppWidgetIdsForUpdate();
				if (appWidgetIds.length > 0) {
					Intent service = new Intent(context, EmhiService.class);
					service.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
					context.startService(service);
				}
			}
		}
	}
}
