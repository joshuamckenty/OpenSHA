package org.opensha.sha.cybershake.maps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.opensha.commons.exceptions.GMT_MapException;
import org.opensha.commons.geo.Location;
import org.opensha.commons.util.FileUtils;
import org.opensha.sha.cybershake.bombay.ModProbConfig;
import org.opensha.sha.cybershake.bombay.ModProbConfigFactory;
import org.opensha.sha.cybershake.bombay.ScenarioBasedModProbConfig;
import org.opensha.sha.cybershake.maps.InterpDiffMap.InterpDiffMapType;

public class PosterImageGen {

	private static void saveCurves(String webAddr, String mainDir, String name, boolean base) throws IOException {
		if (!webAddr.endsWith("/"))
			webAddr += "/";
		if (base)
			webAddr += "basemap";
		else
			webAddr += "interpolated";
		String pngAddr = webAddr + ".300.png";
		String psAddr = webAddr + ".ps";
		
		File pngFile = new File(mainDir + File.separator + name + ".png");
		File psFile = new File(mainDir + File.separator + name + ".ps");
		
		FileUtils.downloadURL(pngAddr, pngFile);
		FileUtils.downloadURL(psAddr, psFile);
	}
	
	private static void writeLocsFile(String fileName) throws IOException {
		FileWriter fw = new FileWriter(fileName);
		
		for (ModProbConfig config : ModProbConfigFactory.modProbConfigs.values()) {
			if (config instanceof ScenarioBasedModProbConfig) {
				Location hypo = ((ScenarioBasedModProbConfig)config).getHypocenter();
				fw.write((float)hypo.getLatitude() + " " + (float)hypo.getLongitude() + " " + config.getName() + "\n");
			}
		}
		
		fw.close();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String mainDir = "/home/kevin/CyberShake/oef/images";
		new File(mainDir).mkdirs();
		
		boolean logPlot = true;
		int imTypeID = 21;
		boolean isProbAt_IML = true;
		double val = 0.2;
		
		boolean gainOnly = false;
		
		String baseMapName = "cb2008";
		
		Double normCustomMin = -8.259081006598409;
		Double normCustomMax = -2.5;
		
		Double gainCustomMin = 0.0;
		Double gainCustomMax = 2.2;
		
		String normLabel = "POE "+(float)val+"G 3sec SA in 1 day";
		String gainLabel = "Probability Gain";
		
		InterpDiffMapType[] types = { InterpDiffMapType.INTERP_NOMARKS };
		HardCodedInterpDiffMapCreator.gainPlotTypes = types;
		HardCodedInterpDiffMapCreator.normPlotTypes = types;

		try {
			writeLocsFile(mainDir + "/locs.txt");
			System.exit(0);
			for (ModProbConfig config : ModProbConfigFactory.modProbConfigs.values()) {
				String name = config.getName().replaceAll(" ", "");
				if (!gainOnly) {
					String normAddr = 
						HardCodedInterpDiffMapCreator.getMap(logPlot, imTypeID, normCustomMin, normCustomMax,
								isProbAt_IML, val, baseMapName, config, false, normLabel);
					saveCurves(normAddr, mainDir, name, false);
				}
				if (config instanceof ScenarioBasedModProbConfig) {
					String gainAddr = 
						HardCodedInterpDiffMapCreator.getMap(logPlot, imTypeID, gainCustomMin, gainCustomMax,
								isProbAt_IML, val, baseMapName, config, true, gainLabel);
					saveCurves(gainAddr, mainDir, name+"_gain", false);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(2);
		}
		System.exit(0);
	}

}
