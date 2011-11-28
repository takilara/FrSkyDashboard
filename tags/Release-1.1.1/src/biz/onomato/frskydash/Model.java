package biz.onomato.frskydash;

public class Model {
	
	private static final String TAG="ModelClass";
	
	public static final int MODEL_TYPE_HELICOPTER=0;
	public static final int MODEL_TYPE_FIXEDWING=1;
	public static final int MODEL_TYPE_CAR=2;
	public static final int MODEL_TYPE_BOAT=3;
	public static final int MODEL_TYPE_MULTIROTOR=4;
	public static final int MODEL_TYPE_UNKNOWN=-1;
	
	private int _type;
	private String _name;
	

	// Constructor
	public Model(String modelName,int modelType)
	{
		setName(modelName);
		setType(modelType);
	}
	public Model(String modelName)
	{
		this(modelName,MODEL_TYPE_UNKNOWN);
	}
	public Model()
	{
		this("Model 1",MODEL_TYPE_UNKNOWN);
	}
	


	
	
	public int getType() {
		return _type;
	}


	public void setType(int modelType) {
		this._type = modelType;
	}


	public String getName() {
		return _name;
	}


	public void setName(String modelName) {
		this._name = modelName;
	}


	// I need to be able to add channels to this model
	public void addChannel(Channel channel)
	{
		
	}
	
	// I need to be able to delete channels from this model
	public void deleteChannel(Channel channel)
	{
		
	}
	
	// I need to be able to add alarms to this model
	public void addAlarm(Alarm alarm)
	{
		
	}
	
	
	// I need to be able to delete alarms from this model
	public void deleteAlarm(Alarm alarm)
	{
		
	}
	
	// I need to be able to save settings to file or config storage
	public void saveSettings()
	{
		
	}
	
	// I need to be able to load settings from file or config storage
	public void loadSettings()
	{
		
	}

}
