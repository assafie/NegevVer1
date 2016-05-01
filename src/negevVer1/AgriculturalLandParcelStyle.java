package negevVer1;

import gov.nasa.worldwind.render.BasicWWTexture;
import gov.nasa.worldwind.render.PatternFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

import repast.simphony.visualization.gis3D.style.DefaultMarkStyle;

public class AgriculturalLandParcelStyle extends DefaultMarkStyle<AgriculturalLandParcel> {
	
	@Override
	public BasicWWTexture getTexture(AgriculturalLandParcel agent, BasicWWTexture texture) {
			
		Color color = Color.GREEN;
	
				
		BufferedImage image = PatternFactory.createPattern(PatternFactory.PATTERN_SQUARE, 
				new Dimension(10, 10), 0.7f,  color);

		return new BasicWWTexture(image);	
	}
	

}