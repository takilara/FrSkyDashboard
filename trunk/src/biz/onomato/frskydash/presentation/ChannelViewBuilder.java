package biz.onomato.frskydash.presentation;

import android.app.Activity;
import android.view.View;
import biz.onomato.frskydash.domain.Channel;

public interface ChannelViewBuilder {

	/**
	 * build view for channel
	 * 
	 * @param a
	 * @param n
	 * @param c
	 * @return
	 */
	public abstract View buildChannelView(Activity a, int n,
			Channel c);

}
