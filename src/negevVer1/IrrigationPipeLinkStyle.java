package negevVer1;


import gov.nasa.worldwind.render.SurfacePolyline;
import gov.nasa.worldwind.render.SurfaceShape;

import java.awt.Color;

import repast.simphony.visualization.gis3D.style.SurfaceShapeStyle;

/**
 * 
 * @author Assaf
 *
 */
public class IrrigationPipeLinkStyle implements SurfaceShapeStyle<IrrigationPipeLink>{

	@Override
	public SurfaceShape getSurfaceShape(IrrigationPipeLink object, SurfaceShape shape) {
	  return new SurfacePolyline();
	}

	@Override
	public Color getFillColor(IrrigationPipeLink obj) {
		return null;
	}

	@Override
	public double getFillOpacity(IrrigationPipeLink obj) {
		return 0;
	}

	@Override
	public Color getLineColor(IrrigationPipeLink obj) {
		if(!obj.isCurrentlyOnline()){
			return Color.GRAY;
		}
		return Color.BLUE;
	}

	@Override
	public double getLineOpacity(IrrigationPipeLink obj) {
		return 1.0;
	}

	@Override
	public double getLineWidth(IrrigationPipeLink obj) {
		return 1;
	}
}