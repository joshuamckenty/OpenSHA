package org.opensha.sha.cybershake.bombay;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.opensha.commons.data.function.ArbitrarilyDiscretizedFunc;
import org.opensha.sha.cybershake.db.CybershakeRun;
import org.opensha.sha.cybershake.db.Cybershake_OpenSHA_DBApplication;
import org.opensha.sha.cybershake.db.DBAccess;
import org.opensha.sha.cybershake.db.HazardCurve2DB;
import org.opensha.sha.cybershake.db.HazardDataset2DB;
import org.opensha.sha.cybershake.db.Runs2DB;

public class CustomCurveUploader {
	
	private HashMap<CybershakeRun, ArbitrarilyDiscretizedFunc> curves;
	private ArrayList<CybershakeRun> runs;
	
	private Runs2DB runs2db;
	private HazardDataset2DB hd2db;
	private HazardCurve2DB hc2db;
	
	public CustomCurveUploader(DBAccess db, String dir) throws IOException {
		runs2db = new Runs2DB(db);
		hd2db = new HazardDataset2DB(db);
		hc2db = new HazardCurve2DB(db);
		loadCurves(dir);
	}
	
	private void loadCurves(String dir) throws FileNotFoundException, IOException {
		curves = new HashMap<CybershakeRun, ArbitrarilyDiscretizedFunc>();
		runs = new ArrayList<CybershakeRun>();
		File dirFile = new File(dir);
		
		for (File file : dirFile.listFiles()) {
			if (!file.isFile())
				continue;
			if (!file.getName().endsWith(".txt"))
				continue;
			if (!file.getName().startsWith("run_"))
				continue;
			int runID = Integer.parseInt(file.getName().split("_")[1]);
			CybershakeRun run = runs2db.getRun(runID);
			ArbitrarilyDiscretizedFunc curve = 
				ArbitrarilyDiscretizedFunc.loadFuncFromSimpleFile(file.getAbsolutePath());
			System.out.println("loaded " + curve.getNum() + " points from "+file.getName());
			curves.put(run, curve);
			runs.add(run);
		}
	}
	
	public void insertCurves(int imTypeID, int probModelID, int timeSpanID, Date timeSpanStart) {
		CybershakeRun run0 = runs.get(0);
		int erfID = run0.getERFID();
		int rvScenID = run0.getRupVarScenID();
		int sgtVarID = run0.getSgtVarID();
		int velModelID = run0.getVelModelID();
		System.out.println("inserting "+curves.size()+" curves!");
		
		int hdID = hd2db.getDatasetID(erfID, rvScenID, sgtVarID, velModelID, probModelID, timeSpanID, timeSpanStart);
		if (hdID < 0) {
//			hdID = hd2db.addNewDataset(erfID, rvScenID, sgtVarID, velModelID, probModelID, timeSpanID, timeSpanStart);
		}
		if (hdID < 0)
			throw new RuntimeException("Couldn't create a Hazard_Dataset_ID!");
		
		System.out.println("HD dataset id: " + hdID);
		
		for (CybershakeRun run : runs) {
			ArbitrarilyDiscretizedFunc curve = curves.get(run);
			System.out.println("About to insert a curve with " + curve.getNum() + " points!");
			
			int curveID = hc2db.getHazardCurveID(run.getRunID(), hdID, imTypeID);
			if (curveID < 0) {
				System.out.println("we have a NEW hazard curve...inserting");
//				hc2db.insertHazardCurve(run.getRunID(), imTypeID, curve, hdID);
			} else {
				System.out.println("updating curve " + curveID);
				hc2db.replaceHazardCurve(curveID, curve);
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String dir = "/home/kevin/CyberShake/parkfield/origCurves";
//		String dir = "/home/kevin/CyberShake/timeDep/curves_1yr";
		
		int probModelID = 1;
		int timeSpanID = 2;
//		Date timeSpanStart = new GregorianCalendar(2010, 0, 1).getTime();
		Date timeSpanStart = null;
		
		int imTypeID = 21;
		
		DBAccess db = null;
		try {
			db = Cybershake_OpenSHA_DBApplication.getAuthenticatedDBAccess(true, true);
//			HazardCurve2DB hc2db = new HazardCurve2DB(db);
//			hc2db.deleteCurvesForDatasetID(9, 4);
			
			CustomCurveUploader up = new CustomCurveUploader(db, dir);
			
			up.insertCurves(imTypeID, probModelID, timeSpanID, timeSpanStart);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(2);
		} finally {
			if (db != null)
				db.destroy();
		}
		System.exit(0);
	}

}
