package biz.onomato.frskydash.hub;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import biz.onomato.frskydash.R;

public class EditPreferences extends PreferenceActivity 
{
	@Override
	public void onCreate(Bundle b)
	{
		super.onCreate(b);
		addPreferencesFromResource(R.xml.hub_preferences);
	}
}
