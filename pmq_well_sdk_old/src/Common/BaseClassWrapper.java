package Common;

import com.cognos.developer.schemas.bibus._3.BaseClass;

public class BaseClassWrapper {
	
	private BaseClass myBaseClass = null;
	
	//Private default constructor, to prevent empty wrappers
	private BaseClassWrapper() {};
	
	//constructor
	public BaseClassWrapper(BaseClass newBaseClass)
	{
		myBaseClass = newBaseClass;
	}
	
	
	public BaseClass getBaseClassObject () 
	{
		return myBaseClass;
	}

	public void setBaseClassObject(BaseClass newBaseClassObject)
	{
		myBaseClass = newBaseClassObject;
	}
	
	//Override toString()
	public String toString()
	{
		if (myBaseClass != null)
		{
			return myBaseClass.getDefaultName().getValue();
		}
		
		return null;
	}
	
	public String getSearchPath()
	{
		if (myBaseClass != null)
		{
			return myBaseClass.getSearchPath().getValue();
		}
		
		return null;
	}
	
}
