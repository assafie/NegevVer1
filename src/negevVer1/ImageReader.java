package negevVer1;

import java.awt.Color;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import negevVer1.Pixel.Classification;

public class ImageReader {
	
	protected Pixel matrix[][];
	protected int xSize;
	protected int ySize;

	public ImageReader(String mapFile) {
		
		BufferedImage img = null;
		try {
			File myImage = new File(mapFile);
			img = ImageIO.read(myImage);
//		    img = ImageIO.read(new File(mapFile));
		    
		} catch (IOException e) {
			System.out.println("Error Reading image file");
			e.printStackTrace();
		}	
		init(img);
	}

	private void init(BufferedImage img) {
		
		xSize = img.getWidth();
		ySize = img.getHeight();
		
		matrix = new Pixel[xSize][ySize];		
		
		for (int i = 0; i < xSize; i++) 
			for (int j = 0; j < ySize; j++){
				matrix[i][j] = new Pixel();
				int colorNum = img.getRGB(i, j);
				calculateClassification(i,j,colorNum);
			}
		//DEBUG
		System.out.println("number of settlement is: " + Settlement.numSettlements);
		
	}
	
	private void calculateClassification(int i, int j, int colorNum) {
		
		if(colorNum == Color.black.getRGB())
			matrix[i][j].classification = Classification.UNSUITABLE;
		else if(colorNum == Color.white.getRGB())
			matrix[i][j].classification = Classification.SUITABLE;
		else if(colorNum == Color.gray.getRGB())
			matrix[i][j].classification = Classification.ROAD;
		else if(colorNum == Color.red.getRGB()){
			matrix[i][j].classification = Classification.SETTLEMENT;
			//debug
			Settlement.numSettlements++;
//			Settlement settlement = new Settlement();
			
		}
		else matrix[i][j].classification = Classification.UNSUITABLE;
		
	}

	public Pixel[][] getMatrix(){
		
		return matrix;
	}

}
