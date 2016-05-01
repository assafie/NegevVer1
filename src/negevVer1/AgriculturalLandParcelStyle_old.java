package negevVer1;

import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfaceShape;


import java.awt.Color;


import repast.simphony.visualization.gis3D.style.SurfaceShapeStyle;

public class AgriculturalLandParcelStyle_old implements SurfaceShapeStyle<AgriculturalLandParcel>{
	
	@Override
	public SurfaceShape getSurfaceShape(AgriculturalLandParcel object, SurfaceShape shape) {
		return new SurfacePolygon();
	}

	@Override
	public Color getFillColor(AgriculturalLandParcel parcel) {
	
		if(parcel.isPotentiallyIrrigated && parcel.isIrrigatedThisCycle){
			return Color.blue; //parcel is irrigated this cycle
		}
		if(parcel.getRainThickness() >= IrrigationManager.rainFedAgThreshold){ //parcel can be cultivated without irrigation
			if(parcel.getStatus() == AgriculturalLandParcel.Status.CULTIVATED)
				return Color.white/*MAGENTA*/; //parcel is cultivated without irrigation this cycle (rain-fed)
			return Color.CYAN; //parcel is not cultivated this cycle - fallow
		}
		return Color.CYAN; //parcel is not cultivated this cycle - fallow	
	}
		
//		if (parcel.getStatus() == AgriculturalLandParcel.Status.CULTIVATED){
//			if(parcel.getCurrentCrop().getCropType() == Crop.Type.COTTON){
//				return Color.WHITE;
//			}
//			else{
//				return Color.YELLOW;
//			}
//		}
//		else if (parcel.getStatus() == AgriculturalLandParcel.Status.FALLOW){
////			return Color.GRAY;
//			return Color.magenta;
//		}
//		else if (parcel.getStatus() == AgriculturalLandParcel.Status.SETTLEMENT_CLAIMED){
//			return Color.CYAN;
//			
//		}
//		else{
//		  return Color.BLUE; //virgin or farmerClaimed - for now - no usage to these 2 statuses
//		}
//	}

	@Override
	public double getFillOpacity(AgriculturalLandParcel obj) {
		return 0.7;
	}

	@Override
	public Color getLineColor(AgriculturalLandParcel parcel) {
		if (parcel.getSOM() >= AgriculturalLandParcel.SOMCultivationThreshold){
			return Color.GREEN;
		}
		else{
			return Color.BLACK;
		}
	}

	@Override
	public double getLineOpacity(AgriculturalLandParcel obj) {
		return 0 /*1.0*/;
	}

	@Override
	public double getLineWidth(AgriculturalLandParcel obj) {
		return 0;
	}

}
