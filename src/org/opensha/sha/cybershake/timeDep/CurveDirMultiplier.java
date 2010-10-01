package org.opensha.sha.cybershake.timeDep;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.opensha.commons.data.function.ArbitrarilyDiscretizedFunc;
import org.opensha.commons.data.function.DiscretizedFuncAPI;
import org.opensha.sha.calc.hazardMap.CurveMultiplier;

public class CurveDirMultiplier {
	
	private String inputDir;
	private String outputDir;
	
	public CurveDirMultiplier(String inputDir, String outputDir) {
		this.inputDir = inputDir;
		if (!outputDir.endsWith(File.separator))
			outputDir += File.separator;
		File outputDirFile = new File(outputDir);
		if (!outputDirFile.exists())
			outputDirFile.mkdir();
		this.outputDir = outputDir;
	}
	
	public void multiplyCurves(double factor) throws FileNotFoundException, IOException {
		File curveDir = new File(inputDir);
		File[] dirList = curveDir.listFiles();
		
		// for each file in the list
		for(File curve : dirList){
			// make sure it's a subdirectory
			if (!curve.isDirectory() && curve.getName().endsWith(".txt")) {
				DiscretizedFuncAPI func =
					ArbitrarilyDiscretizedFunc.loadFuncFromSimpleFile(curve.getAbsolutePath());
				DiscretizedFuncAPI newFunc = CurveMultiplier.multiplyCurve(func, factor);
				String newFileName = outputDir + curve.getName();
				ArbitrarilyDiscretizedFunc.writeSimpleFuncFile(newFunc, newFileName);
				System.out.println("processed " + curve.getName() + " had " + func.getNum() + " has " + newFunc.getNum());
			}
		}
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		String baseDir = "/home/kevin/CyberShake/timeDep/";
		
		String inputDir = baseDir + "curves_30yr";
		String outputDir = baseDir + "curves_1yr_bad";
		
		double factor = 1d / 30d;
		
		CurveDirMultiplier mult = new CurveDirMultiplier(inputDir, outputDir);
		
		mult.multiplyCurves(factor);
	}

}
