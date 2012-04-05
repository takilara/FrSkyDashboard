package biz.onomato.frskydash.activities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.R;
import biz.onomato.frskydash.domain.Frame;
import biz.onomato.frskydash.util.Logger;

public class ActivitySimulator extends Activity implements
		OnSeekBarChangeListener, OnClickListener {
	private static final String TAG = "SimulatorActivity";

	private SeekBar sb_ad1;
	private SeekBar sb_ad2;
	private SeekBar sb_rssitx;
	private SeekBar sb_rssirx;

	private TextView ad1_raw_tv;
	private TextView ad2_raw_tv;
	private TextView rssitx_raw_tv;
	private TextView rssirx_raw_tv;
	private TextView outFrame_tv;

	private CheckBox chkNoise;

	private View btnSend;
	private ToggleButton btnSimTgl, btnToggleFileSim;

	private Handler tickHandler;
	private Runnable runnableTick;
	private FrSkyServer server;
	private Frame _currentFrame;

	private int ad1_raw, ad2_raw, rssitx_raw, rssirx_raw;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		if (FrSkyServer.D)
			Log.i(TAG, "onCreate");
		setContentView(R.layout.activity_simulator);

		// float newVal = globals.setChannelById(AD1, 200);
		// oAd1 = globals.getChannelById(0);

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

		chkNoise = (CheckBox) findViewById(R.id.sim_chkNoise);

		outFrame_tv = (TextView) findViewById(R.id.outFrame);

		btnSend = findViewById(R.id.sim_btnSend);
		btnSend.setOnClickListener(this);

		btnSimTgl = (ToggleButton) findViewById(R.id.sim_tglBtn1);
		btnSimTgl.setOnClickListener(this);

		ad1_raw = 0;
		ad2_raw = 0;
		rssitx_raw = 0;
		rssirx_raw = 0;

		// hcpl simulate using raw log file in intervalled cycle
		btnToggleFileSim = ((ToggleButton) findViewById(R.id.btn_file_cycle));
		btnToggleFileSim.setOnClickListener(this);

		// Code to update GUI cyclic
		tickHandler = new Handler();
		runnableTick = new Runnable() {
			@Override
			public void run() {
				// Log.i(TAG,"Update GUI");
				if (server != null) {
					if (server.sim.running) {
						// sb_ad1.setProgress(server.sim._ad1);
						// sb_ad2.setProgress(server.sim._ad2);
						// sb_rssirx.setProgress(server.sim._rssirx);
						// sb_rssitx.setProgress(server.sim._rssitx);
						updateProgressBar();
					}
					tickHandler.postDelayed(this, 100);
				}
			}
		};
		tickHandler.postDelayed(runnableTick, 100);
		doBindService();
	}

	void doBindService() {
		// bindService(new Intent(this, FrSkyServer.class), mConnection,
		// Context.BIND_AUTO_CREATE);
		if (FrSkyServer.D)
			Log.i(TAG, "Start the server service if it is not already started");
		startService(new Intent(this, FrSkyServer.class));
		if (FrSkyServer.D)
			Log.i(TAG, "Try to bind to the service");
		getApplicationContext().bindService(
				new Intent(this, FrSkyServer.class), mConnection, 0);
		// bindService(new Intent(this, FrSkyServer.class), mConnection,
		// Context.BIND_AUTO_CREATE);
	}

	void doUnbindService() {
		if (server != null) {
			// Detach our existing connection.
			try {
				unbindService(mConnection);
			} catch (Exception e) {
			}
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			server = ((FrSkyServer.MyBinder) binder).getService();
			if (FrSkyServer.D)
				Log.i(TAG, "Bound to Service");
			btnSimTgl.setChecked(server.sim.running);
			// simFrame = server.sim.genFrame(ad1_raw,ad2_raw,rssirx_raw,
			// rssitx_raw);

			// Frame f = new Frame(simFrame);
			_currentFrame = Frame.FrameFromAnalog(ad1_raw, ad2_raw, rssirx_raw,
					rssitx_raw);
			// Frame f = Frame.FrameFromAnalog(ad1_raw,ad2_raw,rssirx_raw,
			// rssitx_raw);
			if (FrSkyServer.D)
				Log.i(TAG, "FC (member): " + _currentFrame.toHuman());
			// Log.i(TAG,"FC (class ): "+Frame.frameToHuman(simFrame));

			outFrame_tv.setText(_currentFrame.toHuman());

			if (FrSkyServer.D)
				Log.d(TAG, "update progress bars");
			updateProgressBar();

		}

		public void onServiceDisconnected(ComponentName className) {
			server = null;
		}
	};

	private void updateProgressBar() {
		sb_ad1.setProgress(server.sim._ad1);
		sb_ad2.setProgress(server.sim._ad2);
		sb_rssirx.setProgress(server.sim._rssirx);
		sb_rssitx.setProgress(server.sim._rssitx);
	}

	public void onClick(View v) {
		// Log.i(TAG,"Some button clicked");
		switch (v.getId()) {
		case R.id.sim_btnSend:
			// globals.parseFrame(simFrame);
			if (server != null)
				server.parseFrame(_currentFrame);
			break;
		case R.id.sim_tglBtn1:
			if (server != null)
				server.setSimStarted(btnSimTgl.isChecked());
			/*
			 * if(btnSimTgl.isChecked()){ globals.sim.start(); } else {
			 * globals.sim.stop(); }
			 */
			break;
		case R.id.sim_chkNoise:

			if (server != null) {
				server.sim.noise = chkNoise.isChecked();
			}
			break;
		case R.id.btn_file_cycle:
			handleFileSim();
			break;
		}
	}

	/**
	 * a thread for cycling the raw file contents providing to server with a
	 * fixed interval
	 */
	private Thread fileSimThread = null;

	/**
	 * all the file sim logic. Open a given file, read bytes, start thread
	 * providing these bytes to the server. TODO let user configure the interval
	 */
	private void handleFileSim() {
		// parse file and send the frames, input generated by server
		// see {@link Frame.toRawBytes} for used format in raw bytes
		// output raw logging should always be signed ints from -128 to 127
		try {
			// start file sim
			if (btnToggleFileSim.isChecked()) {
				// get input interval
				String strInterval = ((TextView) findViewById(R.id.file_cycle_interval))
						.getText().toString();
				int interval;
				// try parsing interval
				try {
					interval = Integer.parseInt(strInterval);
				} catch (Exception e) {
					// log exception
					Logger.e(TAG, "Failed to parse numeric interval input", e);
					// don't block or inform user, just continue with default
					// value
					interval = 3;
				}
				final int finalInterval = interval;
				// get the file
				File file = new File(
						((TextView) findViewById(R.id.file_cycle_path))
								.getText().toString());
				InputStream is = new FileInputStream(file);
				// Get the size of the file
				long length = file.length();
				// You cannot create an array using a long type.
				// It needs to be an int type.
				// Before converting to an int type, check
				// to ensure that file is not larger than Integer.MAX_VALUE.
				if (length > Integer.MAX_VALUE) {
					// File is too large
					throw new Exception("File is too large");
				}
				// Create the byte array to hold the data
				final byte[] bytes = new byte[(int) length];
				// Read in the bytes
				int offset = 0;
				int numRead = 0;
				while (offset < bytes.length
						&& (numRead = is.read(bytes, offset, bytes.length
								- offset)) >= 0) {
					offset += numRead;
				}
				// Ensure all the bytes have been read in
				if (offset < bytes.length) {
					throw new IOException("Could not completely read file "
							+ file.getName());
				}
				// Close the input stream
				is.close();
				// Start simulator with these bytes
				fileSimThread = new Thread() {
					public void run() {
						while (true) {
							// send to server
							server.handleByteBuffer(bytes);
							// wait a bit
							try {
								Thread.sleep(finalInterval * 1000);
							} catch (InterruptedException e) {
								Logger.e(
										TAG,
										"Problem waiting interval file cycle thread",
										e);
							}
						}
					}
				};
				// start the thread
				fileSimThread.start();
				// inform user
				Toast.makeText(getApplicationContext(),
						"File Sim cycle started", Toast.LENGTH_LONG).show();
			}
			// otherwise we need to stop the file sim
			else {
				// only stop when still alive
				if (fileSimThread != null && fileSimThread.isAlive())
					fileSimThread.stop();
				// inform user
				Toast.makeText(getApplicationContext(),
						"File Sim cycle stopped", Toast.LENGTH_LONG).show();
			}
		}
		// handle exceptions
		catch (Exception e) {
			// problem fetching the file
			Toast.makeText(getApplicationContext(),
					"Problem reading file or iterating content",
					Toast.LENGTH_LONG).show();
			Logger.e(TAG, "Problem reading file or iterating content", e);
		}
	}

	public void onProgressChanged(SeekBar sb, int prog, boolean from_user) {
		if (true) {
			switch (sb.getId()) {
			case R.id.sim_sb_ad1:
				ad1_raw = prog;
				server.sim._ad1 = ad1_raw;
				ad1_raw_tv.setText(Integer.toString(prog));
				break;
			case R.id.sim_sb_ad2:
				ad2_raw = prog;
				server.sim._ad2 = ad2_raw;
				ad2_raw_tv.setText(Integer.toString(prog));

				break;
			case R.id.sim_sb_rssitx:
				rssitx_raw = prog;
				server.sim._rssitx = rssitx_raw;
				rssitx_raw_tv.setText(Integer.toString(prog));
				break;
			case R.id.sim_sb_rssirx:
				rssirx_raw = prog;
				server.sim._rssirx = rssirx_raw;
				rssirx_raw_tv.setText(Integer.toString(prog));
				break;
			}

			// simFrame = server.sim.genFrame(ad1_raw,ad2_raw,rssirx_raw,
			// rssitx_raw);
			_currentFrame = Frame.FrameFromAnalog(ad1_raw, ad2_raw, rssirx_raw,
					rssitx_raw);
			outFrame_tv.setText(_currentFrame.toHuman());
		}
	}

	public void onStartTrackingTouch(SeekBar sb) {
	}

	public void onStopTrackingTouch(SeekBar sb) {
	}

	public void onDestroy() {
		super.onDestroy();
		if (FrSkyServer.D)
			Log.i(TAG, "onDestroy");
		doUnbindService();
		// mTts.stop();
		// globals.sim.stop();

	}

	// task testing

	// public void onBackPressed(){
	// Log.i(TAG,"Back pressed");
	//
	// Intent intent = new Intent(this, FrSkyServer.class);
	// Log.i(TAG,"Calling destroy on server");
	// //stopService(intent);
	// server.die();
	//
	//
	// //globals.die();
	// super.onBackPressed();
	// //this.finish();
	// }

	public void onPause() {

		super.onPause();
		if (FrSkyServer.D)
			Log.i(TAG, "onPause");
		// mTts.stop();
		// doUnbindService();
		tickHandler.removeCallbacks(runnableTick);

	}

	public void onResume() {
		super.onResume();
		// mTts.stop();
		doBindService();

		tickHandler.removeCallbacks(runnableTick);
		tickHandler.post(runnableTick);
		if (FrSkyServer.D)
			Log.i(TAG, "onResume");
		// btnSimTgl.setChecked(server.sim.running);
		
		// update state of toggle button at this point
		if( fileSimThread != null )
			btnToggleFileSim.setChecked(fileSimThread.isAlive());
	}

	public void onStop() {
		super.onStop();
		// mTts.stop();
		if (FrSkyServer.D)
			Log.i(TAG, "onStop");
	}
}
