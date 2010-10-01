package org.opensha.sha.cybershake.maps;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.opensha.commons.data.ArbDiscretizedXYZ_DataSet;
import org.opensha.commons.data.function.ArbitrarilyDiscretizedFunc;
import org.opensha.commons.data.function.DiscretizedFuncAPI;
import org.opensha.commons.data.region.CaliforniaRegions;
import org.opensha.commons.exceptions.GMT_MapException;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.Region;
import org.opensha.commons.mapping.gmt.elements.PSXYSymbol;
import org.opensha.commons.mapping.gmt.elements.TopographicSlopeFile;
import org.opensha.commons.mapping.gmt.elements.PSXYSymbol.Symbol;
import org.opensha.commons.util.cpt.CPT;
import org.opensha.sha.calc.hazardMap.HazardDataSetLoader;
import org.opensha.sha.cybershake.HazardCurveFetcher;
import org.opensha.sha.cybershake.bombay.BombayBeachHazardCurveCalc;
import org.opensha.sha.cybershake.bombay.ModProbConfig;
import org.opensha.sha.cybershake.bombay.ModProbConfigFactory;
import org.opensha.sha.cybershake.bombay.ScenarioBasedModProbConfig;
import org.opensha.sha.cybershake.db.CybershakeHazardCurveRecord;
import org.opensha.sha.cybershake.db.CybershakeRun;
import org.opensha.sha.cybershake.db.CybershakeSite;
import org.opensha.sha.cybershake.db.CybershakeSiteInfo2DB;
import org.opensha.sha.cybershake.db.Cybershake_OpenSHA_DBApplication;
import org.opensha.sha.cybershake.db.DBAccess;
import org.opensha.sha.cybershake.db.HazardCurve2DB;
import org.opensha.sha.cybershake.db.Runs2DB;
import org.opensha.sha.cybershake.maps.InterpDiffMap.InterpDiffMapType;
import org.opensha.sha.cybershake.maps.servlet.CS_InterpDiffMapServletAccessor;

public class HardCodedInterpDiffMapCreator {
	
	private static ArbDiscretizedXYZ_DataSet getMainScatter(boolean isProbAt_IML, double val, int imTypeID) {
		DBAccess db = Cybershake_OpenSHA_DBApplication.db;
		int erfID = 35;
		int rupVarScenarioID = 3;
		int sgtVarID = 5;
		int velModelID = 1;
		HazardCurveFetcher fetcher =
			new HazardCurveFetcher(db, erfID, rupVarScenarioID, sgtVarID, velModelID, imTypeID);
		ArrayList<CybershakeSite> sites = fetcher.getCurveSites();
		ArrayList<Double> vals = fetcher.getSiteValues(isProbAt_IML, val);
		
		ArbDiscretizedXYZ_DataSet scatterData = new ArbDiscretizedXYZ_DataSet();
		for (int i=0; i<sites.size(); i++) {
			CybershakeSite site = sites.get(i);
			double siteVal = vals.get(i);
			scatterData.addValue(site.lat, site.lon, siteVal);
		}
		return scatterData;
	}
	
	private static ArbDiscretizedXYZ_DataSet getCustomScatter(ModProbConfig config, int imTypeID,
			boolean isProbAt_IML, double val) throws FileNotFoundException, IOException {
		if (imTypeID != 21)
			throw new IllegalArgumentException("IM type must be 21 for custom map");
		if (!isProbAt_IML)
			throw new IllegalArgumentException("isProbAt_IML must be true for custom map");
//		String dir = "/home/kevin/CyberShake/interpDiffInputFiles/"+singleName+"/";
//		String fname;
//		if (mod)
//			fname = "mod_";
//		else
//			fname = "orig_";
//		fname += (float)val+"g_singleDay.txt";
//		String fileName = dir + fname;
//		File file = new File(fileName);
//		if (file.exists()) {
//			System.out.println("Loading scatter from: " + fileName);
//			return ArbDiscretizedXYZ_DataSet.loadXYZFile(fileName);
//		} else {
//			return loadCustomMapCurves(singleName, isProbAt_IML, val, mod);
//		}
		return loadCustomMapCurves(config, imTypeID, isProbAt_IML, val);
	}
	
	private static ArbDiscretizedXYZ_DataSet loadCustomMapCurves(ModProbConfig config, int imTypeID,
			boolean isProbAt_IML, double val) {
		DBAccess db = Cybershake_OpenSHA_DBApplication.db;
		
		int datasetID = config.getHazardDatasetID(35, 3, 5, 1, null);
		if (datasetID < 0)
			throw new RuntimeException("Couldn't get HC dataset id!");
		
		HazardCurveFetcher fetcher = new HazardCurveFetcher(db, datasetID, imTypeID);
		
		ArrayList<DiscretizedFuncAPI> curves = fetcher.getFuncs();
		ArrayList<CybershakeSite> sites = fetcher.getCurveSites();
		
		ArbDiscretizedXYZ_DataSet xyz = new ArbDiscretizedXYZ_DataSet();
		
		for (int i=0; i<curves.size(); i++) {
			DiscretizedFuncAPI curve = curves.get(i);
			CybershakeSite site = sites.get(i);
			
//			System.out.println("loaded curve with "+curve.getNum()+" vals");
			
			double zVal = HazardDataSetLoader.getCurveVal(curve, isProbAt_IML, val);
			xyz.addValue(site.lat, site.lon, zVal);
		}
		
		return xyz;
	}
	
	private static CybershakeRun getRun(int runID, ArrayList<CybershakeRun> runs) {
		for (CybershakeRun run : runs) {
			if (runID == run.getRunID())
				return run;
		}
		return null;
	}
	
	private static CybershakeSite getSite(int siteID, ArrayList<CybershakeSite> sites) {
		for (CybershakeSite site : sites) {
			if (siteID == site.id)
				return site;
		}
		return null;
	}

	private static ArbDiscretizedXYZ_DataSet loadCustomMapCurves(
			String singleName, boolean isProbAt_IML, double val, boolean mod)
			throws FileNotFoundException, IOException {
		String curveDir = "/home/kevin/CyberShake/"+singleName+"/";
		if (mod)
			curveDir += "mod";
		else
			curveDir += "orig";
		curveDir += "Curves";
		File curveDirFile = new File(curveDir);
		if (curveDirFile.exists()) {
			DBAccess db = Cybershake_OpenSHA_DBApplication.db;
			Runs2DB runs2db = new Runs2DB(db);
			ArrayList<CybershakeRun> runs = runs2db.getRuns();
			CybershakeSiteInfo2DB sites2db = new CybershakeSiteInfo2DB(db);
			ArrayList<CybershakeSite> sites = sites2db.getAllSitesFromDB();
			ArbDiscretizedXYZ_DataSet xyz = new ArbDiscretizedXYZ_DataSet();
			
			for (File curveFile : curveDirFile.listFiles()) {
				if (curveFile.isFile() && curveFile.getName().endsWith(".txt")
						&& curveFile.getName().startsWith("run_")) {
					ArbitrarilyDiscretizedFunc func =
						ArbitrarilyDiscretizedFunc.loadFuncFromSimpleFile(curveFile.getAbsolutePath());
//						System.out.println("Loaded func with "+func.getNum()+" pts from "+curveFile.getName());
					String[] split = curveFile.getName().split("_");
					int runID = Integer.parseInt(split[1]);
					CybershakeRun run = getRun(runID, runs);
					CybershakeSite site = getSite(run.getSiteID(), sites);
					if (site == null)
						throw new RuntimeException("run '"+runID+"' not found!");
					double zVal = HazardDataSetLoader.getCurveVal(func, isProbAt_IML, val);
					xyz.addValue(site.lat, site.lon, zVal);
				}
			}
			return xyz;
		} else {
			throw new FileNotFoundException("Couldn't locate file or curve dir for dataset '"+singleName+"'");
		}
	}
	
	private static ArbDiscretizedXYZ_DataSet loadBaseMap(boolean singleDay, boolean isProbAt_IML,
			double val, int imTypeID, String name) throws FileNotFoundException, IOException {
		int period;
		if (imTypeID == 11)
			period = 5;
		else if (imTypeID == 21)
			period = 3;
		else if (imTypeID == 26)
			period = 2;
		else
			throw new IllegalArgumentException("Unknown IM type id: " + imTypeID);
		String dir = "/home/kevin/CyberShake/baseMaps/"+name+"/";
		String fname = name+"_base_map_"+period+"sec_";
		if (isProbAt_IML) {
			fname += (float)val+"g";
		} else {
			if (val == 0.0004)
				fname += "2percent";
			else if (val == 0.002)
				fname += "10precent";
			else
				throw new IllegalArgumentException("Unown probability val: " + val);
		}
		if (singleDay)
			fname += "_singleDay";
		fname += "_hiRes.txt";
		String fileName = dir + fname;
		System.out.println("Loading basemap from: " + fileName);
		return ArbDiscretizedXYZ_DataSet.loadXYZFile(fileName);
	}
	
	private static PSXYSymbol getHypoSymbol(Region region, Location hypo) {
		if (hypo == null)
			return null;
		Location northWest = new Location(region.getMaxLat(), region.getMinLon());
		Location southEast = new Location(region.getMinLat(), region.getMaxLon());
		Region squareReg = new Region(northWest, southEast);
		if (!squareReg.contains(hypo)) {
			System.out.println("Hypocenter: "+hypo+"\nisn't within region: "+region);
			return null;
		}
		Point2D pt = new Point2D.Double(hypo.getLongitude(), hypo.getLatitude());
		double width = 0.4;
		double penWidth = 5;
		Color penColor = Color.WHITE;
		Color fillColor = Color.RED;
		return new PSXYSymbol(pt, Symbol.STAR, width, penWidth, penColor, fillColor);
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args){
		try {
			boolean logPlot = true;
			int imTypeID = 21;
			Double customMin = null;
			Double customMax = null;
			
			
			boolean isProbAt_IML = true;
			double val = 0.2;
			String baseMapName = "cb2008";
			ModProbConfig config = ModProbConfigFactory.getScenarioConfig(BombayBeachHazardCurveCalc.PARKFIELD_LOC);
			boolean probGain = true;
			String customLabel;
			if (probGain)
				customLabel = "Probability Gain";
			else
				customLabel = "POE "+(float)val+"G 3sec SA in 1 day";
			if (logPlot && !probGain) {
				customMin = -8.259081006598409;
//				customMax = -3.25;
				customMax = -2.5;
			}
			
//			ModProbConfig config = null;
//			boolean isProbAt_IML = false;
//			double val = 0.0004;
//			String baseMapName = "cb2008";
//			String customLabel = "3sec SA, 2% in 50 yrs";
//			boolean probGain = false;
			
			
			String addr = getMap(logPlot, imTypeID, customMin, customMax,
					isProbAt_IML, val, baseMapName, config, probGain,
					customLabel);
			
			System.out.println("Map address: " + addr);
			
			System.exit(0);
		} catch (Throwable t) {
			// TODO Auto-generated catch block
			t.printStackTrace();
			System.exit(1);
		}
	}
	
	protected static InterpDiffMapType[] normPlotTypes = null;
	protected static InterpDiffMapType[] gainPlotTypes = 
			{ InterpDiffMapType.INTERP_NOMARKS, InterpDiffMapType.INTERP_MARKS};
	
	protected static String getMap(boolean logPlot, int imTypeID,
			Double customMin, Double customMax, boolean isProbAt_IML,
			double val, String baseMapName, ModProbConfig config,
			boolean probGain, String customLabel) throws FileNotFoundException,
			IOException, ClassNotFoundException, GMT_MapException {
		boolean singleDay = config != null;
		double baseMapRes = 0.005;
		System.out.println("Loading basemap...");
		ArbDiscretizedXYZ_DataSet baseMap;
		if (!probGain) {
			baseMap = loadBaseMap(singleDay, isProbAt_IML, val, imTypeID, baseMapName);
			System.out.println("Basemap has " + baseMap.getX_DataSet().size() + " points");
		} else {
			baseMap = null;
		}
		
		System.out.println("Fetching curves...");
		ArbDiscretizedXYZ_DataSet scatterData;
		if (singleDay)
			scatterData = getCustomScatter(config, imTypeID, isProbAt_IML, val);
		else
			scatterData = getMainScatter(isProbAt_IML, val, imTypeID);
		
		System.out.println("Creating map instance...");
		GMT_InterpolationSettings interpSettings = GMT_InterpolationSettings.getDefaultSettings();
		Region region = new CaliforniaRegions.CYBERSHAKE_MAP_REGION();
		
		InterpDiffMapType[] mapTypes = normPlotTypes;
		
		CPT cpt = CPT.loadFromStream(HardCodedInterpDiffMapCreator.class.getResourceAsStream(
				"/resources/cpt/MaxSpectrum2.cpt"));
		
		ArbDiscretizedXYZ_DataSet refScatter = null;
		if (probGain) {
			ModProbConfig timeIndepModProb = ModProbConfigFactory.getModProbConfig(1);
			refScatter = getCustomScatter(timeIndepModProb, imTypeID, isProbAt_IML, val);
			scatterData = ProbGainCalc.calcProbGain(refScatter, scatterData);
			mapTypes = gainPlotTypes;;
		}
		
		InterpDiffMap map = new InterpDiffMap(region, baseMap, baseMapRes, cpt, scatterData, interpSettings, mapTypes);
		map.setCustomLabel(customLabel);
		map.setTopoResolution(TopographicSlopeFile.CA_THREE);
		map.setLogPlot(logPlot);
		map.setDpi(300);
		map.setXyzFileName("base_map.xyz");
		map.setCustomScaleMin(customMin);
		map.setCustomScaleMax(customMax);
		
		Location hypo = null;
		if (config != null && config instanceof ScenarioBasedModProbConfig) {
			hypo = ((ScenarioBasedModProbConfig)config).getHypocenter();
		}
		PSXYSymbol symbol = getHypoSymbol(region, hypo);
		if (symbol != null) {
			map.addSymbol(symbol);
		}
		
		String metadata = "isProbAt_IML: " + isProbAt_IML + "\n" +
						"val: " + val + "\n" +
						"singleDay: " + singleDay + "\n" +
						"imTypeID: " + imTypeID + "\n";
		
		System.out.println("Making map...");
		return CS_InterpDiffMapServletAccessor.makeMap(null, map, metadata);
	}

}
