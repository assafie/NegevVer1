package negevVer1;

import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfaceShape;

import java.awt.Color;

import repast.simphony.visualization.gis3D.style.SurfaceShapeStyle;

/**
 * 
 * @author Assaf
 *
 */
public class ZoneStyle implements SurfaceShapeStyle<Zone>{

	@Override
	public SurfaceShape getSurfaceShape(Zone object, SurfaceShape shape) {
		return new SurfacePolygon();
	}

	@Override
	public Color getFillColor(Zone zone) {
		
		if (zone.getType() == Zone.Type.UNSUITABLE){
			return Color.BLACK;
		}
		else if (zone.getType() == Zone.Type.SUITABLE){
			return Color.CYAN;
		}
		else{//CLAIMED
		  return Color.BLUE;
		}
	}

	@Override
	public double getFillOpacity(Zone obj) {
		return 0.25;
	}

	@Override
	public Color getLineColor(Zone zone) {
		
			return Color.WHITE;	
	}

	@Override
	public double getLineOpacity(Zone obj) {
		return 1.0;
	}

	@Override
	public double getLineWidth(Zone obj) {
		return 3;
	}
}