package org.opensha.sha.cybershake.calc;

import java.sql.SQLException;
import java.util.ArrayList;

import org.opensha.commons.data.function.ArbDiscrEmpiricalDistFunc;
import org.opensha.commons.data.function.ArbitrarilyDiscretizedFunc;
import org.opensha.commons.data.function.DiscretizedFuncAPI;
import org.opensha.sha.cybershake.bombay.BombayBeachHazardCurveCalc;
import org.opensha.sha.cybershake.db.CybershakeIM;
import org.opensha.sha.cybershake.db.CybershakeRun;
import org.opensha.sha.cybershake.db.DBAccess;
import org.opensha.sha.cybershake.db.ERF2DB;
import org.opensha.sha.cybershake.db.ERF2DBAPI;
import org.opensha.sha.cybershake.db.PeakAmplitudesFromDB;
import org.opensha.sha.cybershake.db.PeakAmplitudesFromDBAPI;
import org.opensha.sha.cybershake.db.Runs2DB;
import org.opensha.sha.cybershake.db.SiteInfo2DB;
import org.opensha.sha.cybershake.db.SiteInfo2DBAPI;

public class HazardCurveComputation {


	private static final double CUT_OFF_DISTANCE = 200;
	private PeakAmplitudesFromDBAPI peakAmplitudes;
	private ERF2DBAPI erfDB;
	private SiteInfo2DBAPI siteDB;
	private Runs2DB runs2db;
	public static final double CONVERSION_TO_G = 980;
	
	private RuptureProbabilityModifier rupProbMod = null;
	private RuptureVariationProbabilityModifier rupVarProbMod = null;

	//	private ArrayList<ProgressListener> progressListeners = new ArrayList<ProgressListener>();

	public HazardCurveComputation(DBAccess db){
		peakAmplitudes = new PeakAmplitudesFromDB(db);
		erfDB = new ERF2DB(db);
		siteDB = new SiteInfo2DB(db);
		runs2db = new Runs2DB(db);
	}

	public void setRupProbModifier(RuptureProbabilityModifier rupProbMod) {
		this.rupProbMod = rupProbMod;
	}
	
	public void setRupVarProbModifier(RuptureVariationProbabilityModifier rupVarProbMod) {
		this.rupVarProbMod = rupVarProbMod;
	}

	/**
	 * 
	 * @returns the List of supported Peak amplitudes
	 */
	public ArrayList<CybershakeIM> getSupportedSA_PeriodStrings(){

		return peakAmplitudes.getSupportedIMs();
	}

	/**
	 * 
	 * @returns the List of supported Peak amplitudes for a given site, ERF ID, SGT Var ID, and Rup Var ID
	 */
	public ArrayList<CybershakeIM> getSupportedSA_PeriodStrings(int runID){

		return peakAmplitudes.getSupportedIMs(runID);
	}

	/**
	 * Computes the Hazard Curve at the given site 
	 * @param imlVals
	 * @param site
	 * @param erfName
	 * @param srcId
	 * @param rupId
	 * @param imType
	 */
	public DiscretizedFuncAPI computeDeterministicCurve(ArrayList<Double> imlVals, String site,int erfId, int sgtVariation, int rvid,
			int velModelID, int srcId,int rupId,CybershakeIM imType){
		CybershakeRun run = getRun(site, erfId, sgtVariation, rvid, velModelID);
		if (run == null)
			return null;
		else
			return computeDeterministicCurve(imlVals, run, srcId, rupId, imType);
	}

	private CybershakeRun getRun(String site, int erfID, int sgtVarID, int rupVarID, int velModelID) {
		int siteID = siteDB.getSiteId(site);
		ArrayList<CybershakeRun> runIDs = runs2db.getRuns(siteID, erfID, sgtVarID, rupVarID, velModelID, null, null, null, null);
		if (runIDs == null || runIDs.size() < 0)
			return null;
		return runIDs.get(0);
	}

	/**
	 * Computes the Hazard Curve at the given runID 
	 * @param imlVals
	 * @param runID
	 * @param srcId
	 * @param rupId
	 * @param imType
	 */
	public DiscretizedFuncAPI computeDeterministicCurve(ArrayList<Double> imlVals, int runID,
			int srcId,int rupId,CybershakeIM imType){
		CybershakeRun run = runs2db.getRun(runID);
		if (run == null)
			return null;
		else
			return computeDeterministicCurve(imlVals, run, srcId, rupId, imType);
	}


	/**
	 * Computes the Hazard Curve at the given run 
	 * @param imlVals
	 * @param run
	 * @param srcId
	 * @param rupId
	 * @param imType
	 */
	public DiscretizedFuncAPI computeDeterministicCurve(ArrayList<Double> xVals, CybershakeRun run,
			int srcId,int rupId,CybershakeIM imType){

		DiscretizedFuncAPI hazardFunc = new ArbitrarilyDiscretizedFunc();
		int numIMLs  = xVals.size();
		for(int i=0; i<numIMLs; ++i) hazardFunc.set((xVals.get(i)).doubleValue(), 1.0);

		int runID = run.getRunID();

		double qkProb = erfDB.getRuptureProb(run.getERFID(), srcId, rupId);
		ArrayList<Double> imVals;
		try {
			imVals = peakAmplitudes.getIM_Values(runID, srcId, rupId, imType);
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		if (rupProbMod != null)
			qkProb = rupProbMod.getModifiedProb(srcId, rupId, qkProb);
		handleRupture(xVals, imVals, hazardFunc, qkProb, srcId, rupId, rupVarProbMod);

		for(int j=0; j<numIMLs; ++j) 
			hazardFunc.set(hazardFunc.getX(j),(1-hazardFunc.getY(j)));

		return hazardFunc;
	}

	/**
	 * Computes the Hazard Curve at the given site 
	 * @param imlVals
	 * @param site
	 * @param erfName
	 * @param imType
	 */
	public DiscretizedFuncAPI computeHazardCurve(ArrayList<Double> imlVals, String site,String erfName,int sgtVariation, int rvid, int velModelID, CybershakeIM imType){
		int erfId = erfDB.getInserted_ERF_ID(erfName);
		System.out.println("for erfname: " + erfName + " found ERFID: " + erfId + "\n");
		return computeHazardCurve(imlVals, site, erfId, sgtVariation, rvid, velModelID, imType);
	}

	/**
	 * Computes the Hazard Curve at the given site 
	 * @param imlVals
	 * @param site
	 * @param erfName
	 * @param imType
	 */
	public DiscretizedFuncAPI computeHazardCurve(ArrayList<Double> imlVals, String site,int erfId,int sgtVariation, int rvid, int velModelID, CybershakeIM imType){
		CybershakeRun run = getRun(site, erfId, sgtVariation, rvid, velModelID);
		if (run == null)
			return null;
		else
			return computeHazardCurve(imlVals, run, imType);
	}

	/**
	 * Computes the Hazard Curve at the given site 
	 * @param imlVals
	 * @param site
	 * @param erfName
	 * @param imType
	 */
	public DiscretizedFuncAPI computeHazardCurve(ArrayList<Double> imlVals, int runID, CybershakeIM imType){
		CybershakeRun run = runs2db.getRun(runID);
		if (run == null)
			return null;
		else
			return computeHazardCurve(imlVals, run, imType);
	}

	/**
	 * Computes the Hazard Curve at the given site 
	 * @param imlVals
	 * @param site
	 * @param erfName
	 * @param imType
	 */
	public DiscretizedFuncAPI computeHazardCurve(ArrayList<Double> xVals, CybershakeRun run, CybershakeIM imType){
		DiscretizedFuncAPI hazardFunc = new ArbitrarilyDiscretizedFunc();
		int siteID = run.getSiteID();
		int erfID = run.getERFID();
		int runID = run.getRunID();
		int numIMLs  = xVals.size();
		for(int i=0; i<numIMLs; ++i) hazardFunc.set((xVals.get(i)).doubleValue(), 1.0);

		ArrayList<Integer> srcIdList = siteDB.getSrcIdsForSite(siteID, erfID);
		int numSrcs = srcIdList.size();
		for(int srcIndex =0;srcIndex<numSrcs;++srcIndex){
			//			updateProgress(srcIndex, numSrcs);
			System.out.println("Source " + srcIndex + " of " + numSrcs + ".");
			int srcId = srcIdList.get(srcIndex);
			ArrayList<Integer> rupIdList = siteDB.getRupIdsForSite(siteID, erfID, srcId);
			int numRupSize = rupIdList.size();
			for(int rupIndex = 0;rupIndex<numRupSize;++rupIndex){
				int rupId = rupIdList.get(rupIndex);
				double qkProb = erfDB.getRuptureProb(erfID, srcId, rupId);
				ArrayList<Double> imVals;
				try {
					imVals = peakAmplitudes.getIM_Values(runID, srcId, rupId, imType);
				} catch (SQLException e) {
					return null;
				}
				if (rupProbMod != null)
					qkProb = rupProbMod.getModifiedProb(srcId, rupId, qkProb);
				handleRupture(xVals, imVals, hazardFunc, qkProb, srcId, rupId, rupVarProbMod);
			}
		}

		for(int j=0; j<numIMLs; ++j) 
			hazardFunc.set(hazardFunc.getX(j),(1-hazardFunc.getY(j)));

		return hazardFunc;
	}
	
	private static void handleRupture(ArrayList<Double> xVals, ArrayList<Double> imVals,
			DiscretizedFuncAPI hazardFunc, double qkProb,
			int sourceID, int rupID, RuptureVariationProbabilityModifier rupProbVarMod) {
		if (rupProbVarMod == null) {
			// we don't have a rupture variation probability modifier
			handleRupture(xVals, imVals, hazardFunc, qkProb);
			return;
		}
		ArrayList<Integer> modRupIDs = rupProbVarMod.getModVariations(sourceID, rupID);
		if (modRupIDs == null || modRupIDs.size() == 0) {
			// we have a rup var prob mod, but it doesn't apply to this rupture
			handleRupture(xVals, imVals, hazardFunc, qkProb);
			return;
		}
		double modProb = rupProbVarMod.getModifiedProb(sourceID, rupID, qkProb);
		if (modRupIDs.size() == imVals.size()) {
			// we have a rup var prob mod and it is modifying EVERY RV...simple case
			handleRupture(xVals, imVals, hazardFunc, modProb);
			return;
		}
		// if we made it this far it's the complicated case (a mix of modified and unmodified)
		ArrayList<Double> noModVals = new ArrayList<Double>();
		ArrayList<Double> modVals = new ArrayList<Double>();
		
//		String modIDs = "";
//		for (int modRupID : modRupIDs) {
//			modIDs += " " + modRupID;
//		}
//		System.out.println("modRupIDs: " + modIDs);
		
		for (int rvID=0; rvID<imVals.size(); rvID++) {
			if (modRupIDs.contains(new Integer(rvID))) {
//				System.out.println("ADDED A MOD PROB!!!");
				modVals.add(imVals.get(rvID));
			} else {
				noModVals.add(imVals.get(rvID));
			}
		}
		if (modRupIDs.size() != modVals.size())
			throw new IllegalStateException("modRupIDs = " + modRupIDs.size() + "!= modVals = " + modVals.size());
//		System.out.println("src: " + sourceID + " rup: " + rupID + " " +
//				"mod rvs: " + modRupIDs.size() + " modVals: " + modVals.size() + " imVals: " + imVals.size());
		double modsToTotal = (double)modVals.size() / (double)imVals.size();
		handleRupture(xVals, noModVals, hazardFunc, qkProb * (1-modsToTotal));
		handleRupture(xVals, modVals, hazardFunc, modProb * modsToTotal);
	}
	
	private static void handleRupture(ArrayList<Double> xVals, ArrayList<Double> imVals,
			DiscretizedFuncAPI hazardFunc, double qkProb) {
		ArbDiscrEmpiricalDistFunc function = new ArbDiscrEmpiricalDistFunc();
		for (double val : imVals) {
			function.set(val/CONVERSION_TO_G,1);
		}
		setIMLProbs(xVals,hazardFunc, function.getNormalizedCumDist(), qkProb);
	}
	
	public static DiscretizedFuncAPI setIMLProbs( ArrayList<Double> imlVals,DiscretizedFuncAPI hazFunc,
			ArbitrarilyDiscretizedFunc normalizedFunc, double rupProb) {
		// find prob. for each iml value
		int numIMLs  = imlVals.size();
		for(int i=0; i<numIMLs; ++i) {
			double iml = imlVals.get(i);
			double prob=0;
			if(iml < normalizedFunc.getMinX()) prob = 0;
			else if(iml > normalizedFunc.getMaxX()) prob = 1;
			else prob = normalizedFunc.getInterpolatedY(iml);
//			else prob = normalizedFunc.getInterpolatedY_inLogYDomain(iml);
//			else prob = normalizedFunc.getInterpolatedY_inLogXLogYDomain(iml);
			hazFunc.set(i, hazFunc.getY(i)*Math.pow(1-rupProb,1-prob));
		}
		return hazFunc;
	}

	public PeakAmplitudesFromDBAPI getPeakAmpsAccessor() {
		return peakAmplitudes;
	}

	//    public void addProgressListener(ProgressListener listener) {
	//    	progressListeners.add(listener);
	//    }
	//	
	//    public void removeProgressListener(ProgressListener listener) {
	//    	progressListeners.remove(listener);
	//    }
	//    
	//    private void updateProgress(int current, int total) {
	//    	for (ProgressListener listener : progressListeners) {
	//    		listener.setProgress(current, total);
	//    	}
	//    }
}