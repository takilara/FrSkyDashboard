package biz.onomato.frskydash;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class ApplicationSettingsActivity extends Activity implements OnClickListener {

	private static final String TAG = "Application-Settings";
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_applicationsettings);
		
		View btnDeleteLogs = findViewById(R.id.btnDeleteLogs);
        btnDeleteLogs.setOnClickListener(this);
		
		
        
        
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		//test.setText(oAd1.toString());
	}
	
	public void onClick(View v)
	{
		switch(v.getId())
		{
			case R.id.btnDeleteLogs:
				Log.i(TAG,"Delete all logs please");
			break;
		}
	}
	
}
