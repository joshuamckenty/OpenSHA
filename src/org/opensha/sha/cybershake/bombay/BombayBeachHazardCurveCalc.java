package org.opensha.sha.cybershake.bombay;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.opensha.commons.data.function.ArbDiscrEmpiricalDistFunc;
import org.opensha.commons.data.function.ArbitrarilyDiscretizedFunc;
import org.opensha.commons.data.function.DiscretizedFuncAPI;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationUtils;
import org.opensha.commons.util.FileUtils;
import org.opensha.sha.cybershake.calc.HazardCurveComputation;
import org.opensha.sha.cybershake.calc.RuptureProbabilityModifier;
import org.opensha.sha.cybershake.calc.RuptureVariationProbabilityModifier;
import org.opensha.sha.cybershake.db.CybershakeIM;
import org.opensha.sha.cybershake.db.CybershakeRun;
import org.opensha.sha.cybershake.db.Cybershake_OpenSHA_DBApplication;
import org.opensha.sha.cybershake.db.DBAccess;
import org.opensha.sha.cybershake.db.ERF2DB;
import org.opensha.sha.cybershake.db.MeanUCERF2_ToDB;
import org.opensha.sha.cybershake.db.PeakAmplitudesFromDB;
import org.opensha.sha.cybershake.db.SiteInfo2DB;
import org.opensha.sha.earthquake.EqkRupForecast;
import org.opensha.sha.earthquake.ProbEqkRupture;
import org.opensha.sha.earthquake.ProbEqkSource;
import org.opensha.sha.faultSurface.EvenlyGriddedSurfaceAPI;

public class BombayBeachHazardCurveCalc implements RuptureVariationProbabilityModifier {
	
	/** The location of the M4.8 event */
	public static final Location BOMBAY_LOC = new Location(33.318333, -115.728333);
	public static final Location PARKFIELD_LOC = new Location(35.815, -120.374);
	public static final Location PICO_RIVERA_LOC = new Location(33.99, -118.08, 18.9);
	public static final Location YUCAIPA_LOC = new Location(34.058, -117.011, 11.8);
	public static final Location COYOTE_CREEK = new Location(33.4205, -116.4887, 14.0);
//	public static double MAX_DIST_KM = 10;
	
	private Location hypoLocation;
	
	private double increaseMultFactor;
	
	private static final int ERFID = 35;
	private static final int RUP_VAR_SCEN_ID = 3;
	
	private EqkRupForecast ucerf;
	
	private ArrayList<Integer> sources = new ArrayList<Integer>();
	private ArrayList<ArrayList<Integer>> rups = new ArrayList<ArrayList<Integer>>();
	
	private HashMap<String, ArrayList<Integer>> rvMap = new HashMap<String, ArrayList<Integer>>();
	
	private ERF2DB erf2db;
	private SiteInfo2DB site2db;
	private PeakAmplitudesFromDB amps2db;
	private DBAccess db;
	
	private boolean useDepth;
	private String sourceNameConstr;
	private double maxDistance;
	
	private HashMap<String, ArrayList<Location>> rvLocMap = new HashMap<String, ArrayList<Location>>();
	
	public BombayBeachHazardCurveCalc(DBAccess db, double increaseMultFactor, Location hypoLocation,
			double maxDistance, String sourceNameConstr, boolean useDepth) {
		this.db = db;
		this.increaseMultFactor = increaseMultFactor;
		this.hypoLocation = hypoLocation;
		this.useDepth = useDepth;
		this.maxDistance = maxDistance;
		if (sourceNameConstr != null && sourceNameConstr.length() == 0)
			sourceNameConstr = null;
		this.sourceNameConstr = sourceNameConstr;
		if (!useDepth)
			this.hypoLocation = new Location(hypoLocation.getLatitude(), hypoLocation.getLongitude());
		erf2db = new ERF2DB(db);
		site2db = new SiteInfo2DB(db);
		amps2db = new PeakAmplitudesFromDB(db);
		ucerf = MeanUCERF2_ToDB.createUCERF2ERF();
		
		loadSAFRups();
		loadRVMap();
	}
	
	private void loadSAFRups() {
		int numSources = ucerf.getNumSources();
		
		int sourceCount = 0;
		int rupCount = 0;
		
		for (int sourceID=0; sourceID<numSources; sourceID++) {
			ProbEqkSource source = ucerf.getSource(sourceID);
			String name = source.getName();
			if (sourceNameConstr != null && !name.toLowerCase().contains(sourceNameConstr))
				continue;
			ArrayList<Integer> rupIDs = new ArrayList<Integer>();
			for (int rupID=0; rupID<source.getNumRuptures(); rupID++) {
				ProbEqkRupture rup = source.getRupture(rupID);
				if (shouldIncludeRup(rup)) {
					rupIDs.add(rupID);
				}
			}
			if (rupIDs.size() > 0) {
				rups.add(rupIDs);
				sources.add(sourceID);
				sourceCount++;
				rupCount += rupIDs.size();
			}
		}
		System.out.println("Identified " + sourceCount + " sources (" + rupCount + " rups)");
	}
	
	private boolean shouldIncludeRup(ProbEqkRupture rup) {
		EvenlyGriddedSurfaceAPI surface = rup.getRuptureSurface();
		boolean inside = false;
		for (int i=0; i<surface.getNumRows(); i++) {
			for (int j=0; j<surface.getNumCols(); j++) {
				Location loc = surface.getLocation(i, j);
				double dist = LocationUtils.linearDistance(hypoLocation, loc);
				if (dist < maxDistance) {
					return true;
				}
			}
		}
		return false;
	}
	
	private ArrayList<Integer> getRupVars(int sourceID, int rupID) {
		String sql = "SELECT Rup_Var_ID,Hypocenter_Lat,Hypocenter_Lon,Hypocenter_Depth FROM Rupture_Variations " +
					"WHERE ERF_ID=" + ERFID + " AND Rup_Var_Scenario_ID=" + RUP_VAR_SCEN_ID + " " +
					"AND Source_ID=" + sourceID + " AND Rupture_ID=" + rupID;
		ArrayList<Integer> rvs = new ArrayList<Integer>();
		int tot = 0;
		try {
			ResultSet rs = db.selectData(sql);
			boolean success = rs.first();
			while (success) {
				int rvID = rs.getInt("Rup_Var_ID");
				double lat = rs.getDouble("Hypocenter_Lat");
				double lon = rs.getDouble("Hypocenter_Lon");
				double depth = rs.getDouble("Hypocenter_Depth");
				Location loc;
				if (useDepth)
					loc = new Location(lat, lon, depth);
				else
					loc = new Location(lat, lon);
				tot++;
				
				double dist = LocationUtils.linearDistance(loc, hypoLocation);
				if (dist < maxDistance) {
					rvs.add(rvID);
					ArrayList<Location> locs = rvLocMap.get(getKey(sourceID, rupID));
					if (locs == null) {
						locs = new ArrayList<Location>();
						rvLocMap.put(getKey(sourceID, rupID), locs);
					}
					locs.add(loc);
				}
				
				success = rs.next();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Source " + sourceID + ", Rup " + rupID + ", vars: " + rvs.size() + "/" + tot);
		
		return rvs;
	}
	
	public static String getKey(int sourceID, int rupID) {
		return sourceID + " " + rupID;
	}
	
	private void loadRVMap() {
		for (int i=0; i<sources.size(); i++) {
			int sourceID = sources.get(i);
			ArrayList<Integer> rupIDs = rups.get(i);
			for (int rupID : rupIDs) {
				ArrayList<Integer> rvs = getRupVars(sourceID, rupID);
				if (rvs.size() > 0)
					rvMap.put(getKey(sourceID, rupID), rvs);
			}
		}
	}
	
	public void writeSourceRupFile(String fileName) throws IOException {
		FileWriter fw = new FileWriter(fileName);
		for (int i=0; i<sources.size(); i++) {
			int sourceID = sources.get(i);
			ArrayList<Integer> rupIDs = rups.get(i);
			for (int rupID : rupIDs) {
				ArrayList<Integer> rvs = rvMap.get(getKey(sourceID, rupID));
				if (rvs == null)
					continue;
				for (int rv : rvs) {
					fw.write(sourceID + "\t" + rupID + "\t" + rv + "\n");
				}
			}
		}
		fw.close();
	}
	
	public void writeSourceRupInfoFile(String fileName) throws IOException {
		FileWriter fw = new FileWriter(fileName);
		for (int i=0; i<sources.size(); i++) {
			int sourceID = sources.get(i);
			ArrayList<Integer> rupIDs = rups.get(i);
			for (int rupID : rupIDs) {
				ArrayList<Integer> rvs = rvMap.get(getKey(sourceID, rupID));
				if (rvs == null)
					continue;
				double mag = ucerf.getSource(sourceID).getRupture(rupID).getMag();
				double prob = ucerf.getSource(sourceID).getRupture(rupID).getProbability();
				ArrayList<Location> locs = rvLocMap.get(getKey(sourceID, rupID));
				for (int j=0; j<rvs.size(); j++) {
					int rv = rvs.get(j);
					Location loc = locs.get(j);
					double dist = LocationUtils.linearDistance(loc, hypoLocation);
					fw.write(sourceID + "\t" + rupID + "\t" + rv + "\t" + mag + "\t" + prob + "\t" + dist + "\n");
				}
			}
		}
		fw.close();
	}
	
	public static HashMap<String, ArrayList<Integer>> loadMapFromFile(String fileName)
				throws FileNotFoundException, IOException {
		ArrayList<String> lines = FileUtils.loadFile(fileName);
		HashMap<String, ArrayList<Integer>> map = new HashMap<String, ArrayList<Integer>>();
		for (String line : lines) {
			StringTokenizer tok = new StringTokenizer(line);
			int sourceID = Integer.parseInt(tok.nextToken());
			int rupID = Integer.parseInt(tok.nextToken());
			int rvID = Integer.parseInt(tok.nextToken());
			String key = getKey(sourceID, rupID);
			ArrayList<Integer> rvs = map.get(key);
			if (rvs == null) {
				rvs = new ArrayList<Integer>();
				map.put(key, rvs);
			}
			rvs.add(rvID);
		}
		return map;
	}
	
	/**
	 * Computes the Hazard Curve at the given site 
	 * @param imlVals
	 * @param site
	 * @param erfName
	 * @param imType
	 */
	public DiscretizedFuncAPI computeHazardCurve(ArrayList<Double> imlVals, CybershakeRun run, CybershakeIM imType){
		DiscretizedFuncAPI hazardFunc = new ArbitrarilyDiscretizedFunc();
		int siteID = run.getSiteID();
		int erfID = run.getERFID();
		int runID = run.getRunID();
		int numIMLs  = imlVals.size();
		for(int i=0; i<numIMLs; ++i) hazardFunc.set((imlVals.get(i)).doubleValue(), 1.0);
		
		ArrayList<Integer> srcIdList = site2db.getSrcIdsForSite(siteID, erfID);
		int numSrcs = srcIdList.size();
		for(int srcIndex =0;srcIndex<numSrcs;++srcIndex){
//			updateProgress(srcIndex, numSrcs);
			System.out.println("Source " + srcIndex + " of " + numSrcs + ".");
			int srcId = srcIdList.get(srcIndex);
			ArrayList<Integer> rupIdList = site2db.getRupIdsForSite(siteID, erfID, srcId);
			int numRupSize = rupIdList.size();
			for(int rupIndex = 0;rupIndex<numRupSize;++rupIndex){
				int rupId = rupIdList.get(rupIndex);
				double qkProb = erf2db.getRuptureProb(erfID, srcId, rupId);
				ArbDiscrEmpiricalDistFunc function = new ArbDiscrEmpiricalDistFunc();
				ArrayList<Double> imVals;
				try {
					imVals = amps2db.getIM_Values(runID, srcId, rupId, imType);
				} catch (SQLException e) {
					return null;
				}
				for (double val : imVals) {
					function.set(val/HazardCurveComputation.CONVERSION_TO_G,1);
				}
//				ArrayList<Integer> rupVariations = peakAmplitudes.getRupVarationsForRupture(erfId, srcId, rupId);
//				int size = rupVariations.size();
//				for(int i=0;i<size;++i){
//					int rupVarId =  rupVariations.get(i);
//					double imVal = peakAmplitudes.getIM_Value(siteId, erfId, sgtVariation, rvid, srcId, rupId, rupVarId, imType);
//					function.set(imVal/CONVERSION_TO_G,1);
//				}
				HazardCurveComputation.setIMLProbs(imlVals,hazardFunc, function.getNormalizedCumDist(), qkProb);
			}
		}
	     
	    for(int j=0; j<numIMLs; ++j) 
	    	hazardFunc.set(hazardFunc.getX(j),(1-hazardFunc.getY(j)));

		return hazardFunc;
	}
	
	public static void main(String args[]) {
		try {
			DBAccess db = Cybershake_OpenSHA_DBApplication.db;
			
//			String sourceNameConstr = "andreas";
//			BombayBeachHazardCurveCalc calc = new BombayBeachHazardCurveCalc(db, 1000d, BOMBAY_LOC,
//					10d, sourceNameConstr, false);
//			BombayBeachHazardCurveCalc calc = new BombayBeachHazardCurveCalc(db, 1000d, PARKFIELD_LOC,
//					10d, sourceNameConstr, false);
//			String sourceNameConstr = "puente";
			String sourceNameConstr = null;
//			BombayBeachHazardCurveCalc calc = new BombayBeachHazardCurveCalc(db, 1000d, PICO_RIVERA_LOC,
//					15d, sourceNameConstr, true);
//			BombayBeachHazardCurveCalc calc = new BombayBeachHazardCurveCalc(db, 1000d, YUCAIPA_LOC,
//					10d, sourceNameConstr, true);
			BombayBeachHazardCurveCalc calc = new BombayBeachHazardCurveCalc(db, 1000d, COYOTE_CREEK,
					10d, sourceNameConstr, true);
			
			try {
//				calc.writeSourceRupInfoFile("/home/kevin/CyberShake/bombay/rv_info.txt");
//				calc.writeSourceRupInfoFile("/home/kevin/CyberShake/parkfield/rv_info.txt");
//				calc.writeSourceRupInfoFile("/home/kevin/CyberShake/picoRivera/rv_info.txt");
//				calc.writeSourceRupInfoFile("/home/kevin/CyberShake/yucaipa/rv_info.txt");
				calc.writeSourceRupInfoFile("/home/kevin/CyberShake/coyote/rv_info.txt");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			calc.testProbRanges();
			
			System.exit(0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private void testProbRanges() {
		double minOrig = 1;
		double minNew = 1;
		double maxOrig = 0;
		double maxNew = 0;
		for (int i=0; i<sources.size(); i++) {
			int sourceID = sources.get(i);
			ArrayList<Integer> rupIDs = rups.get(i);
			for (int rupID : rupIDs) {
				double origProb = erf2db.getRuptureProb(ERFID, sourceID, rupID);
				ArrayList<Integer> rvs = rvMap.get(getKey(sourceID, rupID));
				if (rvs == null)
					continue;
				for (int rupVarID : rvs) {
//					double newProb = getModifiedProb(sourceID, rupID, rupVarID, origProb);
//					System.out.println("orig: " + origProb + ", mod: " + newProb);
//					if (origProb < minOrig)
//						minOrig = origProb;
//					if (origProb > maxOrig)
//						maxOrig = origProb;
//					if (newProb < maxNew)
//						minNew = newProb;
//					if (newProb > maxNew)
//						maxNew = newProb;
					break;
				}
			}
		}
		System.out.println("1 yr probability ranges:");
		System.out.println("Orig range: " + minOrig + " => " + maxOrig);
		System.out.println("New range: " + minNew + " => " + maxNew);
		System.out.println("1 day probability ranges:");
		System.out.println("Orig range: " + minOrig/365d + " => " + maxOrig/365d);
		System.out.println("New range: " + minNew/365d + " => " + maxNew/365d);
	}
	
	public ArrayList<Integer> getModVariations(int sourceID, int rupID) {
		return rvMap.get(getKey(sourceID, rupID));
	}
	
	public double getModifiedProb(int sourceID, int rupID, double originalProb) {
		return originalProb * increaseMultFactor;
//		return originalProb;
	}

}

