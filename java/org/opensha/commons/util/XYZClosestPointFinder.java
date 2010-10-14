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

package org.opensha.commons.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.opensha.commons.data.XYZ_DataSetAPI;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationUtils;

/**
 * This class takes the path to a Generic Mapping Tools style XYZ file and loads in all of the
 * locations and values. It then allows one to find the value from the file that is closest to
 * a given location.
 * 
 * @author kevin
 *
 */
public class XYZClosestPointFinder {
	ArrayList<Location> locs;
	ArrayList<Double> vals;
	
	public XYZClosestPointFinder(XYZ_DataSetAPI dataset, boolean xIsLat){
		locs = new ArrayList<Location>();
		vals = new ArrayList<Double>();
		ArrayList<Double> lats;
		ArrayList<Double> lons;
		if (xIsLat) {
			lats = dataset.getX_DataSet();
			lons = dataset.getY_DataSet();
		} else {
			lats = dataset.getY_DataSet();
			lons = dataset.getX_DataSet();
		}
		ArrayList<Double> zs = dataset.getZ_DataSet();
		for (int i=0; i<lats.size(); i++) {
			double lat = lats.get(i);
			double lon = lons.get(i);
			double z = zs.get(i);
			Location loc = new Location(lat, lon);
			locs.add(loc);
//			System.out.println("XYZ: adding loc: " + loc + ", val: " + z);
			vals.add(z);
		}
	}
	
	public XYZClosestPointFinder(String fileName) throws FileNotFoundException, IOException {
		
		ArrayList<String> lines = null;
		lines = FileUtils.loadFile(fileName);
		
		locs = new ArrayList<Location>();
		vals = new ArrayList<Double>();
		
		for (String line : lines) {
			line = line.trim();
			if (line.length() < 2)
				continue;
			StringTokenizer tok = new StringTokenizer(line);
			double lat = Double.parseDouble(tok.nextToken());
			double lon = Double.parseDouble(tok.nextToken());
			double val = Double.parseDouble(tok.nextToken());
			locs.add(new Location(lat, lon));
			vals.add(val);
		}
	}
	
	/**
	 * Returns the value at the closest location in the XYZ file (no matter how far
	 * away the closest point is).
	 * 
	 * @param loc
	 * @return
	 */
	public double getClosestVal(Location loc) {
		return getClosestVal(loc, Double.MAX_VALUE);
	}
	
	/**
	 * Returns the value at the closest location in the XYZ file (no matter how far
	 * away the closest point is).
	 * 
	 * @param lat
	 * @param lon
	 * @return
	 */
	public double getClosestVal(double lat, double lon) {
		return getClosestVal(new Location(lat, lon), Double.MAX_VALUE);
	}
	
	/**
	 * Returns the value at the closest location in the XYZ file within a given tolerance.
	 * 
	 * @param loc
	 * @param tolerance
	 * @return
	 */
	public double getClosestVal(Location pt1, double tolerance) {
		double closest = Double.MAX_VALUE;
		double closeVal = 0;
		
		for (int i=0; i<locs.size(); i++) {
			Location pt2 = locs.get(i);
			double val = vals.get(i);
//			double dist = Math.pow(val[0] - lat, 2) + Math.pow(val[1] - lon, 2);
			double dist = LocationUtils.horzDistanceFast(pt1, pt2);
			if (dist < closest) {
				closest = dist;
				closeVal = val;
			}
		}
		
		if (closest < tolerance)
			return closeVal;
		else
			return Double.NaN;
	}
	
	/**
	 * Returns the value at the closest location in the XYZ file within a given tolerance.
	 * 
	 * @param lat
	 * @param lon
	 * @param tolerance
	 * @return
	 */
	public double getClosestVal(double lat, double lon, double tolerance) {
		return getClosestVal(new Location(lat, lon), tolerance);
	}
}
