package biz.onomato.frskydash.activities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

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
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import biz.onomato.frskydash.DataLogger;
import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.R;
import biz.onomato.frskydash.domain.Frame;
import biz.onomato.frskydash.sim.FileSimulatorThread;
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

	private CheckBox chkNoise, chkSensorData;

	private View btnSend;
	private ToggleButton btnSimTgl, btnToggleFileSim;
	private LinearLayout llFilePlayback;

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
		chkSensorData = (CheckBox) findViewById(R.id.sim_chk_sensor_data);
		chkSensorData.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton button, boolean checked) {
				if (server != null) {
					server.getSim().setSensorData(checked);
				}
			}
		});

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

		llFilePlayback = (LinearLayout) findViewById(R.id.sim_ll_filePlayback);

		// Code to update GUI cyclic
		tickHandler = new Handler();
		runnableTick = new Runnable() {
			@Override
			public void run() {
				// Log.i(TAG,"Update GUI");
				if (server != null) {
					if (server.getSim().running) {
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
		// init the file sim thread only once
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
			btnSimTgl.setChecked(server.getSim().running);
			// simFrame = server.sim.genFrame(ad1_raw,ad2_raw,rssirx_raw,
			// rssitx_raw);
			//fileSimThread = new FileSimulatorThread(server);

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

			/**
			 * Hide / Unhide file playback features
			 * 
			 */
			if (server.getFilePlaybackEnabled()) {
				llFilePlayback.setVisibility(View.VISIBLE);
			} else {
				llFilePlayback.setVisibility(View.GONE);
			}

		}

		public void onServiceDisconnected(ComponentName className) {
			server = null;
		}
	};

	private void updateProgressBar() {
		sb_ad1.setProgress(server.getSim()._ad1);
		sb_ad2.setProgress(server.getSim()._ad2);
		sb_rssirx.setProgress(server.getSim()._rssirx);
		sb_rssitx.setProgress(server.getSim()._rssitx);
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
				server.getSim().setNoise(chkNoise.isChecked());
			}
			break;
		case R.id.btn_file_cycle:
			handleFileSim();
			break;
		}
	}

	/**
	 * a thread for cycling the raw file contents providing to server with a
	 * fixed interval. This is static so we can check state on resume
	 */
	private static FileSimulatorThread fileSimThread = null;

	/**
	 * update file sim thread files setting and running state
	 */
	private void handleFileSim() {
		// parse file and send the frames, input generated by server
		// see {@link Frame.toRawBytes} for used format in raw bytes
		// output raw logging should always be signed ints from -128 to 127
		try {
			// start file sim based on state of toggle button
			if (btnToggleFileSim.isChecked()) {
				//stop if needed
				if (fileSimThread != null )
					fileSimThread.stopThread();
				// first see if we need to create a new thread or not
				fileSimThread = new FileSimulatorThread(server);
				// get the file by user input
				String path = ((TextView) findViewById(R.id.file_cycle_path))
						.getText().toString();
				final File[] files = getRawFilesFromPath(path);
				// Start simulator with these bytes
				fileSimThread.setFiles(files);
				// start the thread
				fileSimThread.startThread();
				// inform user
				Toast.makeText(getApplicationContext(),
						"File Sim cycle started", Toast.LENGTH_LONG).show();
			}
			// otherwise we need to stop the file sim
			else {
				// only stop when still alive
				if (fileSimThread != null && fileSimThread.isThreadRunning())
					fileSimThread.stopThread();
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

	/**
	 * helper to retrieve all raw files from a given path
	 * 
	 * @param path
	 * @return
	 */
	private File[] getRawFilesFromPath(String path) {
		// FIXME check if sdcard is mounted!!
		// ...
		// if pat is given and ends with .raw than just add this file only
		if (path != null && !"".equals(path) && path.endsWith(".raw")) {
			File[] files = new File[1];
			files[0] = new File(path);
			return files;
		}
		// if this file path is empty we should use all the .raw files
		// from the default path
		File dir = DataLogger.getLoggingPath(getApplicationContext());
		// otherwise if a file path is given put it doesn't end on raw a
		// directory is supposed so get all the .Raw files from that
		// directory
		if (path != null && !"".equals(path)) {
			// update file with path before getting all te raw files
			dir = new File(path);
		}
		// based on last 2 options iterate files for that dir
		ArrayList<File> files = new ArrayList<File>();
		for (File file : dir.listFiles()) {
			// only use .raw files
			if (file.getName().endsWith(".raw"))
				files.add(file);
		}
		// create new array
		// return files.toArray();
		File[] filesArray = new File[files.size()];
		for (int i = 0; i < files.size(); i++) {
			filesArray[i] = files.get(i);
		}
		return filesArray;
	}

	public void onProgressChanged(SeekBar sb, int prog, boolean from_user) {
		if (true) {
			switch (sb.getId()) {
			case R.id.sim_sb_ad1:
				ad1_raw = prog;
				server.getSim()._ad1 = ad1_raw;
				ad1_raw_tv.setText(Integer.toString(prog));
				break;
			case R.id.sim_sb_ad2:
				ad2_raw = prog;
				server.getSim()._ad2 = ad2_raw;
				ad2_raw_tv.setText(Integer.toString(prog));

				break;
			case R.id.sim_sb_rssitx:
				rssitx_raw = prog;
				server.getSim()._rssitx = rssitx_raw;
				rssitx_raw_tv.setText(Integer.toString(prog));
				break;
			case R.id.sim_sb_rssirx:
				rssirx_raw = prog;
				server.getSim()._rssirx = rssirx_raw;
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
		btnToggleFileSim.setChecked(fileSimThread != null ? fileSimThread
				.isThreadRunning() : false);
	}

	public void onStop() {
		super.onStop();
		// mTts.stop();
		if (FrSkyServer.D)
			Log.i(TAG, "onStop");
	}
}
