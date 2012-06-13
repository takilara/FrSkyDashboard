/**
 * 
 */
package biz.onomato.frskydash.activities;

import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.R;
import biz.onomato.frskydash.domain.Model;
import biz.onomato.frskydash.util.Logger;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author eso
 *
 */
abstract class ActivityBase extends Activity {
	private static final String TAG = "Base Activity";
	
	protected static final int DIALOG_ABOUT_ID = 0;
	protected static final int DIALOG_ALARMS_MISMATCH = 1;
	protected static final int DIALOG_DELETE_MODEL = 2;
	protected static final int DIALOG_DELETE_CHANNEL = 3;

	protected static final String CURRENT_MODEL_NAME_KEY = "CurrentModelName";
	protected static final String CURRENT_MODEL_ID_KEY = "CurrentModelId";
	protected static final String TARGET_MODEL_NAME_KEY = "TargetModelName";
	protected static final String TARGET_MODEL_ID_KEY = "TargetModelId";
	protected static final String DELETE_ID_KEY = "modelId";
	protected static final String DELETE_NAME_KEY = "modelName";
	protected static final String DELETE_CHANNEL_DESCRIPTION_KEY = "channelDescription";
	protected static final String DELETE_CHANNEL_ID_KEY = "channelId";

	protected static final String DELETE_CHANNEL_FROM_MODEL_ID_KEY = "modelId";

	
	
	protected int _clickToDebug = 0;
	protected int _targetModel = -1;
	
	protected FrSkyServer server;
	
	private final IntentFilter mIntentServerFilter = new IntentFilter();
	


	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Logger.i(TAG,"onCreate");
		super.onCreate(savedInstanceState);
		//mIntentServerFilter = new IntentFilter();
		
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		mIntentServerFilter.addAction(FrSkyServer.MESSAGE_STARTED);
		mIntentServerFilter.addAction(FrSkyServer.MESSAGE_ALARM_MISMATCH);
		mIntentServerFilter.addAction(FrSkyServer.MESSAGE_CURRENTMODEL_CHANGED);
		mIntentServerFilter.addAction(FrSkyServer.MESSAGE_MODELMAP_CHANGED);
		
		doBindService();
	}
	
	@Override
	public void onDestroy() {
		Logger.i(TAG, "onDestroy");
		super.onDestroy();
		//doUnbindService();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		registerReceiver(mIntentServerReceiver, mIntentServerFilter); // Used to capture server messages relevant for all activities
		


	}
	
	@Override
	public void onPause() {
		super.onPause();
		Logger.i(TAG, "onPause");
		unregisterReceiver(mIntentServerReceiver);
	}
	
	/**
	 * Executed whenever there is a model change
	 * 
	 */
	abstract protected void onCurrentModelChanged();
	
	/**
	 * Executed whenever contents of modelmap is changed
	 */
	abstract protected void onModelMapChanged();
	
	/**
	 * Activityspecific code that needs to run after properly connected to the server goes here
	 */
	abstract void onServerConnected();
	/**
	 * Activityspecific Cleanup that needs to run before server is destroyed
	 */
	abstract void onServerDisconnected();
	
	protected final void doBindService() {
		Logger.i(TAG, "Start the server service if it is not already started");
		startService(new Intent(this, FrSkyServer.class));
		Logger.i(TAG, "Try to bind to the service");
		getApplicationContext().bindService(
				new Intent(this, FrSkyServer.class), mConnection, 0);
	}

	protected final void doUnbindService() {
		if (server != null) {
			// Detach our existing connection.
			unbindService(mConnection);
		}
		
	}

	private final ServiceConnection mConnection = new ServiceConnection() {
		public final void onServiceConnected(ComponentName className, IBinder binder) {
			Logger.i(TAG, "Bound to Service");
			server = ((FrSkyServer.MyBinder) binder).getService();
			onServerConnected();
		}
		
		public final void onServiceDisconnected(ComponentName className) {
			onServerDisconnected();
			server = null;
		}
	};
	
	
	/**
	 * Put all dialogs here
	 */
	protected Dialog onCreateDialog(int id, Bundle args) {
		Dialog dialog;
		Logger.i(TAG, "Make a dialog on context: " + this.getPackageName());

		switch (id) {
		case DIALOG_ABOUT_ID:
			Logger.i(TAG, "About dialog");
			dialog = new Dialog(this);
			dialog.setContentView(R.layout.about_dialog);
			dialog.setTitle("About " + getString(R.string.app_name));
			TextView tvAboutVersion = (TextView) dialog
					.findViewById(R.id.tvAboutVersion);
			TextView tvAboutAuthor = (TextView) dialog
					.findViewById(R.id.tvAboutAuthor);
			tvAboutAuthor.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					// Log.d(TAG,"clicked author");
					_clickToDebug++;
					if (_clickToDebug > 5) {
						Logger.d(TAG, "Enable debugging");
						Toast.makeText(getApplicationContext(),
								"Debugging enabled", Toast.LENGTH_LONG).show();
						// MenuItem tDebug = (MenuItem)
						// menu.findItem(R.id.menu_debug);
						enableDebugging();
						// tDebug.setVisible(false);
					}
				}

			});

			PackageManager pm = this.getPackageManager();
			try {
				PackageInfo pInfo = pm.getPackageInfo(this.getPackageName(),
						PackageManager.GET_META_DATA);
				tvAboutVersion.setText("Version: " + pInfo.versionName);
				tvAboutAuthor.setText("Author: " + getString(R.string.author));
			} catch (Exception e) {
			}
			break;
		case DIALOG_ALARMS_MISMATCH:
			Logger.e(TAG, "Show alarm mismatch dialog");
			String mCurrentModelName = args.getString(CURRENT_MODEL_NAME_KEY);
			String mTargetModelName = args.getString(TARGET_MODEL_NAME_KEY);
			final int mCurrentModelId = args.getInt(CURRENT_MODEL_ID_KEY);
			final int mTargetModelId = args.getInt(TARGET_MODEL_ID_KEY);
//			Model tm = null;
			if (mTargetModelId != -1) {

				//tm = FrSkyServer.modelMap.get(mTargetModelId);
				Logger.e(TAG, "Allow switch to model " + mTargetModelName);
			}

			//final Model ttm = tm;
			//Model cm = server.getCurrentModel();
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Model Mismatch");
			String msg = "The module configuration seem to be different from the current model '"
					+ mCurrentModelName + "'.";
			if (mTargetModelId != -1) {
				msg += "\n\nThe model looks like '" + mTargetModelName + "'";
				
			}
			msg += "\n\nUse the 'Back' button to cancel.";
			builder.setMessage(msg);
			builder.setCancelable(true);
			builder.setPositiveButton("Update FrSky",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Logger.e(TAG,
									"Send the alarms for current model to module");
							server.sendAlarms(FrSkyServer.modelMap.get(mCurrentModelId));
						}
					});
			if (mTargetModelId != -1) {
				builder.setNeutralButton("Switch to '" + mTargetModelName + "'",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Logger.e(TAG, "Change Currentmodel");
								server.setCurrentModel(mTargetModelId);
								//populateChannelList();
							}
						});
			}
			builder.setNegativeButton("Update '" + mCurrentModelName + "'",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Logger.e(TAG, "Update alarms from module");
							//FrSkyServer.modelMap.get(mCurrentModelId).setFrSkyAlarms(server.getRecordedAlarmMap());
							server.getCurrentModel().setFrSkyAlarms(server.getRecordedAlarmMap());
							FrSkyServer.saveModel(server.getCurrentModel());
						}
					});

			AlertDialog alert = builder.create();
			//Logger.w(TAG, alert.toString());
			//alert.setOnDismissListener(this);

			dialog = alert;

			break;
		case DIALOG_DELETE_MODEL:
			String mName = args.getString(DELETE_NAME_KEY);
			final int mId = args.getInt(DELETE_ID_KEY);
			AlertDialog dlg = new AlertDialog.Builder(this).create();
			dlg.setTitle("Delete "+mName+"?");

			dlg.setMessage("Do you really want to delete the model '"+mName+"'?");
			
			dlg.setButton(AlertDialog.BUTTON_POSITIVE,"Yes", new DialogInterface.OnClickListener() {

	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	            	FrSkyServer.deleteModel(FrSkyServer.modelMap.get(mId));
	            }

	        });
	        dlg.setButton(AlertDialog.BUTTON_NEGATIVE,"No", new DialogInterface.OnClickListener() {

	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	            	Logger.i(TAG,"Cancel Deletion");
	            }

	        });
	        dialog = dlg;
			break;
			
		case DIALOG_DELETE_CHANNEL:
			String mDescription = args.getString(DELETE_CHANNEL_DESCRIPTION_KEY);
			final int mChannelId = args.getInt(DELETE_CHANNEL_ID_KEY);
			final int mModelId = args.getInt(DELETE_CHANNEL_FROM_MODEL_ID_KEY);
			AlertDialog dlg2 = new AlertDialog.Builder(this).create();
			dlg2.setTitle("Delete "+mDescription+"?");

			dlg2.setMessage("Do you really want to delete the channel '"+mDescription+"'?");
			
			dlg2.setButton(AlertDialog.BUTTON_POSITIVE,"Yes", new DialogInterface.OnClickListener() {

	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	            	
	            	FrSkyServer.modelMap.get(mModelId).removeChannel(mChannelId);
	            	
	            	//FIXME: this broadcast should come from FrSkyServer
	            	Intent i = new Intent();
	            	i.setAction(FrSkyServer.MESSAGE_MODELMAP_CHANGED);
	            	sendBroadcast(i);
	            	//populateChannelList();
	            }

	        });
	        dlg2.setButton(AlertDialog.BUTTON_NEGATIVE,"No", new DialogInterface.OnClickListener() {

	            @Override
	            public void onClick(DialogInterface dialog, int which) {

	                //Stop the activity
	            	//_deleteId=-1;
	            	Logger.i(TAG,"Cancel Deletion");
	            }

	        });
	        dialog = dlg2;
			break;

		default:
			dialog = null;
		}
		return dialog;
	}
	
	/**
	 * enable logging by updating flag on {@link Logger} class
	 */
	public void enableDebugging() {
		// _enableDebugActivity=true;
		Logger.D = true;
	}
	
	
	private final BroadcastReceiver mIntentServerReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String msg = intent.getAction();
			Bundle extras = intent.getExtras();
			Logger.i(TAG, "Received Broadcast: '" + msg + "'");
			if (msg.equals(FrSkyServer.MESSAGE_STARTED)) {	// currently not used
				Logger.i(TAG,
							"I have received BroadCast that the server has started");
			}

			else if (msg.equals(FrSkyServer.MESSAGE_ALARM_MISMATCH)) {
				_targetModel = intent.getIntExtra("modelId", -1);
				String mTargetModelName;
				if(_targetModel==-1){
					mTargetModelName = "";
				}
				else
				{
					mTargetModelName = FrSkyServer.modelMap.get(_targetModel).getName();
				}
					
				Logger.w(TAG, "Alarms are not matching");
				
				//dismiss the alarm mismatch dialog to force it to update when requested
				try{removeDialog(DIALOG_ALARMS_MISMATCH);}
				catch (IllegalArgumentException e) {
				// was not previously shown
				}
				
				Bundle args = new Bundle();
				args.putString(CURRENT_MODEL_NAME_KEY, server.getCurrentModel().getName());
				args.putInt(CURRENT_MODEL_ID_KEY, server.getCurrentModel().getId());
				args.putString(TARGET_MODEL_NAME_KEY, mTargetModelName);
				args.putInt(TARGET_MODEL_ID_KEY, _targetModel);
				
				showDialog(DIALOG_ALARMS_MISMATCH,args);
				// populateChannelList();
			}
			
			else if (msg.equals(FrSkyServer.MESSAGE_CURRENTMODEL_CHANGED)) {
				onCurrentModelChanged();
			}
			else if (msg.equals(FrSkyServer.MESSAGE_MODELMAP_CHANGED)) {
				onModelMapChanged();
			}

		}
	};
	
}
