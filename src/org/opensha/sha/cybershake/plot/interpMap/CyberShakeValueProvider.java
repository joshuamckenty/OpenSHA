package org.opensha.sha.cybershake.plot.interpMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.opensha.commons.util.FileUtils;
import org.opensha.sha.cybershake.HazardCurveFetcher;
import org.opensha.sha.cybershake.db.CybershakeSite;

public class CyberShakeValueProvider {
	
	private ArrayList<CybershakeSite> sites;
	private ArrayList<Double> vals;
	
	public CyberShakeValueProvider(HazardCurveFetcher fetcher, ArrayList<Integer> types,
			boolean isProbAt_IML, double level) {
		ArrayList<Double> csVals = fetcher.getSiteValues(isProbAt_IML, level);
		ArrayList<CybershakeSite> origSites = fetcher.getCurveSites();
		
		sites = new ArrayList<CybershakeSite>();
		vals = new ArrayList<Double>();
		
		for (int i=0; i<origSites.size(); i++) {
			CybershakeSite site = origSites.get(i);
			
			if (types != null && !types.contains(site.type_id))
				continue;
			
			sites.add(site);
			
			double csVal = csVals.get(i);
			vals.add(csVal);
		}
	}
	
	public CyberShakeValueProvider(String inputFile) throws FileNotFoundException, IOException {
		sites = new ArrayList<CybershakeSite>();
		vals = new ArrayList<Double>();
		for (String line : FileUtils.loadFile(inputFile, true)) {
			line = line.trim();
			
			StringTokenizer tok = new StringTokenizer(line);
			Double lat = Double.parseDouble(tok.nextToken());
			Double lon = Double.parseDouble(tok.nextToken());
			Double val = Double.parseDouble(tok.nextToken());
			String name = tok.nextToken();
			
			CybershakeSite site = new CybershakeSite(lat, lon, name, name);
			
			sites.add(site);
			vals.add(val);
		}
		System.out.println("Loaded " + vals.size() + " vals from '" + inputFile + "'");
	}
	
	public ArrayList<Double> getVals() {
		return vals;
	}
	
	public ArrayList<CybershakeSite> getSites() {
		return sites;
	}

}
