package biz.onomato.frskydash.presentation;

import biz.onomato.frskydash.domain.Channel;

/**
 * singleton for building channel views
 * 
 * @author hcpl
 *
 */
public class ChannelViewFactory {

	/**
	 * tag for logging
	 */
	private static final String TAG = "ChannelViewFactory";
	
	private static ChannelViewFactory instance = null;
	
	private ChannelViewFactory(){
		//TODO
	}
	
	public static ChannelViewFactory getInstance(){
		if (instance == null)
			instance = new ChannelViewFactory();
		return instance;
	}
	
	public ChannelViewBuilder getBuilderForChannel(Channel channel){
		//TODO
		return new DefaultChannelViewBuilder();
	}

	
}
