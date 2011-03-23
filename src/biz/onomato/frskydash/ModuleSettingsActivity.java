package biz.onomato.frskydash;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class ModuleSettingsActivity extends Activity {

	private static final String TAG = "FrSky-Settings";
	private Channel oAd1;
    MyApp globals;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_modulesettings);
		
		globals = ((MyApp)getApplicationContext());

        //float newVal = globals.setChannelById(AD1, 200);
        
        oAd1 = globals.getChannelById(0);
        
        
        TextView test = (TextView) findViewById(R.id.textView1);
		//test.setText(Float.toString(oAd1.getValue()));
        //test.setText(oAd1.getValue().toPlainString());
        test.setText(oAd1.toString());
		
        
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		TextView test = (TextView) findViewById(R.id.textView1);
		test.setText(oAd1.toString());
	}
}
