package org.opensha.sha.cybershake.bombay;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;

import org.opensha.commons.data.function.ArbitrarilyDiscretizedFunc;
import org.opensha.sha.calc.hazardMap.HazardDataSetLoader;
import org.opensha.sha.cybershake.db.CybershakeSite;
import org.opensha.sha.cybershake.db.Cybershake_OpenSHA_DBApplication;
import org.opensha.sha.cybershake.db.DBAccess;
import org.opensha.sha.cybershake.db.Runs2DB;
import org.opensha.sha.cybershake.db.SiteInfo2DB;

public class MappingInputFileGenerator {
	
	private static double PROB_MULT = 1;

	Runs2DB runs2db;
	SiteInfo2DB sites2db;
	
	public MappingInputFileGenerator(DBAccess db) {
		this.runs2db = new Runs2DB(db);
		this.sites2db = new SiteInfo2DB(db);
	}
	
	public void writeFile(String inputDir, String outputFile, boolean isProbAt_IML, double level)
					throws FileNotFoundException, IOException {
		File dir = new File(inputDir);
		
		FileWriter fw = new FileWriter(outputFile);
		
		for (File file : dir.listFiles()) {
			if (file.isDirectory())
				continue;
			System.out.println("Processing '" + file.getName() + "'");
			StringTokenizer tok = new StringTokenizer(file.getName(), "_");
			tok.nextToken();
			int runID = Integer.parseInt(tok.nextToken());
			int siteID = runs2db.getSiteID(runID);
			CybershakeSite site = sites2db.getSiteFromDB(siteID);
			
			if (site.type_id == CybershakeSite.TYPE_TEST_SITE)
				continue;
			
			ArbitrarilyDiscretizedFunc curve =
						ArbitrarilyDiscretizedFunc.loadFuncFromSimpleFile(file.getAbsolutePath());
			
			double val = HazardDataSetLoader.getCurveVal(curve, isProbAt_IML, level);
			if (isProbAt_IML)
				val *= PROB_MULT;
			
			fw.write(site.lat + "\t" + site.lon + "\t" + val + "\t" + site.short_name + "\n");
		}
		fw.close();
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		MappingInputFileGenerator gen = new MappingInputFileGenerator(Cybershake_OpenSHA_DBApplication.db);
		
//		boolean isProbAt_IML = false;
//		double level = 0.0004;
		
//		PROB_MULT = 1d/365d;
		boolean isProbAt_IML = true;
		double level = 0.2;
//		double level = 0.5;
		
//		String baseDir = "/home/kevin/CyberShake/parkfield/";
//		
//		String inputDir;
//		
//		inputDir = baseDir + "origCurves";
//		gen.writeFile(inputDir, inputDir + ".txt", isProbAt_IML, level);
//		
//		inputDir = baseDir + "modCurves";
//		gen.writeFile(inputDir, inputDir + ".txt", isProbAt_IML, level);
		
//		String inputDir = "/home/kevin/CyberShake/timeDep/curves_1yr_bad";
		String inputDir = "/home/kevin/CyberShake/picoRivera/modCurves";
		gen.writeFile(inputDir, inputDir + ".txt", isProbAt_IML, level);
		
		System.exit(0);
	}

}
