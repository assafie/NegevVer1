package negevVer1;


import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.render.BasicWWTexture;
import gov.nasa.worldwind.render.PatternFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

import repast.simphony.visualization.gis3D.PlaceMark;
import repast.simphony.visualization.gis3D.style.DefaultMarkStyle;

/**
 * 
 * @author Assaf
 *
 */
public class PixelStyle extends DefaultMarkStyle<Pixel>{
	
	
	@Override
	public BasicWWTexture getTexture(Pixel agent, BasicWWTexture texture) {
		
		Color color = null;
		BufferedImage image = null;
		
		switch (agent.classification){
			case UNSUITABLE: color = Color.BLACK;
					image = PatternFactory.createPattern(PatternFactory.PATTERN_SQUARE, 
							new Dimension(2, 2), 0.7f,  color);
					break;
			case SUITABLE: color = Color.WHITE;
					image = PatternFactory.createPattern(PatternFactory.PATTERN_SQUARE, 
							new Dimension(2, 2), 0.7f,  color);
					break;
			case SETTLEMENT: color = Color.RED;
					image = PatternFactory.createPattern(PatternFactory.PATTERN_CIRCLE, 
							new Dimension(2, 2), 0.7f,  color);
					break;
			case ROAD: color = Color.GRAY;
					image = PatternFactory.createPattern(PatternFactory.PATTERN_HLINE, 
							new Dimension(2, 2), 0.7f,  color);
					break;
			default: color = Color.BLACK;
					image = PatternFactory.createPattern(PatternFactory.PATTERN_SQUARE, 
							new Dimension(2, 2), 0.7f,  color);
		
		}
			
		return new BasicWWTexture(image);	
	}
}

