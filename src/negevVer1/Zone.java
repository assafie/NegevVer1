package negevVer1;

public class Zone {
	
	public enum Type{
		UNSUITABLE,
		SUITABLE,
		CLAIMED // not sure if need CLAIMED
	}
	
	public Type type;
	double ID;
	double area; 
	double circumference;
	
	Zone(double myID, double myArea, double myCircum, double myType){
		ID = myID;
		area = myArea;
		circumference = myCircum;
		if( myType==1){
			type = Type.UNSUITABLE;
		}
		else{
			type = Type.SUITABLE;
		}
		
	}
	
	public Type getType(){
		return this.type;
	}
	
	
}