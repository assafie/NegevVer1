package negevVer1;

import com.vividsolutions.jts.geom.Geometry;

public class SoilType {
	
	// 1: Sand ; 2: Loess ; 3: Red Silty Sandstone ; 4: inappropriate soil type for ag.
		public enum Type{ //TODO - changed order of sand and Loess  - make sure no harm done
			SAND,
			LOESS,
			RED_SILTY_SANDSTONE,
			INAPPROPRIATE_SOIL
		}
		Geometry geom;
		Type soilType;
		double area;
		
		public double getArea() {
			return area;
		}

		SoilType(Geometry myGeom, Type myType, double myArea ){
			geom = myGeom;
			soilType = myType;
			area = myArea;
		}

		public Type getType() {
			return soilType;
		}
		
		public String getTypeName(){
			return soilType.toString();
			
//			switch (soilType){
//			case LOESS:
//				return (String)"Loess";
//			case SAND:
//				return (String)"Sand";
//			case RED_SILTY_SANDSTONE:
//				return (String)"Red Silty Sandstone (Hamra)";
//			case INAPPROPRIATE_SOIL:
//				return (String)"Inappropriate soil type for agriculture";
//			default:
//				return (String)"Error: Unsuitable type";		
//				
//			}
				 
		}


}
