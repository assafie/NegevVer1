package negevVer1;

import gov.nasa.worldwind.render.BasicWWTexture;
import gov.nasa.worldwind.render.PatternFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

import repast.simphony.visualization.gis3D.style.DefaultMarkStyle;

public class FarmerAgentStyle extends DefaultMarkStyle<FarmerAgent> {
	
	@Override
	public BasicWWTexture getTexture(FarmerAgent agent, BasicWWTexture texture) {
		Color color = null;
		if(agent.getStarvingStatus() == true){
			color = Color.BLUE; //the farmer is starving
		}
		else{
			color = Color.ORANGE; //the farmer is cultivating a field - therefore is not starving
		}
		
	
				
		BufferedImage image = PatternFactory.createPattern(PatternFactory.PATTERN_CIRCLE, 
				new Dimension(10, 10), 0.7f,  color);

		return new BasicWWTexture(image);	
	}
	

}
