package negevVer1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import negevVer1.Pixel.Classification;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.gis.IntersectsQuery;
import repast.simphony.space.gis.Geography;
import repast.simphony.util.ContextUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public class IrrigationPipeLink {
	
	long availableWaterAmount; //the amount of water available at the head of the pipeline
	long transferedWaterAmount; //the amount of water available to descending links 
	long extractedWaterAmount; //the amount of water distributed to settlements within the reach of this link
	
	static Map<Integer, IrrigationPipeLink> irrigationMap = new HashMap<Integer, IrrigationPipeLink>();
	
	Geometry geom;
	
//	IrrigationPipeLink prevLink; //the previous irrigation pipeline link in the system - where the water is coming from
//	IrrigationPipeLink nextLink; //the next irrigation pipeline link in the system - where the water is going to

	int id; //unique pipeline segment ID
	int onlineYear;
	boolean reclaimedWater;
	double length;
	

	IrrigationPipeLink parentLink;
	List<IrrigationPipeLink> descendantLinks;
	List<Settlement> irrigatedSettlements; 
	
	boolean isCurrentlyOnline;
	boolean processedWaterDemand; //every time step the link need to be asked for its' water demand - this boolean marks whether it was already processed for the time step 
	
//	static List<IrrigationPipeLink> IrrigationPipes = new ArrayList<>(); //list of IrrigationPipeLink
	
	public IrrigationPipeLink(boolean isReclaimed, int myID, int myOnlineYear,  Geometry myGeom, double myLength) {
		id = myID;
		reclaimedWater = isReclaimed;
//		if(reclaimed ==1){
//			reclaimedWater = true;
//		}else{
//			reclaimedWater = false;
//		}
		onlineYear = myOnlineYear;
		geom = myGeom;
		length = myLength;
//		simulationStepCount = 0;
		parentLink = null;
		descendantLinks = new ArrayList<>();
		irrigatedSettlements = new ArrayList<>();
		processedWaterDemand = false;
		
		//TODO - finish constructor - online issues etc.
		if(onlineYear <= IrrigationManager.getSimulationStepCount()){
			isCurrentlyOnline = true;
		}else{
			isCurrentlyOnline = false;
		}
		irrigationMap.put(new Integer(id), this);
	}
	
	@ScheduledMethod(start = 1, interval = 1)
//	public void step() {
////		++simulationStepCount;
//		if(!isCurrentlyOnline){
//			if(onlineYear > IrrigationManager.getSimulationStepCount()){
//				return;
//			}
//			else{
//				isCurrentlyOnline = true;
//				//connect the new irrigation pipeline link with new settlements
//				createNewSettlements(null);
//				
//			}
//		}
//		//TODO - distribute this cycle quota amount of water
//	}

	public boolean isCurrentlyOnline() {
		return isCurrentlyOnline;
	}

	Geometry getGeom() {
		return geom;
	}
		
	//allocate new settlements and parcels in the vicinity of the newly online irrigation pipeline
	void createNewSettlements(Context context){
		if(context==null){
			context = ContextUtils.getContext(this);
		}
		Geography geography = (Geography) context.getProjection("Geography");
		
		//make a Settlement.maxDistanceFromIrrigation buffer around the pipeline
		Geometry pipelineBuffer = ContextCreator.generateBuffer(geography, this.getGeom(), Settlement.maxDistanceFromIrrigation);
//		Envelope pipeEnvelope = pipelineBuffer.getEnvelopeInternal(); //the envelope created is too big (square of min xy - max xy) - and doesn't fit the algorithm needs
		
//		Class road = null;
//		try {
//			road = Class.forName("negevVer1.Road");
//		} catch (ClassNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			System.out.println("class Road not found");
//		}
		//check if there is a road within this area
//		Iterable<Road> envelopeIter = geography.getObjectsWithin(pipeEnvelope, road);
		
		if(!RoadCoord.useRoadSet){
			RoadCoord.calculateRoadSquaredDistanceToSettlement(context);
		}
//		for (Road myRoad : envelopeIter) {
		
		IntersectsQuery query = new IntersectsQuery(geography, pipelineBuffer);	
		for (Object obj : query.query()) {
			//check if there is a road within this area
			if (obj instanceof RoadCoord) {
				RoadCoord myRoadCoord = (RoadCoord) obj;
				
				// allocate new settlements in suitable areas
				if (RoadCoord.roadSet.contains(myRoadCoord)) {
					Pixel settPixel = new Pixel(Classification.SETTLEMENT, myRoadCoord.getGeom(), myRoadCoord.getCoord());
					Settlement newSettlement = new Settlement(settPixel);
					newSettlement.setFutureSettlement(false);

					context.add(newSettlement);
					geography.move(newSettlement, newSettlement.getPixel().getGeom());
					Settlement.settlements.add(newSettlement);

					newSettlement.initialize(context);
					
					//make a relationship between settlement and irrigation pipeline
					IrrigationManager.connectSettlementToPipeline(newSettlement, this);

					RoadCoord.updateRoadSet(myRoadCoord, context);
					RoadCoord.roadSet.remove(myRoadCoord); 
				}
			}
		}
		

		
	}

	IrrigationPipeLink getParentLink() {
		return parentLink;
	}

	void setParentLink(IrrigationPipeLink parentLink) {
		this.parentLink = parentLink;
	}

	public int getOnlineYear() {
		return onlineYear;
	}


	public boolean isReclaimedWater() {
		return reclaimedWater;
	}

	public int getId() {
		return id;
	}

	void setId(int id) {
		this.id = id;
	}

	public double getLength() {
		return length;
	}
	
	

}
