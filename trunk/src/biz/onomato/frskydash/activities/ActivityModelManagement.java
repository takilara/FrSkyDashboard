/*
 * Copyright 2011-2013, Espen Solbu, Hans Cappelle
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

package biz.onomato.frskydash.activities;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.R;
import biz.onomato.frskydash.domain.Model;
import biz.onomato.frskydash.util.Logger;

/**
 * Activity with overview of all configured models
 *
 */
public class ActivityModelManagement extends ActivityBase implements OnClickListener {
	private static final String TAG = "Model Management";
	//private FrSkyServer server;
	//private static final int MODEL_CONFIG_RETURN=0;

	private LinearLayout llModelsLayout;
	private Button btnAddModel;
	private ArrayList<RadioButton> rbList;
	private RadioButton rbCurrentModel;
	//private boolean DEBUG=true;
//	@SuppressWarnings("unused")
	//private int _deleteId=-1;
	//private String _deleteName=null;
	//private int mModelToDelete = -1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON|WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		setContentView(R.layout.activity_modelmanagement);

		rbList = new ArrayList<RadioButton>();
		// Setup components from screen
		Logger.i(TAG,"Setup widgets");
		llModelsLayout = (LinearLayout) findViewById(R.id.llModelsLayout);
		btnAddModel = (Button) findViewById(R.id.btnAddModel);
		
		// Add listeners
		Logger.i(TAG,"Add Listeners");
		//btnAddModel.setOnClickListener(this);
		btnAddModel.setOnClickListener(new OnClickListener() {
			public void onClick(View v){
				Logger.d(TAG,"User Clicked Add Model");
				Intent i = new Intent(getApplicationContext(), ActivityModelConfig.class);
	    		i.putExtra("modelId", (int) -1);	// Should create new model
	    		startActivityForResult(i,MODEL_CONFIG_RETURN);
			}
		});
		
        //doBindService();
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		//populateModelList(); 
		//test.setText(oAd1.toString());
	}
	
	public void onClick(View v)
	{
		int id = v.getId();
		if(id>10000)		// Selecting an existing model
		{
			//remove check from all the radio buttons
			for(RadioButton rb : rbList)
			{
				//Log.w(TAG,"Remove the selection on RadioButton with id "+rb.getId());
				rb.setChecked(false);
			}
			
			//rbList.clear();
			
			int ii = id -10000;
			//rbCurrentModel.setChecked(false);
			rbCurrentModel = (RadioButton) findViewById(id);
			rbCurrentModel.setChecked(true);
			//Model m = new Model(getApplicationContext());
			//m.loadFromDatabase(ii);
			//Model m = FrSkyServer.database.getModel(ii);
			//Model m = FrSkyServer.modelMap.get(ii);
			
			if(server!=null)
			{
				//server.setCurrentModel(m);
				FrSkyServer.setCurrentModel(ii);
				
			}
		}
		else if(id>1000)	// Edit existing model
		{
			int ii=id-1000;
			Intent i = new Intent(this, ActivityModelConfig.class);
    		i.putExtra("modelId", (int) ii);	// Should edit existing model
    		
    		startActivityForResult(i,MODEL_CONFIG_RETURN);
		}
		else if(id>100) // Delete existing model
		{
			int ii=id-100;
			Logger.d(TAG,"Delete model with id:"+ii);
			
			
			//showDeleteDialog(ii);
			
			Bundle args = new Bundle();
	        args.putInt(DELETE_ID_KEY, ii);
	        args.putString(DELETE_NAME_KEY, FrSkyServer.modelMap.get(ii).getName());
			
	        removeDialog(DIALOG_DELETE_MODEL);
			showDialog(DIALOG_DELETE_MODEL,args);
		}
		else
		{
//			switch(v.getId())
//			{
//			///TODO: replace with actions for models
//				case R.id.btnAddModel:
//					if(DEBUG) Log.d(TAG,"User Clicked Add Model");
//					Intent i = new Intent(this, ActivityModelConfig.class);
//		    		i.putExtra("modelId", (long) -1);	// Should create new model
//		    		startActivityForResult(i,MODEL_CONFIG_RETURN);
//					break;
//				default:
//					if(DEBUG) Log.w(TAG,"Unknown button:"+v.getId());
//			}
		}
	}
	
	/**
	 * load all available models. The list is populated with controls that are
	 * referred to with 100+id, 1000+id or 10.000+id.
	 */
	private void populateModelList() {
		// llModelsLayout holds dynamically loaded views, according to the
		// amount of models that are available
		llModelsLayout.removeAllViews();
		rbList.clear();
		// init the current model id, if server is not available we need to init
		// as -1. Otherwise we can get the id form the server
		long currentModelId = server == null ? -1 : FrSkyServer.getCurrentModel()
				.getId();

		// get modelcount so we can check how many models are available when iterating
		int modelCount = FrSkyServer.modelMap.size();
		// iterate all models
		for(Model m : FrSkyServer.modelMap.values())
		{
			Logger.d(TAG,"Set Model (id,name): "+m.getId()+", "+m.getName());
			LinearLayout ll = new LinearLayout(getApplicationContext());
			//id of the model in this iteration (not the current selected model!)
			int id = m.getId();
			
			//textview for the name of the model
			TextView tvName = new TextView(getApplicationContext());
			tvName.setText(m.getName());
			tvName.setLayoutParams(new LinearLayout.LayoutParams(0,LayoutParams.WRAP_CONTENT,1));
			
			//radio button for selection of model
			RadioButton rdThisModel = new RadioButton(getApplicationContext());
			// dynamic Ids of radiobuttons are 10.000+id form the model they refer to
			rdThisModel.setId(10000+id);
			rbList.add(rdThisModel);
			rdThisModel.setOnClickListener(this);
			// only set them selected if this id matches the current model id
			rdThisModel.setChecked(id==currentModelId);
			
			//calculate height
			int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());			
			
			// only allow deletion if there is more than one model
			ImageButton btnDelete = new ImageButton(getApplicationContext());
			btnDelete.setImageResource(R.drawable.ic_menu_delete);
			btnDelete.setScaleType(ImageView.ScaleType.CENTER_CROP);
			btnDelete.setLayoutParams(new LinearLayout.LayoutParams(height,height));
			btnDelete.setId(100+id); // ID for delete should be 100+channelId
			btnDelete.setOnClickListener(new OnClickListener(){
				public void onClick(View v){
					Logger.d(TAG,"Delete model with id:"+(v.getId()-100));
					showDeleteDialog(v.getId()-100);
				}
			});
			//delete model button can only be enabled if there are more than one models
			btnDelete.setEnabled(modelCount>1);
			
			//edit model button
			ImageButton btnEdit = new ImageButton(getApplicationContext());
			btnEdit.setImageResource(R.drawable.ic_menu_edit);
			btnEdit.setScaleType(ImageView.ScaleType.CENTER_CROP);
			btnEdit.setLayoutParams(new LinearLayout.LayoutParams(height,height));
			btnEdit.setId(1000+id);// ID for edit should be 1000+channelId
			btnEdit.setOnClickListener(this);
			
			//add all the widgets to the list
			ll.addView(rdThisModel);
			ll.addView(tvName);
			ll.addView(btnEdit);
			ll.addView(btnDelete);			
			
			ll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
			//ll.setGravity();
			
			llModelsLayout.addView(ll);
			
		}
	}
	
	
	
	private void showDeleteDialog(int id)
	{
		//_deleteId = id;
		//_deleteName = FrSkyServer.modelMap.get(id).getName();

		Bundle args = new Bundle();
        args.putInt(DELETE_ID_KEY, id);
        args.putString(DELETE_NAME_KEY, FrSkyServer.modelMap.get(id).getName());
		
        removeDialog(DIALOG_DELETE_MODEL);
		showDialog(DIALOG_DELETE_MODEL,args);
	}
	
	


	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
    	switch (requestCode)
    	{
    		case MODEL_CONFIG_RETURN:
    			switch(resultCode)
	        	{
	        		case RESULT_OK:
	        			Logger.i(TAG,"User saved new settings");
	        			break;
	        		case RESULT_CANCELED:
	        			Logger.i(TAG,"User cancelled with back");
	        			break;
	        	}
    			break;
    		
    	}
    	// update model list
    	populateModelList();
    }

	/* (non-Javadoc)
	 * @see biz.onomato.frskydash.activities.ActivityBase#onModelChanged()
	 */
	@Override
	protected void onCurrentModelChanged() {
		if(server!=null)
		{
			try
			{
				rbCurrentModel.setChecked(false); // Clear old radiobutton
			}
			catch (Exception e) 
			{}
			rbCurrentModel = (RadioButton) findViewById((int) (10000+FrSkyServer.getCurrentModel().getId()));
			rbCurrentModel.setChecked(true);
		}
		
	}

	/* (non-Javadoc)
	 * @see biz.onomato.frskydash.activities.ActivityBase#onServerConnected()
	 */
	@Override
	void onServerConnected() {
		populateModelList();
		
		rbCurrentModel = (RadioButton) findViewById((int) (10000+FrSkyServer.getCurrentModel().getId()));
		rbCurrentModel.setChecked(true);
	}

	/* (non-Javadoc)
	 * @see biz.onomato.frskydash.activities.ActivityBase#onServerDisconnected()
	 */
	@Override
	void onServerDisconnected() {
		// TODO Auto-generated method stub
		
	}

	

	/* (non-Javadoc)
	 * @see biz.onomato.frskydash.activities.ActivityBase#onModelMapChanged()
	 */
	@Override
	protected void onModelMapChanged() {
		// TODO Auto-generated method stub
		populateModelList();
		
	}
}
