package negevVer1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import negevVer1.Pixel.Classification;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.gis.Geography;

import com.cenqua.ant.SetLogLevel;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.operation.distance.DistanceOp;;

public final class IrrigationManager {
	
	private static int simulationStepCount = 0;
	
	private static IrrigationManager instance;
	
	private static long annualWaterOverallQouta = 225000000; //190000000; 2002. in 2013: 200000000; //225000000; //the initial irrigation water quota for the study area is 200-250 MCM/y
	private static double annualWaterAdditionalQouta = 2000000; //the initial irrigation water quota for the study area is 200-250 MCM/y
	private static long currentAvailableWater;
	private final static long  waterNeededForNewSettCluster = 2000000; //7500000; //amount of irrigation water needed in order to create a new cluster of ag. settlements - 7.5 MCM/y
	public final static double annualWaterQuotaFraction = 1.01; //used in climate change scenario - to diminish annualWaterAdditionalQouta by 2% annually
	public final static double annualPrecipitationFraction = 0.992; //used in climate change scenario - to diminish annual rain amount
	public final static int ClimateChangeSimulationCountDuration = 30; //110;
	
	public static int rainFedAgThreshold = 200; //the threshold for rain-fed agriculture is 200 mm/y of rain
	
	
	
	//The first entity on the list is the root of the irrigation system - the rest of the links are ordered according to the direction of the irrigation infrastructure
	static List<IrrigationPipeLink> IrrigationPipes = new ArrayList<>(); //list of IrrigationPipeLink
	
	
	private IrrigationManager(){
		simulationStepCount = 0;
	}
	
	public static IrrigationManager getInstance(){
		if(instance == null){
			instance = new IrrigationManager();
		}
		return instance;
	}
	
	@ScheduledMethod(start = 1, interval = 1, priority = 2)
	public void step() {
		
		simulationStepCount++;
		currentAvailableWater = getAnnualWaterOverallQouta();
		if(ContextCreator.isClimateChange && simulationStepCount <= ClimateChangeSimulationCountDuration){//in case we are in climate change scenario - annual additional water is diminishing and agricultural water demand increasing
			annualWaterAdditionalQouta = annualWaterAdditionalQouta*annualWaterQuotaFraction;
			AgriculturalLandParcel.setcCubicMeterPerDunamWaterDemand();
		}
		annualWaterOverallQouta+= annualWaterAdditionalQouta; //every year (step) the water quota is increased by annualWaterAdditionalQouta 
		for(Settlement sett :Settlement.settlements){//first we zero all settlements' water quota (from the previous step)
			sett.setCurrentWaterQuota(0);
		}
		List<Settlement> randomSettList = new ArrayList<>(Settlement.settlements);//creating a random list will distribute water in a different order of settlements
		//every cycle
		Collections.shuffle(randomSettList);
		
		for (Settlement sett :randomSettList){
			if(ContextCreator.isClimateChange  && simulationStepCount <= ClimateChangeSimulationCountDuration){
				sett.setRainAnnualAmount(sett.getRainAnnualAmount()*annualPrecipitationFraction);
			}
			distributeWatertoSettlement(sett);
			if(currentAvailableWater <= 0){
				Settlement.logger.log(Level.INFO, "Currently not enough irrigation water available for all settlements, at timestep " + simulationStepCount);
				return;
			}
			
		}
		if(currentAvailableWater >= waterNeededForNewSettCluster){
			createNewCluster();
		}
		Settlement.logger.log(Level.INFO, "Currently leftover available water at timestep " + simulationStepCount + " is: " + currentAvailableWater);
				
//		simulationStepCount++;
//		//reseting processedWaterDemand boolean value - for the current timestep - in order to process all links 
//		for(IrrigationPipeLink link: IrrigationManager.IrrigationPipes){
//			link.processedWaterDemand = false;
//		}
//		//go over all irrigation pipe links - for every link within IrrigationPipes (which is ordered) and calculate how much water it has available - 
//		//then distribute the water to farmers
//		currentAvailableWater = getAnnualWaterOverallQouta();
//		for(IrrigationPipeLink link: IrrigationManager.IrrigationPipes){
//			if(!link.isCurrentlyOnline){
//				if(link.onlineYear > simulationStepCount){
//					break; //since the list is ordered - if this link is not online - all subsequent descendant links are not online
//				}
//				else{
//					link.isCurrentlyOnline = true;
//					//connect the new irrigation pipeline link with new settlements
//					link.createNewSettlements(null);
//				}
//			}
//			//distribute this cycle quota amount of water
//			distributeWatertoSettlements(link);
//						
//			//go over all descending links see how much water they need and divide water accordingly
//			for(IrrigationPipeLink descendantLink: link.descendantLinks){
//				if(descendantLink.isCurrentlyOnline){
//					distributeWatertoSettlements(descendantLink);
//				}
//			}		
//		}
//		Settlement.logger.log(Level.INFO, "Currently leftover available water at timestep " + simulationStepCount + " is: " + currentAvailableWater);
//		annualWaterOverallQouta+= 2000000; //every year (step) the water quota is increased by 2 MCM
	}
	
	private static void distributeWatertoSettlement(Settlement sett){
		long settWaterDemand = sett.getWaterDemand();
		long settIrrigatedAreaWaterDemand = sett.getIrrigatedAreaWaterDemand();
		double settRainThickness = sett.getRainAnnualAmount();
		double allocatedwater;
		double waterRatio;
		if(settRainThickness >=350){
			waterRatio = 0.6;
		}else{
			if(settRainThickness >=250){
				waterRatio = 0.75;
			}
			else{
				waterRatio = 0.9;
			}
		}
		allocatedwater = Math.min(settWaterDemand*waterRatio, settIrrigatedAreaWaterDemand*1.02);
		sett.setCurrentWaterQuota(allocatedwater);
		currentAvailableWater -= allocatedwater;
		
//		double settIrrigatedAreaWaterDemand = (sett.getIrrigatedAreaWaterDemand() *1.02);
//		sett.setCurrentWaterQuota(settIrrigatedAreaWaterDemand);
//		currentAvailableWater -= (settIrrigatedAreaWaterDemand);	
	}
	
	private static void  distributeWatertoSettlements(IrrigationPipeLink link){
		//in case the link "Was taken care of" - was processed already for water distribution to farmers within its reach - no need to do it again
		if(link.processedWaterDemand == true)
			return;
		link.availableWaterAmount = currentAvailableWater;
		long linkExtractedWaterAmount = 0;
		for(Settlement settlement: link.irrigatedSettlements){
			long settlementWaterDemand = settlement.getWaterDemand();
			if(settlementWaterDemand > currentAvailableWater){
				//TODO - no more available water - all other settlements' parcels can't be irrigated - solve synchronization settlement updating issue
				settlement.unmetWaterDemand = true;
				continue; 
			}
			currentAvailableWater-=settlementWaterDemand;
			linkExtractedWaterAmount += settlementWaterDemand;
			settlement.unmetWaterDemand = false;
		}
		link.extractedWaterAmount = linkExtractedWaterAmount;
		link.transferedWaterAmount = currentAvailableWater; //this is the amount of water available for descendant links
		link.processedWaterDemand = true;
		return;
	}
	
	private void createNewCluster(){		
		Context context = RunState.getSafeMasterContext();
		Geography geography = (Geography) context.getProjection("Geography");
		
		long numParcels = 0;
		while(numParcels < Settlement.minNumParcelsPerSettlement){
			if(PotentialAgPixel.pixelSet.isEmpty()){
				Settlement.logger.log(Level.INFO, "No more agricultural area available");
				break;
			}
			PotentialAgPixel pixel = PotentialAgPixel.pixelSet.first();
			Pixel settPixel = new Pixel(Classification.SETTLEMENT, pixel.getGeom(), pixel.getCoord_m());
			Settlement newSettlement = new Settlement(settPixel);
			
			context.add(newSettlement);
			geography.move(newSettlement, newSettlement.getPixel().getGeom());
			numParcels = newSettlement.calculateAgriculturalCapacity(context, pixel.getOrigGeom());
			
			if(numParcels < Settlement.minNumParcelsPerSettlement){
				//remove newSettlement from pixelSet
				PotentialAgPixel.pixelSet.remove(pixel);
//				PotentialAgPixel.pixelSet.remove(PotentialAgPixel.pixelSet.first());
				//restore all other pixels that are Marked to be suitable, and restore Settlement.totalAgLandInStudyarea 
				newSettlement.erasePotentialSettlement();
				//disable potential settlement's immediate neighbors from being potential settlements 
				PotentialAgPixel.updatePixelSet(newSettlement, Math.pow((Settlement.settlementRadius/3),2));
				//remove newSettlement from context and geography
				geography.move(newSettlement, null);
				context.remove(newSettlement);
				newSettlement = null;
				Settlement.numSettlements--;
			}
			else{
				Settlement.settlements.add(newSettlement);
				//assign a farmer for the settlement
				newSettlement.assignFarmerToParcels(context);
				//update all components of pixelSet with the new settlement
				PotentialAgPixel.updatePixelSet(newSettlement, Math.pow((Settlement.settlementRadius*2),2));
				PotentialAgPixel.pixelSet.remove(pixel);
				
				//add road and irrigation infrastructure to new settlement - and update distance to road and irrigation - update pixelSet
				updateRoadAndIrrigationInfrastructure(pixel, context);
			
				
				newSettlement.setRainAnnualAmount(pixel.getRainAnnualAmount());
				//update totalIrrigated area. totalAgLandInStudyarea is updated in AgParcel's constructor
				Settlement.totalIrrigatedAgLandInStudyArea+=newSettlement.getIrrigatedLand(); 
				return;
			}	
		}
	}
	
	private void updateRoadAndIrrigationInfrastructure(PotentialAgPixel pixel, Context context){
		Geography geography = (Geography) context.getProjection("Geography");
		int roadID = pixel.getClosestRDid();
		int irrigationID = pixel.getClosestIrrid();
		Road closestRoad = Road.roadMap.get(new Integer(roadID));
		IrrigationPipeLink closestIrrPipe = IrrigationPipeLink.irrigationMap.get(new Integer(irrigationID));
		
		if(closestRoad == null || closestIrrPipe == null){
			Settlement.logger.log(Level.WARNING, "Error: could not find closest road or irrigation infrastructure to new settlement");
			return;
		}
		//add new road segment to new settlement
//		Coordinate[] roadCoords = closestRoad.getRoadLine().getCoordinates();
		Geometry roadGeomMeter = ContextCreator.convertGeometryFromWGStoCRS(closestRoad.getRoadLine(), geography);
//		Coordinate[] roadCoords = roadGeomMeter.getCoordinates();
		
		DistanceOp distOp = new DistanceOp(roadGeomMeter, pixel.origGeom);
		Coordinate[] newRoadCoord = distOp.nearestPoints();
		double dist = distOp.distance(); //debug
		Settlement.totalAddedRoadKM += dist/1000;
				
//		double distance = pixel.distToRoad;
//		Coordinate pixelCoord = pixel.getCoord_m();
//		Coordinate targetCoord = findNewLineTargetCoord(pixelCoord, roadCoords);
//		if(targetCoord==null){
//			targetCoord = roadGeomMeter.getCentroid().getCoordinate(); 
//		}
//		Coordinate[] newRoadCoord_old = new Coordinate[]{targetCoord, pixelCoord};
//		Geometry newRoadLine = new GeometryFactory().createLineString(newRoadCoord);
		Geometry newRodeMultiLineMeter = createMultiLineString(newRoadCoord);
		
		//get the WGS geometry in order to place in geography
		Geometry newRodeMultiLineWGS =ContextCreator.convertGeometryToWGS(newRodeMultiLineMeter, geography);

		Road myRoad = new Road(newRodeMultiLineWGS, newRodeMultiLineMeter.getLength(), 
				closestRoad.getRoadNum(), (Road.roadMap.size()+100));
		context.add(myRoad);
		geography.move(myRoad, newRodeMultiLineWGS);
	
		//add new pipeline segment to new settlement
//		Coordinate[] irrCoords = closestIrrPipe.getGeom().getCoordinates();
		Geometry irrGeomMeter =  ContextCreator.convertGeometryFromWGStoCRS(closestIrrPipe.getGeom(),geography);
		
		distOp = new DistanceOp(irrGeomMeter, pixel.origGeom);
		Coordinate[] newIrrCoord = distOp.nearestPoints();
		dist = distOp.distance();
		Settlement.totalAddedIrrInfraKM += dist/1000;
		
//		Coordinate[] irrCoords = irrGeomMeter.getCoordinates();
//		distance = pixel.distToIrrigation;
//		targetCoord = findNewLineTargetCoord(pixelCoord, irrCoords);
//		if(targetCoord==null){
//			targetCoord = irrGeomMeter.getCentroid().getCoordinate(); 
//		}
//		Coordinate[] newIrrCoord = new Coordinate[]{targetCoord, pixelCoord};
//		Geometry newIrrLine = new GeometryFactory().createLineString(newIrrCoord);
		Geometry newIrrMultiLineMeter = createMultiLineString(newIrrCoord);
		Geometry newIrrMultiLineWGS = ContextCreator.convertGeometryToWGS(newIrrMultiLineMeter, geography);
		
		IrrigationPipeLink myIrrLink = new IrrigationPipeLink(closestIrrPipe.isReclaimedWater(), 
			(IrrigationPipeLink.irrigationMap.size()+100), IrrigationManager.getSimulationStepCount(), newIrrMultiLineWGS, newIrrMultiLineMeter.getLength());
		context.add(myIrrLink);
		geography.move(myIrrLink, newIrrMultiLineWGS);
	
		//update components of PotentialAgPixel.pixelSet - with the new road and piping infrastructure 
		PotentialAgPixel.updatePixelSetWithNewRoadAndIrrigation(myIrrLink, myRoad, geography);
		
	}
	
	//find the coordinate with minimum distance to the pixelCoord
	private Coordinate findNewLineTargetCoord(Coordinate pixelCoord, Coordinate[] sourceCoords){
		double minDistance, tempDistance;
		minDistance = tempDistance = Integer.MAX_VALUE;
		Coordinate targetCoord = null;
		for(Coordinate coord: sourceCoords){
			if((tempDistance = coord.distance(pixelCoord))< minDistance){
				targetCoord = coord;
				minDistance = tempDistance;
			}
		}
		return targetCoord;
	}
	
	private MultiLineString createMultiLineString(Coordinate[] coords){
		LineString newRoadLineString = new GeometryFactory().createLineString(coords);
		LineString[] lineStrings = new LineString[] {newRoadLineString};
		return new GeometryFactory().createMultiLineString(lineStrings);	
	}
	
	static void createIrrigationPipesTree(Geography geography) {		
		int index = 0;
		int innerIndex = 0;
		
		//iterate over all link - starting from the root and checking for each node which are the other nodes directly connected to it
		for(IrrigationPipeLink link: IrrigationManager.IrrigationPipes){
			
			if(IrrigationPipes.size() <= (index+1))
				break;
			innerIndex = index;
			for(IrrigationPipeLink descendantLink = IrrigationPipes.get(innerIndex+1) ; IrrigationPipes.size() > (innerIndex+1) ; innerIndex++){
				descendantLink = IrrigationPipes.get(innerIndex+1);
				Geometry linkBuffer = ContextCreator.generateBuffer(geography, link.getGeom(), Settlement.pixelSize);
				if(linkBuffer.intersects(descendantLink.geom)){
					descendantLink.setParentLink(link);
					link.descendantLinks.add(descendantLink);
				}
			}	
			index++;
		}
	}
	
	static void connectSettlementToPipeline(Settlement settlement, IrrigationPipeLink link) {
		link.irrigatedSettlements.add(settlement);
		settlement.irrigatingLink = link;
	}

	static int getSimulationStepCount() {
		return simulationStepCount;
	}
	
	public static long getAnnualWaterOverallQouta(){
		return annualWaterOverallQouta; 
	}


}
