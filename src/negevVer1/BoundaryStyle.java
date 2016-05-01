package negevVer1;

import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfaceShape;

import java.awt.Color;

import repast.simphony.visualization.gis3D.style.SurfaceShapeStyle;

public class BoundaryStyle implements SurfaceShapeStyle<Boundary>{
	@Override
	public SurfaceShape getSurfaceShape(Boundary object, SurfaceShape shape) {
		return new SurfacePolygon();
	}

	@Override
	public Color getFillColor(Boundary parcel) {
		return Color.gray;
	}

	@Override
	public double getFillOpacity(Boundary obj) {
		return 0.4;
	}

	@Override
	public Color getLineColor(Boundary parcel) {
		return Color.BLACK;
	}

	@Override
	public double getLineOpacity(Boundary obj) {
		return 0.5;
	}

	@Override
	public double getLineWidth(Boundary obj) {
		return 0.1/*1*/;
	}


}

