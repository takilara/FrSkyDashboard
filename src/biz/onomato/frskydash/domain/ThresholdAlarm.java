/**
 * 
 */
package biz.onomato.frskydash.domain;

/**
 * @author eso
 *
 */
public class ThresholdAlarm extends Alarm {

	public static final int GREATER_THAN = 1;
	public static final int EQUAL_TO = 0;
	public static final int LESSER_THAN = -1;
	
	private double mThresholdMedium;
	private double mThresholdCritical;
	private int mRelation;
	
	public ThresholdAlarm(String description)
	{
		super(description);
	}
	
	public boolean setMediumThreshold(double threshold)
	{
		mThresholdMedium = threshold;
		return true;	//FIXME: change to reflect success
	}
	
	public boolean setCriticalThreshold(double threshold)
	{
		mThresholdCritical = threshold;
		return true;	//FIXME: change to reflect success
	}
	
	public void setRelation(int greaterThanOrLowerThan)
	{
		mRelation = greaterThanOrLowerThan;
	}
	
	@Override
	public int analyze()
	{
		if(mRelation==GREATER_THAN)
		{
			mAlarmLevel = ALARM_LEVEL_NORMAL;
			if(mSourceChannel.getValue()>mThresholdMedium)
			{
				mAlarmLevel = ALARM_LEVEL_MEDIUM;
			}
			if(mSourceChannel.getValue()>mThresholdCritical)
			{
				mAlarmLevel = ALARM_LEVEL_CRITICAL;
			}
		}
		else if (mRelation==LESSER_THAN)
		{
			mAlarmLevel = ALARM_LEVEL_NORMAL;
			if(mSourceChannel.getValue()<mThresholdMedium)
			{
				mAlarmLevel = ALARM_LEVEL_MEDIUM;
			}
			if(mSourceChannel.getValue()<mThresholdCritical)
			{
				mAlarmLevel = ALARM_LEVEL_CRITICAL;
			}
		}
		else if (mRelation==EQUAL_TO)
		{
			mAlarmLevel = ALARM_LEVEL_NORMAL;
			if(mSourceChannel.getValue()==mThresholdMedium)
			{
				mAlarmLevel = ALARM_LEVEL_MEDIUM;
			}
			if(mSourceChannel.getValue()==mThresholdCritical)
			{
				mAlarmLevel = ALARM_LEVEL_CRITICAL;
			}
		}
		return mAlarmLevel;
	}
}
