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
import biz.onomato.frskydash.domain.Channel;
import biz.onomato.frskydash.fragments.FragmentStatus.OnStatusFragmentInteraction;
import biz.onomato.frskydash.util.Logger;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * @author eso
 *
 */
public class FragmentChannelList extends Fragment{
	//TODO: Add interface to all neccessary user interaction
		// * Edit Channel
		// * Toggle speaker on channel
	private static final String TAG = "Fragment ChannelList";
	private static final int GUI_UPDATE_SLEEP_MS = 200;
	private Handler tickHandler;
	private Runnable runnableTick;
	
	private LayoutInflater mInflater;
	private ViewGroup mContainer;
	
	private LinearLayout llChannels=null;

	//OnChannelListFragmentInteraction mCallback;
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        // This makes sure that the container activity has implemented
        // the callback interfaces. If not, it throws an exception
        
        //FIXME: need to add interactions for the channel
        
//        try {
//            mCallback = (OnStatusFragmentInteraction) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
//                    + " must implement OnConfigCurrentModelListener");
//        }
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
					updateValues();

				tickHandler.postDelayed(this, GUI_UPDATE_SLEEP_MS);
			}
		};

		
		
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		Logger.w(TAG, "onCreateView");
        // Inflate the layout for this fragment
		//TODO: Create base LinearLayout for the Fragment
		//TODO: Iterate channels
			//TODO: For each channel, inflate layout, and add to LinearLayout
		//View v = inflater.inflate(R.layout.fragment_status, container, false);
		
		//mInflater = inflater;
		//mContainer = container;
		//LinearLayout llChannels = new LinearLayout(getActivity());
	//		llChannels = new LinearLayout(getActivity());
	//		llChannels.setId(999999);
	//		llChannels.setLayoutParams(new LinearLayout.LayoutParams(
	//				LinearLayout.LayoutParams.MATCH_PARENT,
	//				LinearLayout.LayoutParams.WRAP_CONTENT));

		//TextView tvDesc = new TextView(getActivity());
		//tvDesc.setText("BBBBBRRRRRRGGG");
		//llChannels.addView(tvDesc);
		View v = inflater.inflate(R.layout.view_channel_on_dashboard, container, false);
		
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

		//createChannelViews();
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
	private void updateValues() 
	{
		// Iterate ChannelList and update values
		//TODO: Decide if iterate on server channels, or local views 
	}
	
	private void createChannelViews()
	{
		  
		Logger.w(TAG, "Creating channel views");
		TextView tvDesc = new TextView(getActivity());
		tvDesc.setText("AAAAARRRRRRGGG");
		llChannels.addView(tvDesc);
//		for(Channel ch : FrSkyServer.getCurrentModel().getChannels().values())
//		{
//			View cv = mInflater.inflate(R.layout.view_channel_on_dashboard, mContainer, false);
//			cv.setId(ch.getId());
//			TextView channelDesc = (TextView) cv.findViewById(R.id.channelDesc);
//			channelDesc.setText(ch.getDescription());
//			
//			
//			//llChannels.addView(cv);
//		}
	}
	
}
