package negevVer1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import negevVer1.Pixel.Classification;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiLineString;


public class Road implements Comparable {
		
	static List<Road> roads = new ArrayList<>(); // a list of roads in the study area that are candidates for new Settlements - this list is used exclusively for new Settlement creation purposes.
//	static SortedSet<Road> roadSet = new TreeSet<Road>();
//	static boolean useRoadSet = false;
	
	static Map<Integer, Road> roadMap = new HashMap<Integer, Road>();
	
	static List<Road> initializedRoads = new ArrayList<>();
//	static SortedSet<Road> initializedRoadSet = new TreeSet<Road>();
	
//	Pixel pixel; // the location of the road
//	Double distanceToSettlement;
//	Double squaredDistanceToSettlement;
	Geometry roadLine;
	double length;
	int roadNum;
	int id; //unique road segment ID
	
	//initializing Road according to MultiLineString
	Road(Geometry myRoadLineString , double myLength, int myRoadNum, int myID){
		roadLine = myRoadLineString;
		length = myLength;
		roadNum = myRoadNum;
		id = myID;
		roadMap.put(new Integer(myID), this);
//		pixel = null;
		
	}
	//initializing Road according to pixel
//	Road(Pixel myPixel) {
//		pixel = myPixel;
////		distanceToSettlement = new Double(-1);
//		squaredDistanceToSettlement = new Double(-1);
//	}
//	
	Road(Road myRoad){
//		if(myRoad.pixel != null){
//			pixel = myRoad.pixel;
////			distanceToSettlement = myRoad.getDistanceToSettlement();
//			squaredDistanceToSettlement = myRoad.getSquaredDistanceToSettlement();
//		}
//		else{
			roadLine = myRoad.roadLine;
			length = myRoad.length;
			roadNum = myRoad.roadNum;
//			pixel = null;
			
//		}
	}
	

	@Override
	public int compareTo(Object anotherRoad) {
		Road road2 = (Road)anotherRoad;
		int checkSameRoad;
		if((checkSameRoad = this.getRoadLine().compareTo(road2.getRoadLine())) == 0){//the two roads are the same
			return 0;
		}else{//not the same roads 
			Integer thisRoadNum = new Integer(this.roadNum);
			int result = thisRoadNum.compareTo((Integer)road2.roadNum);
			if(result == 0){//if two road segments belong to the same road number - we still don't want to say that they are equal
				return checkSameRoad;
			}
			return result;
		}
		
	}

//	@Override
//	public int compareTo(Object anotherRoad) {
//		Road road2 = (Road)anotherRoad;
//		int checkSamePixel;
//		if((checkSamePixel = this.getPixel().compareTo(road2.getPixel())) == 0){//the two roads are the same pixel
//			return 0;
//		}else{//not the same roads 
//			int result = this.squaredDistanceToSettlement.compareTo(road2.getSquaredDistanceToSettlement());
//			if(result == 0){//if distance to settlement in 2 different roads is identical - we still don't want to say that they are equal
//				return checkSamePixel;
//			}
//			return result;
//			
//		}
//		return this.distanceToSettlement.compareTo(road2.getDistanceToSettlement());
//		return this.squaredDistanceToSettlement.compareTo(road2.getSquaredDistanceToSettlement());
//	}
		
//	public Double getDistanceToSettlement() {
//		return distanceToSettlement;
//	}
//
//	public void setDistanceToSettlement(Double distanceToSettlement) {
//		this.distanceToSettlement = distanceToSettlement;
//	}
	
//	public void setSquaredDistanceToSettlement(Double squareDistanceToSettlement) {
//		this.squaredDistanceToSettlement = squareDistanceToSettlement;
//	}
//	
//	public Double getSquaredDistanceToSettlement() {
//		return squaredDistanceToSettlement;
//	}

//	Pixel getPixel() {
//		return pixel;
//	}
//	
//	static void calculateRoadSquaredDistanceToSettlement(){
//		for (Road road : Road.roads) {
//			Coordinate[] roadCoords = road.getRoadLine().getCoordinates();
//			for (Coordinate coord: roadCoords){
//				double squareDistance = Settlement.studyAreaMaxSquaredDistance; //study area maximum squared distance 
//				for(Settlement settlement: Settlement.settlements){
////					double currentSquareDistance = Pixel.calculateSquaredDistance(road.pixel, settlement.getPixel());
//					double currentSquareDistance = Math.pow(coord.distance(settlement.getPixel().geom.getCoordinate()), 2);
//					if(currentSquareDistance < squareDistance){
//						squareDistance =currentSquareDistance;
//					}
//				}
//				
//				
////				road.setDistanceToSettlement(distance);
////				road.setSquaredDistanceToSettlement(squareDistance);
//				if(squareDistance > Math.pow((Settlement.settlementRadius*2), 2)){ //add candidate to roadSet only if it's not too close to another existing settlement
//					RoadCoord roadCoordPixel = new RoadCoord(coord, squareDistance);
//					RoadCoord.roadSet.add(roadCoordPixel);
//				}
//			}
//		}
//		
////			double squareDistance = Settlement.studyAreaMaxSquaredDistance; //study area maximum squared distance 
////			for(Settlement settlement: Settlement.settlements){
//////				double currentSquareDistance = Pixel.calculateSquaredDistance(road.pixel, settlement.getPixel());
////				double currentSquareDistance = Math.pow(road.getRoadLine().distance(settlement.getPixel().getGeom()), 2);
////				if(currentSquareDistance < squareDistance){
////					squareDistance =currentSquareDistance;
////				}
////			}
//////			road.setDistanceToSettlement(distance);
////			road.setSquaredDistanceToSettlement(squareDistance);
////			if(squareDistance > Math.pow((Settlement.settlementRadius*2), 2)){ //add candidate to roadSet only if it's not too close to another existing settlement
////				Road.roadSet.add(road);
////			}
////		}
//	
//		//deep copy roadSet to  initializedRoadSet for faster next model re-run
//		for (RoadCoord roadCoord : RoadCoord.roadSet) {
//			RoadCoord newRoadCoord = new RoadCoord(roadCoord);
//			RoadCoord.initializedRoadSet.add(newRoadCoord);
//		}
//		
//		RoadCoord.useRoadSet = true;
//	}
//	
	
	// the new settlement distance to road candidates should be taken into
	// consideration - if distance is smaller - roadSet candidate should be
	// updated accordingly
//	static void updateRoadSet(Road roadSettlement) {
//		SortedSet<Road> removalSet = new TreeSet<Road>();
//		SortedSet<Road> updateSet = new TreeSet<Road>();
//		double minSquareDistanceBetweenSettlements = Math.pow((Settlement.settlementRadius * 2), 2);
////		double minDistanceBetweenSettlements = Settlement.settlementRadius * 2;
//
//		for (Road setRoad : Road.roadSet) {
//			// double distance = calculateDistance(roadSettlement.pixel,setRoad.pixel);
////			for(Settlement settlement: Settlement.settlements){
////				double squaredDistance = Pixel.calculateSquaredDistance(settlement.pixel, setRoad.pixel);
//
////				double squaredDistance = Pixel.calculateSquaredDistance(roadSettlement.pixel, setRoad.pixel);
//				double squaredDistance = Math.pow(setRoad.getRoadLine().distance(roadSettlement.getRoadLine()), 2);
//				if (squaredDistance < minSquareDistanceBetweenSettlements) {// if settlement road candidate is too close to the new settlement - remove from set	
//					removalSet.add(setRoad);
//				} else {
//					if (squaredDistance < setRoad.getSquaredDistanceToSettlement()) {// update new distance to settlement
//						removalSet.add(setRoad);
//						Road newRoad = new Road(setRoad);
//						newRoad.setSquaredDistanceToSettlement(squaredDistance);
//						updateSet.add(newRoad);
//					}
//				}
////			}
//		}
//		Road.roadSet.removeAll(removalSet);
//		Road.roadSet.addAll(updateSet);
//	}
	
	public int getRoadNum() {
		return roadNum;
	}
	void setRoadNum(int roadNum) {
		this.roadNum = roadNum;
	}
	public double getLength() {
		return length;
	}
	
	void setLength(double length) {
		this.length = length;
	}
	public Geometry getRoadLine() {
		return roadLine;
	}
	void setRoadLine(Geometry roadLine) {
		this.roadLine = roadLine;
	}
	public int getId() {
		return id;
	}
	void setId(int id) {
		this.id = id;
	}
}
