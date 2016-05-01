package negevVer1;

import gov.nasa.worldwind.render.BasicWWTexture;
import gov.nasa.worldwind.render.PatternFactory;
import gov.nasa.worldwind.render.SurfacePolyline;
import gov.nasa.worldwind.render.SurfaceShape;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

import repast.simphony.visualization.gis3D.style.DefaultMarkStyle;
import repast.simphony.visualization.gis3D.style.SurfaceShapeStyle;

/**
 * 
 * @author Assaf
 *
 */

//public class RoadStyle extends DefaultMarkStyle<Road> {
//	
//	@Override
//	public BasicWWTexture getTexture(Road agent, BasicWWTexture texture) {
//			
//		Color color = Color.MAGENTA;
//	
//				
//		BufferedImage image = PatternFactory.createPattern(PatternFactory.PATTERN_TRIANGLE_UP, 
//				new Dimension(10, 10), 0.7f,  color);
//
//		return new BasicWWTexture(image);	
//	}
//	
//
//}

public class RoadStyle implements SurfaceShapeStyle<Road>{

	@Override
	public SurfaceShape getSurfaceShape(Road object, SurfaceShape shape) {
	  return new SurfacePolyline();
	}

	@Override
	public Color getFillColor(Road obj) {
		return null;
	}

	@Override
	public double getFillOpacity(Road obj) {
		return 0;
	}

	@Override
	public Color getLineColor(Road obj) {
		return Color.BLACK;
	}

	@Override
	public double getLineOpacity(Road obj) {
		return 1.0;
	}

	@Override
	public double getLineWidth(Road obj) {
		return 1.6;
	}
}