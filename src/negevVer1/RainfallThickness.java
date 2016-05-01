package negevVer1;

import com.vividsolutions.jts.geom.Geometry;

public class RainfallThickness {
	
	int rainYearlyAmount;
	double area;
	Geometry geom;
	
	
	public RainfallThickness(int myAmount, double myArea, Geometry myGeom) {
		rainYearlyAmount = myAmount;
		area = myArea;
		geom = myGeom;
	}

	public int getRainYearlyAmount() {
		return rainYearlyAmount;
	}

	public double getArea() {
		return area;
	}

	void setRainYearlyAmount(int rainYearlyAmount) {
		this.rainYearlyAmount = rainYearlyAmount;
	}

	public Geometry getGeom() {
		return geom;
	}

}
