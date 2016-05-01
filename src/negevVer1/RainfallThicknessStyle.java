package negevVer1;

import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfaceShape;

import java.awt.Color;

import repast.simphony.visualization.gis3D.style.SurfaceShapeStyle;

public class RainfallThicknessStyle implements SurfaceShapeStyle<RainfallThickness>{
	@Override
	public SurfaceShape getSurfaceShape(RainfallThickness object, SurfaceShape shape) {
		return new SurfacePolygon();
	}

	@Override
	public Color getFillColor(RainfallThickness parcel) {
		return Color.CYAN;
	}

	@Override
	public double getFillOpacity(RainfallThickness obj) {
		return 0.05;
	}

	@Override
	public Color getLineColor(RainfallThickness parcel) {
		return Color.WHITE;
	}

	@Override
	public double getLineOpacity(RainfallThickness obj) {
		return 0.4;
	}

	@Override
	public double getLineWidth(RainfallThickness obj) {
		return 1;
	}


}
