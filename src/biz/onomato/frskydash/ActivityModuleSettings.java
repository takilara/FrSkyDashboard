package biz.onomato.frskydash;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityModuleSettings extends Activity implements OnItemSelectedListener {

	private static final String TAG = "FrSky-Settings";
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_modulesettings);
		
		//stringlist for values
		int from = 20;
		int to = 110;
		String[] values = new String[to-from+1];
		for(int i=from;i<=to;i++)
		{
			values[i-from]=Integer.toString(i);
		}
		
		//FrSkySettings_spinner_level
		ArrayAdapter<CharSequence> RSSIalarm1LevelAdapter = ArrayAdapter.createFromResource(this, R.array.alarm_level, android.R.layout.simple_spinner_item );
		RSSIalarm1LevelAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);
			 
		Spinner RSSIalarm1LevelSpinner = (Spinner) findViewById( R.id.FrSkySettings_spinner_level );
		RSSIalarm1LevelSpinner.setAdapter( RSSIalarm1LevelAdapter );
		RSSIalarm1LevelSpinner.setOnItemSelectedListener(this);
		
		//FrSkySettings_spinner_relative
		ArrayAdapter<CharSequence> RSSIalarm1RelAdapter = ArrayAdapter.createFromResource(this, R.array.alarm_relative, android.R.layout.simple_spinner_item );
		RSSIalarm1RelAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);
			 
		Spinner RSSIalarm1RelSpinner = (Spinner) findViewById( R.id.FrSkySettings_spinner_relative );
		RSSIalarm1RelSpinner.setAdapter( RSSIalarm1RelAdapter );
		RSSIalarm1RelSpinner.setOnItemSelectedListener(this);
		
		
		//FrSkySettings_spinner_value
		//ArrayAdapter<CharSequence> RSSIalarm1ValueAdapter = ArrayAdapter.createFromResource(this, values, android.R.layout.simple_spinner_item );
		ArrayAdapter<String> RSSIalarm1ValueAdapter = new ArrayAdapter<String> (this, android.R.layout.simple_spinner_dropdown_item,values );
		//RSSIalarm1ValueAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);
			 
		Spinner RSSIalarm1ValueSpinner = (Spinner) findViewById( R.id.FrSkySettings_spinner_value );
		RSSIalarm1ValueSpinner.setAdapter( RSSIalarm1ValueAdapter );
		RSSIalarm1ValueSpinner.setOnItemSelectedListener(this);		
				
				
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
	}
	
	



	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		// TODO Auto-generated method stub
		Toast.makeText(parent.getContext(), "The planet is " +
		          parent.getItemAtPosition(pos).toString(), Toast.LENGTH_LONG).show();
		
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
	
}


