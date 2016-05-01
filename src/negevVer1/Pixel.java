package negevVer1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class Pixel implements Comparable {
	
	public enum Classification{
		UNSUITABLE,
		SUITABLE,
		CLAIMED,
		ROAD,
		SETTLEMENT,
		MARKED // Marked is only used during allocating agricultural parcels 
	}
	Geometry geom;
	private double xCoord_m; //coordinate system in Meters
	private double yCoord_m;
	private Coordinate coord_m;
	
//	private double xCoord_d; //coordinate system in Degrees
//	private double yCoord_d;
//	private Coordinate coord_d;
	
	Classification classification;
	
//	static List<Pixel> pixels = new ArrayList<>(); //list of all pixels in the system
	
	static Map<Coordinate,Pixel> pixelMap = new HashMap<Coordinate,Pixel>(); //list of all pixels in the system
	
	static List<Pixel> InitializationPixels = new ArrayList<>(); //list of pixels snapshot at model initialization time used for fast re-initialization
	
	static final double unsuitableSlope = 15; //in degrees. the threshold for topography's slope suitable for agriculture
	
	Pixel(){}
	
	Pixel(Classification myClassification, Geometry myGeom, Coordinate coord){
		this.classification = myClassification;
		this.geom = myGeom;
//		this.xCoord_m = Math.floor(coord.x * 100) / 100;
//		this.yCoord_m = Math.floor(coord.y * 100) / 100;
		
		this.xCoord_m = Math.floor(coord.x);
		this.yCoord_m = Math.floor(coord.y);
		setCoord_m(new Coordinate(xCoord_m, yCoord_m));
		
//		this.xCoord_d = xcoordD;
//		this.yCoord_d = ycoordD;
//		setCoord_d(new Coordinate(xCoord_d, yCoord_d));//TODO
	}

	Pixel(Pixel myPixel) {
		this.classification = myPixel.classification;
		this.geom = myPixel.geom;
		this.xCoord_m = myPixel.xCoord_m;
		this.yCoord_m = myPixel.yCoord_m;
		setCoord_m(new Coordinate(xCoord_m, yCoord_m));
		
//		this.xCoord_d = myPixel.xCoord_d;
//		this.yCoord_d = myPixel.yCoord_d;
//		setCoord_d(new Coordinate(xCoord_d, yCoord_d));
	}

	public Geometry getGeom() {
		return geom;
	}

	public double getxCoord() {
		return xCoord_m;
	}

	public double getyCoord() {
		return yCoord_m;
	}


	public Classification getClassification() {
		return classification;
	}

	public void setClassification(Classification classification) {
		this.classification = classification;
	}

	void setGeom(Geometry geom) {
		this.geom = geom;
	}

	@Override
	public int compareTo(Object anotherPixel) {
		
		Pixel pix2 = (Pixel)anotherPixel;
		return this.coord_m.compareTo(pix2.getCoord_m());
//		Pixel pix2 = (Pixel)anotherPixel;
//		if(this.xCoord > pix2.xCoord){
//			return 1;
//		}else if(this.xCoord < pix2.xCoord){
//			return -1;
//		}else if(this.yCoord > pix2.yCoord){
//			return 1;
//		}else if(this.yCoord < pix2.yCoord){
//			return -1;
//		}
//		return 0;
		
	}
	
	public boolean isSuitable(){
		if (this.classification == Classification.SUITABLE){
			return true;
		}
		return false;
	
	}

	public Coordinate getCoord_m() {
		return coord_m;
	}

	private void setCoord_m(Coordinate coord) {
		this.coord_m = coord;
	}

//	public Coordinate getCoord_d() {
//		return coord_d;
//	}
//
//	private void setCoord_d(Coordinate coord_d) {
//		this.coord_d = coord_d;
//	}
	
	static double calculateDistance(Pixel pixA, Pixel pixB){
		return Math.sqrt(Math.pow((pixA.getxCoord() - pixB.getxCoord()), 2) + Math.pow((pixA.getyCoord() - pixB.getyCoord()), 2));
	}
	
	static double calculateSquaredDistance(Pixel pixA, Pixel pixB){
		return (Math.pow((pixA.getxCoord() - pixB.getxCoord()), 2) + Math.pow((pixA.getyCoord() - pixB.getyCoord()), 2));
	}
	

}
