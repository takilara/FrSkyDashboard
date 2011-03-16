package biz.onomato.frskydash;

import android.app.Activity;
import android.os.Bundle;

public class ModuleSettingsActivity extends Activity {
	private static final String TAG = "FrSky-Settings";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.modulesettings);
	}
}
