/*******************************************************************************
 * Copyright 2009 OpenSHA.org in partnership with
 * the Southern California Earthquake Center (SCEC, http://www.scec.org)
 * at the University of Southern California and the UnitedStates Geological
 * Survey (USGS; http://www.usgs.gov)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.opensha.sha.cybershake.plot.interpMap;

import java.io.IOException;
import java.util.ArrayList;

import org.opensha.commons.util.XYZClosestPointFinder;
import org.opensha.sha.cybershake.HazardCurveFetcher;
import org.opensha.sha.cybershake.db.CybershakeSite;
import org.opensha.sha.cybershake.db.Cybershake_OpenSHA_DBApplication;
import org.opensha.sha.cybershake.db.DBAccess;
import org.opensha.sha.cybershake.plot.HazardCurvePlotter;
import org.opensha.sha.cybershake.plot.HazardCurves2XYZ;

/**
 * This class takes the CyberShake hazard curves, and computes the difference with a base map
 * at each point. The closest point in the base map is taken.
 * 
 * @author kevin
 *
 */
public class HazardCurvePointDifferences {
	
	private XYZClosestPointFinder xyz;
	private CyberShakeValueProvider provider;
	
	public HazardCurvePointDifferences(CyberShakeValueProvider provider, String comparisonFile) throws IOException {
		this.provider = provider;
		xyz = new XYZClosestPointFinder(comparisonFile);
	}
	
	public ArrayList<Double> getSiteDifferenceValues() {
		ArrayList<Double> csVals = provider.getVals();
		ArrayList<CybershakeSite> sites = provider.getSites();
		
		ArrayList<Double> diff = new ArrayList<Double>();
		
		for (int i=0; i<sites.size(); i++) {
			CybershakeSite site = sites.get(i);
			
			double csVal = csVals.get(i);
			double compVal = xyz.getClosestVal(site.lat, site.lon);
			
			diff.add(csVal - compVal);
		}
		
		return diff;
	}
	
	public void writeXYZ(String fileName) throws IOException {
		ArrayList<Double> vals = getSiteDifferenceValues();
		HazardCurves2XYZ.writeXYZ(fileName, provider.getSites(), vals, null);
	}
	
	public void writeLabelsFile(String labelsFile) throws IOException {
		HazardCurves2XYZ.writeLabelsFile(labelsFile, provider.getSites());
	}
	
	public static void main(String args[]) {
		try {
			String compFile = null;
			String outFile = null;
			String labelsFile = null;
			String types = "";
			
			int imTypeID = 21;
			
			boolean isProbAt_IML = false;
			double level = 0.0004;
			
			String inputFile = null;
			
			if (args.length == 0) {
				System.err.println("WARNING: Running from debug mode!");
				compFile = "/home/kevin/CyberShake/baseMaps/cb2008/cb2008_base_map_2percent_hiRes_0.005.txt";
				outFile = "/home/kevin/CyberShake/interpolatedDiffMap/diffs.txt";
				labelsFile = "/home/kevin/CyberShake/interpolatedDiffMap/markers.txt";
			} else if (args.length >= 3 && args.length <= 7) {
				compFile = args[0];
				outFile = args[1];
				labelsFile = args[2];
				imTypeID = Integer.parseInt(args[3]);
				if (args.length == 5) {
					types = args[4];
				} else if (args.length == 6) {
					isProbAt_IML = Boolean.parseBoolean(args[4]);
					level = Double.parseDouble(args[5]);
				} else if (args.length == 7) {
					types = args[4];
					isProbAt_IML = Boolean.parseBoolean(args[5]);
					level = Double.parseDouble(args[6]);
				}
			} else {
				System.err.println("USAGE: HazardCurvePointDifferences base_map_file outFile labelsFile imTypeID [types] " +
						"[isProbAt_IML level]");
				System.exit(1);
			}
			
			System.out.println("*****************************");
			System.out.println("Basemap: " + compFile);
			System.out.println("Diff output: " + outFile);
			System.out.println("Labels: " + labelsFile);
			System.out.println("IM Type ID: " + imTypeID);
			System.out.println("Types: " + types);
			if (isProbAt_IML) {
				System.out.println("Prob of exceeding IML of:  " + level);
			} else {
				System.out.println("IML at Prob of: " + level);
			}
			System.out.println("*****************************\n");
			
			ArrayList<Integer> typeIDs = null;
			
			if (types.length() > 0) {
				if (types.startsWith("-F")) {
					inputFile = types.substring(2);
					types = "";
				} else {
					ArrayList<String> idSplit = HazardCurvePlotter.commaSplit(types);
					typeIDs = new ArrayList<Integer>();
					for (String idStr : idSplit) {
						int id = Integer.parseInt(idStr);
						typeIDs.add(id);
						System.out.println("Added site type: " + id);
					}
				}
			}
			
			CyberShakeValueProvider prov;
			if (inputFile != null) {
				prov = new CyberShakeValueProvider(inputFile);
			} else {
				DBAccess db = Cybershake_OpenSHA_DBApplication.db;
				HazardCurveFetcher fetcher = new HazardCurveFetcher(db, 35, 3, 5, 1, imTypeID);
				
				prov = new CyberShakeValueProvider(fetcher, typeIDs, isProbAt_IML, level);
			}
			HazardCurvePointDifferences diff = new HazardCurvePointDifferences(prov, compFile);
			
			diff.writeXYZ(outFile);
			diff.writeLabelsFile(labelsFile);
			
			System.exit(0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}

}
