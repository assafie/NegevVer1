package negevVer1;

import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfaceShape;

import java.awt.Color;

import repast.simphony.visualization.gis3D.style.SurfaceShapeStyle;

public class SoilTypeStyle implements SurfaceShapeStyle<SoilType>{
	@Override
	public SurfaceShape getSurfaceShape(SoilType object, SurfaceShape shape) {
		return new SurfacePolygon();
	}

	@Override
	public Color getFillColor(SoilType parcel) {
		
		if (parcel.getType() == SoilType.Type.LOESS){
			return Color.ORANGE;
		}
		else if (parcel.getType() == SoilType.Type.SAND){
			return Color.YELLOW;
		}
		else if (parcel.getType() == SoilType.Type.RED_SILTY_SANDSTONE){
			return Color.PINK;
		}
		else if (parcel.getType() == SoilType.Type.INAPPROPRIATE_SOIL){
			return Color.GRAY;
		}
		else{
		  return Color.GRAY; //default - inappropriate soil type
		}
	}

	@Override
	public double getFillOpacity(SoilType obj) {
		return 0.25;
	}

	@Override
	public Color getLineColor(SoilType parcel) {
		return Color.magenta;
	}

	@Override
	public double getLineOpacity(SoilType obj) {
		return 0/*0.3*/;
	}

	@Override
	public double getLineWidth(SoilType obj) {
		return 0/*0.5*/;
	}


}