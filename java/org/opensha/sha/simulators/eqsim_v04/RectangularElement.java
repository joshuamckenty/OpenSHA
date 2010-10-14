package org.opensha.sha.simulators.eqsim_v04;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.opensha.commons.geo.Location;
import org.opensha.sha.earthquake.FocalMechanism;
import org.opensha.sha.faultSurface.FourPointEvenlyGriddedSurface;

/**
 * This uses "id" rather than "index" to avoid confusion from the fact that our indexing 
 * starts from zero (and that in EQSIM docs starts from 1)
 * @author field
 *
 */
public class RectangularElement {
	
	// these are the official variables
	private int id;	// this is referred to as "index" in the EQSIM documentation
	private Vertex[] vertices;
	private FocalMechanism focalMechanism;
	private double slipRate;
	private double aseisFactor;
	boolean perfect;

	// these are other variable (e.g., used by Ward's simulator)
	private String sectionName;
	private int sectionID;	
	private int faultID;
	private int numAlongStrike;
	private int numDownDip;
	
	/**
	 * This creates the RectangularElement from the supplied information.  Note that this assumes
	 * the vertices correspond to a perfect rectangle.
	 * @param id - an integer for identification (referred to as "index" in the EQSIM documentation)
	 * @param vertices - a list of 4 vertices, where the order is as follows as viewed 
	 *                   from the positive side of the fault: 0th is top left, 1st is lower left,
	 *                   2nd is lower right, and 3rd is upper right (counter clockwise)
	 * @param sectionName - the name of the fault section that this element is on
	 * @param faultID - the ID of the original fault (really needed?)
	 * @param sectionID - the ID of the associated fault section
	 * @param numAlongStrike - index along strike on the fault section
	 * @param numDownDip - index down dip
	 * @param slipRate - slip rate (meters/year; note that this is different from the EQSIM convention (which is m/s))
	 * @param aseisFactor - aseismicity factor
	 * @param focalMechanism - this contains the strike, dip, and rake
	 */
	public RectangularElement(int id, Vertex[] vertices, String sectionName,
			int faultID, int sectionID, int numAlongStrike, int numDownDip,
			double slipRate, double aseisFactor, FocalMechanism focalMechanism, 
			boolean perfectRect) {

		if(vertices.length !=4 )
			throw new RuntimeException("RectangularElement: vertices.length should equal 4");
		
		this.id = id;
		this.vertices = vertices;
		this.sectionName = sectionName;
		this.faultID = faultID;
		this.sectionID = sectionID;
		this.numAlongStrike = numAlongStrike;
		this.numDownDip = numDownDip;
		this.slipRate = slipRate;
		this.aseisFactor = aseisFactor;
		this.focalMechanism = focalMechanism;
		this.perfect = perfectRect;
		
		this.perfect = true;
		
	}
	
	public FourPointEvenlyGriddedSurface getGriddedSurface() {
		return new FourPointEvenlyGriddedSurface(vertices[0],vertices[1],vertices[2],vertices[3]);
	}
	
	/**
	 * This returns the section name for now
	 * @return
	 */
	public String getName() {
		return sectionName;
	}

	public int getID() {
		return id;
	}

	public Vertex[] getVertices() {
		return vertices;
	}
	
	public FocalMechanism getFocalMechanism() {
		return focalMechanism;
	}
	
	public double getSlipRate() {
		return slipRate;
	}

	public double getAseisFactor() {
		return aseisFactor;
	}

	/**
	 * This tells whether it's a perfect rectangle
	 * @return
	 */
	public boolean isPerfect() {
		return perfect;
	}
	
	public int getPerfectInt() {
		if(perfect) return 1;
		else return 0;
	}
	
	public String getSectionName() {
		return sectionName;
	}

	public int getSectionID() {
		return sectionID;
	}
	
	public int getFaultID() {
		return faultID;
	}
	
	public int getNumAlongStrike() {
		return numAlongStrike;
	}

	public int getNumDownDip() {
		return numDownDip;
	}

	
	public String toWardFormatLine() {
		// this is Steve's ordering
		Location newTop1 = vertices[0];
		Location newTop2 = vertices[3];
		Location newBot1 = vertices[1];;
		Location newBot2 = vertices[2];;
		String line = id + "\t"+
			numAlongStrike + "\t"+
			numDownDip + "\t"+
			faultID + "\t"+
			sectionID + "\t"+
			(float)slipRate + "\t"+
			"NA" + "\t"+  // elementStrength not available
			(float)focalMechanism.getStrike() + "\t"+
			(float)focalMechanism.getDip() + "\t"+
			(float)focalMechanism.getRake() + "\t"+
			(float)newTop1.getLatitude() + "\t"+
			(float)newTop1.getLongitude() + "\t"+
			(float)newTop1.getDepth()*-1000 + "\t"+
			(float)newBot1.getLatitude() + "\t"+
			(float)newBot1.getLongitude() + "\t"+
			(float)newBot1.getDepth()*-1000 + "\t"+
			(float)newBot2.getLatitude() + "\t"+
			(float)newBot2.getLongitude() + "\t"+
			(float)newBot2.getDepth()*-1000 + "\t"+
			(float)newTop2.getLatitude() + "\t"+
			(float)newTop2.getLongitude() + "\t"+
			(float)newTop2.getDepth()*-1000 + "\t"+
			sectionName;
		return line;
	}
	


}
