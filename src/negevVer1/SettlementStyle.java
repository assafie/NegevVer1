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
public class SettlementStyle extends DefaultMarkStyle<Settlement>{
	
	
	@Override
	public BasicWWTexture getTexture(Settlement agent, BasicWWTexture texture) {
		
		Color color = null;
		if(agent.isFutureSettlement()){
			color = Color.MAGENTA;
		}
		else{
			color = Color.RED;
		}
	
				
		BufferedImage image = PatternFactory.createPattern(PatternFactory.PATTERN_CIRCLE, 
				new Dimension(8, 8), 0.7f,  color);

		return new BasicWWTexture(image);	
	}
}