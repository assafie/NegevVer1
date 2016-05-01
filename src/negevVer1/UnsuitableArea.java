package negevVer1;

import negevVer1.Pixel.Classification;
import negevVer1.UnsuitableArea.Type;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class UnsuitableArea {
	
	// 1: Natural Reserve ; 2: Urban ; 3: Forest ; 4: Riparian zone
	public enum Type{
		NATURAL_RES,
		URBAN,
		FOREST,
		RIPARIAN_ZONE,
		OTHER,
		INAPPROPRIATE_SOIL
	}
	Geometry geom;
	Type unsuitableType;
	double area;
	
	public double getArea() {
		return area;
	}

	UnsuitableArea(Geometry myGeom, Type myType, double myArea ){
		geom = myGeom;
		unsuitableType = myType;
		area = myArea;
	}

	public Type getType() {
		return unsuitableType;
	}
	
	public String getTypeName(){
		switch (unsuitableType){
		case NATURAL_RES:
			return (String)"Natural Reserve";
		case URBAN:
			return (String)"Urban";
		case FOREST:
			return (String)"Forest";
		case RIPARIAN_ZONE:
			return (String)"Riparian Zone";
		case OTHER:
			return (String)"Army Base or Industrial/Commercial zone";
		case INAPPROPRIATE_SOIL:
			return (String)"Inappropriate soil type for agriculture";
		default:
			return (String)"Unsuitable type";		
			
		}
			 
	}

}
