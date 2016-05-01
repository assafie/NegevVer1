package negevVer1;

import java.util.SortedSet;
import java.util.TreeSet;

import repast.simphony.context.Context;
import repast.simphony.space.gis.Geography;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class RoadCoord implements Comparable{
	
	static SortedSet<RoadCoord> roadSet = new TreeSet<RoadCoord>();
	static boolean useRoadSet = false;
	
	static SortedSet<RoadCoord> initializedRoadSet = new TreeSet<RoadCoord>();
	
	Coordinate coord; // the coordinate of the specific road pixel
	Double squaredDistanceToSettlement;
	Geometry geom;
	
	//initializing Road according to pixel
	RoadCoord(Coordinate myCoord, double squareDistance, Geometry myGeom) {
		coord = myCoord;
//		distanceToSettlement = new Double(-1);
		squaredDistanceToSettlement = new Double(squareDistance);
		geom = myGeom;
	}
	
	RoadCoord(RoadCoord roadCoord) {
		// TODO Auto-generated constructor stub
		if(roadCoord.coord !=null){
			coord = roadCoord.coord;
			squaredDistanceToSettlement = roadCoord.squaredDistanceToSettlement;
			geom = roadCoord.getGeom();
		}
	}
	
	@Override
	public int compareTo(Object anotherRoadCoord) {
		RoadCoord roadCoord2 = (RoadCoord)anotherRoadCoord;
		int checkSameRoad;
		if((checkSameRoad = this.getCoord().compareTo(roadCoord2.getCoord())) == 0){//the two road coords are the same
			return 0;
		}else{//not the same roads 
			int result = this.squaredDistanceToSettlement.compareTo(roadCoord2.getSquaredDistanceToSettlement());
			if(result == 0){//if minimum distance to settlement in 2 different road coordinates  is identical - we still don't want to say that they are equal
				return checkSameRoad;
			}
			return result;
		}
		
	}
	
	static void calculateRoadSquaredDistanceToSettlement(Context context){
		for (Road road : Road.roads) {
			Coordinate[] roadCoords = road.getRoadLine().getCoordinates();
			for (Coordinate coord: roadCoords){
				double squareDistance = Settlement.studyAreaMaxSquaredDistance; //study area maximum squared distance 
				for(Settlement settlement: Settlement.settlements){
//					double currentSquareDistance = Pixel.calculateSquaredDistance(road.pixel, settlement.getPixel());
					double currentMinSquareDistance = Math.pow(coord.distance(settlement.getPixel().geom.getCoordinate()), 2);
					if(currentMinSquareDistance < squareDistance){
						squareDistance =currentMinSquareDistance;
					}
				}
				
				
//				road.setDistanceToSettlement(distance);
//				road.setSquaredDistanceToSettlement(squareDistance);
				if(squareDistance > Math.pow((Settlement.settlementRadius*2), 2)){ //add candidate to roadSet only if it's not too close to another existing settlement
					GeometryFactory geomFactory = new GeometryFactory();
					Point roadCoordGeom = geomFactory.createPoint(coord);			
					RoadCoord roadCoordPixel = new RoadCoord(coord, squareDistance, roadCoordGeom);
					RoadCoord.roadSet.add(roadCoordPixel);
					context.add(roadCoordPixel);
					Geography geography = (Geography) context.getProjection("Geography");
					geography.move(roadCoordPixel, roadCoordGeom);
				}
			}
		}
	
		//deep copy roadSet to  initializedRoadSet for faster next model re-run
		for (RoadCoord roadCoord : RoadCoord.roadSet) {
			RoadCoord newRoadCoord = new RoadCoord(roadCoord);
			RoadCoord.initializedRoadSet.add(newRoadCoord);
		}
		
		RoadCoord.useRoadSet = true;
	}
	
	// the new settlement distance to road candidates should be taken into
	// consideration - if distance is smaller - roadSet candidate should be
	// updated accordingly
	static void updateRoadSet(RoadCoord roadSettlement, Context context) {
		SortedSet<RoadCoord> removalSet = new TreeSet<RoadCoord>();
		SortedSet<RoadCoord> updateSet = new TreeSet<RoadCoord>();
		double minSquareDistanceBetweenSettlements = Math.pow((Settlement.settlementRadius * 2), 2);
//		double minDistanceBetweenSettlements = Settlement.settlementRadius * 2;

		for (RoadCoord setRoad : RoadCoord.roadSet) {
			// double distance = calculateDistance(roadSettlement.pixel,setRoad.pixel);
//			for(Settlement settlement: Settlement.settlements){
//				double squaredDistance = Pixel.calculateSquaredDistance(settlement.pixel, setRoad.pixel);

//				double squaredDistance = Pixel.calculateSquaredDistance(roadSettlement.pixel, setRoad.pixel);
				double squaredDistance = Math.pow(setRoad.getCoord().distance(roadSettlement.getCoord()), 2);
				if (squaredDistance < minSquareDistanceBetweenSettlements) {// if settlement road candidate is too close to the new settlement - remove from set	
					removalSet.add(setRoad);
				} else {
					if (squaredDistance < setRoad.getSquaredDistanceToSettlement()) {// update new distance to settlement
						removalSet.add(setRoad);
						RoadCoord newRoad = new RoadCoord(setRoad);
						newRoad.setSquaredDistanceToSettlement(squaredDistance);
						updateSet.add(newRoad);
					}
				}
//			}
		}
		RoadCoord.roadSet.removeAll(removalSet);
		context.removeAll(removalSet);
		RoadCoord.roadSet.addAll(updateSet);
		context.addAll(updateSet);
	}
	
	void setSquaredDistanceToSettlement(Double squareDistanceToSettlement) {
		this.squaredDistanceToSettlement = squareDistanceToSettlement;
	}
	
	public Double getSquaredDistanceToSettlement() {
		return squaredDistanceToSettlement;
	}

	Coordinate getCoord() {
		return coord;
	}

	Geometry getGeom() {
		return geom;
	}

	void setGeom(Geometry geom) {
		this.geom = geom;
	}
	
	
	

}
