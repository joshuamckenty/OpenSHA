package org.opensha.sha.cybershake.bombay;

import java.io.IOException;
import java.util.ArrayList;

import org.opensha.sha.cybershake.calc.RuptureProbabilityModifier;
import org.opensha.sha.cybershake.calc.RuptureVariationProbabilityModifier;
import org.opensha.sha.cybershake.db.Cybershake_OpenSHA_DBApplication;
import org.opensha.sha.cybershake.db.DBAccess;

import scratch.kevin.cybershake.BulkCSCurveReplacer;

public class CalcCurves {
	
	private DBAccess db;
	
	private BulkCSCurveReplacer calc;
	
	public CalcCurves(DBAccess db, ArrayList<Integer> ims) {
		this.db = db;
		calc = new BulkCSCurveReplacer(db);
		calc.setRecalcIMs(ims);
	}
	
	public void calc(String dir, RuptureProbabilityModifier rupProbMod,
			RuptureVariationProbabilityModifier rupVarProbMod) throws IOException {
		calc.setRupRpobModifier(rupProbMod);
		calc.setRupVarProbModifier(rupVarProbMod);
		calc.recalculateAllCurves(dir);
	}
	
	public static void main(String args[]) {
		try {
			DBAccess db = Cybershake_OpenSHA_DBApplication.db;
			ArrayList<Integer> ims = new ArrayList<Integer>();
			ims.add(21);
			CalcCurves calc = new CalcCurves(db, ims);
			
//			String baseDir = "/home/kevin/CyberShake/bombay/";
//			String baseDir = "/home/kevin/CyberShake/parkfield/";
//			String baseDir = "/home/kevin/CyberShake/picoRivera/";
//			String baseDir = "/home/kevin/CyberShake/yucaipa/";
			String baseDir = "/home/kevin/CyberShake/coyote/";
			double increaseMultFactor = 1000;
			
			String origDir = baseDir + "origCurves/";
			String modDir = baseDir + "modCurves/";
			
			Div365ProbModifier div365 = new Div365ProbModifier();
			
//			calc.calc(origDir, div365, null);
//			BombayBeachHazardCurveCalc bombay = new BombayBeachHazardCurveCalc(db, increaseMultFactor,
//					BombayBeachHazardCurveCalc.PARKFIELD_LOC, 10d, "andreas", false);
//			BombayBeachHazardCurveCalc bombay = new BombayBeachHazardCurveCalc(db, increaseMultFactor,
//					BombayBeachHazardCurveCalc.PICO_RIVERA_LOC, 10d, null, false);
//			BombayBeachHazardCurveCalc bombay = new BombayBeachHazardCurveCalc(db, increaseMultFactor,
//					BombayBeachHazardCurveCalc.YUCAIPA_LOC, 10d, null, false);
			BombayBeachHazardCurveCalc bombay = new BombayBeachHazardCurveCalc(db, increaseMultFactor,
					BombayBeachHazardCurveCalc.COYOTE_CREEK, 10d, null, false);
			calc.calc(modDir, div365, bombay);
			db.destroy();
			System.exit(0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}

}
