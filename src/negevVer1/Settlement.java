package negevVer1;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.measure.unit.SI;

import org.apache.commons.digester.annotations.providers.SetTopRuleProvider;
import org.apache.commons.logging.impl.AvalonLogger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.operation.DefaultCoordinateOperationFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import negevVer1.AgriculturalLandParcel.Status;
import negevVer1.Pixel.Classification;

import com.sun.org.apache.bcel.internal.generic.NEWARRAY;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.util.GeometricShapeFactory;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.gis.GeographyWithin;
import repast.simphony.query.space.gis.IntersectsQuery;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.UTMFinder;
import repast.simphony.util.ContextUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.TreeMultimap;

/**
 * @author Assaf Chen
 * 
 */
public class Settlement {

	static Handler consoleHandler = new ConsoleHandler();
	static Logger logger = Logger.getAnonymousLogger();
	
	private static DefaultCoordinateOperationFactory cFactory = new DefaultCoordinateOperationFactory();
	
	static boolean initialization = true; // refers to existing settlements
											// during model initialization
	static boolean studyAreaFull = false;
	static long numSettlements = 0; // a global variable - number of settlements in the study area.
	static double totalAgLandInStudyArea = 0; //the total agricultural area of all settlements within the study area in m^2
	static double totalIrrigatedAgLandInStudyArea = 0; //the total agricultural area of all settlements within the study area in m^2
	static double totalAddedRoadKM = 0; //the total length of road system added during the model (in km)
	static double totalAddedIrrInfraKM = 0; //the total length of irrigation infrastructure added during the model (in km)
	
	static final int settlementRadius = 1500; //3000; // the radius in meters of every settlement
											
	static final float pixelSize = 30;
	static final double MooreNbh = Math.sqrt(Math.pow(pixelSize, 2)*2);
	static final double studyAreaMaxSquaredDistance = Math.pow(40000, 2); //meters - the maximum distance across the study area (27*28 km)
	static final double maxDistanceFromIrrigation = 1500; //meters - maximum distance from irrigation to be connected to system 
				
	static final int minRainThicknessForRainfedAg = 200; // 200 mm of annual rain is the minimum required for rainfed agriculture
	static final long minNumParcelsPerSettlement = 55; //35; //463 parcels - 10.8 dunam each = 5000 dunam; //TODO - change to fit
	static final int numPixelsInParcel = 120; /*120;*/ //12

	static final double annPopGrowthRate = 0.03; //3% annual population growth rate - for all settlements within the study area

	static List<Settlement> settlements = new ArrayList<>(); //list of active settlements
	
	static List<Settlement> InitializationSettlements = new ArrayList<>(); //list of active settlements at model initialization time used for fast re-initialization
	
	static List<Settlement> potentialSettlements = new ArrayList<>(); //list of potential settlements
	
	static List<Settlement> InitializationpotentialSettlements = new ArrayList<>(); //list of potential settlements - to be used for model reruns - for fast re-initialization
	
	static final int expectedValuesPerKey = 4; //every 4 neighbors have the same distance from the Settlement pixel.
	static final int expectedKeys = (int)((Math.PI * Math.pow(settlementRadius, 2))/ (Math.pow(pixelSize,2) * expectedValuesPerKey)) ;
	static private HashMultimap<Double, Pixel> hm =  HashMultimap.create(expectedKeys,expectedValuesPerKey); //Auxiliary structure for calculateAgriculturalCapacity() method
	static private TreeMultimap<Double, Pixel> tmm = TreeMultimap.create(hm); //Auxiliary structure for calculateAgriculturalCapacity() method

	static final int numPixelInSettlement = (int)((Math.PI * Math.pow(settlementRadius, 2))/ (Math.pow(pixelSize,2))) ;
	static private HashMultimap<Pixel, Pixel> ngbrMap =  HashMultimap.create(numPixelInSettlement,8);  //a mapping of all pixels within a settlement's radius - to their immediate neighbors 
	
	long ID; // a unique ID for every settlement
	long numFarmers; // current number of farmers within the settlement
	Pixel pixel; // the location of the settlement
	
	boolean isFutureSettlement = false;
	boolean unmetWaterDemand = false;  //if there is not enough irrigation water for the settlement

	private double farmerRatio; // initial fraction of farmers out of settlemet's carrying capacity -
								// used only during model initialization - for existing settlements
	
	List<FarmerAgent> farmers; // list of the settlement’s Farmers
	List<AgriculturalLandParcel> settlementParcels; // an inventory of all
													// agricultural parcels
													// within the
													// settlementRadius.
	TreeMap<Integer, AgriculturalLandParcel> settlementAvailableParcels; // a list of parcels which are not already claimed.

	long numParcels; // total number of agricultural parcels within the
						// settlementRadius.
	long numAvailableParcels; // total number of agricultural parcels within the
								// settlementRadius - which are not claimed yet
								// by a farmer.
	IrrigationPipeLink irrigatingLink;
	// long farmerCapacity; //the maximum farmers’ capacity of the settlement (equals to numParcels/2)

	int settlementAge; // the age in years of a settlement.
	private long excesiveHouseholds; // the number of new farmers without land for the coming next year.
	String name = "";
	List<Boundary> settBoundaries = new ArrayList<>(); //list of all agricultural boundaries belonging to this settlement
	double currentAgriArea = 0; //the total cultivated agricultural area that belongs to this settlement in m^2
	double arableLand = 0; //the total potential arable lands within the boundaries of the settlement in m^2
	double irrigatedLand = 0; //the total irrigated area that belongs to this settlement in m^2
	double currentWaterQuota = 0; //the yearly water quota given to the settlement in every time-step (year)
	double currentlyUnusedWater = 0; //the amount of unused water quota at the end of each time-step (year)
	double rainAnnualAmount = 0;

	

	Settlement(Pixel myPixel) {
		this(myPixel, (int)numSettlements + 3001);
	}
		
//		++numSettlements; // update number of settlements within the study area
//		ID = numSettlements + 3000; //add 3000 in order to have a unique ID number (different than existing settlements at init time)
//		pixel = myPixel;
//		farmerRatio = RandomHelper.nextDoubleFromTo(0.3, 0.75);
//		numParcels = numAvailableParcels = numFarmers = excesiveHouseholds = 0;
//		settlementAge = 0;
////		isFutureSettlement = false;
//		farmers = new ArrayList<>();
//		settlementParcels = new ArrayList<>();
//		settlementAvailableParcels = new TreeMap<Integer, AgriculturalLandParcel>();
//
//		if (!initialization) {
////			Context context = ContextUtils.getContext(pixel);
//			Context context = RunState.getSafeMasterContext();
//			Geography geography = (Geography) context.getProjection("Geography");
//			context.add(this);
//			geography.move(this, pixel.getGeom());
//			Settlement.settlements.add(this);
//		}
//
//	}
	
	Settlement(Pixel myPixel, int myID) {
		++numSettlements; // update number of settlements within the study area
		ID = (long)myID;
		pixel = myPixel;
		farmerRatio = RandomHelper.nextDoubleFromTo(0.3, 0.75);
		numParcels = numAvailableParcels = numFarmers = excesiveHouseholds = 0;
		settlementAge = 0;
		farmers = new ArrayList<>();
		settlementParcels = new ArrayList<>();
		settlementAvailableParcels = new TreeMap<Integer, AgriculturalLandParcel>();

//		if (!initialization) {
////			Context context = ContextUtils.getContext(pixel);
//			Context context = RunState.getSafeMasterContext();
//			Geography geography = (Geography) context.getProjection("Geography");
//			context.add(this);
//			geography.move(this, pixel.getGeom());
//			Settlement.settlements.add(this);
//		}

	}
	//copy const. for deep copying (copy by value) - for copying from InitializationSettlements list during model rerun- not including Farmer data
	Settlement(Settlement another, Context context){
		ID = another.ID;
		pixel = another.pixel;
		farmerRatio = RandomHelper.nextDoubleFromTo(0.3, 0.75);
		numParcels = another.numParcels;
		
		numAvailableParcels = another.numAvailableParcels;
		numFarmers = 0;
		excesiveHouseholds = 0;
		settlementAge = 0;
		
		isFutureSettlement = another.isFutureSettlement;
		
		farmers = new ArrayList<>();
//		settlementParcels = another.settlementParcels;
		settlementParcels = new ArrayList<>();
		settlementAvailableParcels = new TreeMap<Integer, AgriculturalLandParcel>();
		int key = 0;
		for(AgriculturalLandParcel parcel: another.settlementParcels ){ // deep copy of Ag. parcels - in order to keep snapshot of present state
			AgriculturalLandParcel newParcel = new AgriculturalLandParcel(parcel, this, context);
			settlementParcels.add(newParcel);
			settlementAvailableParcels.put(key++, newParcel);
		}
		
		if (!initialization) {
			Geography geography = (Geography) context.getProjection("Geography");
			context.add(this);
			geography.move(this, pixel.getGeom());
			Settlement.settlements.add(this);
		}
//		settlementAvailableParcels = (TreeMap<Integer, AgriculturalLandParcel>)another.settlementAvailableParcels.clone(); //this is still shallow copying - but no internal changes to the AgriculturalLandParcel
//		//is performed - therefore it is fine.
	}

	public Settlement(Settlement futureSettlement) { // special constructor for future potential settlements and for initializing InitializationSettlements for future reruns
		ID = futureSettlement.ID;
		pixel = futureSettlement.pixel;
		farmerRatio = RandomHelper.nextDoubleFromTo(0.3, 0.75);
		numParcels = futureSettlement.numParcels;
		
		numAvailableParcels = futureSettlement.numAvailableParcels;
		numFarmers = 0;
		excesiveHouseholds = 0;
		settlementAge = 0;
		
		isFutureSettlement = futureSettlement.isFutureSettlement;
		
		farmers = new ArrayList<>();
		settlementParcels = new ArrayList<>();
		settlementAvailableParcels = new TreeMap<Integer, AgriculturalLandParcel>();
		int key = 0;
		for(AgriculturalLandParcel parcel: futureSettlement.settlementParcels ){ // deep copy of Ag. parcels - in order to keep snapshot of present state
			AgriculturalLandParcel newParcel = new AgriculturalLandParcel(parcel, this);
			settlementParcels.add(newParcel);
			settlementAvailableParcels.put(key++, newParcel);
		}
	}
	Pixel getPixel() {
		return pixel;
	}

	// This method is called in model first initialization only
	void initialize(Context context) {

		calculateAgriculturalCapacity(context);
		
		//copy this settlement into InitializationSettlements - for faster re-initialization
		Settlement newSettlement = new Settlement(this); //deep copy (by value and not by Reference)
		InitializationSettlements.add(newSettlement);
		
//		assignFarmersForExistingSettlement(context);
		
		assignFarmerToParcels(context);

		//debug
//		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		System.out.println("Number of farmers in settlement " + this.ID + " is: " + numFarmers);
//		System.out.println("Total number of farmers in model is: " + FarmerAgent.totalNumFarmers);
	}
	
	
	//increase the number of farmers according to annPopGrowthRate
	//find parcels for each additional farmer within this settlement
	//if not enough parcels - search in other existing settlements
	//if still not enough parcels - create a new settlement and assign parcels for new farmers there 
	@ScheduledMethod(start = 1, interval = 1, priority = 1)
	public void step() {
		
		//ag. parcel status is decided here according to irrigation 
		settlementAge++;
		
		//in case we are in the climate change scenario - update annual rain amount for each of the settlements' parcels
		if(ContextCreator.isClimateChange  && IrrigationManager.getSimulationStepCount() <= IrrigationManager.ClimateChangeSimulationCountDuration){
			for(AgriculturalLandParcel parcel: settlementParcels)
				parcel.updateAnnualRainAmountClimateChange();
		}
		
		if(currentWaterQuota <=0){
			for(AgriculturalLandParcel parcel: settlementParcels){
				parcel.setIrrigatedThisCycle(false); //zero from previous cycle
				parcel.setStatusAccordingToRainOnly(true);
			}
			return;
		}
		double waterQuota = currentWaterQuota;
				
		List<AgriculturalLandParcel> randomParcelList = new ArrayList<>(settlementParcels);//creating a random list will distribute water in a different order of 
		//parcels every cycle
		Collections.shuffle(randomParcelList);
		
		boolean isZeroWaterQuota = false;
		for(AgriculturalLandParcel parcel: randomParcelList){
			parcel.setIrrigatedThisCycle(false); //zero from previous cycle
			if(!parcel.isPotentiallyIrrigated){
				parcel.setStatusAccordingToRainOnly(isZeroWaterQuota);
				continue;
			}
			double parcelDemand = parcel.getWaterDemand();
			if(waterQuota - parcelDemand <= 0 ){
				isZeroWaterQuota = true;
				parcel.setStatusAccordingToRainOnly(isZeroWaterQuota);
				continue; //we continue to scan all parcels
			}
			waterQuota -= parcelDemand;
			parcel.setStatus(Status.CULTIVATED);
			if(!parcel.getOwner().currentCultivatedParcels.contains(parcel))
				parcel.getOwner().currentCultivatedParcels.add(parcel);
			parcel.setIrrigatedThisCycle(true);
		}
		currentlyUnusedWater = waterQuota;
		
		
		//ag. parcel status not decided here
//		settlementAge++;
//		
//		if(currentWaterQuota <=0){
//			return;
//		}
//		double waterQuota = currentWaterQuota;
//				
//		List<AgriculturalLandParcel> randomParcelList = new ArrayList<>(settlementParcels);//creating a random list will distribute water in a different order of 
//		//parcels every cycle
//		Collections.shuffle(randomParcelList);
//		
//		for(AgriculturalLandParcel parcel: randomParcelList){
//			parcel.setIrrigatedThisCycle(false); //zero from previous cycle
//			if(!parcel.isPotentiallyIrrigated){
//				continue;
//			}
//			double parcelDemand = parcel.getWaterDemand();
//			if(waterQuota - parcelDemand <= 0 ){
//				continue; //we continue and not break because there might be another smaller parcel with less water demand that can use available currentWaterQuota
//			}
//			waterQuota -= parcelDemand;
//			parcel.setIrrigatedThisCycle(true);
//		}
//		currentlyUnusedWater = waterQuota;
		

		
		
//		if(isFutureSettlement()){
//			//check if settlement has access to irrigation
//			Context context = ContextUtils.getContext(this);
//			Geography geography = (Geography) context.getProjection("Geography");
//			
//			Geometry settBuffer = ContextCreator.generateBuffer(geography, getPixel().getGeom(), Settlement.maxDistanceFromIrrigation);
//			for(IrrigationPipeLink link: IrrigationManager.IrrigationPipes){
//				if(link.isCurrentlyOnline()){
//					if(settBuffer.intersects(link.getGeom())){
////					if(this.getPixel().getGeom().isWithinDistance(link.getGeom(), Settlement.maxDistanceFromIrrigation)){
//						setFutureSettlement(false);
//						initialize(context);
//						IrrigationManager.connectSettlementToPipeline(this, link);
//						break;
//					}
//				}
//			}
			
			//slow algorithm using IntersectsQuery()
//			Geometry settBuffer = ContextCreator.generateBuffer(geography, getPixel().getGeom(), Settlement.maxDistanceFromIrrigation);
////			Geometry settBuffer = settlement.pixel.getGeom().buffer(Settlement.maxDistanceFromIrrigation);
//			IntersectsQuery query = new IntersectsQuery(geography, settBuffer);	
//			for (Object obj : query.query()) {
//				if (obj instanceof IrrigationPipeLink){
//					IrrigationPipeLink link = (IrrigationPipeLink)obj;
//					if(link.isCurrentlyOnline()){
//						setFutureSettlement(false);
//						initialize(context);
//						break;
//					}
//
//				}
//			}
//			return;
//		}
//		settlementAge++;
//		if(studyAreaFull){
//			return;
//		}
//		Context context = ContextUtils.getContext(this);
//		long additionalFarmers = calculateNextCycleAdditionalFarmers();
//		if(enoughCapacity(additionalFarmers)){
//			
//			assignAvailableParcelsForNewFarmers(additionalFarmers, context, false);
//		}
//		//not enough parcels in existing settlement to support new farmers
//		else{
//			//allocate the remainder available parcels in this settlement for some of the additional farmers
//			long numReminderFarmersInSett = numAvailableParcels/2;
//			if(numReminderFarmersInSett > 0){
//				additionalFarmers-= assignAvailableParcelsForNewFarmers(numReminderFarmersInSett, context, false);
//			}
//			excesiveHouseholds = additionalFarmers;
//			//search for parcels in another settlements
//			searchForLand();
			
			//debug
//			System.out.println("Total number of farmers in model is: " + FarmerAgent.totalNumFarmers);
//		}	
	}

	
	// This method allocates farmers for a settlement that existed in the model
	// initialization.
	void assignFarmersForExistingSettlement(Context context) {

		if (numAvailableParcels < minNumParcelsPerSettlement) {
			System.out.println("Error: Settlement " + ID + " doesn't have the minumum needed agricultural parcels");
			return;
		}
		long farmerCapacity = (numParcels / 2) /*-1*/;
		long additionalFarmers = (long) (farmerCapacity * farmerRatio);
		assignAvailableParcelsForNewFarmers(additionalFarmers, context, false);
		
//		for (int i = 0; i < additionalFarmers; ++i) {
//			if (settlementAvailableParcels.size() < 2) {
//				System.out.println("Error: not enough parcels for farmers, in Settlement "	+ ID + "number of available parcels is: " + settlementAvailableParcels.size());
//				return;
//			} else {
//				List<AgriculturalLandParcel> farmerParcels = new ArrayList<>();
//				// remove the last index in order to save time with the reorganization of the list after the removal
////				AgriculturalLandParcel activeParcel = settlementAvailableParcels
////						.remove(settlementAvailableParcels.size() - 1); 
////				AgriculturalLandParcel fallowParcel = settlementAvailableParcels
////						.remove(settlementAvailableParcels.size() - 1);
//				AgriculturalLandParcel activeParcel = settlementAvailableParcels.remove(settlementAvailableParcels.firstKey()); 
//				AgriculturalLandParcel fallowParcel = settlementAvailableParcels.remove(settlementAvailableParcels.firstKey());
//				numAvailableParcels -= 2;
//
//				activeParcel.setStatus(AgriculturalLandParcel.Status.CULTIVATED);
//				fallowParcel.setStatus(AgriculturalLandParcel.Status.FALLOW);
//				activeParcel.setInitialActiveSOM();
//				fallowParcel.setInitialFallowSOM();
//
//				farmerParcels.add(activeParcel);
//				farmerParcels.add(fallowParcel);
//
//				FarmerAgent farmer = new FarmerAgent(this, farmerParcels, context);
//				farmers.add(farmer);
//			}
//		}
	}

	void assignFarmerToExistingParcels(Context context) {//TODO make sure works!
		if(settlementParcels.isEmpty())
			return;
		
		List<AgriculturalLandParcel> farmerParcels = new ArrayList<>();
		boolean isActive = true; //used to alternate between active and fallow parcels' initialization 
		AgriculturalLandParcel.Status status;
				
		for(AgriculturalLandParcel parcel : settlementParcels){
			if(isActive){
				status = AgriculturalLandParcel.Status.CULTIVATED;
				parcel.setInitialActiveSOM();
			}else{
				status = AgriculturalLandParcel.Status.FALLOW;
				parcel.setInitialFallowSOM();
			}
			parcel.setStatus(status);
			farmerParcels.add(parcel);
			isActive = !isActive;
		}
		numAvailableParcels = 0;
		FarmerAgent farmer = new FarmerAgent(this, farmerParcels, context);
		farmers.add(farmer);
		numFarmers++;
	}
	
	void assignFarmerToParcels(Context context) {
		if (numAvailableParcels < minNumParcelsPerSettlement) {
			System.out.println("Error: Settlement " + ID + " doesn't have the minumum needed agricultural parcels");
			return;
		}
		assignAvailableParcelsForFarmer(context, false);
		
	}
	
	
	long calculateAgriculturalCapacity(Context context, Geometry geom){
		long settNumParcels = 0;
		Geography geography = (Geography)context.getProjection("Geography"); 
		//find all pixels within a settlementRadius distance from the settlement
		double radius = settlementRadius; // meters
		tmm.clear();
		
//		Geometry geom = (Geometry) this.getPixel().getGeom();
//		Geometry geom3 = ContextCreator.convertGeometrytoUTM(geom, geography);
//		Geometry geom2 = geography.getGeometry(this);
		Geometry pipeBufferGeom = geom.buffer(settlementRadius);
		Geometry pipeBufferGeomConverted = ContextCreator.convertGeometryToWGS(pipeBufferGeom, geography);
				
		//debug - measure how much time it takes for GeographyWithin query
		long myStart = System.nanoTime();
		
		IntersectsQuery query = new IntersectsQuery(geography, pipeBufferGeomConverted); 
//		GeographyWithin within = new GeographyWithin(geography, radius, this);
	
		// debug
		long myTime = System.nanoTime() - myStart;
		logger.log(Level.FINE, "-------------------------------------------");
		logger.log(Level.FINE, "time to find neighbors within " + radius +" meters using Geography within is " + myTime + " nanoseconds");
		logger.log(Level.FINE, "-------------------------------------------");

		//debug
		myStart = System.currentTimeMillis();
		int querySize = 0;
		
		for (Object obj : query.query()) {
//		for (Object obj : within.query()) {
			++querySize;
			if (obj instanceof Pixel){
				Pixel pix = (Pixel) obj;
				//no need to insert pixel to list if it isn't suitable for ag.
				if (pix.isSuitable()) {				
					double distance = Pixel.calculateSquaredDistance(pix, this.pixel);
					tmm.put(distance, pix);
				}
			}
		}
		
		myTime = System.currentTimeMillis() - myStart;
		logger.log(Level.FINE, "------ number of elements in Query is " + querySize );
		logger.log(Level.FINE, "number of elements in tree multimap is " + tmm.size());
		logger.log(Level.FINE, "time to update multi map is " + myTime + " miliseconds");
		logger.log(Level.FINE, "-------------------------------------------");
		
		if(tmm.size()*2 < querySize){//if more than half the pixels in the candidate settlement is not suitable for agriculture - quit
			return -1;
		}
		
		ngbrMap.clear(); 
		myStart = System.currentTimeMillis();
		
		for(Iterator<Pixel> iter = tmm.values().iterator(); iter.hasNext();) {
			Pixel pix = iter.next();
			if(pix.isSuitable()){
				Coordinate pixelCoord = pix.getCoord_m();
				double x_coord = pixelCoord.x;
				double y_coord = pixelCoord.y;
				for(double x= x_coord-pixelSize; x <= x_coord+pixelSize; x+=pixelSize){
					for(double y= y_coord-pixelSize; y <= y_coord+pixelSize; y+=pixelSize){
						for(double i=-2 ; i<=2 ; i++){//in case the pixels are not aligned exactly "pixelSize" meters away from eachother 
							for(double j=-2; j<=2 ; j++){
								Pixel ngbr = Pixel.pixelMap.get(new Coordinate(x+i,y+j));
								if(ngbr !=null && ngbr.isSuitable() && !ngbr.equals(pix)){
									ngbrMap.put(pix, ngbr);  //the pix pixel is the key pixel and ngbr is one of its immediate neighbors
								}
								
							}
						}
//						Pixel ngbr = Pixel.pixelMap.get(new Coordinate(x,y));
//						if(ngbr !=null && ngbr.isSuitable() && !ngbr.equals(pix)){
//							ngbrMap.put(pix, ngbr);  //the pix pixel is the key pixel and ngbr is one of its immediate neighbors
//						}
					}
				}
			}
		}
		myTime = System.currentTimeMillis() - myStart;
		logger.log(Level.FINE, "time to create a pixel neighbor table is " + myTime + " miliseconds");
		
		
		//start allocating parcels from the vicinity of the settlement going outwards - the 4 closest neighbors (Van Newman neighborhood) 
		//of the settlement are pixelSize away (30 meters). TreeMulitmap.values() returns all values sorted according to ascending
		//distance from the settlement and ascending order of pixels according to their coordinates
		int key = 0;
		myStart = System.currentTimeMillis();
		for(Iterator<Pixel> iter = tmm.values().iterator(); iter.hasNext();) {
			Pixel candidate = iter.next();
			if(candidate.isSuitable()){
				candidate.classification = Pixel.Classification.MARKED;
				List<Pixel> parcelPix = allocateParcel2(candidate, geography);
				 if (parcelPix !=null){
					 ++settNumParcels;
					 Geometry parcelGeom = (MultiPolygon)calculateGeometry(parcelPix);
					 //calculate meter geometry in order to find parcel's area in m^2
					 Geometry parcelMeterGeom = ContextCreator.convertGeometryFromWGStoCRS(parcelGeom, geography); 
					 
					 AgriculturalLandParcel parcel = new AgriculturalLandParcel(this,parcelPix, parcelGeom, 
							 parcelMeterGeom.getArea(), context);
					//update new parcel's - soilType, rainThickness, isPotentiallyIrrigated, and settlement's arable and irrigated land.
					 updateNewParcel(parcel, candidate);
			 
					 settlementParcels.add(parcel);
					 settlementAvailableParcels.put(key++, parcel);
				 } 
				 //if we can't allocate a parcel (parcelPix==null) then restore candidate's classification so it could be used for another parcel 
				 //no need  - releasing is done within allocateParcel()
				 else{
					 candidate.classification = Pixel.Classification.SUITABLE;
				 }
			}     
	    }
		
		myTime = System.currentTimeMillis() - myStart;
		logger.log(Level.FINE, "time to allocate agricultural parcels is " + myTime + " miliseconds");
			
		numParcels = numAvailableParcels = settNumParcels;

		// debug
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		logger.log(Level.FINE, timeStamp + ": number of parcels is: " + numAvailableParcels + " in settlement: " + this.ID);
		logger.log(Level.FINE, "Total number of farmers in model is: " + FarmerAgent.totalNumFarmers);
		
		return settNumParcels;
	}
	//update new parcel's - soilType, rainThickness, isPotentiallyIrrigated, and settlement's arable and irrigated land.
	private void updateNewParcel(AgriculturalLandParcel parcel, Pixel candidate){
		double parcelArea = parcel.getArea();
//		parcel.setArea(parcelArea);
		this.arableLand+=parcelArea;
//		this.currentAgriArea+=parcelArea;

		PotentialAgPixel agPixel = (PotentialAgPixel)candidate;
		parcel.setSoilType(agPixel.getSoilType());
		parcel.setRainThickness(agPixel.getRainAnnualAmount());
		
		parcel.setIrrigated(true);//new settlements are created with irrigation infrastructure
		irrigatedLand+=parcelArea;
		
		int irrigationID = agPixel.getClosestIrrid();
		IrrigationPipeLink closestIrrPipe = IrrigationPipeLink.irrigationMap.get(new Integer(irrigationID));
		if(closestIrrPipe != null){
			parcel.setReclaimedWaterIrrigation(closestIrrPipe.isReclaimedWater());
		}
		
		

	}

	
	// calculate and return the number of agricultural parcels that are within a
	// distance of settlementRadius from this settlement.
	// This method should be called only once per settlement’s lifetime (usually
	// when the settlement is created).
	// This method will create - List < AgriculturalLandParcel> settlementParcels
	// and List < AgriculturalLandParcel> settlementAvailableParcels
	// initially: settlementParcels == settlementAvailableParcels
	long calculateAgriculturalCapacity(Context context){
	
		long settNumParcels = 0;
		
		Geography geography = (Geography)context.getProjection("Geography"); 
		
		//find all pixels within a settlementRadius distance from the settlement
		double radius = settlementRadius; // meters
		
//		int expectedValuesPerKey = 4; //every 4 neighbors have the same distance from the Settlement pixel.
//		int expectedKeys = (int)((Math.PI * Math.pow(settlementRadius, 2))/ (Math.pow(pixelSize,2) * expectedValuesPerKey)) ;
////		HashMultimap<Double, Pixel> hm =  HashMultimap.create(8000,4);
//		HashMultimap<Double, Pixel> hm =  HashMultimap.create(expectedKeys,expectedValuesPerKey);
//		TreeMultimap<Double, Pixel> tmm = TreeMultimap.create(hm);
		
		tmm.clear();
		
		//debug - measure how much time it takes for GeographyWithin query
		long myStart = System.nanoTime();
//		long myStart = System.currentTimeMillis();
		
		GeographyWithin within = new GeographyWithin(geography, radius, this);
		
		// debug
		long myTime = System.nanoTime() - myStart;
		System.out.println("-------------------------------------------");
		System.out.println("time to find neighbors within " + radius +" meters using Geography within is " + myTime + " nanoseconds");
		System.out.println("-------------------------------------------");

//		myStart = System.nanoTime();
//		Envelope settlementEnvelope = new Envelope(this.getPixel().getCoord_m());
//		settlementEnvelope.expandBy((radius));
//		convertToUTM(geography, settlementEnvelope);
//			
//		Class pixel = null;
//		try {
//			pixel = Class.forName("negevVer1.Pixel");
//		} catch (ClassNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			System.out.println("class Pixel not found");
//		}
//		Iterable<Pixel> envelopeIter = geography.getObjectsWithin(settlementEnvelope, pixel);	
		
//		myTime = System.nanoTime() - myStart;
//		System.out.println("-------------------------------------------");
//		System.out.println("time to find neighbors within " + radius +" meters using geography.getObjectsWithin is " + myTime + " nanoseconds");
//		System.out.println("-------------------------------------------");
		
		//debug
		myStart = System.currentTimeMillis();
		int querySize = 0; //debug
		
		for (Object obj : within.query()) {
//		for (Object obj : envelopeIter) {
			++querySize;
			if (obj instanceof Pixel){
				Pixel pix = (Pixel) obj;
				//no need to insert pixel to list if it isn't suitable for ag.
				if (pix.isSuitable()) {
					//debug - distance with sqrt
//					myStart = System.nanoTime();
//					double oldDistance = calculateDistance(pix, this.pixel);
//					myStop = System.nanoTime();
//					myTime = myStop - myStart;
//					System.out.println(myTime);
					
					double distance = Pixel.calculateSquaredDistance(pix, this.pixel);
					tmm.put(distance, pix);
				}
			}
		}
		
		
		myTime = System.currentTimeMillis() - myStart;
		System.out.println("------ number of elements in Query is " + querySize );
		System.out.println("number of elements in tree multimap is " + tmm.size());
		System.out.println("time to update multi map is " + myTime + " miliseconds");
		System.out.println("-------------------------------------------");
//		System.out.println("Finished Calculating distance with sqrt");
//		System.out.println("-------------------------------------------");
	
		ngbrMap.clear(); 
		myStart = System.currentTimeMillis();
		
		for(Iterator<Pixel> iter = tmm.values().iterator(); iter.hasNext();) {
			Pixel pix = iter.next();
			if(pix.isSuitable()){
				Coordinate pixelCoord = pix.getCoord_m();
				double x_coord = pixelCoord.x;
				double y_coord = pixelCoord.y;
				for(double x= x_coord-pixelSize; x <= x_coord+pixelSize; x+=pixelSize){
					for(double y= y_coord-pixelSize; y <= y_coord+pixelSize; y+=pixelSize){
						Pixel ngbr = Pixel.pixelMap.get(new Coordinate(x,y));
						if(ngbr !=null && ngbr.isSuitable() && !ngbr.equals(pix)){
							ngbrMap.put(pix, ngbr);  //the pix pixel is the key pixel and ngbr is one of its immediate neighbors
						}
					}
				}
			}
		}
		myTime = System.currentTimeMillis() - myStart;
		System.out.println("time to create a pixel neighbor table is " + myTime + " miliseconds");
		
		
		//start allocating parcels from the vicinity of the settlement going outwards - the 4 closest neighbors (Van Newman neighborhood) 
		//of the settlement are pixelSize away (30 meters). TreeMulitmap.values() returns all values sorted according to ascending
		//distance from the settlement and ascending order of pixels according to their coordinates
		int key = 0;
		myStart = System.currentTimeMillis();
		for(Iterator<Pixel> iter = tmm.values().iterator(); iter.hasNext();) {
			Pixel candidate = iter.next();
			if(candidate.isSuitable()){
				candidate.classification = Pixel.Classification.MARKED;
				List<Pixel> parcelPix = allocateParcel2(candidate, geography);
				 if (parcelPix !=null){
					 ++settNumParcels;
					 Geometry parcelGeom = calculateGeometry(parcelPix);
					 AgriculturalLandParcel parcel = new AgriculturalLandParcel(this,parcelPix, parcelGeom, parcelGeom.getArea(), context);
					 settlementParcels.add(parcel);
					 settlementAvailableParcels.put(key++, parcel);
				 } 
				 //if we can't allocate a parcel (parcelPix==null) then restore candidate's classification so it could be used for another parcel 
				 //no need  - releasing is done within allocateParcel()
				 else{
					 candidate.classification = Pixel.Classification.SUITABLE;
				 }
			}     
	    }
		
		myTime = System.currentTimeMillis() - myStart;
		System.out.println("time to allocate agricultural parcels is " + myTime + " miliseconds");
			
		numParcels = numAvailableParcels = settNumParcels;

		// debug
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		System.out.println(timeStamp + ": number of parcels is: " + numAvailableParcels + " in settlement: " + this.ID);
		System.out.println("Total number of farmers in model is: " + FarmerAgent.totalNumFarmers);
		
		logger.log(Level.FINE, "Total number of farmers in model is: " + FarmerAgent.totalNumFarmers);
		
		return settNumParcels;
	}

//	private void convertToUTM(Geography geography, Envelope envelope) {
//		boolean convert = !geography.getUnits(0).equals(SI.METER);
//
//		CoordinateReferenceSystem utm = null;
////		Geometry buffer = null;
//		CoordinateReferenceSystem crs = geography.getCRS();
//		
//		
//		Envelope e2 = null;
//
//		try {
//			// convert p to UTM
//			if (convert) {
//				utm = UTMFinder.getUTMFor(geom, crs);
//				e2 = JTS.transform(envelope, cFactory.createOperation(crs, utm).getMathTransform());
//			}
//		
//	}
		
	private Geometry calculateGeometry(List<Pixel> parcelPix) {
		
		Coordinate[] coordinates = new Coordinate[parcelPix.size()+1];
				
		for (int i=0; i< parcelPix.size(); i++ ) {
//			coordinates[i] = parcelPix.get(i).getCoord_d();
			coordinates[i] = parcelPix.get(i).getGeom().getCoordinate();
//			coordinates[i] = parcelPix.get(i).getCoord_m(); //TODO - make sure it works with metric
		}
		coordinates[coordinates.length-1] = coordinates[0]; //in order to create a closed polygon
		GeometryFactory fact = new GeometryFactory();
//		MultiPoint mPoint = fact.createMultiPoint(coordinates);
		Polygon poly = fact.createPolygon(coordinates);
//		Geometry test = mPoint.convexHull();
		Geometry convexPoly = poly.convexHull();
		Polygon[] polygons = {(Polygon)convexPoly};
		MultiPolygon mPoly = fact.createMultiPolygon(polygons);
		return mPoly;
		
//		return mPoint.convexHull(); // this is a polygon with 13 points (12 point with first and last the same)
	 
		
		
//		Geometry geom = mPoint.getBoundary(); //this is of type GeometryCollection with zero points and zero area
		
//		System.out.println("geometry area is: " + mPoint.convexHull().getArea());
//		System.out.println("geometry number of points is: " + mPoint.convexHull().getNumPoints());
		
		
		
//		return mPoint.getGeometryN(0); //only one point
//		return mPoint;
//		return mPoint.getEnvelope(); // not working - it is a polygon with 5 points...
//		return mPoint.getCentroid(); //the center of the parcel 
//		LinearRing linear = fact.createLinearRing(coordinates); //not working
//		Polygon poly = new Polygon(linear, null, fact);
//		return poly; //not working
	}
	
private Geometry calculateMeterGeometry(List<Pixel> parcelPix) {
		Coordinate[] coordinates = new Coordinate[parcelPix.size()];
		for (int i=0; i< parcelPix.size(); i++ ) {
			coordinates[i] = parcelPix.get(i).getCoord_m(); //TODO - make sure it works with metric
		}
		GeometryFactory fact = new GeometryFactory();
		MultiPoint mPoint = fact.createMultiPoint(coordinates);

//		System.out.println("geometry area is: " + mPoint.convexHull().getArea());
//		System.out.println("geometry number of points is: " + mPoint.convexHull().getNumPoints());
		
		
		return mPoint.convexHull(); // this is a polygon with 13 points (12 point with first and last the same)
//		return mPoint.getGeometryN(0); //only one point
//		return mPoint;
//		return mPoint.getEnvelope(); // not working - it is a polygon with 5 points...
//		return mPoint.getCentroid(); //the center of the parcel 
//		LinearRing linear = fact.createLinearRing(coordinates); //not working
//		Polygon poly = new Polygon(linear, null, fact);
//		return poly; //not working
	}


	private List<Pixel> allocateParcel2(Pixel pixel, Geography geography) {
		
		//debug - measure how much time it takes for allocateParcel2 function - using the neighbor table
		long myStart = System.currentTimeMillis();
		
		List<Pixel> parcelPixels = new ArrayList<Pixel>();
		parcelPixels.add(pixel);
		int listIndex = 0; //traversing over the parcelPixels list
		int counter = 1;
		while(counter < numPixelsInParcel){
			for(Pixel ngbr : ngbrMap.get(parcelPixels.get(listIndex))){
				if(ngbr.isSuitable()){
					++counter;
					ngbr.classification = Pixel.Classification.MARKED;
					parcelPixels.add(ngbr);
					if(counter >= numPixelsInParcel){
						break;
					}
					
				}
			}
			if(counter <= listIndex+1){ //if there are no more new pixels added to the parcelPixels list - we can't continue looking 
				for (Pixel myPixel : parcelPixels) {
					myPixel.classification = Pixel.Classification.SUITABLE; //un-mark the pixels in the list so they can be used for another parcel
				}
				return null;
			}
			++listIndex; //go to the next neighbor and check for more pixels	
		}
		// go over the list and change classification to CLAIMED
		for (Pixel myPixel : parcelPixels) {
			myPixel.classification = Pixel.Classification.CLAIMED;
		}
		long myTime = System.currentTimeMillis() - myStart;
        
        logger.log(Level.FINER, "time to allocate parcel using Neighbor table is " + myTime + " milliseconds");
		logger.log(Level.FINER, "--------------------------------------------------------------");
		return parcelPixels;
	}

	private List<Pixel> allocateParcel(Pixel pixel, Geography geography) {

		List<Pixel> parcelPixels = new ArrayList<Pixel>();
		parcelPixels.add(pixel);
		int listIndex = 0; //traversing over the parcelPixels list
		int counter = 1; // the number of pixels already added to the parcel - we need 12 pixels to create a parcel

		//debug - measure how much time it takes for GeographyWithin query
		long myStart1 = System.currentTimeMillis();
		
		while(counter < 12){
			
			//get all 4 immediate Van Newman neighbors
			//IntersectsQuery query = new IntersectsQuery(geography,parcelPixels.get(listIndex).geom.buffer(pixelSize + 0.5)); 
//			GeographyWithin within = new GeographyWithin(geography, pixelSize+1, parcelPixels.get(listIndex));	
			
			long myStart = System.currentTimeMillis();
			
			//get all 8 immediate Moore neighbors
			GeographyWithin within = new GeographyWithin(geography, MooreNbh+1, parcelPixels.get(listIndex));

	        long myTime = System.currentTimeMillis() - myStart;
	        logger.log(Level.FINEST, "time to execute Geography within of 8 neighbors is " + myTime + " milliseconds");
//	        System.out.println("time to execute Geography within of 8 neighbors is " + myTime + " milliseconds");
	        
	        myStart = System.currentTimeMillis();
	        long myStart2 = System.currentTimeMillis();
	        long debugCnt = 0;
	        
			for (Object obj : within.query()) {
				myTime = System.currentTimeMillis() - myStart2;
		        logger.log(Level.FINEST, "Beginning of For loop iteration ------------- " + myTime + " milliseconds");
		        if (!(obj instanceof Pixel)) {
		        	logger.log(Level.FINEST, " instance is not a pixel but a " + obj.getClass().toString());
		        	debugCnt++;
		        }
				if (obj instanceof Pixel) {
					myTime = System.currentTimeMillis() - myStart2;
			        logger.log(Level.FINEST, "Check type of instance____________ " + myTime + " milliseconds");
					Pixel pix = (Pixel) obj;
					if(pix.isSuitable()){
						myTime = System.currentTimeMillis() - myStart2;
				        logger.log(Level.FINEST, "check isSuitable() " + myTime + " milliseconds");
						++counter;
						pix.classification = Pixel.Classification.MARKED;
						parcelPixels.add(pix);
						if(counter >= 12){
							break;
						}
						
						myTime = System.currentTimeMillis() - myStart2;
				        logger.log(Level.FINEST, "Change pixel classification " + myTime + " milliseconds");
						
					}
				}
				myTime = System.currentTimeMillis() - myStart2;
		        logger.log(Level.FINEST, "End of For loop iteration ------------- " + myTime + " milliseconds");
		        myStart2 = System.currentTimeMillis();
			}
			logger.log(Level.FINEST, "NUMBER OF NON PIXEL OBJECT is " + debugCnt); 
			myTime = System.currentTimeMillis() - myStart;
	        logger.log(Level.FINEST, "time to execute for loop - going over query results is " + myTime + " milliseconds");
	        
			//check if there are any more candidates left to iterate on - if not - not enough pixels available 
			//in order to create a parcel - return null
			if(counter <= listIndex+1){
				for (Pixel myPixel : parcelPixels) {
					myPixel.classification = Pixel.Classification.SUITABLE; //un-mark the pixels in the list so they can be used for another parcel
				}
				
				myTime = System.currentTimeMillis() - myStart1;
				logger.log(Level.FINER, "allocating parcel failed. Elapsed time: " + myTime + " milliseconds");
				logger.log(Level.FINER, "--------------------------------------------------------------");
//				System.out.println("allocating parcel failed. Elapsed time: " + myTime + " milliseconds");
//		        System.out.println("--------------------------------------------------------------");
		        
				return null;
			}
			++listIndex; //go to the next neighbor and check for more pixels
		}
		
		//debug
        long myTime = System.currentTimeMillis() - myStart1;
        
        logger.log(Level.FINER, "time to allocate parcel using Geography within is " + myTime + " milliseconds");
		logger.log(Level.FINER, "--------------------------------------------------------------");
//        System.out.println("time to allocate parcel using Geography within is " + myTime + " milliseconds");
//        System.out.println("--------------------------------------------------------------");
        
		// go over the list and change classification to CLAIMED
		for (Pixel myPixel : parcelPixels) {
			myPixel.classification = Pixel.Classification.CLAIMED;
		}
		return parcelPixels;
	}
	
	
	
	
	// Try to allocate a parcel around the passed pixel
//	private List<Pixel> allocateParcel_old(Pixel pixel, Geography geography) {
//
//		List<Pixel> parcelPixels = new ArrayList<>();
//
//		// IntersectsQuery query = new
//		// IntersectsQuery(geography,pixel.geom.buffer(distance)); //doing the
//		// same thing with intersect query instead of within query
//
//		GeographyWithin within = new GeographyWithin(geography, HectareRadius,
//				pixel);
//		for (Object obj : within.query()) {
//			if (obj instanceof Pixel) {
//				Pixel pix = (Pixel) obj;
//				if (pix.classification != Pixel.Classification.SUITABLE) {
//					return null;
//				} else {
//					parcelPixels.add(pix);
//
//				}
//			}
//		}
//		parcelPixels.add(pixel);
//		// go over the list and change classification to CLAIMED
//		for (Pixel myPixel : parcelPixels) {
//			myPixel.classification = Pixel.Classification.CLAIMED;
//		}
//
//		return parcelPixels;
//	}


	// calculate the additional number of farmers for the next coming year.
	public long calculateNextCycleAdditionalFarmers() {
		return Math.max(1,((long)(this.numFarmers * annPopGrowthRate)));
	}

	// does the settlement have enough available parcels for additional farmers
	// for the next coming year?
	private Boolean enoughCapacity(long additionalFarmers) {
		if(numAvailableParcels >= additionalFarmers*2)
		{
			return true;
		}
		return false;
	}

	// assigning available parcels within this settlement for new farmers. Return the number of farmers that were allocated parcels.
	private long assignAvailableParcelsForNewFarmers(long additionalFarmers, Context context, boolean isNewSettlement) {
		long addedFarmers = 0;
		for (int i = 0; i < additionalFarmers; ++i) {
			if (settlementAvailableParcels.size() < 2) {
				System.out.println("Error: not enough parcels for farmers, in Settlement number "	+ ID + 
						". number of added farmers is: " + addedFarmers + " out of " + additionalFarmers + " additional farmers");
				return addedFarmers;
			} else {
				List<AgriculturalLandParcel> farmerParcels = new ArrayList<>();
				AgriculturalLandParcel activeParcel = settlementAvailableParcels.remove(settlementAvailableParcels.firstKey()); 
				AgriculturalLandParcel fallowParcel = settlementAvailableParcels.remove(settlementAvailableParcels.firstKey());
				numAvailableParcels -= 2;
				numFarmers++;
				addedFarmers++;

				activeParcel.setStatus(AgriculturalLandParcel.Status.CULTIVATED);
				fallowParcel.setStatus(AgriculturalLandParcel.Status.FALLOW);
				if (isNewSettlement) {
					activeParcel.setSOM(AgriculturalLandParcel.parcelInitialSOM);
					fallowParcel.setSOM(AgriculturalLandParcel.parcelInitialSOM);
				} else {
					activeParcel.setInitialActiveSOM();
					fallowParcel.setInitialFallowSOM();
				}

				farmerParcels.add(activeParcel);
				farmerParcels.add(fallowParcel);

				FarmerAgent farmer = new FarmerAgent(this, farmerParcels, context);
				farmers.add(farmer);
			}
		}
//		System.out.println("Number of farmers in settlement number: " + this.ID + " is: " + numFarmers);
		return addedFarmers;
	}

	private void assignAvailableParcelsForFarmer(Context context, boolean isNewSettlement) {
		List<AgriculturalLandParcel> farmerParcels = new ArrayList<>();
		boolean isActive = true; //used to alternate between active and fallow parcels' initialization 
		while(!settlementAvailableParcels.isEmpty()){
			AgriculturalLandParcel parcel = initializeParcel(isActive, isNewSettlement );
			farmerParcels.add(parcel);
			isActive = !isActive;
		}
		numAvailableParcels = 0;
		FarmerAgent farmer = new FarmerAgent(this, farmerParcels, context);
		farmers.add(farmer);
		numFarmers++;
	}
	
	private AgriculturalLandParcel initializeParcel(boolean isActive, boolean isNewSettlement){
		AgriculturalLandParcel.Status status;
		if(isActive){
			status = AgriculturalLandParcel.Status.CULTIVATED;
		}else{
			status = AgriculturalLandParcel.Status.FALLOW;
		}
		AgriculturalLandParcel parcel = settlementAvailableParcels.remove(settlementAvailableParcels.firstKey());
		parcel.setStatus(status);
		if (isNewSettlement) {
			parcel.setSOM(AgriculturalLandParcel.parcelInitialSOM);
		} else {
			if (isActive) {
				parcel.setInitialActiveSOM();
			} else {
				parcel.setInitialFallowSOM();
			}
		}
		return parcel;
	}
	
	// find land for the new farmers that can’t be accommodated in this
	// settlement.
	// Return the number of settled farmers, and update numEmigrants if
	// necessary.
	private long searchForLand() {
		long overallNumFarmers = excesiveHouseholds;
		Context context = ContextUtils.getContext(this);
		long allocatedFarmers = moveToExistingSettlements(this.excesiveHouseholds, context);
		excesiveHouseholds-= allocatedFarmers;
		
		while(excesiveHouseholds > 0){ //not enough space in existing settlements - need to create new one/s
			Settlement newSettlement = establishNewSettlement(context);
			if(newSettlement == null){
				System.out.println("Can't create new Settlements - area is full");
				System.out.println("Total number of farmers in model is: " + FarmerAgent.totalNumFarmers);
				studyAreaFull = true;
				return overallNumFarmers-excesiveHouseholds;
			}
			if(newSettlement.enoughCapacity(excesiveHouseholds)){
				allocatedFarmers = newSettlement.assignAvailableParcelsForNewFarmers(excesiveHouseholds, context, true);
				excesiveHouseholds-= allocatedFarmers;
			}
			//not enough parcels in new settlement to support excessive farmers
			else{
				//allocate the remainder available parcels in this settlement for some of the excessive farmers
				long numReminderFarmersInSett = newSettlement.numAvailableParcels/2;
				if(numReminderFarmersInSett >0){
					excesiveHouseholds-= newSettlement.assignAvailableParcelsForNewFarmers(numReminderFarmersInSett, context, true);
				}
			}
		}
		return overallNumFarmers;
	}

	// look for a land in existing settlements.
	// Return the number of farmers that found a place.
	private long moveToExistingSettlements(long farmers, Context context) {
		long allocatedFarmers = 0;
		for (Settlement settlement : Settlement.settlements) {
			if(allocatedFarmers >= farmers){
				return allocatedFarmers;
			}
			if(settlement.enoughCapacity((farmers-allocatedFarmers))){
				allocatedFarmers += settlement.assignAvailableParcelsForNewFarmers((farmers-allocatedFarmers), context, false);
			}
			else{
				long numReminderFarmersInSett = settlement.numAvailableParcels/2;
				if(numReminderFarmersInSett >0){
					allocatedFarmers += settlement.assignAvailableParcelsForNewFarmers(numReminderFarmersInSett, context, false);
				}
				
			}	
		}
		return allocatedFarmers;
	}

	// if not enough land in existing settlements – try to find if it is
	// possible to establish a new settlement along the existing roads.
	//A new settlement will be no more than 500 meters away from the road. A candidate pixel that is the farthest away
	//from all existing settlements, will be chosen for a new settlement.
	// This method will also call calculateAgriculturalCapacity()
	private Settlement establishNewSettlement(Context context) {
//		if(Road.useRoadSet == true){
//			return chooseSettlementPixel(context); 
//		}
//		
//		Geography geography = (Geography) context.getProjection("Geography");
//		for (Road road : Road.roads) {
////			if(road.getDistanceToSettlement() != -1){//once this pixel has distance set up - it is in the SortedSet - no need to re-examine it. 
////				continue;
////			}
//			double squareDistance = studyAreaMaxSquaredDistance; //study area maximum squared distance 
//			for(Settlement settlement: Settlement.settlements){
//				double currentSquareDistance = Pixel.calculateSquaredDistance(road.pixel, settlement.getPixel());
//				if(currentSquareDistance < squareDistance){
//					squareDistance =currentSquareDistance;
//				}
//			}
////			road.setDistanceToSettlement(distance);
//			road.setSquaredDistanceToSettlement(squareDistance);
//			if(squareDistance > Math.pow((settlementRadius*2), 2)){ //add candidate to roadSet only if it's not too close to another existing settlement
//				Road.roadSet.add(road);
//			}
//		}
//		Road.useRoadSet = true;
//		
//		//deep copy roadSet to  initializedRoadSet for faster next model re-run
//		for (Road road : Road.roadSet) {
//			Road newRoad = new Road(road);
//			Road.initializedRoadSet.add(newRoad);
//		}
//		return chooseSettlementPixel(context);
		
		if (Settlement.potentialSettlements.isEmpty()){
			return null;
		}
		
		Settlement newSettlement = Settlement.potentialSettlements.remove(0);
		newSettlement.setFutureSettlement(false);
		Geography geography = (Geography) context.getProjection("Geography");
		context.add(newSettlement);
		geography.move(newSettlement, newSettlement.getPixel().getGeom());
		Settlement.settlements.add(newSettlement);
		for(AgriculturalLandParcel parcel: newSettlement.settlementParcels ){ //add parcels to context
			context.add(parcel);
			geography.move(parcel, parcel.getParcelGeom());
		}

		return newSettlement;
			
			
			
			
//			double distance = maxDistance;
//			GeographyWithin within = new GeographyWithin(geography, maxDistance , road.pixel);
//
//			for (Object obj : within.query()) {
//				if (obj instanceof Settlement) {
//					Settlement settlement = (Settlement) obj;
//					double currentDistance = calculateDistance(road.pixel, settlement.getPixel());
//					if(currentDistance < distance){
//						distance =currentDistance;
//					}
//				}
//			}
//			//distance now holds the minimal distance between the road pixel to any settlement within maxDistance
//			if(distance >= maxDistance){
//				//there are no settlements within maxDistance away from this road pixel - we can construct a new settlement
//				//no need to look further
//				Settlement newSettlement = new Settlement(road.pixel);
//				newSettlement.calculateAgriculturalCapacity(context);
//				
//				removeSurroundingRoadPixels(context, road); //it is now a settlement. no more settlements could be constructed in this area.
//				return newSettlement;
//			}
//			//insert road pixel and distance into an ordered sortedSet
//			else{
//				road.setDistanceToSettlement(distance);
//				if(distance > settlementRadius){ //don't add candidate if it's too close to another exisitng settlement
//					Road.roadSet.add(road);
//				}	
//			}			
//		}
//		//there are no road pixels with maxDistance away from settlements - this is not going to change - therefore next time use Road sortedSet - no need to do any more searches 
//		Road.useRoadSet = true;
//		return chooseSettlementPixel(context);
	}
	
	//once a new settlement is allocated on a road pixel - all surrounding road pixels
	//within settlementRadius distance should be removed from Road.roads and Road.roadSet - since they can no longer be candidates for new settlements
//	private void removeSurroundingRoadPixels(Context context, Road road){
//		Geography geography = (Geography) context.getProjection("Geography");
//		
//		//debug - measure how much time it takes for GeographyWithin query
//		long myStart = System.currentTimeMillis();
//		
//		GeographyWithin within = new GeographyWithin(geography, settlementRadius , road.pixel);
//		
//		// debug
//		long myStop = System.currentTimeMillis();
//		long myTime = myStop - myStart;
//		System.out.println("-------------------------------------------");
//		System.out.println("time to removeSurroundingRoadPixels using Geography within is " + myTime);
//		System.out.println("-------------------------------------------");
//
//		for (Object obj : within.query()) {
//			if (obj instanceof Road) {
//				Road myRoad = (Road) obj;
//				Road.roads.remove(myRoad);
//				Road.roadSet.remove(myRoad);
//			}
//		}
//		Road.roads.remove(road);
//		updateRoadSet(road);
//	}
	
	//choose the first element from Road.roadSet that contains a road pixel that is closest to any other existing settlement
	//update Road.roadSet to consider new Settlement: once a new settlement is allocated on a road pixel - all surrounding road pixels
	//within settlementRadius distance should be removed from Road.roadSet - since they can no longer be candidates for new settlements
	private Settlement chooseSettlementPixel(Context context){
		if(RoadCoord.roadSet.isEmpty()){
			return null;
		}
		RoadCoord road2Settlement = RoadCoord.roadSet.first(); //choose the closest settlement candidate to existing settlements' group
		
//		GeometryFactory geomFactory = new GeometryFactory();
//		Point roadCoordGeom = geomFactory.createPoint(road2Settlement.getCoord());
		Pixel settPixel = new Pixel(Classification.SETTLEMENT, road2Settlement.getGeom(), road2Settlement.getCoord());
		Settlement newSettlement = new Settlement(settPixel);
		newSettlement.calculateAgriculturalCapacity(context);
		
		RoadCoord.updateRoadSet(road2Settlement, context);
		RoadCoord.roadSet.remove(road2Settlement);
		return newSettlement;
	}
	

	// Returns the number of unclaimed available agricultural parcels.
	public long calculateNumAvailableParcels() {
		return numParcels - (numFarmers * 2);
	}

	List<AgriculturalLandParcel> getParcels() {
		return settlementParcels;
	}

	TreeMap<Integer,AgriculturalLandParcel> getAvailableParcels() {
		return settlementAvailableParcels;
	}
	
	public boolean isFutureSettlement() {
		return isFutureSettlement;
	}
	
	void setFutureSettlement(boolean isFutureSettlement) {
		this.isFutureSettlement = isFutureSettlement;
	}
	
	public static double getAnnpopgrowthrate() {
		return annPopGrowthRate;
	}
	
	public long getID() {
		return ID;
	}
	
	public long getNumFarmers() {
		return numFarmers;
	}
	
	public boolean isUnmetWaterDemand() {
		return unmetWaterDemand;
	}
	
	TreeMap<Integer, AgriculturalLandParcel> getSettlementAvailableParcels() {
		return settlementAvailableParcels;
	}
	
	public long getNumParcels() {
		return numParcels;
	}
	
	public int getSettlementAge() {
		return settlementAge;
	}
	
	public String getName() {
		return name;
	}
	
	void setName(String name) {
		this.name = name;
	}
	
	public Coordinate getCordinate(){
		return pixel.getCoord_m();
	}

	public double getAgriArea() {
		return currentAgriArea;
	}
	
	void setAgriArea(double agriArea) {
		this.currentAgriArea = agriArea;
	}

	public long getNumSettlements() {
		return numSettlements;
	}

	public long getNumAvailableParcels() {
		return numAvailableParcels;
	}

	public double getTotalAgriArea() {
		return totalAgLandInStudyArea;
	}
	
	public static double getTotalIrrigatedAgLandInStudyArea() {
		return totalIrrigatedAgLandInStudyArea;
	}
	
	public static double getTotalAgLandInStudyArea() {
		return totalAgLandInStudyArea;
	}
	//not static in order to show in model
	public double getTotalAddedRoadKM() {
		return totalAddedRoadKM;
	}
	//not static in order to show in model
	public double getTotalAddedIrrInfraKM() {
		return totalAddedIrrInfraKM;
	}

	public static void setTotalAddedIrrInfraKM(double totalAddedIrrInfraKM) {
		Settlement.totalAddedIrrInfraKM = totalAddedIrrInfraKM;
	}

	public static void setTotalAgLandInStudyArea(double totalAgLandInStudyArea) {
		Settlement.totalAgLandInStudyArea = totalAgLandInStudyArea;
	}

	public static void setTotalAddedRoadKM(double totalAddedRoadKM) {
		Settlement.totalAddedRoadKM = totalAddedRoadKM;
	}

	//water demand of Settlement's agricultural area (both irrigated and non irrigated fields)
	public long getWaterDemand() {
		return (long) ((currentAgriArea/1000)* AgriculturalLandParcel.cubicMeterPerDunam); //500 cubic meters of water per dunam
	}
	
	//water demand of Settlement's irrigated fields' agricultural area
	public long getIrrigatedAreaWaterDemand(){
		return (long) ((irrigatedLand/1000)* AgriculturalLandParcel.cubicMeterPerDunam); //500 cubic meters of water per dunam
	}
	
	//water demand for the whole study area (for all parcels - regardless of irrigation)
	public long getTotalWaterDemand() {
		return (long) ((totalAgLandInStudyArea/1000)* AgriculturalLandParcel.cubicMeterPerDunam);
	}
	
	//irrigation water demand for the whole study area
		public long getTotalIrrigatedWaterDemand() {
			return (long) ((totalIrrigatedAgLandInStudyArea/1000)* AgriculturalLandParcel.cubicMeterPerDunam);
		}

	public double getArableLand() {
		return arableLand;
	}

	void setArableLand(double arableLand) {
		this.arableLand = arableLand;
	}

	public double getRainAnnualAmount() {
		return rainAnnualAmount;
	}

	void setRainAnnualAmount(double rainAnnualAmount) {
		this.rainAnnualAmount = rainAnnualAmount;
	}

	public double getIrrigatedLand() {
		return irrigatedLand;
	}

	void setIrrigatedLand(double irrigatedLand) {
		this.irrigatedLand = irrigatedLand;
	}
	
	public double getCurrentWaterQuota() {
		return currentWaterQuota;
	}

	void setCurrentWaterQuota(double waterQuota) {
		currentWaterQuota = waterQuota;
	}

	public double getCurrentlyUnusedWater() {
		return currentlyUnusedWater;
	}

	void erasePotentialSettlement() {
		Context context = RunState.getSafeMasterContext();
		Geography geography = (Geography) context.getProjection("Geography");
		for(AgriculturalLandParcel parcel: settlementParcels){
			for(Pixel pix: parcel.parcelPixels){
				pix.classification = Pixel.Classification.SUITABLE;
			}
			geography.move(parcel, null);
			context.remove(parcel);
			parcel = null;
		}
		settlements.remove(this);
		Settlement.totalAgLandInStudyArea-=this.currentAgriArea;
		
		
	}

//	private long calculateAgriculturalCapacity_old(Context context){
//		 long settNumParcels = 0;
//		
//		 Geography geography = (Geography)context.getProjection("Geography");
//		
//		 //find all pixels within a settlementRadius distance from the settlement
//		 double distance = settlementRadius; // meters
//		
//		 GeographyWithin within = new GeographyWithin(geography, distance, this);
//		 int key = 0;
//		 for (Object obj : within.query()) {
//			 if (obj instanceof Pixel){
//				 Pixel pix = (Pixel)obj;
//				 if(pix.classification== Pixel.Classification.SUITABLE){
//					 List<Pixel> parcelPix = allocateParcel(pix, geography);
//					 if (parcelPix !=null){
//						 ++settNumParcels;
//						 Geometry parcelGeom = pix.getGeom().buffer(HectareRadius); //change to rectangle - look at immediate neighbor first
//						
//						 // //debug lines
//						 // System.out.println("The number of pixels in parcel is: " +
//						 //parcelPix.size());
//						 // System.out.println("the parcel is "+ parcelGeom.isValid() + 
//						 //" valid, with area: " + parcelGeom.getArea() +
//						 // " and is " + parcelGeom.isRectangle() + " a rectangle with number of "
//						 //+ parcelGeom.getNumPoints() +
//						 // " points and centroid at " +
//						 // parcelGeom.getCentroid().toString() );
//						 //
//						 Geometry recParcel = parcelGeom.getEnvelope();
//						 //
//						 // //debug lines
//						 // System.out.println("the parcel is "+ recParcel.isValid() +
//						 //" valid, with area: " + recParcel.getArea() +
//						 // " and is " + recParcel.isRectangle() + " a rectangle with number of "
//						 //+ recParcel.getNumPoints() +
//						 // " points and centroid at " +
//						 // recParcel.getCentroid().toString() );
//						
////						 AgriculturalLandParcel parcel = new AgriculturalLandParcel(this,
////						 parcelPix, parcelGeom, context);
//						 AgriculturalLandParcel parcel = new AgriculturalLandParcel(this,
//						 parcelPix, recParcel, context);
//						
//						 settlementParcels.add(parcel);
//						 settlementAvailableParcels.put(key++, parcel);
//					 }
//				 }
//			 }
//		 }
//		 numParcels = numAvailableParcels = settNumParcels;
//		
//		 //debug line
//		 System.out.println("number of parcels is: " + numAvailableParcels +
//		 "in settlement: " + this.ID );
//		
//		 return settNumParcels;
//		 }

}
