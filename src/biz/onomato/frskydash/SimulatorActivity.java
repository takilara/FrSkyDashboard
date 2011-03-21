package biz.onomato.frskydash;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.SeekBar;

public class SimulatorActivity extends Activity implements OnSeekBarChangeListener {
	private static final String TAG = "Simulator";
	private Channel oAd1;
    MyApp globals;
    private SeekBar sb_ad1;
    private SeekBar sb_ad2;
    private SeekBar sb_rssitx;
    private SeekBar sb_rssirx;
    
    private TextView ad1_raw_tv;
    private TextView ad2_raw_tv;
    private TextView rssitx_raw_tv;
    private TextView rssirx_raw_tv;
    private TextView outFrame_tv;
    
    private int ad1_raw, ad2_raw,rssitx_raw,rssirx_raw;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simulator);
		
		globals = ((MyApp)getApplicationContext());

        //float newVal = globals.setChannelById(AD1, 200);
        
        oAd1 = globals.getChannelById(0);
        
        sb_ad1 = (SeekBar) findViewById(R.id.sim_sb_ad1);
        sb_ad1.setOnSeekBarChangeListener(this);
        
        sb_ad2 = (SeekBar) findViewById(R.id.sim_sb_ad2);
        sb_ad2.setOnSeekBarChangeListener(this);
        
        sb_rssitx = (SeekBar) findViewById(R.id.sim_sb_rssitx);
        sb_rssitx.setOnSeekBarChangeListener(this);
        
        sb_rssirx = (SeekBar) findViewById(R.id.sim_sb_rssirx);
        sb_rssirx.setOnSeekBarChangeListener(this);
		
        ad1_raw_tv = (TextView) findViewById(R.id.sim_ad1raw);
        ad2_raw_tv = (TextView) findViewById(R.id.sim_ad2raw);
        rssitx_raw_tv = (TextView) findViewById(R.id.sim_rssitxraw);
        rssirx_raw_tv = (TextView) findViewById(R.id.sim_rssirxraw);
        
        outFrame_tv = (TextView) findViewById(R.id.outFrame);

        ad1_raw		= 0;
        ad2_raw		= 0;
        rssitx_raw	= 0;
        rssirx_raw 	= 0;
        
	}
	
	
	public void onProgressChanged(SeekBar sb,int prog,boolean from_user)
	{
		switch (sb.getId()) {
	    	case R.id.sim_sb_ad1:
	    		ad1_raw = prog;
	    		ad1_raw_tv.setText(Integer.toString(prog));
	    		break;
	    	case R.id.sim_sb_ad2:
	    		ad2_raw = prog;
	    		ad2_raw_tv.setText(Integer.toString(prog));
	    		
	    		break;
	    	case R.id.sim_sb_rssitx:
	    		rssitx_raw = prog;
	    		rssitx_raw_tv.setText(Integer.toString(prog));
	    		break;
	    	case R.id.sim_sb_rssirx:
	    		rssirx_raw = prog;
	    		rssirx_raw_tv.setText(Integer.toString(prog));
	    		break;
		}
		outFrame_tv.setText(genString());
	}
	
	public void onStartTrackingTouch(SeekBar sb)
	{
	}

	public void onStopTrackingTouch(SeekBar sb)
	{
	}

	private String genString()
	{
		String tString = "";
		StringBuffer buf = new StringBuffer();
		
		buf.append(Character.forDigit(126,10)); // 7E
		buf.append(Character.forDigit(254,10)); // FE
		
		// ADD AD1
		if(ad1_raw==126)
		{
			buf.append(Character.forDigit(125,10)); // 7D
			buf.append(Character.forDigit(94,10));    // 5E
		}
		else
		{
			buf.append(Character.forDigit(ad1_raw,10));
		}
		
		// ADD AD2
		if(ad2_raw==126)
		{
			buf.append(Character.forDigit(125,10));   // 7D
			buf.append(Character.forDigit(94,10));    // 5E
		}
		else
		{
			buf.append(Character.forDigit(ad2_raw,10));
		}
		
		for(int n=0;n<6;n++)
		{
			buf.append(Character.forDigit(0,10));
		}
		
		buf.append(Character.forDigit(126,16)); // 7E
		
		tString = buf.toString();
		return tString;
	}
	
}
