package negevVer1;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import repast.simphony.space.gis.Geography;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.TreeMultimap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.distance.DistanceOp;


public class PotentialAgPixel extends Pixel {

	double distToRoad; 
	double distToIrrigation;
	int rainAnnualAmount;
	SoilType.Type soilType;
	private Coordinate coordWGS;
	int closestRDid; //ID of closest road segment
	int closestIrrid; //ID of closest irrigation pipeline segment
	Geometry origGeom; //original geometry - original CRS (Israeli in this case)
	
//	static final int expectedValuesPerKey = 4; //every 4 neighbors have the same distance from the irrigation pixel.
//	static final int expectedKeys = (int) (10000 / Settlement.pixelSize) ; //10 km is the maximum measured distance to irrigation
//	static HashMultimap<Double, Pixel> potAgPixHm =  HashMultimap.create(expectedKeys,expectedValuesPerKey);
//	static TreeMultimap<Double, Pixel> potAgPixTmm = TreeMultimap.create(potAgPixHm); //a structure to hold potential ag pixels sorted according to distance from irrigation
	static SortedSet<PotentialAgPixel> pixelSet = new TreeSet<PotentialAgPixel>();

	
	public PotentialAgPixel(Classification myClassification, Geometry myGeomWGS, Coordinate coord, 
			double distToIrr, double distToRd, int rainAmount, SoilType.Type soil, Coordinate coord_WGS, int rdID, int irrID, Geometry geomOrig) {
		super(myClassification, myGeomWGS, coord);
		distToRoad = distToRd;
		distToIrrigation = distToIrr;
		rainAnnualAmount = rainAmount;
		soilType = soil;
		coordWGS = coord_WGS;
		closestRDid = rdID;
		closestIrrid = irrID;
		origGeom = geomOrig;
	}
	
	private PotentialAgPixel(PotentialAgPixel otherPixel){
		super(otherPixel.classification, otherPixel.geom, otherPixel.getCoord_m());
		distToRoad = otherPixel.getDistToRoad();
		distToIrrigation = otherPixel.getDistToIrrigation();
		rainAnnualAmount = otherPixel.getRainAnnualAmount();
		soilType = otherPixel.getSoilType();
		coordWGS = otherPixel.getCoordWGS();
		closestRDid = otherPixel.getClosestRDid();
		closestIrrid = otherPixel.getClosestIrrid();
		origGeom = otherPixel.getOrigGeom();
	}
	@Override
	public boolean equals(Object anotherPixel){
		PotentialAgPixel pix2 = (PotentialAgPixel)anotherPixel;
		if(this.getCoord_m().compareTo(pix2.getCoord_m()) == 0)
			return true; //this is the only way 2 pixels are the same
		return false;	
	}
	
	@Override
	public int compareTo(Object anotherPixel) {
		//distance to irrigation has most weight
		PotentialAgPixel pix2 = (PotentialAgPixel)anotherPixel;
//		if(this.getCoordWGS().equals(pix2.getCoordWGS())){
		if(this.getCoord_m().compareTo(pix2.getCoord_m()) == 0){
//		if(this.getxCoord() == pix2.getxCoord() && this.getyCoord() == pix2.getyCoord()){
			return 0; //this is the only way 2 pixels are the same
		}
		if(this.distToIrrigation < pix2.distToIrrigation)
			return -1;
		if(this.distToIrrigation == pix2.distToIrrigation){
			if(this.distToRoad < pix2.distToRoad){
				return -1;
			}
			if(this.distToRoad == pix2.distToRoad){
				if(this.soilType.ordinal() <= pix2.soilType.ordinal() )
					return -1;
			}
		}
		return 1;
		
		//distance to road has most weight
//		PotentialAgPixel pix2 = (PotentialAgPixel)anotherPixel;
////		if(this.getCoordWGS().equals(pix2.getCoordWGS())){
//		if(this.getCoord_m().compareTo(pix2.getCoord_m()) == 0){
//			
////			Settlement.logger.log(Level.INFO, "new coordinate "  + this.getCoord_m().toString() + "    " + pix2.getCoord_m().toString() );
//			
////		if(this.getxCoord() == pix2.getxCoord() && this.getyCoord() == pix2.getyCoord()){
//			return 0; //this is the only way 2 pixels are the same
//		}
//		if(this.distToRoad < pix2.distToRoad)
//			return -1;
//		if(this.distToRoad == pix2.distToRoad){
//			if(this.distToIrrigation < pix2.distToIrrigation){
//				return -1;
//			}
//			if(this.distToIrrigation == pix2.distToIrrigation){
//				if(this.soilType.ordinal() <= pix2.soilType.ordinal() )
//					return -1;
//			}
//		}
//		return 1;
		
		//combined distance to road and irrigation has varying weight
//		PotentialAgPixel pix2 = (PotentialAgPixel)anotherPixel;
//		if(this.getCoord_m().compareTo(pix2.getCoord_m()) == 0){
//			return 0; //this is the only way 2 pixels are the same
//		}
//		if((this.distToRoad < pix2.distToRoad)&&(this.distToIrrigation < pix2.distToIrrigation))
//			return -1;
//		if((this.distToRoad > pix2.distToRoad)&&(this.distToIrrigation > pix2.distToIrrigation))
//			return 1;
//		if((this.distToRoad == pix2.distToRoad)&&(this.distToIrrigation == pix2.distToIrrigation)){
//			if(this.soilType.ordinal() < pix2.soilType.ordinal() )
//				return -1;
//			return 1;
//		}
//		//20% road 80% irrigation
//		double pix1CombinedDist = (this.distToRoad)*0.8 + (this.distToIrrigation)*0.2;
//		double pix2CombinedDist = (pix2.distToRoad)*0.8 + (pix2.distToIrrigation)*0.2;
//		if(pix1CombinedDist <= pix2CombinedDist){
//			return -1;
//		}
//		return 1;
		
		
		//combined distance to road and irrigation has most weight
//		PotentialAgPixel pix2 = (PotentialAgPixel)anotherPixel;
//		if(this.getCoord_m().compareTo(pix2.getCoord_m()) == 0){
//			return 0; //this is the only way 2 pixels are the same
//		}
//		if((this.distToRoad < pix2.distToRoad)&&(this.distToIrrigation < pix2.distToIrrigation))
//			return -1;
//		if((this.distToRoad > pix2.distToRoad)&&(this.distToIrrigation > pix2.distToIrrigation))
//			return 1;
//		if((this.distToRoad == pix2.distToRoad)&&(this.distToIrrigation == pix2.distToIrrigation)){
//			if(this.soilType.ordinal() < pix2.soilType.ordinal() )
//				return -1;
//			return 1;
//		}
//		double pix1CombinedDist = this.distToRoad + this.distToIrrigation;
//		double pix2CombinedDist = pix2.distToRoad + pix2.distToIrrigation;
//		if(pix1CombinedDist <= pix2CombinedDist){
//			return -1;
//		}
//		return 1;
	}

	public double getDistToRoad() {
		return distToRoad;
	}

	void setDistToRoad(double distToRoad) {
		this.distToRoad = distToRoad;
	}

	public double getDistToIrrigation() {
		return distToIrrigation;
	}

	void setDistToIrrigation(double distToIrrigation) {
		this.distToIrrigation = distToIrrigation;
	}

	public int getRainAnnualAmount() {
		return rainAnnualAmount;
	}
	//rain amount should not be changed, unless in climate change scenario
	void setRainAnnualAmount(int rainAnnualAmount) {
		this.rainAnnualAmount = rainAnnualAmount;
	}

	public SoilType.Type getSoilType() {
		return soilType;
	}
	
	public String getSoilTypeName(){
		return soilType.toString();
	}

	public Coordinate getCoordWGS() {
		return coordWGS;
	}

	void setCoordWGS(Coordinate coordWGS) {
		this.coordWGS = coordWGS;
	}

	public Geometry getOrigGeom() {
		return origGeom;
	}
    public int getClosestRDid() {
		return closestRDid;
	}

	void setClosestRDid(int closestRDid) {
		this.closestRDid = closestRDid;
	}

	public int getClosestIrrid() {
		return closestIrrid;
	}

	void setClosestIrrid(int closestIrrid) {
		this.closestIrrid = closestIrrid;
	}

	//will remove all potential settlements' locations that are within a specific distance from the specified 
	//settlement pixel.
	static void updatePixelSet(Settlement newSettlement, double minSquareDistance) {
//		double minDistanceFromSett = Math.pow((Settlement.settlementRadius*2),2);
		Collection<PotentialAgPixel> removalSet = new LinkedHashSet<PotentialAgPixel>();
		Iterator<PotentialAgPixel> iter = pixelSet.iterator();
		while(iter.hasNext()){
			PotentialAgPixel pixel = iter.next();
			if(pixel.classification != Pixel.Classification.SUITABLE){
				removalSet.add(pixel);
				continue;
			}
			double squareDistance = Math.pow(pixel.getCoord_m().distance(newSettlement.getPixel().getCoord_m()), 2);
			if(squareDistance < minSquareDistance){
				removalSet.add(pixel);
				continue; 
			}	
		}
		PotentialAgPixel.pixelSet.removeAll(removalSet);
	}

	static void updatePixelSetWithNewRoadAndIrrigation(IrrigationPipeLink myIrrLink, Road myRoad, Geography geography) {
		Geometry irrGeom = myIrrLink.getGeom();
		Geometry roadGeom = myRoad.getRoadLine();
		
		Geometry irrGeomM = ContextCreator.convertGeometryFromWGStoCRS(irrGeom, geography);
		Geometry roadGeomM = ContextCreator.convertGeometryFromWGStoCRS(roadGeom, geography);
		
		Collection<PotentialAgPixel> removalSet = new HashSet<PotentialAgPixel>();
		Collection<PotentialAgPixel> insertSet = new HashSet<PotentialAgPixel>();
		
		//updating the Pixel.pixelMap where all pixels are stored too - by adding the updated pixels thus replacing
		//the old ones.
//		Map<Coordinate,Pixel> pixelMapInsert = new HashMap<Coordinate,Pixel>();
		
		double distIrr, distRd;
		boolean distIrrChanged, distRdChanged;
		for(PotentialAgPixel pixel: pixelSet){
			Geometry pixGeom = pixel.getOrigGeom();
			distIrrChanged = distRdChanged = false;
			
			if((distIrr = DistanceOp.distance(irrGeomM, pixGeom)) < pixel.getDistToIrrigation() ){
			
//			if((distIrr = irrGeomM.distance(pixGeom)) < pixel.getDistToIrrigation() ){
				distIrrChanged = true;
			}
			if((distRd = DistanceOp.distance(roadGeomM, pixGeom)) < pixel.getDistToRoad()){
//			if((distRd = roadGeomM.distance(pixGeom)) < pixel.getDistToRoad()){
				distRdChanged = true;
			}
			if(distIrrChanged || distRdChanged){
				//create a new pixel - and update its distances
				PotentialAgPixel newPixel = new PotentialAgPixel(pixel);
				if(distIrrChanged){
					newPixel.setDistToIrrigation(distIrr);
					newPixel.setClosestIrrid(myIrrLink.getId());
				}
				if(distRdChanged){
					newPixel.setDistToRoad(distRd);
					newPixel.setClosestRDid(myRoad.getId());
				}
				//add pixel to removal set
				removalSet.add(pixel);
				//add newPixel to insert set
				insertSet.add(newPixel);
//				pixelMapInsert.put(newPixel.getCoord_m(), newPixel);
			}
		}
		pixelSet.removeAll(removalSet);
		pixelSet.addAll(insertSet);
//		Pixel.pixelMap.putAll(pixelMapInsert);
		return;
	}
	

	
	
	
}
