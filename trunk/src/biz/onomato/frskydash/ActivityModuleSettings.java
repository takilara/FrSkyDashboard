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
		
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.alarm_level, android.R.layout.simple_spinner_item );
		adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);
			 
		Spinner s = (Spinner) findViewById( R.id.FrSkySettings_spinner_level );
		s.setAdapter( adapter );
		s.setOnItemSelectedListener(this);
		
				
				
				
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		TextView test = (TextView) findViewById(R.id.textView1);
		//test.setText(oAd1.toString());
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


