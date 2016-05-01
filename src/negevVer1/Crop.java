package negevVer1;

public class Crop {
	
	public enum Type{
		COTTON,
		CEREAL 		
	}
	
	public Type cropType;
	

	Crop(Type crop){
		cropType = crop;
	}

	
	Type getCropType() {
		return cropType;
	}
		

}
