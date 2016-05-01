package negevVer1;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.measure.unit.SI;

import jogamp.opengl.util.av.impl.FFMPEGMediaPlayer.PixelFormat;
import negevVer1.Pixel.Classification;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.DefaultCoordinateOperationFactory;
import org.geotools.util.GeometryConverterFactory;
import org.jaitools.imageutils.shape.GeomCollectionIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.referencing.operation.TransformException;


import com.google.common.collect.HashMultimap;
import com.google.common.collect.TreeMultimap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

import repast.simphony.context.Context;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunState;
import repast.simphony.query.space.gis.GeographyWithin;
import repast.simphony.query.space.gis.IntersectsQuery;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.gis.UTMFinder;
import repast.simphony.util.ContextUtils;

/**
 * @author Assaf Chen
 * 
 */
public class ContextCreator implements ContextBuilder {

	private static DefaultCoordinateOperationFactory cFactory = new DefaultCoordinateOperationFactory();
	private static CoordinateReferenceSystem defaultCRS = DefaultGeographicCRS.WGS84;

	private static Settlement remoteSett;
	public final static boolean isClimateChange = false;

	// static SortedSet<Pixel> pixelSet = new TreeSet<Pixel>();

	public Context build(Context context) {

		// The mapFile contains a 2D map of the Kita study area. The associated
		// colors are: White- suitable for agriculture;
		// Black - not for agriculture; Gray - road network; Red - settlements.
		// String mapFile = "misc/KITA_MAPOFAGRILAND2_PARTIAL.bmp";
		// String mapFile = "misc/KITA_MAPOFAGRILAND.img";
		// String mapFile = "misc/KITA_MAPOFAGRILAND1.tif";

		Settlement.consoleHandler.setLevel(Level.FINEST);
		Settlement.logger.addHandler(Settlement.consoleHandler);
		Settlement.logger.setUseParentHandlers(false); // in order to have the
														// logging into the
														// console only once

		Settlement.logger.setLevel(Level.INFO);

		GeographyParameters geoParams = new GeographyParameters();
		Geography geography = GeographyFactoryFinder.createGeographyFactory(
				null).createGeography("Geography", context, geoParams);

		// debug
		CoordinateReferenceSystem crs = geography.getCRS();
		Settlement.logger.log(Level.INFO, "Geography coordinate system is: "
				+ crs.toString());

		// geography.setCRS("EPSG:32629");
		// geography.setCRS("EPSG:32636"); //WGS 84 / UTM zone 36N
		geography.setCRS("EPSG:2039"); // Israel / Israeli TM Grid

		crs = geography.getCRS();
		Settlement.logger.log(Level.INFO, "Geography coordinate system is: "
				+ crs.toString());

		// Instantiating the IrrigationManager singleton and adding it to the
		// context - in order for its step function to be called
		context.add(IrrigationManager.getInstance());

		// in case the model is restarted - we need to re-initialize all global
		// variables and boot up existing settlements stored in memory
		if (Settlement.initialization == false) {
			initializeGlobalVars(context);

			for (Settlement settlement : Settlement.settlements) {
				settlement.assignFarmersForExistingSettlement(context);
			}

			return context;
		}
		// loading rain amounts per year per location within the study area
		loadRainThickness("gisdata/RainAmountPolygon.shp", context, geography);

		// loading existing settlements. Using rain data to set for each
		// settlement rain-fed potential. Also loading boundaries and
		// affiliating them with settlements
		loadSettlements("gisdata/Settlements.shp", context, geography);
//		loadSettlements("gisdata/Settlements_2002.shp", context, geography); //for validation run from year 2002

		// layers that indicate areas that can't be cultivated (UnsuitableArea)
		loadRestrictedAreas("gisdata/Unsuitable_Land_RA.shp", context, geography);

		// loading soil type according to available soil types within the study
		// area
		loadSoilType("gisdata/Lithology_RA.shp", context, geography);

		// loading agricultural parcels of RS sources only. Loading only in case
		// area is not restricted for agricultural use (using the Unsuitable
		// layer)
		// updating home settlement, rainThickness and soil type to ag. parcels
		loadAgParcels("gisdata/agParcelsRA_ISR.shp", context, geography);
//		loadAgParcels("gisdata/agParcelsRA_ISR_2002.shp", context, geography); //for validation run from year 2002
	
		// loading main study areas's roads to the system
		loadRoads("gisdata/Road_system.shp", context, geography);
//		loadRoads("gisdata/Road_system_2002.shp", context, geography); //for validation run from year 2002

		// loading all irrigation piping, distinguishing between reclaimed and
		// fresh water sources. For every pipe line finding all agricultural
		// parcels within
		// Settlement.maxDistanceFromIrrigation and adding irrigation
		// information to these parcels
		loadIrrigationPiping("gisdata/pipeline_all_ISR.shp", context, geography);
//		loadIrrigationPiping("gisdata/pipeline_all_ISR_2002.shp", context, geography); //for validation run from year 2002

		loadUnclaimedAgLAnd("gisdata/unclaimedAgland_30m_ISR.shp", context, geography);
//		loadUnclaimedAgLAnd("gisdata/unclaimed_ag_2002_final.shp", context, geography); //for validation run from year 2002
//		loadUnclaimedAgLAnd("gisdata/unclaimed_ag_2002.shp", context, geography); //for validation run from year 2002
		
		for (Settlement sett : Settlement.settlements) {
			for (AgriculturalLandParcel parcel : sett.settlementParcels) {
				if (parcel.isPotentiallyIrrigated) {
					sett.irrigatedLand += parcel.area;
					Settlement.totalIrrigatedAgLandInStudyArea += parcel.area;
				}
			}
			sett.assignFarmerToExistingParcels(context); // TODO - make sure
															// this function
															// works well in
															// this location
		}

		// loadTopography("gisdata/dem_slope_deg_point_RA_ISR.shp", context,
		// geography); //according to DEM map of the area -
		// there is no significant amount of pixels with slope larger than 10
		// degrees within the study area

		// initializeModel(context, geography);
		//

		// RunState.getSafeMasterContext()

		return context;
	}

	private void loadUnclaimedAgLAnd(String filename, Context context, Geography geography) {
		SimpleFeatureIterator fiter = getLayerFeatures(filename);
		while (fiter.hasNext()) {
			SimpleFeature feature = fiter.next();
			Geometry geom = (Geometry) feature.getDefaultGeometry();
			Geometry geomConverted = geom;
			if (geography.getUnits(0).equals(SI.METER)) {
				geomConverted = convertGeometryToWGS(geom, geography);
			}

			if (geomConverted instanceof Point) {
				int type = (int) feature.getAttribute("SoilType");  //1: Loess - second in rank for agriculture
				//4: Sand - best for agriculture in this study area
				//5: Red silty sandstone (Hamra) - 3rd in rank for agriculture
				//6, 11, 12, 15, 24, 29: - lowest priority for agriculture
				
				double distToIrrigation = (double) feature.getAttribute("DistToIrr"); // distance to irrigation in meters - up to 10 km. If more than = -1
				if (distToIrrigation == -1)
					distToIrrigation = (Integer.MAX_VALUE)/2 + RandomHelper.nextDoubleFromTo(1, 7); //to make the number unique TODO - check that works
				int irrId = (int) feature.getAttribute("IrrID"); //pipe ID - that is closest to pixel
				double distToRoad = (double) feature.getAttribute("DistToRd"); // distance to road system in meters - up to 10 km. If more than = -1
				if (distToRoad == -1)
					distToRoad = (Integer.MAX_VALUE)/2 + RandomHelper.nextDoubleFromTo(1, 7); 
				int rdId = (int) feature.getAttribute("RdID"); //road ID - that is closest to pixel
				int rainAmount = (int) feature.getAttribute("Rainfall"); //rain annual amount in mm/y
				
				
				SoilType.Type soilType = configureSoilType(type);
				
				PotentialAgPixel agPixel = new PotentialAgPixel(Classification.SUITABLE, geomConverted, geom.getCoordinate(), distToIrrigation, 
						distToRoad, rainAmount, soilType, geomConverted.getCoordinate(), rdId, irrId, geom);
				if(agPixel!= null && soilType != SoilType.Type.INAPPROPRIATE_SOIL){
					context.add(agPixel);
					geography.move(agPixel, geomConverted);
//					PotentialAgPixel.potAgPixTmm.put(distToIrrigation, agPixel);
					Pixel.pixelMap.put(agPixel.getCoord_m(), agPixel);
					if(validatePixelDistanceFromOtherSettlements(agPixel))
						PotentialAgPixel.pixelSet.add(agPixel);
				}
			}
		}	
	}

	// this method makes sure the potential ag pixel is not too close to other
	// existing settlements
	private boolean validatePixelDistanceFromOtherSettlements(
			PotentialAgPixel agPixel) {
		if (agPixel == null)
			return false;
		for (Settlement mySett : Settlement.settlements) {
			double squareDistance = Math.pow(
					agPixel.getCoord_m().distance(
							mySett.getPixel().getCoord_m()), 2);
			if (squareDistance < Math.pow((Settlement.settlementRadius * 2), 2)) {
				return false;
			}
		}
		return true;
	}

	private void loadRainThickness(String filename, Context context,
			Geography geography) {
		SimpleFeatureIterator fiter = getLayerFeatures(filename);
		while (fiter.hasNext()) {
			SimpleFeature feature = fiter.next();
			Geometry geom = (Geometry) feature.getDefaultGeometry();
			Geometry geomConverted = geom;
			if (geography.getUnits(0).equals(SI.METER)) {
				geomConverted = convertGeometryToWGS(geom, geography);
			}
			if (geomConverted instanceof MultiPolygon) {
				double area = (double) feature.getAttribute("Area"); // in
																		// squared
																		// meters
				int rainAmount = (int) feature.getAttribute("Rainfall"); // in
																			// mm/y
				RainfallThickness myRain = new RainfallThickness(rainAmount,
						area, geomConverted);
				context.add(myRain);
				geography.move(myRain, geomConverted);
			}
		}
	}

	private void loadTopography(String filename, Context context,
			Geography geography) {
		SimpleFeatureIterator fiter = getLayerFeatures(filename);
		while (fiter.hasNext()) {
			SimpleFeature feature = fiter.next();
			Geometry geom = (Geometry) feature.getDefaultGeometry();
			Geometry geomConverted = geom;
			if (geography.getUnits(0).equals(SI.METER)) {
				geomConverted = convertGeometryToWGS(geom, geography);
			}
			if (geomConverted instanceof Point) {
				double slope = (double) feature.getAttribute("GRID_CODE"); // slope
																			// in
																			// degrees
				if (slope > Pixel.unsuitableSlope) {
					Pixel unsuitSlope = new Pixel(
							Pixel.Classification.UNSUITABLE, geomConverted,
							geomConverted.getCoordinate());
					context.add(unsuitSlope);
					geography.move(unsuitSlope, geomConverted);
				}
			}
		}
	}

	private void loadSoilType(String filename, Context context,
			Geography geography) {
		SimpleFeatureIterator fiter = getLayerFeatures(filename);
		while (fiter.hasNext()) {
			SimpleFeature feature = fiter.next();
			Geometry geom = (Geometry) feature.getDefaultGeometry();
			Geometry geomConverted = geom;
			if (geography.getUnits(0).equals(SI.METER)) {
				geomConverted = convertGeometryToWGS(geom, geography);
			}

			if (geomConverted instanceof MultiPolygon) {
				double area = (double) feature.getAttribute("AREA"); // in
																		// meter^2
				int type = (int) feature.getAttribute("TypeCode"); // 1: Loess
				// 4: Sand
				// 5: Red silty sandstone (Hamra)
				// 6, 11, 12, 15, 24, 29: inappropriate soil type for ag. - will
				// be marked as "unsuitable area"
				SoilType.Type soilType = configureSoilType(type);
				if (soilType == SoilType.Type.INAPPROPRIATE_SOIL) {
					UnsuitableArea unSuitArea = new UnsuitableArea(
							geomConverted,
							UnsuitableArea.Type.INAPPROPRIATE_SOIL, area);
					if (unSuitArea != null) {
						context.add(unSuitArea);
						geography.move(unSuitArea, geomConverted);
					}
				}
				SoilType soil = new SoilType(geomConverted, soilType, area);
				if (soil != null) {
					context.add(soil);
					geography.move(soil, geomConverted);
				}
			}
		}
	}

	private void loadIrrigationPiping(String filename, Context context,
			Geography geography) {
		SimpleFeatureIterator fiter = getLayerFeatures(filename);
		while (fiter.hasNext()) {
			SimpleFeature feature = fiter.next();
			Geometry geom = (Geometry) feature.getDefaultGeometry();
			Geometry geomConverted = geom;
			if (geography.getUnits(0).equals(SI.METER)) {
				geomConverted = convertGeometryToWGS(geom, geography);
			}

			if (geomConverted instanceof MultiLineString) {
				// int length = (int) feature.getAttribute("LENGTH"); // in
				// meters
				int reclaimedWater = (int) feature.getAttribute("Reclaimed"); // Reclaimed=1
																				// reclaimed
																				// water;
																				// Reclaimed=0
																				// fresh
																				// water
				boolean isReclaimed = (reclaimedWater == 1) ? true : false;
				int id = (int) feature.getAttribute("ID"); // unique ID for
															// pipeline segment
				// Coordinate[] myCoords = geomConverted.getCoordinates();
				IrrigationPipeLink myPipe = new IrrigationPipeLink(isReclaimed,
						id, IrrigationManager.getSimulationStepCount(),
						geomConverted, geom.getLength());
				context.add(myPipe);
				geography.move(myPipe, geomConverted);

				Geometry pipeBufferGeom = geom
						.buffer(Settlement.maxDistanceFromIrrigation);
				Geometry pipeBufferGeomConverted = convertGeometryToWGS(
						pipeBufferGeom, geography);
				IntersectsQuery query = new IntersectsQuery(geography,
						pipeBufferGeomConverted);
				for (Object obj : query.query()) {
					if (obj instanceof AgriculturalLandParcel) {
						AgriculturalLandParcel parcel = (AgriculturalLandParcel) obj;
						// double distanceInMeters =
						// myPipe.getGeom().distance(parcel.getParcelGeom()) *
						// 111325; //the factor is converting distance from
						// degrees to meters
						// if(distanceInMeters >
						// Settlement.maxDistanceFromIrrigation){
						// Settlement.logger.log(Level.FINE,
						// "Error in loading IrrigationPiping - distance greater than maxDistanceFromIrrigation. Distance is: "
						// + distanceInMeters);
						// parcel.setIrrigated(false); //actually no need -
						// because by default this indicator is set to false.
						// }else{
						parcel.setReclaimedWaterIrrigation(myPipe
								.isReclaimedWater());// set water source to
														// parcel too - if
														// irrigated with
														// reclaimed or fresh
														// water
						// if(parcel.isPotentiallyIrrigated != true){
						parcel.setIrrigated(true);
						// // parcel.settlement.irrigatedLand+=parcel.area;
						// //this operation doesn't work as expected here - we
						// perform it at end of initialization instead
						// }
						// }

					}
				}

				// GeographyWithin within = new GeographyWithin(geography,
				// Settlement.maxDistanceFromIrrigation, myPipe.getGeom());
				// for (Object obj : within.query()) {
				// if (obj instanceof AgriculturalLandParcel){
				// AgriculturalLandParcel parcel = (AgriculturalLandParcel) obj;
				// double distance =
				// myPipe.getGeom().distance(parcel.getParcelGeom());
				// parcel.setIrrigated(true);
				// }
				// }
			}
		}
	}

	private void loadRoads(String filename, Context context, Geography geography) {
		SimpleFeatureIterator fiter = getLayerFeatures(filename);
		// HashMultimap<Integer, Road> hm = HashMultimap.create(30,3);
		// TreeMultimap<Integer, Road> roadTree = TreeMultimap.create(hm);
		while (fiter.hasNext()) {
			SimpleFeature feature = fiter.next();
			Geometry geom = (Geometry) feature.getDefaultGeometry();
			Geometry geomConverted = geom;
			if (geography.getUnits(0).equals(SI.METER)) {
				geomConverted = convertGeometryToWGS(geom, geography);
			}

			if (geomConverted instanceof MultiLineString) {
				int id = (int) feature.getAttribute("ID"); // A unique object id
															// for every road
															// segment - last
															// one is 104
				double length = (double) feature.getAttribute("LENGTH"); // in
																			// meters
				int roadNum = (int) feature.getAttribute("ISROADNR"); // Road
																		// number
				Road myRoad = new Road(geomConverted, length, roadNum, id);
				context.add(myRoad);
				geography.move(myRoad, myRoad.getRoadLine());
				// roadTree.put(roadNum, myRoad);
			}
		}
		// TODO - union of all roads with same number
		// Iterator<Road> iter = roadTree.values().iterator();
		// Road myRoad = null;
		// int roadNum = 0;
		// double roadLength = 0;
		// MultiLineString myLine = null;
		// if(iter.next()!= null){
		// myRoad = iter.next();
		// roadNum = myRoad.getRoadNum();
		// roadLength = myRoad.getLength();
		// myLine = myRoad.getRoadLine();
		// }
		// while(iter.hasNext()){
		// Road myRoad2 = iter.next();
		// if(myRoad2 != null){
		// if(roadNum == myRoad2.getRoadNum()){
		// roadLength += myRoad2.getLength();
		// myLine.union(myRoad2.getRoadLine());
		// myRoad2 = null; //we are joining this road with the other ones with
		// the same road number, so no need to keep it anymore
		// continue;
		// }
		// else{//this current road does not have the same road number
		// myRoad.setRoadLine(myLine);
		// myRoad.setLength(roadLength);
		// Road myFinalRoad = new Road(myRoad);
		// Road.roads.add(myFinalRoad);
		// context.add(myFinalRoad);
		// geography.move(myFinalRoad, myFinalRoad.getRoadLine());
		//
		// myRoad = myRoad2;
		// roadNum = myRoad.getRoadNum();
		// roadLength = myRoad.getLength();
		// myLine = myRoad.getRoadLine();
		// }
		//
		// }
		// }
		// //need to insert last road that is not taken care of by the while
		// loop
		// myRoad.setRoadLine(myLine);
		// myRoad.setLength(roadLength);
		// Road.roads.add(myRoad);
		// context.add(myRoad);
		// geography.move(myRoad, myRoad.getRoadLine());
		//
		// roadTree.clear();
	}

	// This method will initialize existing settlements, assign agricultural
	// parcels for each settlement
	// and create farmers for every settlement - with 2 assigned agricultural
	// fields - one active , the other in fallow.
	private void initializeModel(Context context, Geography geography) {

		// initialize snapshots of model structure of pixels, settlements and
		// roads at init time - for fast re-initialization
		// for (Pixel myPixel : Pixel.pixels) {
		// Pixel newPixel = new Pixel(myPixel);
		// Pixel.InitializationPixels.add(newPixel);
		// }

		// order irrigation pipes according to their graphical tree
		// configuration
		IrrigationManager.createIrrigationPipesTree(geography);

		for (Pixel myPixel : Pixel.pixelMap.values()) {
			Pixel newPixel = new Pixel(myPixel);
			Pixel.InitializationPixels.add(newPixel);
		}

		for (Settlement settlement : Settlement.settlements) {
			// check if settlement has access to irrigation
			Geometry settBuffer = this.generateBuffer(geography,
					settlement.pixel.getGeom(),
					Settlement.maxDistanceFromIrrigation);
			// Geometry settBuffer =
			// settlement.pixel.getGeom().buffer(Settlement.maxDistanceFromIrrigation);

			for (IrrigationPipeLink link : IrrigationManager.IrrigationPipes) {
				if (link.isCurrentlyOnline()) {
					if (settBuffer.intersects(link.getGeom())) {
						settlement.setFutureSettlement(false);
						settlement.initialize(context);
						// make a relationship between settlement and irrigation
						// pipeline
						IrrigationManager.connectSettlementToPipeline(
								settlement, link);
						break;
					}
				}
			}

			// IntersectsQuery query = new IntersectsQuery(geography,
			// settBuffer);
			// for (Object obj : query.query()) {
			// if (obj instanceof IrrigationPipeLink){
			// IrrigationPipeLink link = (IrrigationPipeLink)obj;
			// if(link.isCurrentlyOnline()){
			// settlement.setFutureSettlement(false);
			// settlement.initialize(context);
			// break;
			// }
			//
			// }
			// }
			// settlement.initialize(context);
		}

		for (Road myRoad : Road.roads) {
			Road newRoad = new Road(myRoad);
			Road.initializedRoads.add(newRoad);
		}
		if (!RoadCoord.useRoadSet) {
			RoadCoord.calculateRoadSquaredDistanceToSettlement(context);
		}
		// prepareFutureSettlements(context);

		for (IrrigationPipeLink link : IrrigationManager.IrrigationPipes) {
			if (link.isCurrentlyOnline()) {
				link.createNewSettlements(context);
			}
		}

		Settlement.initialization = false; // Finished with initialization
	}

	private void prepareFutureSettlements(Context context) {
		Geography geography = (Geography) context.getProjection("Geography");
		if (!RoadCoord.useRoadSet) {
			RoadCoord.calculateRoadSquaredDistanceToSettlement(context);
		}

		// //deep copy roadSet to initializedRoadSet for faster next model
		// re-run
		// for (Road road : Road.roadSet) {
		// Road newRoad = new Road(road);
		// Road.initializedRoadSet.add(newRoad);
		// }

		while (!RoadCoord.roadSet.isEmpty()) {
			RoadCoord road2Settlement = RoadCoord.roadSet.first(); // choose the
																	// closest
																	// settlement
																	// candidate
																	// to
																	// existing
																	// settlements'
																	// group

			// GeometryFactory geomFactory = new GeometryFactory();
			// Point roadCoordGeom =
			// geomFactory.createPoint(road2Settlement.getCoord());
			Pixel settPixel = new Pixel(Classification.SETTLEMENT,
					road2Settlement.getGeom(), road2Settlement.getCoord());
			Settlement newSettlement = new Settlement(settPixel);
			newSettlement.setFutureSettlement(true);
			context.add(newSettlement);
			geography.move(newSettlement, newSettlement.getPixel().getGeom());
			newSettlement.calculateAgriculturalCapacity(context);

			RoadCoord.updateRoadSet(road2Settlement, context);
			RoadCoord.roadSet.remove(road2Settlement);

			Settlement.potentialSettlements.add(newSettlement);
			Settlement initPotentialSettlement = new Settlement(newSettlement); // Deep
																				// copy
																				// -
																				// in
																				// order
																				// to
																				// be
																				// used
																				// for
																				// model
																				// re-run
			Settlement.InitializationpotentialSettlements
					.add(initPotentialSettlement);
		}
	}

	private void loadFeatures(String filename, Context context,
			Geography geography) {
		URL url = null;
		try {
			url = new File(filename).toURL();
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		SimpleFeatureIterator fiter = null;
		ShapefileDataStore store = null;
		try {
			store = new ShapefileDataStore(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		try {
			fiter = store.getFeatureSource().getFeatures().features();
		} catch (IOException e) {
			e.printStackTrace();
		}

		while (fiter.hasNext()) {
			SimpleFeature feature = fiter.next();
			Geometry geom = (Geometry) feature.getDefaultGeometry();
			Object agent = null;

			// debug line
			// System.out.println(geom.toString());

			if (geom instanceof MultiPolygon) {
				MultiPolygon mp = (MultiPolygon) feature.getDefaultGeometry();
				geom = (Polygon) mp.getGeometryN(0);

				double ID = (double) feature.getAttribute("ID");
				double area = (double) feature.getAttribute("Area"); // in
																		// meter^2
				double circumference = (double) feature
						.getAttribute("Perimeter"); // in meters
				double type = (double) feature.getAttribute("GRIDCODE"); // 1=unsuitable;
																			// 2=suitable

				agent = new Zone(ID, area, circumference, type);

			} else if (geom instanceof Point) {
				geom = (Point) feature.getDefaultGeometry();
				Coordinate coordM;
				// if(geography.getUnits(0).equals(SI.METER)){
				// coordM = geom.getCoordinate();
				// }else{
				// long myStart = System.nanoTime();
				// Geometry geomM = convertGeometry(geom, geography);
				// long myTime = System.nanoTime() - myStart;
				// Settlement.logger.log(Level.FINE,
				// "-------------------------------------------");
				// Settlement.logger.log(Level.FINE,
				// "time to convert geometry using method1 is " + myTime +
				// " nanoseconds");
				// Settlement.logger.log(Level.FINE,
				// "-------------------------------------------");
				//
				// coordM = geomM.getCoordinate();
				// }

				long type = (long) feature.getAttribute("GRID_CODE"); // 1=unsuitable;
																		// 2=suitable;
																		// 3=settlement;
																		// 4=road

				double xcoordM = (double) feature.getAttribute("xcoord_m");
				double ycoordM = (double) feature.getAttribute("ycoord_m");

				coordM = new Coordinate(xcoordM, ycoordM); // TODO - omit once
															// algorithm for
															// convertGeometry()
															// is faster
				//
				// double xcoordD = (double) feature.getAttribute("xcoord");
				// double ycoordD = (double) feature.getAttribute("ycoord");

				agent = configurePixel(type, geom, context, coordM);
			} else if (geom instanceof MultiLineString) {
				MultiLineString line = (MultiLineString) feature
						.getDefaultGeometry();
				geom = (LineString) line.getGeometryN(0);
				Coordinate[] pipelineCoordM;
				if (geography.getUnits(0).equals(SI.METER)) {
					pipelineCoordM = geom.getCoordinates();
				} else {
					Geometry geomM = convertGeometry(geom, geography);
					pipelineCoordM = geomM.getCoordinates();
				}

				int ID = (int) feature.getAttribute("ID");
				int reclaimed = (int) feature.getAttribute("Reclaimed");
				boolean isReclaimed = (reclaimed == 1) ? true : false;
				agent = new IrrigationPipeLink(isReclaimed, ID,
						IrrigationManager.getSimulationStepCount(), geom,
						geom.getLength());
				if (agent != null) {
					IrrigationManager.IrrigationPipes
							.add((IrrigationPipeLink) agent);
				}

				// agent = new Road(ID);
			}

			if (agent != null) {
				// add pixel to Pixel list
				if (agent instanceof Pixel) {
					Pixel pix = (Pixel) agent;
					// Pixel.pixels.add((Pixel)agent);
					Pixel.pixelMap.put(pix.getCoord_m(), (Pixel) agent);
				}
				// DEBUG - at this stage all entities are Pixels
				// if (!(agent instanceof Pixel)){
				// Settlement.logger.log(Level.INFO,
				// " instance is not a pixel but a " +
				// agent.getClass().toString());
				// }

				context.add(agent);
				geography.move(agent, geom);
			} else {
				System.out.println("NULL agent for  " + geom);
			}

		}
	}

	private SimpleFeatureIterator getLayerFeatures(String filename) {
		URL url = null;
		try {
			url = new File(filename).toURL();
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		SimpleFeatureIterator fiter = null;
		ShapefileDataStore store = null;
		try {
			store = new ShapefileDataStore(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		try {
			fiter = store.getFeatureSource().getFeatures().features();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fiter;
	}

	// Loads a polygon shapefile layer of agricultural restricted areas
	private void loadRestrictedAreas(String filename, Context context,
			Geography geography) {
		SimpleFeatureIterator fiter = getLayerFeatures(filename);
		while (fiter.hasNext()) {
			SimpleFeature feature = fiter.next();
			Geometry geom = (Geometry) feature.getDefaultGeometry();
			Geometry geomConverted = geom;
			if (geography.getUnits(0).equals(SI.METER)) {
				geomConverted = convertGeometryToWGS(geom, geography);
			}

			if (geomConverted instanceof MultiPolygon) {
				// MultiPolygon mp = (MultiPolygon)feature.getDefaultGeometry();
				// geomConverted = (Polygon)mp.getGeometryN(0);

				double area = (double) feature.getAttribute("AREA"); // in
																		// meter^2
				double perimeter = (double) feature.getAttribute("PERIMETER"); // in
																				// meters
				int type = (int) feature.getAttribute("Type"); // 1: Natural
																// Reserve
																// 2: Urban
																// 3: Forest
																// 4: Riparian
																// zone
																// 5: Other
																// 6:
																// inappropriate
																// soil type for
																// ag. - is
																// loaded only
																// from the soil
																// type layer
				UnsuitableArea.Type unsuitType = configureRestrictedType(type);
				UnsuitableArea unSuitArea = new UnsuitableArea(geomConverted,
						unsuitType, area);
				if (unSuitArea != null) {
					context.add(unSuitArea);
					geography.move(unSuitArea, geomConverted);
				}
			}
		}
	}

	// Loads a polygon shapefile layer of agricultural parcels
	private void loadAgParcels(String filename, Context context,
			Geography geography) {
		SimpleFeatureIterator fiter = getLayerFeatures(filename);
		while (fiter.hasNext()) {
			SimpleFeature feature = fiter.next();
			Geometry geom = (Geometry) feature.getDefaultGeometry();
			Geometry geomConverted = geom;
			if (geography.getUnits(0).equals(SI.METER)) {
				geomConverted = convertGeometryToWGS(geom, geography);
				// geomConverted = convertGeometryFromUTMtoWGS(geom,
				// geography);//test
			}

			if (geom instanceof MultiPolygon) {
				// double perimeter = (double)
				// feature.getAttribute("Perimeter"); // in meters
				IntersectsQuery query = new IntersectsQuery(geography,
						geomConverted);
				AgriculturalLandParcel parcel = null;
				boolean unsuitable = false;
				int rainAmount = 0;
				SoilType.Type mySoilType = SoilType.Type.INAPPROPRIATE_SOIL;
				boolean enteredForLoop = false;
				for (Object obj : query.query()) {
					enteredForLoop = true;
					if ((obj instanceof Boundary)) {
						if (parcel != null) {// in case it's a big parcel that
												// "sits" on more than 1
												// boundary - we want to include
												// it only once.
							// parcel can belong to more than 1 boundary since
							// the input for the ag. parcels is from RS sources.
							continue;
						}
						Boundary bound = (Boundary) obj;
						Settlement settlement = bound.getMySettlement();
						double area = (double) feature.getAttribute("Area"); // in
																				// meter^2
						// double area = (double) feature.getAttribute("AREA");
						// // in meter^2
						parcel = new AgriculturalLandParcel(settlement, null,
								geomConverted, area, context);
						// parcel.setRainThickness(settlement.getRainAnnualAmount());//get
						// rain amount from home Settlement
						settlement.settlementParcels.add(parcel);
						continue;
						// break;
					}
					if ((obj instanceof UnsuitableArea)) {
						unsuitable = true;
						continue;
					}
					if ((obj instanceof RainfallThickness)) {
						RainfallThickness rain = (RainfallThickness) obj;
						rainAmount = rain.getRainYearlyAmount();
						continue;
					}
					if ((obj instanceof SoilType)) {
						SoilType soil = (SoilType) obj;
						mySoilType = soil.getType();
						continue;
					}
				}
				if (parcel != null) {// get the rain amount and soil type for
										// the parcel (this is the reason why
										// there is no break after initializing
										// the agParcel within the for loop)
					parcel.setRainThickness(rainAmount);
					parcel.setSoilType(mySoilType);
					// debug
					// Settlement.logger.log(Level.INFO,
					// "parcel's soil type toString() is: " +
					// parcel.GetSoilTypeName());
					// Settlement.logger.log(Level.INFO,
					// "parcel's soil type Name() is: " +
					// parcel.getSoilType().name());
				}
				// if this parcel does not belong to any nearby (not within a
				// boundary) settlement and is not within an unsuitable area, we
				// will add this plot to
				// a pool of parcels that belong to distant settlements as is
				// the situation in this study area - in order to account for
				// these parcels' water demand
				if (parcel == null && unsuitable == false
						&& enteredForLoop == true) {
					double area = (double) feature.getAttribute("Area"); // in
																			// meter^2
					// double area = (double) feature.getAttribute("AREA"); //
					// in meter^2
					getRemoteSett(geomConverted.getCentroid(), context);
					parcel = new AgriculturalLandParcel(remoteSett, null,
							geomConverted, area, context);
					parcel.setRainThickness(rainAmount);
					parcel.setSoilType(mySoilType);
					remoteSett.settlementParcels.add(parcel);
				}
			}
		}
	}

	// loads a point vector layer of settlements
	private void loadSettlements(String filename, Context context,
			Geography geography) {
		SimpleFeatureIterator fiter = getLayerFeatures(filename);
		// List<Coordinate> settCoords = new ArrayList<>();
		while (fiter.hasNext()) {
			SimpleFeature feature = fiter.next();
			Geometry geom = (Geometry) feature.getDefaultGeometry();
			Geometry geomConverted = geom;
			if (geography.getUnits(0).equals(SI.METER)) {
				geomConverted = convertGeometryToWGS(geom, geography);
			}
			// Object agent = null;

			if (geomConverted instanceof Point) {
				Coordinate coordM; // coordinate in meters
				Coordinate coordD; // coordinate in degrees
				if (geography.getUnits(0).equals(SI.METER)) {
					coordM = geom.getCoordinate();
				} else {
					Geometry geomM = convertGeometry(geom, geography);
					// Geometry geomM = convertGeometryWGS(geom, geography);
					coordM = geomM.getCoordinate();
				}
				coordD = geomConverted.getCoordinate();
				String name = (String) feature.getAttribute("NAME");
				int ID = (int) feature.getAttribute("ID");

				// get rain thickness for the settlement
				int rainAmount = 0;
				IntersectsQuery query = new IntersectsQuery(geography,
						geomConverted);
				for (Object obj : query.query()) {
					if ((obj instanceof RainfallThickness)) {
						RainfallThickness rain = (RainfallThickness) obj;
						rainAmount = rain.getRainYearlyAmount();
						break;
					}
				}
				// double xcoordM = (double) feature.getAttribute("XCOORD");
				// double ycoordM = (double) feature.getAttribute("YCOORD");
				// coordM = new Coordinate(xcoordM, ycoordM); //TODO - omit once
				// algorithm for convertGeometry() is faster

				Pixel myPixel = new Pixel(Classification.SETTLEMENT,
						geomConverted, coordM);
				Settlement mySettlement = new Settlement(myPixel, ID);
				mySettlement.setName(name);
				mySettlement.setRainAnnualAmount(rainAmount);
				// Add Settlement into context
				context.add(mySettlement);
				geography.move(mySettlement, geomConverted);
				Settlement.settlements.add(mySettlement);
			}
		}
		// for each settlement find its boundary
		SimpleFeatureIterator boundFiter = getLayerFeatures("gisdata/MoAg_bound_singlepoly.shp");
		while (boundFiter.hasNext()) {
			SimpleFeature feature = boundFiter.next();
			Geometry geom = (Geometry) feature.getDefaultGeometry();
			Geometry geomConverted = geom;
			if (geography.getUnits(0).equals(SI.METER)) {
				geomConverted = convertGeometryToWGS(geom, geography);
			}

			// test
			// Geometry geomM = convertGeometryWGS(geom, geography);
			// IntersectsQuery query = new IntersectsQuery(geography, geomM);
			// for (Object obj : query.query()) {
			// if ((obj instanceof Settlement)) {
			// System.out.println("Boundary includes settlement " );
			//
			// }
			// }
			//

			if (geomConverted instanceof MultiPolygon) {
				// get ID
				int ID = (int) feature.getAttribute("ID");
				double area = (double) feature.getAttribute("Area");
				// TODO - make settlements' list - asorted list by ID
				for (Settlement mySett : Settlement.settlements) {
					if (mySett.getID() == ID) {
						Boundary settBound = new Boundary(geomConverted, ID,
								area, mySett);
						mySett.settBoundaries.add(settBound);
						mySett.setArableLand(mySett.getArableLand() + area);
						context.add(settBound);
						geography.move(settBound, geomConverted);
						break;
					}
				}
			}
		}

		// VoronoiDiagramBuilder voronoi = new VoronoiDiagramBuilder();
		// SimpleFeatureIterator studyAreaFiter =
		// getLayerFeatures("gisdata/Research_Area_model.shp");
		// SimpleFeature feature = fiter.next();
		// Geometry geom = (Geometry)feature.getDefaultGeometry();
		// Geometry geomConverted = geom;
		// if(geography.getUnits(0).equals(SI.METER)){
		// geomConverted = convertGeometryToWGS(geom, geography);
		// }
		// Envelope env = geomConverted.getEnvelopeInternal();
		// voronoi.setClipEnvelope(env);
		// voronoi.setSites(settCoords);
		// GeometryFactory fact = new GeometryFactory();
		// Geometry voronoiDiagram = voronoi.getDiagram(fact);

	}

	private UnsuitableArea.Type configureRestrictedType(int type) {

		switch (type) {
		case 1:
			return UnsuitableArea.Type.NATURAL_RES;
		case 2:
			return UnsuitableArea.Type.URBAN;
		case 3:
			return UnsuitableArea.Type.FOREST;
		case 4:
			return UnsuitableArea.Type.RIPARIAN_ZONE;
		case 5:
			return UnsuitableArea.Type.OTHER;
		case 6:
			return UnsuitableArea.Type.INAPPROPRIATE_SOIL;
		default:
			return UnsuitableArea.Type.FOREST;
		}
	}

	private SoilType.Type configureSoilType(int type) {

		switch (type) {
		case 1:
			return SoilType.Type.LOESS;
		case 4:
			return SoilType.Type.SAND;
		case 5:
			return SoilType.Type.RED_SILTY_SANDSTONE;
		default:
			return SoilType.Type.INAPPROPRIATE_SOIL;
		}
	}

	private Pixel configurePixel(long myType, Geometry geom, Context context,
			Coordinate coord) {
		Pixel myPixel = null;
		int type = (int) myType;
		Geography geography = (Geography) context.getProjection("Geography");
		switch (type) {
		case 1:
			myPixel = new Pixel(Classification.UNSUITABLE, geom, coord);
			break;
		case 2:
			myPixel = new Pixel(Classification.SUITABLE, geom, coord);
			break;
		case 3:
			myPixel = new Pixel(Classification.SETTLEMENT, geom, coord);

			Settlement mySettlement = new Settlement(myPixel);
			// Add Settlement into context
			context.add(mySettlement);
			geography.move(mySettlement, geom);
			Settlement.settlements.add(mySettlement);
			break;
		case 4:
			myPixel = new Pixel(Classification.ROAD, geom, coord);
			// Road myRoad = new Road(myPixel);
			// Road.roads.add(myRoad);
			// Add Road into context - since we don't show the pixels layer in
			// the display we need to add the road into the context and display
			// it
			// context.add(myRoad);
			// geography.move(myRoad, geom);
			break;
		default:
			System.out.println("Unsuitable Pixel type");
			myPixel = new Pixel(Classification.UNSUITABLE, geom, coord);
		}

		return myPixel;

	}

	public static Geometry convertGeometry(Geometry geom, Geography geography) {
		boolean convert = !geography.getUnits(0).equals(SI.METER);
		CoordinateReferenceSystem utm = null;
		CoordinateReferenceSystem crs = geography.getCRS();
		Geometry g2 = geom;

		try {
			// convert p to UTM
			if (convert) {
				utm = UTMFinder.getUTMFor(geom, crs);
				MathTransform transform = CRS.findMathTransform(crs, utm);
				g2 = JTS.transform(geom, transform);
				// g2 = JTS.transform(geom, cFactory.createOperation(crs,
				// utm).getMathTransform());
			}
		} catch (FactoryException e) {
			e.printStackTrace();
		} catch (TransformException e) {
			e.printStackTrace();
		}
		return g2;
	}

	public static Geometry convertGeometrytoUTM(Geometry geom,
			Geography geography) {
		// boolean convert = !geography.getUnits(0).equals(SI.METER);
		CoordinateReferenceSystem utm = null;
		CoordinateReferenceSystem crs = geography.getCRS();
		Geometry g2 = geom;

		try {
			// convert p to UTM
			// if (convert) {
			utm = UTMFinder.getUTMFor(geom, crs);
			MathTransform transform = CRS.findMathTransform(crs, utm);
			g2 = JTS.transform(geom, transform);
			// g2 = JTS.transform(geom, cFactory.createOperation(crs,
			// utm).getMathTransform());
			// }
		} catch (FactoryException e) {
			e.printStackTrace();
		} catch (TransformException e) {
			e.printStackTrace();
		}
		return g2;
	}

	public static Geometry convertGeometryFromUTMtoWGS(Geometry geom,
			Geography geography) {
		boolean convert = geography.getUnits(0).equals(SI.METER);
		CoordinateReferenceSystem utm = null;
		CoordinateReferenceSystem wgs = defaultCRS;
		CoordinateReferenceSystem crs = geography.getCRS();
		Geometry g2 = geom;

		try {
			if (convert) {
				utm = UTMFinder.getUTMFor(geom, crs);
				MathTransform transform = CRS.findMathTransform(utm, wgs);
				g2 = JTS.transform(geom, transform);
				// g2 = JTS.transform(geom, cFactory.createOperation(crs,
				// utm).getMathTransform());
			}
		} catch (FactoryException e) {
			e.printStackTrace();
		} catch (TransformException e) {
			e.printStackTrace();
		}
		return g2;
	}

	public static Geometry convertGeometryToWGS(Geometry geom,
			Geography geography) {
		boolean convert = geography.getUnits(0).equals(SI.METER);
		CoordinateReferenceSystem wgs = defaultCRS;
		CoordinateReferenceSystem crs = geography.getCRS();
		Geometry g2 = geom;

		try {
			if (convert) {
				// utm = UTMFinder.getUTMFor(geom, crs);
				MathTransform transform = CRS.findMathTransform(crs, wgs);
				g2 = JTS.transform(geom, transform);
				// g2 = JTS.transform(geom, cFactory.createOperation(crs,
				// utm).getMathTransform());
			}
		} catch (FactoryException e) {
			e.printStackTrace();
		} catch (TransformException e) {
			e.printStackTrace();
		}
		return g2;
	}

	public static Geometry convertGeometryFromWGStoCRS(Geometry geom,
			Geography geography) {
		// boolean convert = geography.getUnits(0).equals(SI.METER);
		CoordinateReferenceSystem wgs = defaultCRS;
		CoordinateReferenceSystem crs = geography.getCRS();
		Geometry g2 = geom;

		try {
			// if (convert) {
			// utm = UTMFinder.getUTMFor(geom, crs);
			MathTransform transform = CRS.findMathTransform(wgs, crs);
			g2 = JTS.transform(geom, transform);
			// g2 = JTS.transform(geom, cFactory.createOperation(crs,
			// utm).getMathTransform());
			// }
		} catch (FactoryException e) {
			e.printStackTrace();
		} catch (TransformException e) {
			e.printStackTrace();
		}
		return g2;
	}

	public static Geometry convertGeometryWGS(Geometry geom, Geography geography) {
		// boolean convert = geography.getUnits(0).equals(SI.METER);
		CoordinateReferenceSystem wgs = defaultCRS;
		CoordinateReferenceSystem crs = geography.getCRS();
		Geometry g2 = geom;

		try {
			// if (convert) {
			// utm = UTMFinder.getUTMFor(geom, crs);
			MathTransform transform = CRS.findMathTransform(crs, wgs);
			g2 = JTS.transform(geom, transform);
			// g2 = JTS.transform(geom, cFactory.createOperation(crs,
			// utm).getMathTransform());
			// }
		} catch (FactoryException e) {
			e.printStackTrace();
		} catch (TransformException e) {
			e.printStackTrace();
		}
		return g2;
	}

	public static Geometry generateBuffer(Geography geography, Geometry geom,
			double distance) {
		boolean convert = !geography.getUnits(0).equals(SI.METER);

		CoordinateReferenceSystem utm = null;
		Geometry buffer = null;
		CoordinateReferenceSystem crs = geography.getCRS();
		Geometry g2 = geom;

		try {
			// convert p to UTM
			if (convert) {
				utm = UTMFinder.getUTMFor(geom, crs);
				g2 = JTS.transform(geom, cFactory.createOperation(crs, utm)
						.getMathTransform());
			}

			buffer = g2.buffer(distance);

			// convert buffer back to geography's crs.
			if (convert) {
				buffer = JTS.transform(buffer,
						cFactory.createOperation(utm, crs).getMathTransform());
			}
		} catch (FactoryException e) {
			e.printStackTrace();
		} catch (TransformException e) {
			e.printStackTrace();
		}

		return buffer;
	}

	private void initializeGlobalVars(Context context) {
		Geography geography = (Geography) context.getProjection("Geography");

		// Pixel.pixels = null;
		// Pixel.pixels = new ArrayList<>();

		Pixel.pixelMap = null;
		Pixel.pixelMap = new HashMap<Coordinate, Pixel>();

		for (Pixel myPixel : Pixel.InitializationPixels) {
			Pixel newPixel = new Pixel(myPixel);
			Geometry geom = newPixel.getGeom();
			context.add(newPixel);
			geography.move(newPixel, geom);
			Pixel.pixelMap.put(newPixel.getCoord_m(), newPixel);
			// Pixel.pixels.add(newPixel); //maybe no need for this line - TODO
			// - check
		}

		Settlement.numSettlements = Settlement.InitializationSettlements.size();
		Settlement.studyAreaFull = false;

		Settlement.settlements = new ArrayList<>();
		// go over every saved existing settlement from previous initialization
		// run and add it to the context
		for (Settlement existingSettlement : Settlement.InitializationSettlements) {
			Settlement newSettlement = new Settlement(existingSettlement,
					context);
			// Geometry geom = newSettlement.getPixel().getGeom();
			// context.add(newSettlement);
			// geography.move(newSettlement, geom);

			// //go over every AgLandParcel belonging to the Settlement and add
			// it into the context and geography
			// for (AgriculturalLandParcel agParcel:
			// newSettlement.settlementParcels){
			// Geometry agParcelGeom = agParcel.getParcelGeom();
			// context.add(agParcel);
			// geography.move(agParcel, agParcelGeom);
			// }
			//
			// Settlement.settlements.add(newSettlement);
		}

		Settlement.potentialSettlements = new ArrayList<>();
		// go over every saved future potential settlement from previous
		// initialization run and add it list of potential settlements
		for (Settlement futureSettlement : Settlement.InitializationpotentialSettlements) {
			Settlement newSettlement = new Settlement(futureSettlement);
			Settlement.potentialSettlements.add(newSettlement);
		}

		FarmerAgent.numEmigrants = FarmerAgent.totalNumFarmers = FarmerAgent.numStarving = 0;

		Road.roads = new ArrayList<>();
		RoadCoord.roadSet = new TreeSet<RoadCoord>();

		for (Road myRoad : Road.initializedRoads) {
			Road newRoad = new Road(myRoad);
			Geometry geom = newRoad.getRoadLine();
			// Geometry geom = newRoad.getPixel().getGeom();
			context.add(newRoad);
			geography.move(newRoad, geom);
			Road.roads.add(newRoad);
		}

		for (RoadCoord myRoadCoord : RoadCoord.initializedRoadSet) {
			RoadCoord newRoad = new RoadCoord(myRoadCoord);
			RoadCoord.roadSet.add(newRoad);
		}

		RoadCoord.useRoadSet = true;
	}

	static Settlement getRemoteSett(Geometry geom, Context context) {
		if (remoteSett == null) {
			Pixel myPixel = new Pixel(Classification.SETTLEMENT, geom,
					geom.getCoordinate());
			remoteSett = new Settlement(myPixel);
			remoteSett.setName("Remote Settlement");
			Geography geography = (Geography) context
					.getProjection("Geography");
			context.add(remoteSett);
			geography.move(remoteSett, geom);
			Settlement.settlements.add(remoteSett);
		}
		return remoteSett;
	}

}

// BufferedImage image = ImageIO.read(new File("circle1.bmp"));
// byte[][] greenInputData = new byte[30][40];
//
// for (int x = 0; x < inputData.length; x++)
// {
// for (int y = 0; y < inputData[x].length; y++)
// {
// int color = image.getRGB(x, y);
// //alpha[x][y] = (byte)(color>>24);
// //red[x][y] = (byte)(color>>16);
// greenInputData[x][y] = (byte)(color>>8);
// //blue[x][y] = (byte)(color);