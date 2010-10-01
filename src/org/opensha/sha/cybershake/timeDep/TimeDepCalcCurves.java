package org.opensha.sha.cybershake.timeDep;

import java.util.ArrayList;

import org.opensha.sha.cybershake.bombay.CalcCurves;
import org.opensha.sha.cybershake.db.Cybershake_OpenSHA_DBApplication;
import org.opensha.sha.cybershake.db.DBAccess;

public class TimeDepCalcCurves {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			DBAccess db = Cybershake_OpenSHA_DBApplication.db;
			ArrayList<Integer> ims = new ArrayList<Integer>();
			ims.add(21);
			CalcCurves calc = new CalcCurves(db, ims);
			
			String baseDir = "/home/kevin/CyberShake/timeDep/";
			
			String curveDir = baseDir + "curves/";
			
			TimeDependentRupProbMod probMod = new TimeDependentRupProbMod(2010);
			
			calc.calc(curveDir, probMod, null);
			db.destroy();
			System.exit(0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}

}
