package ee.kuli.emhi.ilm;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class MapPreferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.map_prefs);
	}

}
