package negevVer1;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import negevVer1.AgriculturalLandParcel.Status;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.gis.Geography;
import repast.simphony.util.ContextUtils;

public class FarmerAgent {
	
	static long totalNumFarmers = 0; //Total number of active Farmers within the study area
	static long numEmigrants = 0; //current number of farmers which have emigrated away from the study area. Currently not implemented
	static long numStarving = 0; //current number of starving families.

	List<AgriculturalLandParcel> parcels;  //every farmer has 2 parcels (in ver2 - it is the Settlement's number of parcels) 
//	AgriculturalLandParcel currentCultivatedParcel;
	List<AgriculturalLandParcel> currentCultivatedParcels;
//	Crop currentCrop;  //Not sure if Farmer needs to have the crop - maybe should be only at the parcel level and not delegated to the farmer...
//	Crop prevCrop;
	Boolean isStarving;
	Settlement mySettlement; 
	
	
	// The received list of parcels contains 2 parcels - the first is the active and the second is fallow. SOM is already set. 
	//In version2 - farmer has all parcels in a settlement - one farmer per settlement - half the parcels are initialized as active, the other half are fallow
		FarmerAgent(Settlement settlement, List <AgriculturalLandParcel> myParcels, Context context){
			++totalNumFarmers; //Update total number of farmers in the study area
			mySettlement = settlement;
			parcels = new ArrayList<>(myParcels);
			currentCultivatedParcels = new ArrayList<>();
			isStarving = false;
//			currentCultivatedParcel = myParcels.get(0);
			
			for(AgriculturalLandParcel parcel: parcels ){
				parcel.setOwner(this);
				if(parcel.getStatus()==AgriculturalLandParcel.Status.CULTIVATED){
//					currentCultivatedParcel = parcel;
					currentCultivatedParcels.add(parcel);
//					currentCrop = parcel.getCurrentCrop();
				}
			}
//			if(currentCrop == null){
//				System.out.println("Error - Farmer has no CULTIVATED parcel");
//				currentCrop = new Crop(Crop.Type.COTTON);
//			}
//			prevCrop = currentCrop; //doesn't have meaning here - initialize so its not null
			
			// insert the Farmer into the context and locate it in its Active parcel geography (TODO - or should it be located in its Settlement???)
			Geography geography = (Geography) context.getProjection("Geography");
			context.add(this);
//			geography.move(this, currentCultivatedParcel.getParcelGeom());
			geography.move(this, currentCultivatedParcels.get(0).getParcelGeom());
				
		}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
//		chooseFieldForNextYear();
//		chooseFieldsForNextYear();
		updateFields();
	}
	
	private void updateFields(){
		for(AgriculturalLandParcel parcel: parcels ){
			parcel.calculateEndOfYearSOM();
			if(parcel.getStatus() == Status.CULTIVATED){
				parcel.setCurrentCrop(chooseNextYearCrop(parcel));//this will update the parcel both on parcels and currentCultivatedParcels lists
				parcel.advanceCycleCount();
			}
		}
		//move Farmer to the active parcel
		Context context = ContextUtils.getContext(mySettlement);
		Geography geography = (Geography) context.getProjection("Geography");
		if(currentCultivatedParcels.size()==0)
		{
			Settlement.logger.log(Level.FINE, "FarmerAgent has no cultivated parcels");
		}
		else{
			geography.move(this, currentCultivatedParcels.get(0).getParcelGeom());
		}
		//set starving status according to ratio of currently cultivated fields from the overall Farmer's parcels
		if(currentCultivatedParcels.size() * 2 >= parcels.size()){
			setStarvingStatus(false);
		}else{
			setStarvingStatus(true);
		}			
	}
	
	private void chooseFieldsForNextYear() {
		//not enough water for irrigation - for the whole Settlement - TODO - check if parcels can be rain-fed cultivated - according to precipitation layer
		if(mySettlement.unmetWaterDemand == true){
			setStarvingStatus(true);
			for(AgriculturalLandParcel parcel: parcels ){
				parcel.calculateEndOfYearSOM();
				parcel.setStatus(AgriculturalLandParcel.Status.FALLOW);
			}
			currentCultivatedParcels = new ArrayList<>(); // empty cultivated parcels' list - since there is no water for irrigation
			return;
		}
		
		AgriculturalLandParcel alternativeParcel = null;
		AgriculturalLandParcel currentActiveParcel = null;
		for(AgriculturalLandParcel parcel: parcels ){
			parcel.calculateEndOfYearSOM();
			if(parcel.getStatus()==AgriculturalLandParcel.Status.CULTIVATED){
				if(parcel.getSOM() >= AgriculturalLandParcel.SOMCultivationThreshold){//continue cultivating currently active parcel
					parcel.setCurrentCrop(chooseNextYearCrop(parcel));//this will update the parcel both on parcels and currentCultivatedParcels lists
					parcel.advanceCycleCount();
					
				}
				else{
					// SOM < SOMCultivationThreshold. change the parcel's status to FALLOW 
					parcel.setStatus(AgriculturalLandParcel.Status.FALLOW);
//					parcel.setCycleCount(0);
					currentCultivatedParcels.remove(parcel);
				}
			}else{
				if(parcel.getStatus()==AgriculturalLandParcel.Status.FALLOW && parcel.getSOM() >= AgriculturalLandParcel.SOMCultivationThreshold){
					parcel.setStatus(AgriculturalLandParcel.Status.CULTIVATED);
//					parcel.setCycleCount(2);//in order to start for next year with new cycle
					parcel.setCurrentCrop(chooseNextYearCrop(parcel)); //the cycle continues from where it stopped since the parcel was last in production
					parcel.advanceCycleCount();
					if(!currentCultivatedParcels.contains(this))
						currentCultivatedParcels.add(parcel);
				}	
			}
		}		
		//move Farmer to the active parcel
		Context context = ContextUtils.getContext(mySettlement);
		Geography geography = (Geography) context.getProjection("Geography");
		geography.move(this, currentCultivatedParcels.get(0).getParcelGeom());
		
		//set starving status according to ratio of currently cultivated fields from the overall Farmer's parcels
		if(currentCultivatedParcels.size() * 2 >= parcels.size()){
			setStarvingStatus(false);
		}else{
			setStarvingStatus(true);
		}
	}
	

//	private AgriculturalLandParcel chooseFieldForNextYear() {
//		AgriculturalLandParcel alternativeParcel = null;
//		AgriculturalLandParcel currentActiveParcel = null;
//		
//		for(AgriculturalLandParcel parcel: parcels ){
//			parcel.calculateEndOfYearSOM();
//			if(parcel.getStatus()==AgriculturalLandParcel.Status.CULTIVATED){
//				if(parcel.getSOM() >= AgriculturalLandParcel.SOMCultivationThreshold){//continue cultivating currently active parcel
//					setStarvingStatus(false);
//					prevCrop = currentCrop;
//					currentCrop = parcel.setCurrentCrop(chooseNextYearCrop(parcel));
//					parcel.advanceCycleCount();
//					currentActiveParcel = parcel; //current active parcel can continue to be cultivated in the next coming cycle
//				}
//				else{
//					// SOM < SOMCultivationThreshold. change the parcel's status to FALLOW 
//					parcel.setStatus(AgriculturalLandParcel.Status.FALLOW);
//					parcel.setCycleCount(0);
//				}
//			}else{
//				if(parcel.getStatus()==AgriculturalLandParcel.Status.FALLOW && parcel.getSOM() >= AgriculturalLandParcel.SOMCultivationThreshold){
//					alternativeParcel = parcel; //Fallow parcel can be cultivated in the next coming cycle
//				}
//				
//			}
//		}
//		if(currentActiveParcel != null){
//			return currentActiveParcel;
//		}
//		if (alternativeParcel == null){ //all parcels SOM is below SOMCultivationThreshold - both parcels will be in FALLOW next cycle
//			setStarvingStatus(true);
//			return null;
//		}//We are switching to the other parcel - change its' status to CULTIVATED
//		alternativeParcel.setStatus(AgriculturalLandParcel.Status.CULTIVATED);
//		setStarvingStatus(false);
//		prevCrop = alternativeParcel.getCurrentCrop(); //In this case - it is the last crop that was cultivated before the parcel went Fallow
//		currentCrop = alternativeParcel.setCurrentCrop(new Crop(Crop.Type.COTTON));
//		alternativeParcel.setCycleCount(0);
//		
//		currentCultivatedParcel = alternativeParcel;
//		//move Farmer to the active parcel
//		Context context = ContextUtils.getContext(mySettlement);
//		Geography geography = (Geography) context.getProjection("Geography");
//		geography.move(this, currentCultivatedParcel.getParcelGeom());
//		
//		return alternativeParcel;
//		
//	}
	
	private Crop chooseNextYearCrop(AgriculturalLandParcel parcel){
		return parcel.determineNextCrop();
		
	}
	
	public boolean getStarvingStatus(){
		return isStarving;
	}
	
	void setStarvingStatus(boolean starvingCondition){
		if(isStarving != starvingCondition){
			if(starvingCondition == true){
				FarmerAgent.numStarving++; //update the number of starving families in the study area accordingly
			}
			else{
				FarmerAgent.numStarving--;
			}
			isStarving = starvingCondition;
		}
	}
	public String getSettlementName(){
		return mySettlement.getName();
	}
	
	public int getNumParcels(){
		return parcels.size();
	}
	
	public int getCurrentActiveNumParcels(){
		return currentCultivatedParcels.size();
	}
	
	public static long getNumStarving() {
		return numStarving;
	}

	public static long getTotalNumFarmers() {
		return totalNumFarmers;
	}

	public static long getNumEmigrants() {
		return numEmigrants;
	}
	

}
