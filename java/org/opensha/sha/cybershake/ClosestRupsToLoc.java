package org.opensha.sha.cybershake;

import java.util.ArrayList;
import java.util.Collections;

import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationUtils;
import org.opensha.commons.param.ParameterAPI;
import org.opensha.commons.param.StringParameter;
import org.opensha.sha.cybershake.db.CybershakeSiteInfo2DB;
import org.opensha.sha.cybershake.db.Cybershake_OpenSHA_DBApplication;
import org.opensha.sha.cybershake.db.MeanUCERF2_ToDB;
import org.opensha.sha.cybershake.openshaAPIs.CyberShakeERF;
import org.opensha.sha.earthquake.EqkRupForecastAPI;
import org.opensha.sha.earthquake.ProbEqkRupture;
import org.opensha.sha.earthquake.ProbEqkSource;
import org.opensha.sha.faultSurface.EvenlyGriddedSurfaceAPI;

public class ClosestRupsToLoc {
	
	static class Record implements Comparable<Record> {
		int sourceID;
		int rupID;
		double mag;
		double dist;
		String name;
		
		public Record(int sourceID, int rupID, double mag, double dist, String name) {
			this.sourceID = sourceID;
			this.rupID = rupID;
			this.mag = mag;
			this.dist = dist;
			this.name = name;
		}

		public int compareTo(Record o) {
			if (mag == o.mag)
				return 0;
			else if (mag < o.mag)
				return -1;
			else
				return 1;
		}
		
		public String toString() {
			return "src: " + sourceID + ", rup: " + rupID + ", mag: " + mag + ", dist: " + dist + ", name: " + name;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		EqkRupForecastAPI erf = MeanUCERF2_ToDB.createUCERF2ERF();
//		EqkRupForecastAPI erf = new CyberShakeERF();
//		StringParameter selector = (StringParameter) erf.getAdjustableParameterList().getParameter(CyberShakeERF.ERF_ID_SELECTOR_PARAM);
		int id = 34;
//		
//		String idStr = null;
//		for (String name : (ArrayList<String>)selector.getAllowedStrings()) {
//			System.out.println(name);
//			if (name.startsWith("" + id))
//				idStr = name;
//		}
//		if (idStr == null) {
//			System.out.println("NO MATCH!");
//			System.exit(1);
//		}
//		selector.setValue(idStr);
		erf.updateForecast();
		
		Location site = new Location(34.03852, -116.64795);
//		Location site = new Location(33.93597, -116.57794);
		
		ArrayList<Record> recs = new ArrayList<Record>();
		
		for (int sourceID=0; sourceID<erf.getNumSources(); sourceID++) {
			ProbEqkSource source = erf.getSource(sourceID);
			for (int rupID=0; rupID<source.getNumRuptures(); rupID++) {
				ProbEqkRupture rup = source.getRupture(rupID);
				EvenlyGriddedSurfaceAPI surface = rup.getRuptureSurface();
				double closestDist = 99999999;
				for (int i=0; i<surface.getNumRows(); i++) {
					for (int j=0; j<surface.getNumCols(); j++) {
						Location loc = (Location)surface.get(i, j);
						double distKM = LocationUtils.horzDistanceFast(site, loc);
						if (distKM < closestDist)
							closestDist = distKM;
					}
				}
				if (closestDist < 15)
					recs.add(new Record(sourceID, rupID, rup.getMag(), closestDist, source.getName()));
			}
		}
		
		Collections.sort(recs);
		
		CybershakeSiteInfo2DB csSite = null;
		
		for (Record rec: recs) {
			if (id != 35) {
				if (csSite == null)
					csSite = new CybershakeSiteInfo2DB(Cybershake_OpenSHA_DBApplication.db);
				int newID = csSite.getMatchedCSSourceID(erf, id, rec.sourceID);
				System.out.println("ERF 35=" + rec.sourceID + " ERF "+id+"=" + newID);
			}
			System.out.println(rec);
		}
		System.exit(0);
	}

}
