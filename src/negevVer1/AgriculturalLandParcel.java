package negevVer1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import negevVer1.SoilType.Type;

import com.vividsolutions.jts.geom.Geometry;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.gis.Geography;
import repast.simphony.util.ContextUtils;

public class AgriculturalLandParcel {
	
	public enum Status{
		VIRGIN,		//not claimed - currently not in use - by Default a parcel belongs to a Settlement
		SETTLEMENT_CLAIMED,		//belongs to a specific settlement – not yet cultivated 
		FARMER_CLAIMED,		//belongs to a specific farmer – not yet cultivated 
		CULTIVATED,		//the field is currently used for cropping 
		FALLOW		//the field is currently in fallow /rest 		
	}

	Status status;
	FarmerAgent owner;
	Crop currentCrop;
	Crop nextCrop;
	static final List <String> cropCycle = Arrays.asList("COTTON" , "CEREAL" , "CEREAL"); // The crop cycle is: cotton-cereal-cereal
	double SOM;
	double area; //agricultural parcel's area in m^2
	int cycleCount;  //year 0-2 within the cropCycle
	Settlement settlement;
	
	List<Pixel> parcelPixels;
	Geometry parcelGeom;
	double rainThickness = 0 ;
	SoilType.Type soilType = SoilType.Type.INAPPROPRIATE_SOIL; //default value
	boolean isPotentiallyIrrigated = false; //default value
	boolean isReclaimedWaterIrrigation = false; //if parcel is irrigated then there are 2 potential water sources: reclaimed water and fresh water.
	//If reclaimed water - then this indicator is true, otherwise false (fresh water, or in the case this parcel is not irrigated).
	boolean isIrrigatedThisCycle = false;
	
	static double cubicMeterPerDunam = 500; //each dunam of agricultural land needs 500 m^3/y
	static final float parcelInitialSOM = 43; // t/ha
	static final float SOMCultivationThreshold = 25; // t/ha
	static final float SOMFallowThreshold = 18;  // t/ha
	static final double increaseAnnualFractionCMPD = 0.0033; //in case of climate change scenario - this is the annual increase ratio in agricultural water demand (0.33%)
	
	//float distanceFromSettlement; //in rings (currently not implemented)
	
//	public AgriculturalLandParcel(Settlement mySettlement, List<Pixel> pixList, Geometry geom) {
//		settlement = mySettlement;
//		status = AgriculturalLandParcel.Status.SETTLEMENT_CLAIMED;//by Default a parcel belongs to a Settlement
//		parcelPixels = new ArrayList<>(pixList);
//		parcelGeom = geom;
//		
//		SOM = parcelInitialSOM;
//		cycleCount = 0;
//		currentCrop = new Crop(Crop.Type.COTTON);
//		nextCrop = new Crop(Crop.Type.CEREAL);
//		
//		// insert the parcel into the context and geography
//		Context context = ContextUtils.getContext(pixList.get(0));
//		Geography geography = (Geography) context.getProjection("Geography");
//		context.add(this);
//		geography.move(this, parcelGeom);
//	}
	public AgriculturalLandParcel(Settlement mySettlement, List<Pixel> pixList, Geometry geom, double myArea, Context context) {
		settlement = mySettlement;
		status = AgriculturalLandParcel.Status.SETTLEMENT_CLAIMED;//by Default a parcel belongs to a Settlement
		if(pixList==null){
			parcelPixels = new ArrayList<>(); //pre-existing parcels don't have pixels information
		}else{
			parcelPixels = new ArrayList<>(pixList);
		}
		parcelGeom = geom;
		area = myArea;
		
		SOM = parcelInitialSOM;
		cycleCount = 0;
		currentCrop = new Crop(Crop.Type.COTTON);
		nextCrop = new Crop(Crop.Type.CEREAL);
		
		settlement.currentAgriArea+=area;
		++settlement.numParcels;
		Settlement.totalAgLandInStudyArea+=area;
		
		// insert the parcel into the context and geography
		Geography geography = (Geography) context.getProjection("Geography");
		context.add(this);
		geography.move(this, parcelGeom);
	}
	
//	public AgriculturalLandParcel(Settlement mySettlement, List<Pixel> pixList, Geometry geom, Context context) {
//		settlement = mySettlement;
//		status = AgriculturalLandParcel.Status.SETTLEMENT_CLAIMED;//by Default a parcel belongs to a Settlement
//		parcelPixels = new ArrayList<>(pixList);
//		parcelGeom = geom;
//		
//		SOM = parcelInitialSOM;
//		cycleCount = 0;
//		currentCrop = new Crop(Crop.Type.COTTON);
//		nextCrop = new Crop(Crop.Type.CEREAL);
//		
//		if(!mySettlement.isFutureSettlement()){ // we want to add parcels to context only for existing Settlements
//			// insert the parcel into the context and geography
//			Geography geography = (Geography) context.getProjection("Geography");
//			context.add(this);
//			geography.move(this, parcelGeom);
//		}
//	}
	
	
	AgriculturalLandParcel(AgriculturalLandParcel parcel, Settlement mySettlement, Context context) {
		settlement = mySettlement;
		status = parcel.getStatus();
		parcelPixels = parcel.parcelPixels;
		parcelGeom = parcel.parcelGeom;
		
		SOM = parcel.getSOM();
		cycleCount = 0;
		currentCrop = new Crop(Crop.Type.COTTON);
		nextCrop = new Crop(Crop.Type.CEREAL);
		
		// insert the parcel into the context and geography
		Geography geography = (Geography) context.getProjection("Geography");
		context.add(this);
		geography.move(this, parcelGeom);
	}


	public AgriculturalLandParcel(AgriculturalLandParcel parcel, Settlement settlement2) {
		settlement = settlement2;
		status = parcel.getStatus();
		parcelPixels = parcel.parcelPixels;
		parcelGeom = parcel.parcelGeom;
		
		SOM = parcel.getSOM();
		cycleCount = 0;
		currentCrop = new Crop(Crop.Type.COTTON);
		nextCrop = new Crop(Crop.Type.CEREAL);
	}


	Geometry getParcelGeom() {
		return parcelGeom;
	}

	public double getSOM() {
		return SOM;
	}

	void setSOM(double sOM) {
		SOM = sOM;
	}

	double calculateEndOfYearSOM(){
		if (status == Status.CULTIVATED){
			if(cycleCount == 0){
				SOM-=2.38;
			}else{
				SOM-=1.19;
			}
		}else{
			if (status == Status.FALLOW){
				SOM+=0.963;
				if(SOM > parcelInitialSOM){
					SOM = parcelInitialSOM;  //parcel can't have SOM higher than parcelInitialSOM
				}
			}
		}
		return SOM;
	}
	
	public Boolean cultivateNextCycle(){
		if (SOM > SOMCultivationThreshold
				&& (status == Status.CULTIVATED || status == Status.FALLOW)) {
			return true;
		}
		return false;
	}
	
	Crop determineNextCrop(){
		switch (cycleCount) {
		case 0:
			nextCrop = new Crop(Crop.Type.CEREAL);
			break;
		case 1:
			nextCrop = new Crop(Crop.Type.CEREAL);
			break;
		case 2:
			nextCrop = new Crop(Crop.Type.COTTON);
			break;
		default:
			nextCrop = new Crop(Crop.Type.COTTON);
			System.out.println("Error in cycleCount - illegal value of: "
					+ cycleCount);
			break;

		}
		return nextCrop;
		
	}
	
	//For fields that have already been cultivated during initialization
	void setInitialActiveSOM(){
		SOM = parcelInitialSOM - (RandomHelper.nextIntFromTo(0, 11)*1.586); 
	}
	
	//For fields that have already been in fallow during initialization
	void setInitialFallowSOM(){
		SOM = SOMFallowThreshold + (RandomHelper.nextIntFromTo(0, 17)*0.963); //fields that have already been in fallow during initialization
	}

	
	void setStatus(Status status){
		this.status = status;
	}
	
	public Status getStatus(){
		return status;
	}
	
	public String getStatusName(){
		return status.toString();
	}
	
	public FarmerAgent getOwner() {
		return owner;
	}

	void setOwner(FarmerAgent owner) {
		this.owner = owner;
	}

	public Crop getCurrentCrop() {
		return currentCrop;
	}

	Crop setCurrentCrop(Crop myCurrentCrop) {
		currentCrop = myCurrentCrop;
		return currentCrop;
	}

	public Crop getNextCrop() {
		return nextCrop;
	}


	void setCycleCount(int i) {
		cycleCount = i;
	}


	void advanceCycleCount() {
		cycleCount = (cycleCount +1) % 3;
	}

	public double getArea() {
		return area;
	}
	
	//in order to calculate total area in fallow - within the model
	public double getFallowArea(){
		if(this.status == Status.FALLOW){
			return this.area;
		}
		return 0;
	}
	//in order to calculate total irrigated area per cycle - within the model
	public double getIrrigatedArea(){
		if(this.isIrrigatedThisCycle == true){
			return this.area;
		}
		return 0;
	}
	
	public double getCultivatedArea(){
		if(this.status == Status.CULTIVATED){
			return this.area;
		}
		return 0;
	}
	
	void setArea(double area) {
		this.area = area;
	}

	public String getSettlementName(){
		return settlement.getName();
	}
	
	public long getSettlementID(){
		return settlement.getID();
	}

	public double getRainThickness() {
		return rainThickness;
	}

	void setRainThickness(double rainThickness) {
		this.rainThickness = rainThickness;
	}

	void setIrrigated(boolean b) {
		isPotentiallyIrrigated = b;
	}

	public boolean isPotentiallyIrrigated() {
		return isPotentiallyIrrigated;
	}

	public boolean isReclaimedWaterIrrigation() {
		return isReclaimedWaterIrrigation;
	}

	void setReclaimedWaterIrrigation(boolean isReclaimedWaterIrrigation) {
		this.isReclaimedWaterIrrigation = isReclaimedWaterIrrigation;
	}

	public SoilType.Type getSoilType() {
		return soilType;
	}
	
	public String getSoilTypeName(){
		return soilType.toString();
	}

	void setSoilType(SoilType.Type soil) {
		this.soilType = soil;
	}

	public boolean isIrrigatedThisCycle() {
		return isIrrigatedThisCycle;
	}

	void setIrrigatedThisCycle(boolean isIrrigatedThisCycle) {
		this.isIrrigatedThisCycle = isIrrigatedThisCycle;
	}
	
	Settlement getSettlement() {
		return settlement;
	}
	
	public double getWaterDemand(){
		return (area/1000) * AgriculturalLandParcel.cubicMeterPerDunam;
	}
	
	void setStatusAccordingToRainOnly(boolean isZeroWaterQuota){
		//choose a random integer between 1 and 10
		int frequency =  RandomHelper.nextIntFromTo(1, 10);
		
		//20% chance this field will be in fallow next year - regardless rain amounts
		if(frequency > 8){
			setStatus(Status.FALLOW);
			getOwner().currentCultivatedParcels.remove(this);
			return;
		}
		//if water quota for the settlement has reached zero - chances are higher that this parcel will be in fallow - regardless of rain amount
		if(isZeroWaterQuota){
			if(frequency > 5){
				setStatus(Status.FALLOW);
				getOwner().currentCultivatedParcels.remove(this);
				return;
			}
		}
		
		if(getRainThickness() >= Settlement.minRainThicknessForRainfedAg){
			setStatus(Status.CULTIVATED);
			if(!getOwner().currentCultivatedParcels.contains(this))
				getOwner().currentCultivatedParcels.add(this);
		}
		else{
			setStatus(Status.FALLOW);
			getOwner().currentCultivatedParcels.remove(this);
		}
	}
	
	//used in climate change scenario
	static void setcCubicMeterPerDunamWaterDemand(){
		cubicMeterPerDunam += cubicMeterPerDunam*increaseAnnualFractionCMPD;
	}
	//used in climate change scenario
	void updateAnnualRainAmountClimateChange(){
		if(getRainThickness() <= 0){
			setRainThickness(this.getSettlement().getRainAnnualAmount());
		}
		setRainThickness(rainThickness* IrrigationManager.annualPrecipitationFraction);
	}

//	public void setNextCrop(Crop nextCrop) {
//		this.nextCrop = nextCrop;
//	}
	
}
