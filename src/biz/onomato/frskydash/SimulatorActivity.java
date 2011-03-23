package biz.onomato.frskydash;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.util.Log;
import android.widget.SeekBar;
import java.util.TimerTask;
import java.util.Timer;
import android.os.Handler;
import java.lang.Runnable;

public class SimulatorActivity extends Activity implements OnSeekBarChangeListener, OnClickListener {
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
    private boolean _simEnabled;
    
    private View btnSend;
    private View btnSimTgl;
    private int[] simFrame;
    
 
    private Handler tickHandler;
    private Runnable runnableTick;
    
    
    private int ad1_raw, ad2_raw,rssitx_raw,rssirx_raw;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_simEnabled=false;
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
        
        btnSend = findViewById(R.id.sim_btnSend);
        btnSend.setOnClickListener(this);

        btnSimTgl = findViewById(R.id.sim_tglBtn1);
        btnSimTgl.setOnClickListener(this);
        
        ad1_raw		= 0;
        ad2_raw		= 0;
        rssitx_raw	= 0;
        rssirx_raw 	= 0;
        
        simFrame = genFrame();
		outFrame_tv.setText(globals.frameToHuman(simFrame));
		
		tickHandler = new Handler();
		
		runnableTick = new Runnable() {
			@Override
			public void run()
			{
				sb_ad1.setProgress(sb_ad1.getProgress()+1);
				if(sb_ad1.getProgress()>=255) {sb_ad1.setProgress(0);}
				
				sb_ad2.setProgress(sb_ad2.getProgress()-1);
				if(sb_ad2.getProgress()<=0) {sb_ad2.setProgress(255);}
				
				//Log.i("SIM","Automatic post new frame");
				
				simFrame = genFrame();
				//outFrame_tv.setText(globals.frameToHuman(simFrame));
				globals.parseFrame(simFrame);
				tickHandler.postDelayed(this, 30);
			}
		};
		
		
        
	}
	
	public void onClick(View v) {
		//Log.i(TAG,"Some button clicked");
    	switch (v.getId()) {
    		case R.id.sim_btnSend:
    			globals.parseFrame(simFrame);
    			break;
    		case R.id.sim_tglBtn1:
    			_simEnabled = !_simEnabled;
    			if(_simEnabled){
    				tickHandler.post(runnableTick);
    			}
    			else
    			{
    				tickHandler.removeCallbacks(runnableTick);
    			}
    	}
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
		simFrame = genFrame();
		outFrame_tv.setText(globals.frameToHuman(simFrame));
	}
	
	public void onStartTrackingTouch(SeekBar sb)
	{
	}

	public void onStopTrackingTouch(SeekBar sb)
	{
	}

	private int[] genFrame()
	{
		int[] inBuf = new int[4];
		int[] buf = new int[30];
		
		inBuf[0] = ad1_raw;
		inBuf[1] = ad2_raw;
		inBuf[2] = rssirx_raw;
		inBuf[3] = rssitx_raw*2 & 0xff;
		
		// Add the header
		buf[0] = 0x7e;
		buf[1] = 0xfe;

		// loop through the simulated values to see if we need to bytestuff the array
		int i = 2;
		for(int n=0;n<inBuf.length;n++)
		{
			if(inBuf[n]==0x7e)
			{
				buf[i]=0x7d;
				buf[i+1]=0x5e;
				i++;
			}
			else
			{
				buf[i] = inBuf[n];
			}
			i++;
		}
		
		// add the last 4 0x00's
		for(int n=0;n<4;n++)
		{
			buf[i]=0x00;
			i++;
		}
		
		// add the ending 0x7e
		buf[i] = 0x7e;
		
		int[] outBuf = new int[i+1];
		
		for(int n=0;n<i+1;n++)
		{
			outBuf[n]=buf[n];
		}
		
		return outBuf;
	}
	
	// task testing
	
	
}
