package negevVer1;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;

public class Boundary {
	
	static List<Boundary> boundaries = new ArrayList<>(); //list of all boundaries
	
	Geometry geom;
	int ID;
	double area;
	
	Settlement settlement;
	
	Geometry getGeom() {
		return geom;
	}

	public int getID() {
		return ID;
	}

	Settlement getMySettlement() {
		return settlement;
	}
	
	public String getSettlementName() {
		return this.settlement.getName();
	}

	void setMySettlement(Settlement mySettlement) {
		this.settlement = mySettlement;
	}

	Boundary(Geometry myGeom, int myID, double myArea, Settlement mySett){
		geom = myGeom;
		ID = myID;
		area = myArea;
		settlement = mySett;
		Boundary.boundaries.add(this);
	}
	
	public double getArea() {
		return area;
	}


}
