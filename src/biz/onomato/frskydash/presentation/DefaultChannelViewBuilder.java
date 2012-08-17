package biz.onomato.frskydash.presentation;

import android.app.Activity;
import android.content.Intent;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.R;
import biz.onomato.frskydash.activities.ActivityChannelConfig;
import biz.onomato.frskydash.activities.ActivityDashboard;
import biz.onomato.frskydash.domain.Channel;
import biz.onomato.frskydash.util.Logger;

/**
 * channel view builder for default channels
 * 
 * @author hcpl
 *
 */
public class DefaultChannelViewBuilder implements ChannelViewBuilder {
	
	/**
	 * tag for logging
	 */
	private static final String TAG = "DefaultChannelViewBuilder";
	
	@Override
	public View buildChannelView(Activity a, int n, Channel c){
		// combine all in a single view so we can move that code to
		// channel presentation builder
		LinearLayout singleChannelView = new LinearLayout(a);
		singleChannelView.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		singleChannelView.setOrientation(LinearLayout.VERTICAL);
		
		buildChannelView(a, singleChannelView, c, n);
		
		return singleChannelView;

	}
	
	/**
	 * Channel presentation related code for a complete channel view
	 * 
	 * @param singleChannelView
	 * @param c
	 * @param n
	 */
	private void buildChannelView(Activity a, LinearLayout singleChannelView, final Channel c, int n){
		// create layout objects
		LinearLayout llLine = new LinearLayout(a);
		llLine.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

		LinearLayout llVals = new LinearLayout(a);
		llVals.setLayoutParams(new LinearLayout.LayoutParams(0,
				LinearLayout.LayoutParams.WRAP_CONTENT, 1));
		llVals.setGravity(Gravity.CENTER_HORIZONTAL);

		//create description view
		TextView tvDesc = createChannelDescriptionView(a, c);
		singleChannelView.addView(tvDesc);

		// create channel value view
		createChannelValueView(a, n, c, llLine, llVals);
		singleChannelView.addView(llLine);
	}

	/**
	 * Channel presentation related code for channel description
	 * 
	 * @param c
	 * @return
	 */
	private TextView createChannelDescriptionView(Activity a, final Channel c) {
		// Add Description
		TextView tvDesc = new TextView(a);
		tvDesc.setText(c.getDescription());
		tvDesc.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		return tvDesc;
	}

	/**
	 * channel presentation related code for channel value
	 * 
	 * @param n
	 * @param c
	 * @param llLine
	 * @param llVals
	 * @return
	 */
	private void createChannelValueView(final Activity a, int n, final Channel c, LinearLayout llLine,
			LinearLayout llVals) {
		// btn
		ImageButton btnEdit = new ImageButton(a);
		// btnEdit.setText("...");
		btnEdit.setImageResource(R.drawable.ic_menu_edit);

		int height = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, 40, a.getResources()
						.getDisplayMetrics());
		btnEdit.setLayoutParams(new LinearLayout.LayoutParams(height,
				height));

		btnEdit.setScaleType(ImageView.ScaleType.CENTER_CROP);
		// ID for delete should be 100+channelId
		btnEdit.setId(ActivityDashboard.ID_CHANNEL_BUTTON_EDIT + c.getId());

		btnEdit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//debug info on delete
				Logger.d(TAG, "Edit channel "
						+ FrSkyServer.getCurrentModel().getChannels().get(v.getId() - 1000)
								.getDescription());
				// Launch editchannel with channel attached..
				Intent i = new Intent(a,
						ActivityChannelConfig.class);
				i.putExtra(ActivityChannelConfig.EXTRA_CHANNEL_ID,
						v.getId() - ActivityDashboard.ID_CHANNEL_BUTTON_EDIT);
				i.putExtra(ActivityChannelConfig.EXTRA_MODEL_ID,c.getModelId());
				a.startActivityForResult(i, ActivityDashboard.CHANNEL_CONFIG_RETURN);
			}
		});

		llLine.addView(btnEdit);

		// Value
		Logger.d(TAG, "Add TextView for Value: " + c.getValue(true));
		TextView tvValue = new TextView(a);
		tvValue.setText("" + c.getValue());
		tvValue.setGravity(Gravity.RIGHT);

		tvValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 35);
		tvValue.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		tvValue.setId(ActivityDashboard.ID_CHANNEL_TEXTVIEW_VALUE + n);
		c.setTextViewId(ActivityDashboard.ID_CHANNEL_TEXTVIEW_VALUE + n);
		llVals.addView(tvValue);
		// llLine.addView(tvValue);

		// Unit
		Logger.d(TAG, "Add TextView for Unit: " + c.getShortUnit());
		TextView tvUnit = new TextView(a);
		tvUnit.setText("" + c.getShortUnit());
		tvUnit.setGravity(Gravity.LEFT);
		LinearLayout.LayoutParams llpUnits = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		llpUnits.setMargins(10, 0, 0, 0);
		tvUnit.setLayoutParams(llpUnits);
		tvUnit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
		llVals.addView(tvUnit);
		// llVals.setBackgroundColor(0xffff0000);

		llLine.addView(llVals);

		ImageView speakerV = new ImageView(a);
		// speakerV.setBackgroundResource(android.R.drawable.ic_lock_silent_mode);
		if (c.getSilent()) {
			// speakerV.setImageResource(android.R.drawable.ic_lock_silent_mode);
			speakerV.setImageResource(R.drawable.ic_lock_silent_mode);
		} else {
			speakerV.setImageResource(R.drawable.ic_lock_silent_mode_off);
			speakerV.setColorFilter(0xff00ff00);
		}
		speakerV.setClickable(true);
		speakerV.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT, 0));
		speakerV.setId(ActivityDashboard.ID_CHANNEL_BUTTON_SILENT + c.getId());
		speakerV.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ImageView iv = (ImageView) v;
				int channelId = v.getId() - ActivityDashboard.ID_CHANNEL_BUTTON_SILENT;
				Logger.d(TAG, "Change silent on channel with id: " + channelId);
				Channel c = FrSkyServer.getCurrentModel().getChannels().get(channelId);
				// if(DEBUG)
				// Log.d(TAG,"Edit channel "+currentModel.getChannels()[v.getId()-1000].getDescription());
				Logger.d(TAG, "Toggle silent on " + c.getDescription());
				boolean s = !c.getSilent();
				c.setSilent(s);
				// c.saveToDatabase();
				// FrSkyServer.database.saveChannel(c);
				FrSkyServer.saveChannel(c);
				// or SAVE_MODEL
				if (s) {
					iv.setImageResource(R.drawable.ic_lock_silent_mode);
					// iv.setColorFilter(0xff00ff00);
					iv.clearColorFilter();
				} else {
					iv.setImageResource(R.drawable.ic_lock_silent_mode_off);
					iv.setColorFilter(0xff00ff00);
				}

				// Launch editchannel with channel attached..
			}
		});

		llLine.addView(speakerV);
	}

}
