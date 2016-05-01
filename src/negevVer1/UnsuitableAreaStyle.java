package negevVer1;

import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfaceShape;

import java.awt.Color;

import repast.simphony.visualization.gis3D.style.SurfaceShapeStyle;

public class UnsuitableAreaStyle implements SurfaceShapeStyle<UnsuitableArea>{
	@Override
	public SurfaceShape getSurfaceShape(UnsuitableArea object, SurfaceShape shape) {
		return new SurfacePolygon();
	}

	@Override
	public Color getFillColor(UnsuitableArea parcel) {
		
		if (parcel.getType() == UnsuitableArea.Type.NATURAL_RES){
			return Color.MAGENTA/*YELLOW*/;
		}
		else if (parcel.getType() == UnsuitableArea.Type.URBAN){
			return Color.ORANGE;
		}
		else if (parcel.getType() == UnsuitableArea.Type.FOREST){
			return Color.GREEN;
		}
		else if (parcel.getType() == UnsuitableArea.Type.RIPARIAN_ZONE){
			return Color.PINK;
		}
		else if (parcel.getType() == UnsuitableArea.Type.OTHER){
			return Color.GRAY;
		}
		else if (parcel.getType() == UnsuitableArea.Type.INAPPROPRIATE_SOIL){
			return Color.lightGray;
		}
		else{
		  return Color.GREEN; //default - forest
		}
	}

	@Override
	public double getFillOpacity(UnsuitableArea obj) {
		return 0.7;
	}

	@Override
	public Color getLineColor(UnsuitableArea parcel) {
		return Color.YELLOW;
	}

	@Override
	public double getLineOpacity(UnsuitableArea obj) {
		return 0/*0.5*/;
	}

	@Override
	public double getLineWidth(UnsuitableArea obj) {
		return 0/*1*/;
	}


}
