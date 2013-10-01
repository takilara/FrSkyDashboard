/*
 * Copyright 2011-2013, Espen Solbu
 * 
 * This file is part of FrSky Dashboard.
 *
 *  FrSky Dashboard is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FrSky Dashboard is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FrSky Dashboard.  If not, see <http://www.gnu.org/licenses/>.
 */

package biz.onomato.frskydash.fragments;

import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.R;
import biz.onomato.frskydash.util.Logger;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * @author eso
 *
 */
public class FragmentStatus extends Fragment {
	
	private static final String TAG = "Fragment STATUS";
	private static final int GUI_UPDATE_SLEEP_MS = 200;
	private TextView tv_statusBt, tv_statusRx, tv_statusTx;
	private TextView tv_rssitx, tv_rssirx, tv_fps, tv_bad;
	
	private ImageButton btnConfigCurrentModel, btnConfigCurrentModelsAlarms;
	private TextView tv_modelName;
	
	private Handler tickHandler;
	private Runnable runnableTick;
	
	private int mFlashCounter = 0;
	OnStatusFragmentInteraction mCallback;
	
	
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        // This makes sure that the container activity has implemented
        // the callback interfaces. If not, it throws an exception
        try {
            mCallback = (OnStatusFragmentInteraction) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnConfigCurrentModelListener");
        }
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Logger.i(TAG, "onCreate");

		// Handler to perform cyclic updates
		tickHandler = new Handler();
		runnableTick = new Runnable() {
			@Override
			public void run() {
					//Logger.w(TAG,"Update Fragment GUI");
					updateStatus();

				tickHandler.postDelayed(this, GUI_UPDATE_SLEEP_MS);
			}
		};

		
		
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
		View v = inflater.inflate(R.layout.fragment_status, container, false);
		
		// Find various GUI Components in the fragment
		tv_statusBt = (TextView) v.findViewById(R.id.fs_tvConnBt);
		tv_statusRx = (TextView) v.findViewById(R.id.fs_tvConnRx);
		tv_statusTx = (TextView) v.findViewById(R.id.fs_tvConnTx);

		tv_rssitx = (TextView) v.findViewById(R.id.fs_tvRSSItx);
		tv_rssirx = (TextView) v.findViewById(R.id.fs_tvRSSIrx);
		tv_fps = (TextView) v.findViewById(R.id.fs_tvFps);
		tv_bad = (TextView) v.findViewById(R.id.fs_tvBad);

		tv_modelName = (TextView) v.findViewById(R.id.fs_tvModelName);
		
		btnConfigCurrentModel = (ImageButton) v.findViewById(R.id.dash_btnConfigCurrentModel);
		//btnConfigCurrentModelsAlarms = (ImageButton) v.findViewById(R.id.dash_btnConfigCurrentModelsAlarms);
		
		btnConfigCurrentModel.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Logger.d(TAG, "Launch config on current model");
				mCallback.onConfigCurrentModel();
			}
		});
		//btnConfigCurrentModelsAlarms.setOnClickListener(this);
		return v;
    }
	
	/**
	 * Place code that needs to be executed *after* the activity is fully created here
	 */
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Logger.i(TAG, "onActivityCreated");
        
        // Start the updates
        tickHandler.postDelayed(runnableTick, GUI_UPDATE_SLEEP_MS);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		tickHandler.removeCallbacks(runnableTick);
	}
	
	@Override
	public void onResume() {
		super.onResume();

		// enable updates
		Logger.d(TAG, "onResume");
		// _enableDebugActivity=false;
		tickHandler.removeCallbacks(runnableTick);
		tickHandler.post(runnableTick);
	}

	
/**
 * Update the contents of the Status Fragment
 * @author Espen Solbu 	
 */
	private void updateStatus() {
		//TODO: Reduce concatenation to boost performance
		//TODO: stop doing findViewById (suggestion to build view list with channel id's as keys when changing model/resume)
		//TODO: stop containing viewId in Channel
		
			tv_modelName.setText(FrSkyServer.getCurrentModelName());
	
		
			tv_rssitx.setText("RSSItx: "
					+ FrSkyServer.getSourceChannel(
							FrSkyServer.CHANNEL_ID_RSSITX).toValueString());
			tv_rssitx.setTextColor(FrSkyServer.getSourceChannel(
					FrSkyServer.CHANNEL_ID_RSSITX).getColor());
			
			
			tv_rssirx.setText("RSSIrx: "
					+ FrSkyServer.getSourceChannel(
							FrSkyServer.CHANNEL_ID_RSSIRX).toValueString());
			tv_rssirx.setTextColor(FrSkyServer.getSourceChannel(
							FrSkyServer.CHANNEL_ID_RSSIRX).getColor());
			
			tv_fps.setText("FPS: " + FrSkyServer.getFps());
			tv_bad.setText("Bad: " + FrSkyServer.badFrames+"/"+FrSkyServer.getDroppedFrames());
			
			
			
			// FIXME: Simplyfy this (move stuff to server)
			
			// set status lights
			if (FrSkyServer.statusBt) {
				tv_statusBt.setBackgroundColor(0xff00aa00);
				tv_statusBt.setText("Bt: UP");
				tv_statusBt.setTextColor(0xff000000);
			} else {
				// if connecting, do something else..
				if (FrSkyServer.getConnecting()) {
					mFlashCounter++;
					if (mFlashCounter >= 8)
						mFlashCounter = 0;
					if (mFlashCounter < 4) {
						tv_statusBt.setBackgroundColor(0xff00aa00);
						tv_statusBt.setText("Bt: Connecting");
						tv_statusBt.setTextColor(0xff000000);
					} else {
						tv_statusBt.setBackgroundColor(0xff000000);
						tv_statusBt.setText("Bt: Connecting");
						tv_statusBt.setTextColor(0xffaaaaaa);
					}
				} else {
					tv_statusBt.setBackgroundColor(0xffff0000);
					tv_statusBt.setText("Bt: DOWN");
					tv_statusBt.setTextColor(0xff000000);
				}
			}

			// if(server.fps>0)
			if (FrSkyServer.statusRx) {
				tv_statusRx.setBackgroundColor(0xff00aa00);
				tv_statusRx.setText("Rx: UP");

				// tv_fps.setTextColor(0xffbbbbbb);
			} else {
				tv_statusRx.setBackgroundColor(0xffff0000);
				tv_statusRx.setText("Rx: DOWN");

				// tv_fps.setTextColor(0xffff5500);
			}

			if (FrSkyServer.statusTx) {
				tv_statusTx.setBackgroundColor(0xff00aa00);
				tv_statusTx.setText("Tx: UP");
			} else {
				tv_statusTx.setBackgroundColor(0xffff0000);
				tv_statusTx.setText("Tx: DOWN");
			}

		
	}

	
	// Container Activity must implement these interfaces
	/**
	 * Interface definition for a callback to be invoked when user performs actions in the Status Fragment.
	 * @author Espen Solbu
	 *
	 */
    public interface OnStatusFragmentInteraction {
    	/**
    	 * This hook is called whenever the user performs an action to configure the current model
    	 */
        public void onConfigCurrentModel();
    }
    
    // I think i'll remove this button from the frontpage
//    public interface OnConfigCurrentModelModuleAlarmsListener {
//        public void onConfigCurrentModelModuleAlarms();
//    }
}
