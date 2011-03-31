package biz.onomato.frskydash;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.util.Log;
import android.widget.SeekBar;
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
    private ToggleButton btnSimTgl;
    private int[] simFrame;
    
 
    private Handler tickHandler;
    private Runnable runnableTick;
    
    
    private int ad1_raw, ad2_raw,rssitx_raw,rssirx_raw;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG,"onCreate");
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

        btnSimTgl = (ToggleButton) findViewById(R.id.sim_tglBtn1);
        btnSimTgl.setOnClickListener(this);
        
        ad1_raw		= 0;
        ad2_raw		= 0;
        rssitx_raw	= 0;
        rssirx_raw 	= 0;
        
        simFrame = globals.sim.genFrame(ad1_raw,ad2_raw,rssirx_raw, rssitx_raw);
		outFrame_tv.setText(globals.frameToHuman(simFrame));
		
		// Code to update GUI cyclic
        tickHandler = new Handler();
		
		runnableTick = new Runnable() {
			@Override
			public void run()
			{
//				Log.i(TAG,"Update GUI");
				if(globals.sim.running)
				{
					sb_ad1.setProgress(globals.sim._ad1);
					sb_ad2.setProgress(globals.sim._ad2);
					sb_rssirx.setProgress(globals.sim._rssirx);
					sb_rssitx.setProgress(globals.sim._rssitx);
			    	
			    	
				}
				tickHandler.postDelayed(this, 100);
			}
		};
		tickHandler.postDelayed(runnableTick, 100);
		
        
	}
	
	public void onClick(View v) {
		//Log.i(TAG,"Some button clicked");
    	switch (v.getId()) {
    		case R.id.sim_btnSend:
    			globals.parseFrame(simFrame);
    			break;
    		case R.id.sim_tglBtn1:
    			
    			if(btnSimTgl.isChecked()){
    				globals.sim.start();
    			}
    			else
    			{
    				globals.sim.stop();
    			}
    			break;
    	}
    }
	
	public void onProgressChanged(SeekBar sb,int prog,boolean from_user)
	{
		if(true){
			switch (sb.getId()) {
		    	case R.id.sim_sb_ad1:
		    		ad1_raw = prog;
		    		globals.sim._ad1 = ad1_raw;
		    		ad1_raw_tv.setText(Integer.toString(prog));
		    		break;
		    	case R.id.sim_sb_ad2:
		    		ad2_raw = prog;
		    		globals.sim._ad2 = ad2_raw;
		    		ad2_raw_tv.setText(Integer.toString(prog));
		    		
		    		break;
		    	case R.id.sim_sb_rssitx:
		    		rssitx_raw = prog;
		    		globals.sim._rssitx = rssitx_raw;
		    		rssitx_raw_tv.setText(Integer.toString(prog));
		    		break;
		    	case R.id.sim_sb_rssirx:
		    		rssirx_raw = prog;
		    		globals.sim._rssirx = rssirx_raw;
		    		rssirx_raw_tv.setText(Integer.toString(prog));
		    		break;
			}
		
		simFrame = globals.sim.genFrame(ad1_raw,ad2_raw,rssirx_raw, rssitx_raw);
		outFrame_tv.setText(globals.frameToHuman(simFrame));
		}
	}
	
	public void onStartTrackingTouch(SeekBar sb)
	{
	}

	public void onStopTrackingTouch(SeekBar sb)
	{
	}

	
	
	public void onDestroy(){
		super.onDestroy();
		Log.i(TAG,"onDestroy");
		//mTts.stop();
		globals.sim.stop();
       	
    }
	// task testing
	

    public void onBackPressed(){
    	Log.i(TAG,"Back pressed");
		
    	Intent intent = new Intent(this, FrSkyServer.class);
    	stopService(intent);

    	
		globals.die();
		super.onBackPressed();
    	//this.finish();
    }
	
    public void onPause(){
    	
    	super.onPause();
    	//mTts.stop();
    	tickHandler.removeCallbacks(runnableTick);
    	Log.i(TAG,"onPause");
    }
    
    public void onResume(){
    	super.onResume();
    	//mTts.stop();
    	tickHandler.removeCallbacks(runnableTick);
    	tickHandler.post(runnableTick);
    	Log.i(TAG,"onResume");
    	btnSimTgl.setChecked(globals.sim.running);
    }
    
    public void onStop(){
    	super.onStop();
    	//mTts.stop();
    	Log.i(TAG,"onStop");
    }
}
