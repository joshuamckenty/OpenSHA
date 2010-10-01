package org.opensha.sha.simulators.eqsim_v04;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ListIterator;
import java.util.StringTokenizer;

import org.apache.commons.math.MathException;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.opensha.commons.calc.FaultMomentCalc;
import org.opensha.commons.calc.MomentMagCalc;
import org.opensha.commons.calc.magScalingRelations.magScalingRelImpl.Ellsworth_B_WG02_MagAreaRel;
import org.opensha.commons.calc.magScalingRelations.magScalingRelImpl.HanksBakun2002_MagAreaRel;
import org.opensha.commons.calc.magScalingRelations.magScalingRelImpl.Shaw_2007_MagAreaRel;
import org.opensha.commons.calc.magScalingRelations.magScalingRelImpl.WC1994_MagAreaRelationship;
import org.opensha.commons.data.NamedObjectComparator;
import org.opensha.commons.data.function.ArbDiscrEmpiricalDistFunc;
import org.opensha.commons.data.function.ArbitrarilyDiscretizedFunc;
import org.opensha.commons.data.function.DiscretizedFuncAPI;
import org.opensha.commons.data.function.EvenlyDiscretizedFunc;
import org.opensha.commons.data.function.XY_DataSet;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationUtils;
import org.opensha.commons.geo.LocationVector;
import org.opensha.commons.geo.PlaneUtils;
import org.opensha.commons.util.FileUtils;
import org.opensha.refFaultParamDb.vo.FaultSectionPrefData;
import org.opensha.sha.earthquake.FocalMechanism;
import org.opensha.sha.earthquake.calc.recurInterval.BPT_DistCalc;
import org.opensha.sha.earthquake.calc.recurInterval.LognormalDistCalc;
import org.opensha.sha.earthquake.rupForecastImpl.WGCEP_UCERF_2_Final.data.finalReferenceFaultParamDb.DeformationModelPrefDataFinal;
import org.opensha.sha.faultSurface.EvenlyGridCenteredSurface;
import org.opensha.sha.faultSurface.StirlingGriddedSurface;
import org.opensha.sha.gui.controls.PlotColorAndLineTypeSelectorControlPanel;
import org.opensha.sha.gui.infoTools.GraphiWindowAPI_Impl;
import org.opensha.sha.gui.infoTools.PlotCurveCharacterstics;
import org.opensha.sha.imr.param.PropagationEffectParams.DistanceRupParameter;
import org.opensha.sha.magdist.ArbIncrementalMagFreqDist;

/**
 * This class reads and writes various files, as well as doing some analysis of simulator results.
 * 
 * Note that this class could extend some class representing the "Container" defined in EQSIM, but it's not clear that generality is necessary.
 * 
 * Things to keep in mind:
 * 
 * Indexing in the EQSIM files starts from 1, not zero.  Therefore, here we refer to their "indices" as IDs to avoid confusion.
 * For example, the ID for the ith RectangularElement in rectElementsList (rectElementsList.getID()) is one less than i 
 * (same goes for other lists).
 * 
 * All units in EQSIM files are SI
 * 
 * Note that slip rates in EQSIM files are in units of m/s, whereas we convert these to m/yr internally here.
 * 
 * We assume the first vertex in each element here is the first on the upper edge 
 * (traceFlag=2 if the element is at the top); this is not checked for explicitly
 * 
 * @author field
 *
 */
public class General_EQSIM_Tools {

	protected final static boolean D = false;  // for debugging
	
	private ArrayList<FaultSectionPrefData> allFaultSectionPrefData;
	ArrayList<RectangularElement> rectElementsList;
	ArrayList<Vertex> vertexList;
	ArrayList<ArrayList<RectangularElement>> rectElementsListForSections;
	ArrayList<ArrayList<Vertex>> vertexListForSections;
	ArrayList<String> namesOfSections;
	ArrayList<Integer> faultIDs_ForSections;
	ArrayList<EQSIM_Event> eventList;
	
	final static String GEOM_FILE_SIG = "EQSim_Input_Geometry_2";	// signature of the geometry file
	final static int GEOM_FILE_SPEC_LEVEL = 2;
	final static String EVENT_FILE_SIG = "EQSim_Output_Event_2";
	final static int EVENT_FILE_SPEC_LEVEL = 2;
	final static double SECONDS_PER_YEAR = 365*24*60*60;


	/**
	 * This constructor makes the list of RectangularElements from a UCERF2 deformation model
	 * @param deformationModelID	- D2.1 = 82; D2.2 = 83; D2.3 = 84; D2.4 = 85; D2.5 = 86; D2.6 = 87
	 * @param aseisReducesArea		- whether or not to reduce area (otherwise reduces slip rate?)
	 * @param maxDiscretization		- the maximum element size
	 */
	public General_EQSIM_Tools(int deformationModelID, boolean aseisReducesArea,
			double maxDiscretization) {
		
		mkElementsFromUCERF2_DefMod(deformationModelID, aseisReducesArea, maxDiscretization);
	}
	
	
	/**
	 * This constructor loads the data from an EQSIM_v04 Geometry file
	 * @param filePathName		 - full path and file name
	 */
	public General_EQSIM_Tools(String filePathName) {
		
		ArrayList<String> lines=null;
		try {
			lines = FileUtils.loadJarFile(filePathName);
			System.out.println("Number of file lines: "+lines.size()+" (in "+filePathName+")");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		loadFromEQSIMv04_GeometryLines(lines);
	}
	
	
	/**
	 * This constructor loads the data from either an EQSIM_v04 Geometry file (formatType=0)
	 * or from Steve Ward's format (formatType=1).
	 * @param filePathName		 - full path and file name
	 * @param formatType		 - set as 0 for EQSIM_v04 Geometry file or 1 for Steve Ward's format
	 */
	public General_EQSIM_Tools(String filePathName, int formatType) {
		System.out.println(filePathName);
		ArrayList<String> lines=null;
		try {
			lines = FileUtils.loadJarFile(filePathName);
			System.out.println("Number of file lines: "+lines.size()+" (in "+filePathName+")");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(formatType==0)
			loadFromEQSIMv04_GeometryLines(lines);
		else if (formatType==1)
			loadFromSteveWardLines(lines);
		else
			throw new RuntimeException("format type not supported");
	}

	
	/**
	 * This constructor loads the data from an EQSIM_v04 Geometry file
	 * @param url		 - full URL path name
	 */
	public General_EQSIM_Tools(URL url) {
		
		ArrayList<String> lines=null;
		
		try {
			lines = FileUtils.loadFile(url);
			System.out.println("Number of file lines: "+lines.size()+" (in "+url+")");
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		loadFromEQSIMv04_GeometryLines(lines);
	}
	
	public ArrayList<String> getSectionsNameList() {
		return namesOfSections;
	}

	public void read_EQSIMv04_EventsFile(URL url) {
		ArrayList<String> lines=null;
		try {
			lines = FileUtils.loadFile(url);
			System.out.println("Number of file lines: "+lines.size()+" (in "+url+")");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		read_EQSIMv04_EventsFile(lines);
	}
	
	private void read_EQSIMv04_EventsFile(String filePathName) {

		ArrayList<String> lines=null;
		try {
			lines = FileUtils.loadJarFile(filePathName);
			System.out.println("Number of file lines: "+lines.size()+" (in "+filePathName+")");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		read_EQSIMv04_EventsFile(lines);
	}
	
	private void read_EQSIMv04_EventsFile(ArrayList<String> lines) {
		
		ListIterator<String> linesIterator = lines.listIterator();
		
		// get & check first line (must be the signature line)
		String line = linesIterator.next();
		StringTokenizer tok = new StringTokenizer(line);
		int kindOfLine = Integer.parseInt(tok.nextToken());
		String fileSignature = tok.nextToken();
		int fileSpecLevel = Integer.parseInt(tok.nextToken());
//		if(kindOfLine != 101 || !fileSignature.equals(EVENT_FILE_SIG) || fileSpecLevel < EVENT_FILE_SPEC_LEVEL)
		if(kindOfLine != 101 || !fileSignature.equals(EVENT_FILE_SIG))
			throw new RuntimeException("wrong type of event input file; your first file line is:\n\n\t"+line+"\n");

		eventList = new ArrayList<EQSIM_Event>();
		EQSIM_Event currEvent = null;
		EventRecord evRec = new EventRecord(); // this one never used, but created to compile
		int numEventRecs=0;
		while (linesIterator.hasNext()) {
			line = linesIterator.next();
			tok = new StringTokenizer(line);
			kindOfLine = Integer.parseInt(tok.nextToken());
			if(kindOfLine ==200) {	// event record
				evRec = new EventRecord(line);
				numEventRecs+=1;
				
				// check whether this is the first event in the list
				if(eventList.size() == 0) {
					EQSIM_Event event = new EQSIM_Event(evRec);
					eventList.add(event);
					currEvent = event;
				}
				else { // check whether this is part of currEvent (same ID)
					if(currEvent.isSameEvent(evRec)) {
						currEvent.add(evRec);
					}
					else { // it's a new event
						EQSIM_Event event = new EQSIM_Event(evRec);
						eventList.add(event);
						currEvent = event;
					}
				}
			}
			else if(kindOfLine ==201) {	// Slip map record
				evRec.addSlipAndElementData(line); // add to the last event record created
			}
		}
		
		System.out.println("Num Events = "+this.eventList.size()+"\tNum Event Records = "+numEventRecs);
	}
	
	
	
	
	/**
	 * This creates the data from lines from an EQSIM Geometry file
	 * @param lines
	 * @return
	 */
	private void loadFromEQSIMv04_GeometryLines(ArrayList<String> lines) {
		
		// note that the following lists have indices that start from 0
		rectElementsList = new ArrayList<RectangularElement>();
		vertexList = new ArrayList<Vertex>();
		rectElementsListForSections = new ArrayList<ArrayList<RectangularElement>> ();
		vertexListForSections = new ArrayList<ArrayList<Vertex>>();
		namesOfSections = new ArrayList<String>();
		faultIDs_ForSections = new ArrayList<Integer>();
		
		ListIterator<String> linesIterator = lines.listIterator();
		
		// get & check first line (must be the signature line)
		String line = linesIterator.next();
		StringTokenizer tok = new StringTokenizer(line);
		int kindOfLine = Integer.parseInt(tok.nextToken());
		String fileSignature = tok.nextToken();
		int fileSpecLevel = Integer.parseInt(tok.nextToken());
		if(kindOfLine != 101 || !fileSignature.equals(GEOM_FILE_SIG) || fileSpecLevel < GEOM_FILE_SPEC_LEVEL)
			throw new RuntimeException("wrong type of input file");
		
		int n_section=-1, n_vertex=-1,n_triangle=-1, n_rectangle=-1;

		while (linesIterator.hasNext()) {
			
			line = linesIterator.next();
			tok = new StringTokenizer(line);
			kindOfLine = Integer.parseInt(tok.nextToken());
			
			// read "Fault System Summary Record" (values kept are use as a check later)
			if(kindOfLine == 200) {
				n_section=Integer.parseInt(tok.nextToken());
				n_vertex=Integer.parseInt(tok.nextToken());
				n_triangle=Integer.parseInt(tok.nextToken());
				n_rectangle=Integer.parseInt(tok.nextToken());
				// the rest of the line contains:
				// lat_lo lat_hi lon_lo lon_hi depth_lo depth_hi comment_text
			}
			
			// read "Fault Section Information Record"
			if(kindOfLine == 201) {
				int sid = Integer.parseInt(tok.nextToken());  // section ID
				String name = tok.nextToken();
				int n_sect_vertex=Integer.parseInt(tok.nextToken());
				int n_sect_triangle=Integer.parseInt(tok.nextToken());
				int n_sect_rectangle=Integer.parseInt(tok.nextToken());
				tok.nextToken(); // lat_lo
				tok.nextToken(); // lat_hi
				tok.nextToken(); // lon_lo
				tok.nextToken(); // lon_hi
				tok.nextToken(); // depth_lo
				tok.nextToken(); // depth_hi
				tok.nextToken(); // das_lo
				tok.nextToken(); // das_hi
				int fault_id = Integer.parseInt(tok.nextToken());
				// the rest of the line contains: comment_text
				
				// check for triangular elements
				if(n_sect_triangle>0) throw new RuntimeException("Don't yet support trinagles");
				
				namesOfSections.add(name);
				faultIDs_ForSections.add(fault_id);

				// read the vertices for this section
				ArrayList<Vertex> verticesForThisSect = new ArrayList<Vertex>();
				for(int v=0; v<n_sect_vertex; v++) {
					line = linesIterator.next();
					tok = new StringTokenizer(line);
					kindOfLine = Integer.parseInt(tok.nextToken());
					if(kindOfLine != 202) throw new RuntimeException("Problem with file (line should start with 202)");
					int id = Integer.parseInt(tok.nextToken());
					double lat = Double.parseDouble(tok.nextToken());
					double lon = Double.parseDouble(tok.nextToken());
					double depth = -Double.parseDouble(tok.nextToken())/1000; 	// convert to km & change sign
					double das = Double.parseDouble(tok.nextToken())/1000;		// convert to km
					int trace_flag = Integer.parseInt(tok.nextToken());
					// the rest of the line contains:
					// comment_text
					
					Vertex vertex = new Vertex(lat,lon,depth, id, das, trace_flag); 
					verticesForThisSect.add(vertex);
					vertexList.add(vertex);
				}
				vertexListForSections.add(verticesForThisSect);
				
				// now read the elements
				ArrayList<RectangularElement> rectElemForThisSect = new ArrayList<RectangularElement>();
				for(int r=0; r<n_sect_rectangle; r++) {
					line = linesIterator.next();
					tok = new StringTokenizer(line);
					kindOfLine = Integer.parseInt(tok.nextToken());
					if(kindOfLine != 204) throw new RuntimeException("Problem with file (line should start with 204)");
					int id = Integer.parseInt(tok.nextToken());
					int vertex_1_ID = Integer.parseInt(tok.nextToken());
					int vertex_2_ID = Integer.parseInt(tok.nextToken());
					int vertex_3_ID = Integer.parseInt(tok.nextToken());
					int vertex_4_ID = Integer.parseInt(tok.nextToken());
				    double rake = Double.parseDouble(tok.nextToken());
				    double slip_rate = Double.parseDouble(tok.nextToken())*SECONDS_PER_YEAR; // convert to meters per year
				    double aseis_factor = Double.parseDouble(tok.nextToken());
				    double strike = Double.parseDouble(tok.nextToken());
				    double dip = Double.parseDouble(tok.nextToken());
				    int perfect_flag = Integer.parseInt(tok.nextToken());
					// the rest of the line contains: comment_text
				    boolean perfectBoolean = false;
				    if(perfect_flag == 1) perfectBoolean = true;
				    Vertex[] vertices = new Vertex[4];
				    
				    vertices[0] = vertexList.get(vertex_1_ID-1);  // vertex index is one minus vertex ID
				    vertices[1] = vertexList.get(vertex_2_ID-1);
				    vertices[2] = vertexList.get(vertex_3_ID-1);
				    vertices[3] = vertexList.get(vertex_4_ID-1);
				    int numAlongStrike = -1;// unknown
				    int numDownDip = -1;	// unknown
				    FocalMechanism focalMechanism = new FocalMechanism(strike,dip,rake);
				    RectangularElement rectElem = new RectangularElement(id, vertices, name, fault_id, sid, numAlongStrike, 
				    													numDownDip, slip_rate, aseis_factor, focalMechanism, perfectBoolean);
				    rectElemForThisSect.add(rectElem);
				    rectElementsList.add(rectElem);
				    
				}
				rectElementsListForSections.add(rectElemForThisSect);
			}
		}
		
		// check the numbers of things:  n_sction, n_vertex, n_triangle, n_rectangle
		if(n_section != namesOfSections.size())
			throw new RuntimeException("something wrong with number of sections");
		if(n_vertex != vertexList.size())
			throw new RuntimeException("something wrong with number of vertices");
		if(n_rectangle != rectElementsList.size())
			throw new RuntimeException("something wrong with number of eleents");
		
		System.out.println("namesOfSections.size()="+namesOfSections.size()+"\tvertexList.size()="+vertexList.size()+"\trectElementsList.size()="+rectElementsList.size());
		
		// check that indices are in order, and that index is one minus the ID:
		for(int i=0;i<vertexList.size();i++) {
			if(i != vertexList.get(i).getID()-1) throw new RuntimeException("vertexList index problem at "+i);
		}
		for(int i=0;i<rectElementsList.size();i++) {
			if(i != rectElementsList.get(i).getID()-1) throw new RuntimeException("rectElementsList index problem at "+i);
		}
		
	}

	
	
	/**
	 * This returns the list of RectangularElement objects
	 * @return
	 */
	public ArrayList<RectangularElement> getElementsList() { return rectElementsList; }

	
	/**
	 * This makes the elements from a UCERF2 deformation model
	 * @param deformationModelID	- D2.1 = 82; D2.2 = 83; D2.3 = 84; D2.4 = 85; D2.5 = 86; D2.6 = 87
	 * @param aseisReducesArea		- whether or not to reduce area (otherwise reduces slip rate?)
	 * @param maxDiscretization		- the maximum element size
	 */
	public void mkElementsFromUCERF2_DefMod(int deformationModelID, boolean aseisReducesArea, 
			double maxDiscretization) {
		
		rectElementsList = new ArrayList<RectangularElement>();
		vertexList = new ArrayList<Vertex>();
		rectElementsListForSections = new ArrayList<ArrayList<RectangularElement>> ();
		vertexListForSections = new ArrayList<ArrayList<Vertex>>();
		namesOfSections = new ArrayList<String>();
		faultIDs_ForSections = null;	// no info for this

		
		// fetch the sections
		DeformationModelPrefDataFinal deformationModelPrefDB = new DeformationModelPrefDataFinal();
		allFaultSectionPrefData = deformationModelPrefDB.getAllFaultSectionPrefData(deformationModelID);

		//Alphabetize:
		Collections.sort(allFaultSectionPrefData, new NamedObjectComparator());

		/*		  
		  // write sections IDs and names
		  for(int i=0; i< this.allFaultSectionPrefData.size();i++)
				System.out.println(allFaultSectionPrefData.get(i).getSectionId()+"\t"+allFaultSectionPrefData.get(i).getName());
		 */

		// remove those with no slip rate
		if (D)System.out.println("Removing the following due to NaN slip rate:");
		for(int i=allFaultSectionPrefData.size()-1; i>=0;i--)
			if(Double.isNaN(allFaultSectionPrefData.get(i).getAveLongTermSlipRate())) {
				if(D) System.out.println("\t"+allFaultSectionPrefData.get(i).getSectionName());
				allFaultSectionPrefData.remove(i);
			}	 
				
		// Loop over sections and create the simulator elements
		int elementID =0;
		int numberAlongStrike = 0;
		int numberDownDip;
		int faultNumber = -1; // unknown for now
		int sectionNumber =0;
		double elementSlipRate=0;
		double elementAseis;
		double elementStrike=0, elementDip=0, elementRake=0;
		String sectionName;
//		System.out.println("allFaultSectionPrefData.size() = "+allFaultSectionPrefData.size());
		for(int i=0;i<allFaultSectionPrefData.size();i++) {
			ArrayList<RectangularElement> sectionElementsList = new ArrayList<RectangularElement>();
			ArrayList<Vertex> sectionVertexList = new ArrayList<Vertex>();
			sectionNumber +=1; // starts from 1, not zero
			FaultSectionPrefData faultSectionPrefData = allFaultSectionPrefData.get(i);
			StirlingGriddedSurface surface = new StirlingGriddedSurface(faultSectionPrefData.getSimpleFaultData(aseisReducesArea), maxDiscretization, maxDiscretization);
			EvenlyGridCenteredSurface gridCenteredSurf = new EvenlyGridCenteredSurface(surface);
			double elementLength = gridCenteredSurf.getGridSpacingAlongStrike();
			double elementDDW = gridCenteredSurf.getGridSpacingDownDip(); // down dip width
			elementRake = faultSectionPrefData.getAveRake();
			elementSlipRate = faultSectionPrefData.getAveLongTermSlipRate()/1000;
			elementAseis = faultSectionPrefData.getAseismicSlipFactor();
			sectionName = faultSectionPrefData.getName();
			for(int col=0; col<gridCenteredSurf.getNumCols();col++) {
				numberAlongStrike += 1;
				for(int row=0; row<gridCenteredSurf.getNumRows();row++) {
					elementID +=1; // starts from 1, not zero
					numberDownDip = row+1;
					Location centerLoc = gridCenteredSurf.get(row, col);
					Location top1 = surface.get(row, col);
					Location top2 = surface.get(row, col+1);
					Location bot1 = surface.get(row+1, col);
					double[] strikeAndDip = PlaneUtils.getStrikeAndDip(top1, top2, bot1);
					elementStrike = strikeAndDip[0];
					elementDip = strikeAndDip[1];	
					
					double hDistAlong = elementLength/2;
					double dipRad = Math.PI*elementDip/180;
					double vDist = (elementDDW/2)*Math.sin(dipRad);
					double hDist = (elementDDW/2)*Math.cos(dipRad);
					
//					System.out.println(elementID+"\telementDDW="+elementDDW+"\telementDip="+elementDip+"\tdipRad="+dipRad+"\tvDist="+vDist+"\thDist="+hDist);
					
					LocationVector vect = new LocationVector(elementStrike+180, hDistAlong, 0);
					Location newMid1 = LocationUtils.location(centerLoc, vect);  // half way down the first edge
					vect.set(elementStrike-90, hDist, -vDist); // up-dip direction
					Location newTop1 = LocationUtils.location(newMid1, vect);
					vect.set(elementStrike+90, hDist, vDist); // down-dip direction
					Location newBot1 = LocationUtils.location(newMid1, vect);
					 
					vect.set(elementStrike, hDistAlong, 0);
					Location newMid2 = LocationUtils.location(centerLoc, vect); // half way down the other edge
					vect.set(elementStrike-90, hDist, -vDist); // up-dip direction
					Location newTop2 = LocationUtils.location(newMid2, vect);
					vect.set(elementStrike+90, hDist, vDist); // down-dip direction
					Location newBot2 = LocationUtils.location(newMid2, vect);
					
					 // @param traceFlag - tells whether is on the fault trace  (0 means no; 1 means yes, but not
					 // 		              the first or last point; 2 means yes & it's the first; and 3 means yes 
					 //                    & it's the last point)
					
					
					// set DAS
					double das1 = col*elementLength;	// this is in km
					double das2 = das1+elementLength;	// this is in km
					// set traceFlag - tells whether is on the fault trace  (0 means no; 1 means yes, but not the 
					// first or last point; 2 means yes & it's the first; and 3 means yes & it's the last point)
					int traceFlagBot = 0;
					int traceFlagTop1 = 0;
					int traceFlagTop2 = 0;
					if(row ==0) {
						traceFlagTop1 = 1;
						traceFlagTop2 = 1;
					}
					if(row==0 && col==0) traceFlagTop1 = 2;
					if(row==0 && col==gridCenteredSurf.getNumCols()-1) traceFlagTop2 = 3;

					Vertex[] elementVertices = new Vertex[4];
					elementVertices[0] = new Vertex(newTop1,vertexList.size()+1, das1, traceFlagTop1);  
					elementVertices[1] = new Vertex(newBot1,vertexList.size()+2, das1, traceFlagBot);
					elementVertices[2] = new Vertex(newBot2,vertexList.size()+3, das2, traceFlagBot);
					elementVertices[3] = new Vertex(newTop2,vertexList.size()+4, das2, traceFlagTop2);
					
					FocalMechanism focalMech = new FocalMechanism(elementStrike, elementDip, elementRake);
										
					RectangularElement simSurface =
						new RectangularElement(elementID, elementVertices, sectionName,
								faultNumber, sectionNumber, numberAlongStrike, numberDownDip,
								elementSlipRate, elementAseis, focalMech, true);
					
					rectElementsList.add(simSurface);
					vertexList.add(elementVertices[0]);
					vertexList.add(elementVertices[1]);
					vertexList.add(elementVertices[2]);
					vertexList.add(elementVertices[3]);
					
					sectionElementsList.add(simSurface);
					sectionVertexList.add(elementVertices[0]);
					sectionVertexList.add(elementVertices[1]);
					sectionVertexList.add(elementVertices[2]);
					sectionVertexList.add(elementVertices[3]);

					
//					String line = elementID + "\t"+
//						numberAlongStrike + "\t"+
//						numberDownDip + "\t"+
//						faultNumber + "\t"+
//						sectionNumber + "\t"+
//						(float)elementSlipRate + "\t"+
//						(float)elementStrength + "\t"+
//						(float)elementStrike + "\t"+
//						(float)elementDip + "\t"+
//						(float)elementRake + "\t"+
//						(float)newTop1.getLatitude() + "\t"+
//						(float)newTop1.getLongitude() + "\t"+
//						(float)newTop1.getDepth()*-1000 + "\t"+
//						(float)newBot1.getLatitude() + "\t"+
//						(float)newBot1.getLongitude() + "\t"+
//						(float)newBot1.getDepth()*-1000 + "\t"+
//						(float)newBot2.getLatitude() + "\t"+
//						(float)newBot2.getLongitude() + "\t"+
//						(float)newBot2.getDepth()*-1000 + "\t"+
//						(float)newTop2.getLatitude() + "\t"+
//						(float)newTop2.getLongitude() + "\t"+
//						(float)newTop2.getDepth()*-1000 + "\t"+
//						sectionName;
//
//					System.out.println(line);
				}
			}
			rectElementsListForSections.add(sectionElementsList);
			vertexListForSections.add(sectionVertexList);
			namesOfSections.add(faultSectionPrefData.getName());
		}
		System.out.println("rectElementsList.size()="+rectElementsList.size());
		System.out.println("vertexList.size()="+vertexList.size());
		
		/*
		for(int i=0;i<allFaultSectionPrefData.size();i++) {
			ArrayList<RectangularElement> elList = rectElementsListForSections.get(i);
			ArrayList<Vertex> verList = vertexListForSections.get(i);;
			System.out.println(allFaultSectionPrefData.get(i).getName());
			System.out.println("\tEl Indices:  "+elList.get(0).getID()+"\t"+elList.get(elList.size()-1).getID());
//			System.out.println("\tVer Indices:  "+verList.get(0).getID()+"\t"+verList.get(verList.size()-1).getID());
		}
		*/
	}
	
	
	public void writeToWardFile(String fileName) throws IOException {
		FileWriter efw = new FileWriter(fileName);
		for (RectangularElement rectElem : rectElementsList) {
			efw.write(rectElem.toWardFormatLine() + "\n");
		}
		efw.close();
	}

	
	/**
	 * This loads from Steve Wards file format (at least for the format he sent on Sept 2, 2010).  This
	 * implementation does not put DAS for traceFlag in the vertices, and there are assumptions about the
	 * ordering of things in his file.  Note also that his NAS does not start over for each section, but
	 * rather starts over for each fault.
	 * @param lines
	 */
	private void loadFromSteveWardLines(ArrayList<String> lines) {

		
		// now need to fill these lists
		rectElementsList = new ArrayList<RectangularElement>();
		vertexList = new ArrayList<Vertex>();
		rectElementsListForSections = new ArrayList<ArrayList<RectangularElement>> ();
		vertexListForSections = new ArrayList<ArrayList<Vertex>>();
		namesOfSections = new ArrayList<String>();
		faultIDs_ForSections = new ArrayList<Integer>();
		
		int lastSectionID = -1;
		ArrayList<RectangularElement> currentRectElForSection = null;
		ArrayList<Vertex> currVertexListForSection = null;
		
		int numVertices= 0; // to set vertexIDs

		
		for (String line : lines) {
			if (line == null || line.length() == 0)
				continue;

			StringTokenizer tok = new StringTokenizer(line);

			int id = Integer.parseInt(tok.nextToken()); // unique number ID for each element
			int numAlongStrike = Integer.parseInt(tok.nextToken()); // Number along strike
			int numDownDip = Integer.parseInt(tok.nextToken()); // Number down dip
			int faultID = Integer.parseInt(tok.nextToken()); // Fault Number
			int sectionID = Integer.parseInt(tok.nextToken()); // Segment Number
			double slipRate = Double.parseDouble(tok.nextToken()); // Slip Rate in m/y.
			double strength = Double.parseDouble(tok.nextToken()); // Element Strength in Bars (not used).
			double strike = Double.parseDouble(tok.nextToken()); // stike
			double dip = Double.parseDouble(tok.nextToken()); // dip
			double rake = Double.parseDouble(tok.nextToken()); // rake
			FocalMechanism focalMechanism = new FocalMechanism(strike, dip, rake);

			Vertex[] vertices = new Vertex[4];
			// 0th vertex
			double lat = Double.parseDouble(tok.nextToken());
			double lon = Double.parseDouble(tok.nextToken());
			double depth = Double.parseDouble(tok.nextToken()) / -1000d;
			numVertices+=1;
			vertices[0] = new Vertex(lat, lon, depth, numVertices);
			// 1st vertex
			lat = Double.parseDouble(tok.nextToken());
			lon = Double.parseDouble(tok.nextToken());
			depth = Double.parseDouble(tok.nextToken()) / -1000d;
			numVertices+=1;
			vertices[1] = new Vertex(lat, lon, depth, numVertices);
			// 2nd vertex
			lat = Double.parseDouble(tok.nextToken());
			lon = Double.parseDouble(tok.nextToken());
			depth = Double.parseDouble(tok.nextToken()) / -1000d;
			numVertices+=1;
			vertices[2] = new Vertex(lat, lon, depth, numVertices);
			// last vertex
			lat = Double.parseDouble(tok.nextToken());
			lon = Double.parseDouble(tok.nextToken());
			depth = Double.parseDouble(tok.nextToken()) / -1000d;
			numVertices+=1;
			vertices[3] = new Vertex(lat, lon, depth, numVertices);

			String name = null;
			while (tok.hasMoreTokens()) {
				if (name == null)
					name = "";
				else
					name += " ";
				name += tok.nextToken();
			}
			String sectionName = name;
			
			RectangularElement rectElem = new RectangularElement(id, vertices, sectionName,faultID, sectionID, 
					numAlongStrike, numDownDip, slipRate, Double.NaN, focalMechanism, true);
			
			rectElementsList.add(rectElem);
			
			// check if this is a new section
			if(sectionID != lastSectionID) {
				// encountered a new section
				currentRectElForSection = new ArrayList<RectangularElement>();
				currVertexListForSection = new ArrayList<Vertex>();
				rectElementsListForSections.add(currentRectElForSection);
				vertexListForSections.add(currVertexListForSection);
				namesOfSections.add(sectionName);
				faultIDs_ForSections.add(faultID);
			}
			currentRectElForSection.add(rectElem);
			for(int i=0; i<4;i++) {
				vertexList.add(vertices[i]);
				currVertexListForSection.add(vertices[i]);
			}
	}
		
		// check that indices are in order, and that index is one minus the ID:
		for(int i=0;i<vertexList.size();i++) {
			int idMinus1 = vertexList.get(i).getID()-1;
			if(i != idMinus1) throw new RuntimeException("vertexList index problem at index "+i+" (ID-1="+idMinus1+")");
		}
		for(int i=0;i<rectElementsList.size();i++) {
			if(i != rectElementsList.get(i).getID()-1) throw new RuntimeException("rectElementsList index problem at "+i);
		}
		
		System.out.println("namesOfSections.size()="+namesOfSections.size()+"\tvertexList.size()="+vertexList.size()+"\trectElementsList.size()="+rectElementsList.size());


	}

	
	
	/**
	 * The creates a EQSIM V04 Geometry file for the given instance.
	 * @param fileName
	 * @param infoLines - each line here should NOT end with a new line char "\n" (this will be added)
	 * @param titleLine
	 * @param author
	 * @param date
	 * @throws IOException
	 */
	public void writeTo_EQSIM_V04_GeometryFile(String fileName, ArrayList<String> infoLines, String titleLine, 
			String author, String date) throws IOException {
			FileWriter efw = new FileWriter(fileName);
			
			// write the standard file signature info
			efw.write("101 "+GEOM_FILE_SIG +" "+GEOM_FILE_SPEC_LEVEL+ "\n");
			
			// add the file-specific meta data records/lines
			if(titleLine!=null)
				efw.write("111 "+titleLine+ "\n");
			if(author!=null)
				efw.write("112 "+author+ "\n");
			if(date!=null)
				efw.write("113 "+date+ "\n");
			if(infoLines!=null)
				for(int i=0; i<infoLines.size();i++)
					efw.write("110 "+infoLines.get(i)+ "\n");
			
			// add the standard descriptor records/lines for the Geometry file (read from another file)
			String fullPath = "org/opensha/sha/simulators/eqsim_v04/ALLCAL_Model_v04/ALLCAL_Ward_Geometry.dat";
			ArrayList<String> lines=null;
			try {
				lines = FileUtils.loadJarFile(fullPath);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			for(int l=0;l<lines.size();l++) {
				String line = lines.get(l);
				StringTokenizer tok = new StringTokenizer(line);
				int kindOfLine = Integer.parseInt(tok.nextToken());
				if(kindOfLine==120 || kindOfLine==121 || kindOfLine==103)
					efw.write(line+"\n");
			}
			
			// now add the data records/lines 
			
			// Fault System Summary Record:
			// 200 n_section n_vertex n_triangle n_rectangle lat_lo lat_hi lon_lo lon_hi depth_lo depth_hi comment_text
			efw.write("200 "+namesOfSections.size()+" "+vertexList.size()+" 0 "+rectElementsList.size()+" "+
							getMinMaxFileString(vertexList, false)+"\n");

			// loop over sections
			for(int i=0;i<namesOfSections.size();i++) {
				ArrayList<Vertex> vertListForSect = vertexListForSections.get(i);
				ArrayList<RectangularElement> rectElemForSect = rectElementsListForSections.get(i);
				String fault_id;
				if(faultIDs_ForSections == null)
					fault_id = "NA";
				else
					fault_id = faultIDs_ForSections.get(i).toString();
				// Fault Section Information Record:
				// 201 sid name n_vertex n_triangle n_rectangle lat_lo lat_hi lon_lo lon_hi depth_lo depth_hi das_lo das_hi fault_id comment_text
				efw.write("201 "+(i+1)+" "+namesOfSections.get(i)+" "+vertListForSect.size()+" 0 "+
						rectElemForSect.size()+" "+getMinMaxFileString(vertListForSect, true)+" "+fault_id+"\n");
				for(int v=0; v<vertListForSect.size(); v++) {
					Vertex vert = vertListForSect.get(v);
					// Vertex Record: 202 ID lat lon depth das trace_flag comment_text
					efw.write("202 "+vert.getID()+" "+(float)vert.getLatitude()+" "+(float)vert.getLongitude()+" "+
							(float)(vert.getDepth()*-1000)+" "+(float)vert.getDAS()*1000+" "+vert.getTraceFlag()+"\n");
				}
				for(int e=0; e<rectElemForSect.size(); e++) {
					RectangularElement elem = rectElemForSect.get(e);
					Vertex[] vert = elem.getVertices();
					FocalMechanism focalMech = elem.getFocalMechanism();
					// Rectangle Record:  204 ID vertex_1 vertex_2 vertex_3 vertex_4 rake slip_rate aseis_factor strike dip perfect_flag comment_text
					efw.write("204 "+elem.getID()+" "+vert[0].getID()+" "+vert[1].getID()+" "+vert[2].getID()+" "+
							vert[3].getID()+" "+(float)focalMech.getRake()+" "+(float)(elem.getSlipRate()/SECONDS_PER_YEAR)+" "+
							(float)elem.getAseisFactor()+" "+(float)focalMech.getStrike()+" "+(float)focalMech.getDip()
							+" "+elem.getPerfectInt()+"\n");
				}
			}
			
			// add the last line
			efw.write("999 End\n");

			efw.close();
	}
	
	
	/**
	 * This produces the string of min and max lat, lon, depth, and (optionally) DAS from the
	 * given list of vertices.  There are no spaces before or after the first and last values,
	 * respectively.  Depth and DAS values are converted to meters (from km).
	 * @param vertexList
	 * @param includeDAS
	 * @return
	 */
	private String getMinMaxFileString(ArrayList<Vertex> vertexList, boolean includeDAS) {
		double minLat=Double.MAX_VALUE, maxLat=-Double.MAX_VALUE;
		double minLon=Double.MAX_VALUE, maxLon=-Double.MAX_VALUE;
		double minDep=Double.MAX_VALUE, maxDep=-Double.MAX_VALUE;
		double minDAS=Double.MAX_VALUE, maxDAS=-Double.MAX_VALUE;
		for(Vertex vertex: vertexList) {
			if(vertex.getLatitude()<minLat) minLat = vertex.getLatitude();
			if(vertex.getLongitude()<minLon) minLon = vertex.getLongitude();
			if(vertex.getDepth()<minDep) minDep = vertex.getDepth();
			if(vertex.getDAS()<minDAS) minDAS = vertex.getDAS();
			if(vertex.getLatitude()>maxLat) maxLat = vertex.getLatitude();
			if(vertex.getLongitude()>maxLon) maxLon = vertex.getLongitude();
//			if(!includeDAS) System.out.println(maxLon);
			if(vertex.getDepth()>maxDep) maxDep = vertex.getDepth();
			if(vertex.getDAS()>maxDAS) maxDAS = vertex.getDAS();
		}
		String string = (float)minLat+" "+(float)maxLat+" "+(float)minLon+" "+(float)maxLon+" "+(float)maxDep*-1000+" "+(float)minDep*-1000;
		if(includeDAS) string += " "+(float)minDAS*1000+" "+(float)maxDAS*1000;
		return string;
	}
	
	
	/**
	 * 
	 * @return
	 */
	public ArbIncrementalMagFreqDist computeTotalMagFreqDist(double minMag, double maxMag, int numMag, boolean makePlot) {
		ArbIncrementalMagFreqDist mfd = new ArbIncrementalMagFreqDist(minMag, maxMag, numMag);
		
		double simDurr = getSimulationDurationInYears();
		for(EQSIM_Event event : eventList) {
			mfd.addResampledMagRate(event.getMagnitude(), 1.0/simDurr, true);
		}
		mfd.setName("Total MFD");
		mfd.setInfo(" ");
		
		if(makePlot){
			ArrayList<EvenlyDiscretizedFunc> mfdList = new ArrayList<EvenlyDiscretizedFunc>();
			mfdList.add(mfd);
			mfdList.add(mfd.getCumRateDistWithOffset());
			mfdList.get(1).setName("Cumulative Distribution");
			mfdList.get(1).setInfo(" ");
			GraphiWindowAPI_Impl graph = new GraphiWindowAPI_Impl(mfdList, "Total Mag Freq Dist"); 
			graph.setX_AxisLabel("Magnitude");
			graph.setY_AxisLabel("Rate (per yr)");
			graph.setX_AxisRange(4.5, 8.5);
			double yMin = Math.pow(10,Math.floor(Math.log10(1/getSimulationDurationInYears())));
			double yMax = graph.getMaxY();
			if(yMin<yMax) {
				graph.setY_AxisRange(yMin,yMax);
				graph.setYLog(true);
			}

		}

		return mfd;
	}
	
		
// 

	/**
	 * This returns a list of incremental MFDs reflecting the rates of nucleation (as a function of mag) 
	 * on each fault section.  It also optionally makes plots.
	 */
	/**
	 * @param minMag
	 * @param maxMag
	 * @param numMag
	 * @param makeOnePlotWithAll - plot all incremental dists in one graph
	 * @param makeSeparatePlots - make separate plots of incremental and cumulative distributions
	 * @return
	 */
	public ArrayList<ArbIncrementalMagFreqDist> computeMagFreqDistByFaultSection(double minMag, double maxMag, int numMag, 
			boolean makeOnePlotWithAll, boolean makeSeparatePlots) {
		
		ArrayList<ArbIncrementalMagFreqDist> mfdList = new ArrayList<ArbIncrementalMagFreqDist>();
		for(int s=0; s<namesOfSections.size(); s++) {
			ArbIncrementalMagFreqDist mfd = new ArbIncrementalMagFreqDist(minMag, maxMag, numMag);
			mfd.setName(namesOfSections.get(s)+" MFD");
			mfd.setInfo(" ");
			mfdList.add(mfd);
		}
		
		double simDurr = getSimulationDurationInYears();
		for(EQSIM_Event event : eventList) {
			int sectionIndex = event.get(0).getSectionID()-1;	// nucleates on first (0th) event record, and index is one minus ID 
			mfdList.get(sectionIndex).addResampledMagRate(event.getMagnitude(), 1.0/simDurr, true);
		}
		
		double yMin = Math.pow(10,Math.floor(Math.log10(1/getSimulationDurationInYears())));
		if(makeOnePlotWithAll){
			GraphiWindowAPI_Impl graph = new GraphiWindowAPI_Impl(mfdList, "Mag Freq Dists");   
			graph.setX_AxisLabel("Magnitude");
			graph.setY_AxisLabel("Rate (per yr)");
			graph.setX_AxisRange(4.5, 8.5);
			double yMax = graph.getMaxY();
			if(yMin<yMax) {
				graph.setY_AxisRange(yMin,yMax);
				graph.setYLog(true);
			}
		}
		
		if(makeSeparatePlots) {
			for(ArbIncrementalMagFreqDist mfd :mfdList) {
				ArrayList<EvenlyDiscretizedFunc> mfdList2 = new ArrayList<EvenlyDiscretizedFunc>();
				mfdList2.add(mfd);
				mfdList2.add(mfd.getCumRateDistWithOffset());
				mfdList2.get(1).setName("Cumulative Distribution");
				mfdList2.get(1).setInfo(" ");
				GraphiWindowAPI_Impl graph = new GraphiWindowAPI_Impl(mfdList2, mfd.getName()); 
				graph.setX_AxisLabel("Magnitude");
				graph.setY_AxisLabel("Rate (per yr)");
				graph.setX_AxisRange(4.5, 8.5);
				double yMax = graph.getMaxY();
				if(yMin<yMax) {
					graph.setY_AxisRange(yMin,yMax);
					graph.setYLog(true);
				}
			}
		}
		
		return mfdList;
	}

	
	/**
	 * This tells whether all events have data on the slip on each element
	 * @return
	 */
	public int getNumEventsWithElementSlipData() {
		int numTrue =0;
		for (EQSIM_Event event : eventList) {
			if(event.hasElementSlipsAndIDs()) numTrue +=1;
		}
		return numTrue;
	}
	
	public ArrayList<EQSIM_Event> getEventsList() {
		return eventList;
	}
	
	public void randomizeEventTimes() {
		System.out.println("Event Times have been randomized");
		double firstEventTime=eventList.get(0).getTime();
		double simDurInSec = eventList.get(eventList.size()-1).getTime() - firstEventTime;
		for(EQSIM_Event event:eventList)
			event.setTime(firstEventTime+Math.random()*simDurInSec);
		Collections.sort(eventList);
		
	}
	
	/**
	 * 
	 */
	public void plotYearlyEventRates() {
		
		double startTime=eventList.get(0).getTime();
		int numYears = (int)getSimulationDurationInYears();
		EvenlyDiscretizedFunc evPerYear = new EvenlyDiscretizedFunc(0.0, numYears+1, 1.0);
		for(EQSIM_Event event :eventList) {
			int year = (int)((event.getTime()-startTime)/SECONDS_PER_YEAR);
			evPerYear.add(year, 1.0);
		}
		ArrayList<EvenlyDiscretizedFunc> funcList = new ArrayList<EvenlyDiscretizedFunc>();
		funcList.add(evPerYear);
		GraphiWindowAPI_Impl graph = new GraphiWindowAPI_Impl(funcList, "Num Events Per Year"); 
		graph.setX_AxisLabel("Year");
		graph.setY_AxisLabel("Number");
	}
	
	
	public void plotNormRI_Distribution(ArrayList<Double> normRI_List) {
		// find max value
		double max=0;
		for(Double val:normRI_List)
			if(val>max) max = val;
		double delta=0.1;
		int num = (int)Math.ceil(max/delta)+2;
		EvenlyDiscretizedFunc dist = new EvenlyDiscretizedFunc(delta/2, num,delta);
		dist.setTolerance(2*delta);
		int numData=normRI_List.size();
		for(Double val:normRI_List)
			dist.add(val, 1.0/(numData*delta));  // this makes it a true PDF
		
		// now make the function list for the plot
		ArrayList<EvenlyDiscretizedFunc> funcList = new ArrayList<EvenlyDiscretizedFunc>();
		

		// add best-fit BPT function
		BPT_DistCalc bpt_calc = new BPT_DistCalc();
		bpt_calc.fitToThisFunction(dist, 0.5, 1.5, 11, 0.1, 1.5, 151);
		EvenlyDiscretizedFunc fitBPT_func = bpt_calc.getPDF();
		fitBPT_func.setName("Best Fit BPT Dist");
		fitBPT_func.setInfo("(mean="+(float)bpt_calc.getMean()+", aper="+(float)bpt_calc.getAperiodicity()+")");
		funcList.add(fitBPT_func);
		
		// add best-fit Lognormal dist function
		LognormalDistCalc logNorm_calc = new LognormalDistCalc();
		logNorm_calc.fitToThisFunction(dist, 0.5, 1.5, 11, 0.1, 1.5, 151);
		EvenlyDiscretizedFunc fitLogNorm_func = logNorm_calc.getPDF();
		fitLogNorm_func.setName("Best Fit Lognormal Dist");
		fitLogNorm_func.setInfo("(mean="+(float)bpt_calc.getMean()+", aper="+(float)bpt_calc.getAperiodicity()+")");
		funcList.add(fitLogNorm_func);
		
		// add the histogram created here
		dist.setName("Recur. Int. Dist");
		dist.setInfo("(Number of points = "+ numData+")");
		funcList.add(dist);
		GraphiWindowAPI_Impl graph = new GraphiWindowAPI_Impl(funcList, "Normalized Recurence Intervals"); 
		graph.setX_AxisLabel("RI (yrs)");
		graph.setY_AxisLabel("Density");
		ArrayList<PlotCurveCharacterstics> curveCharacteristics = new ArrayList<PlotCurveCharacterstics>();
		curveCharacteristics.add(new PlotCurveCharacterstics(PlotColorAndLineTypeSelectorControlPanel.SOLID_LINE, Color.BLACK, 2));
		curveCharacteristics.add(new PlotCurveCharacterstics(PlotColorAndLineTypeSelectorControlPanel.SOLID_LINE, Color.BLUE, 2));
		curveCharacteristics.add(new PlotCurveCharacterstics(PlotColorAndLineTypeSelectorControlPanel.HISTOGRAM, Color.RED, 2));
		graph.setPlottingFeatures(curveCharacteristics);
		graph.setX_AxisRange(0, 5);
		

	}
	
	
	
	public void testTimePredictability(double magThresh, String fileName) {

		FileWriter fw_timePred;
		FileWriter fw_eventTimes;
		try {
			fw_timePred = new FileWriter(fileName);
			fw_eventTimes = new FileWriter("eventTimes");

			double[] lastTimeForElement = new double[rectElementsList.size()];
			double[] lastSlipForElement = new double[rectElementsList.size()];
			for(int i=0; i<lastTimeForElement.length;i++) lastTimeForElement[i]=-1;  // initialize to bogus value so we can check
			
			int numEvents=0;
			int numBad=0;
			double minElementArea = Double.MAX_VALUE;
			double maxElementArea = 0;
			int counter=-1;
			
			ArrayList<Double> obsIntervalList = new ArrayList<Double>();
			ArrayList<Double> tpInterval1List = new ArrayList<Double>();
			ArrayList<Double> tpInterval2List = new ArrayList<Double>();
			ArrayList<Double> spInterval1List = new ArrayList<Double>();
			ArrayList<Double> spInterval2List = new ArrayList<Double>();
			ArrayList<Double> norm_tpInterval2List = new ArrayList<Double>();
			ArrayList<Integer> firstSectionList = new ArrayList<Integer>();
			
			System.out.println("Minimum Magnitude Considered for time and slip predicatbility = "+magThresh);
			
			// write file header
			fw_timePred.write("counter\tobsInterval\ttpInterval1\tnorm_tpInterval1\ttpInterval2\tnorm_tpInterval2\t"+
					"spInterval1\tnorm_spInterval1\tspInterval2\tnorm_spInterval2\t"+
					"aveLastSlip\taveSlip\teventMag\teventID\tfirstSectionID\tnumSectionsInEvent\tsectionsInEventString\n");
			
			
			// loop over all events
			for(EQSIM_Event event:eventList) {
				numEvents +=1;
				double eventTime = event.getTime();
				
				/*				
				// write out Berryessa event info
				for(EventRecord evRec: event) {
					if(evRec.getSectionID() == 6) {
						System.out.println(event.getID()+"\t"+event.getMagnitude()+"\t"+(float)event.getTimeInYears()+"\t"+event.size());
					}
				}
*/				
				if(event.hasElementSlipsAndIDs() && event.getMagnitude() >= magThresh) {
					boolean goodSample = true;
					double eventMag = event.getMagnitude();
					String sectionsInEventString = new String();
					ArrayList<Double> slipList = new ArrayList<Double>();
					ArrayList<Integer> elementID_List = new ArrayList<Integer>();
					// collect slip and ID data from all event records
					for(EventRecord evRec: event) {
						if(eventTime != evRec.getTime()) throw new RuntimeException("problem with event times");  // just a check
						slipList.addAll(evRec.getElementSlipList());
						elementID_List.addAll(evRec.getElementID_List());
						sectionsInEventString += namesOfSections.get(evRec.getSectionID()-1) + " + ";
					}
					// get average date of last event and average predicted date of next
					double aveLastEvTime=0;
					double ave_tpNextEvTime=0;
					double ave_spNextEvTime=0;
					double aveSlipRate =0;
					double aveLastSlip =0;
					double aveSlip=0;
					int numElements = slipList.size();
					for(int e=0;e<numElements;e++) {
						int index = elementID_List.get(e).intValue() -1;
						double lastTime = lastTimeForElement[index];
						double lastSlip = lastSlipForElement[index];
						double slipRate = rectElementsList.get(index).getSlipRate();
						double area = rectElementsList.get(index).getGriddedSurface().getSurfaceArea();
						if(area<minElementArea) minElementArea = area;
						if(area>maxElementArea) maxElementArea = area;
//						if(slipRate == 0) {  // there are few of these, and I don't know what to do about them
//							goodSample=false;
//							System.out.println("slip rate is zero for element "+index+"; last slip is "+lastSlip);
//						}
						aveLastEvTime += lastTime;
						if(slipRate != 0) {  // there are a few of these, and I don't know what else to do
							ave_tpNextEvTime += lastTime + lastSlip/(slipRate/SECONDS_PER_YEAR);
							ave_spNextEvTime += lastTime + slipList.get(e)/(slipRate/SECONDS_PER_YEAR);
						}
						aveSlipRate += slipRate/SECONDS_PER_YEAR;
						aveLastSlip += lastSlip;
						aveSlip += slipList.get(e);
						// mark as bad sample if  lastTime is -1
						if(lastTime==-1){
							goodSample=false;
//							System.out.println("time=0 for element"+e+" of event"+eventNum);
						}
					}
					aveLastEvTime /= numElements;
					ave_tpNextEvTime /= numElements;
					ave_spNextEvTime /= numElements;
					aveSlipRate /= numElements;
					aveLastSlip /= numElements;
					aveSlip /= numElements;
					double obsInterval = (eventTime-aveLastEvTime)/SECONDS_PER_YEAR;
					double tpInterval1 = (ave_tpNextEvTime-aveLastEvTime)/SECONDS_PER_YEAR;
					double tpInterval2 = (aveLastSlip/aveSlipRate)/SECONDS_PER_YEAR;
					double spInterval1 = (ave_spNextEvTime-aveLastEvTime)/SECONDS_PER_YEAR;
					double spInterval2 = (aveSlip/aveSlipRate)/SECONDS_PER_YEAR;
					double norm_tpInterval1 = obsInterval/tpInterval1;
					double norm_tpInterval2 = obsInterval/tpInterval2;
					double norm_spInterval1 = obsInterval/spInterval1;
					double norm_spInterval2 = obsInterval/spInterval2;
					
					// skip those that have zero aveSlipRate (causes Inf for tpInterval2 &spInterval2)
					if(aveSlipRate == 0) goodSample = false;
					
					if(goodSample) {
						counter += 1;
						fw_timePred.write(counter+"\t"+obsInterval+"\t"+
								tpInterval1+"\t"+(float)norm_tpInterval1+"\t"+
								tpInterval2+"\t"+(float)norm_tpInterval2+"\t"+
								spInterval1+"\t"+(float)norm_spInterval1+"\t"+
								spInterval2+"\t"+(float)norm_spInterval2+"\t"+
								(float)aveLastSlip+"\t"+(float)aveSlip+"\t"+
								(float)eventMag+"\t"+event.getID()+"\t"+
								event.get(0).getSectionID()+"\t"+
								event.size()+"\t"+sectionsInEventString+"\n");
						// save for calculating correlations
						obsIntervalList.add(obsInterval);
						tpInterval1List.add(tpInterval1);
						tpInterval2List.add(tpInterval2);
						spInterval1List.add(spInterval1);
						spInterval2List.add(spInterval2);
						firstSectionList.add(event.get(0).getSectionID());
						norm_tpInterval2List.add(norm_tpInterval2);
					}
					else {
//						System.out.println("event "+ eventNum+" is bad");
						numBad += 1;
					}

					// now fill in the last event data for next time
					for(int e=0;e<numElements;e++) {
						int index = elementID_List.get(e).intValue() -1;
						lastTimeForElement[index] = eventTime;
						lastSlipForElement[index] = slipList.get(e);
					}
				}
			}
			
			plotNormRI_Distribution(norm_tpInterval2List);
			
			System.out.println("\nMinimum Magnitude Considered in Time Dependence = "+magThresh+"\n");

			// Print correlations
			double[] result;
			System.out.println("\nCorrelations (and chance it's random) between all Observed and Predicted Intervals:");
			result = this.getCorrelationAndP_Value(tpInterval1List, obsIntervalList);
			System.out.println("\t"+(float)result[0]+"\t("+result[1]+") for tpInterval1 (num pts ="+tpInterval1List.size()+")");
			result = this.getCorrelationAndP_Value(tpInterval2List, obsIntervalList);
			System.out.println("\t"+(float)result[0]+"\t("+result[1]+") for tpInterval2 (num pts ="+tpInterval2List.size()+")");
			result = this.getCorrelationAndP_Value(spInterval1List, obsIntervalList);
			System.out.println("\t"+(float)result[0]+"\t("+result[1]+") for spInterval1 (num pts ="+spInterval1List.size()+")");
			result = this.getCorrelationAndP_Value(spInterval2List, obsIntervalList);
			System.out.println("\t"+(float)result[0]+"\t("+result[1]+") for spInterval2 (num pts ="+spInterval2List.size()+")");

			// now do correlations for each section
			System.out.println("\nCorrelations (and chance it's random) between Observed and tpInterval2 by Section:");
			for(int s=0;s<namesOfSections.size();s++) {
				ArrayList<Double> vals1 = new ArrayList<Double>();
				ArrayList<Double> vals2 = new ArrayList<Double>();
				for(int i=0;i<obsIntervalList.size();i++) {
					if(firstSectionList.get(i).intValue() == s+1) {
						vals1.add(obsIntervalList.get(i));
						vals2.add(tpInterval2List.get(i));
					}
				}
				if(vals1.size()>2) {
					result = this.getCorrelationAndP_Value(vals1, vals2);
					System.out.println("\t"+(s+1)+"\t"+(float)result[0]+"\t("+(float)result[1]+
							")\tfor section "+namesOfSections.get(s)+" (num points = "+vals1.size()+")");
				}
				else
					System.out.println("\t"+(s+1)+"\tNaN\t\t\t\t"+
							namesOfSections.get(s)+" (num points = "+vals1.size()+")");
			}
			
			System.out.println("\n"+numBad+" events were bad");
			
			System.out.println("minElementArea="+(float)minElementArea+"\tmaxElementArea"+(float)maxElementArea);
			
			fw_timePred.close();
			fw_eventTimes.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}
	
	
	/**
	 * This computes the correlation coefficient and the p-value between the two lists.  
	 * The p-value represents the two-tailed significance of the result (and it depends on the 
	 * number of points).  This represents the probability that a truly random process
	 * would produce a correlation greater than the value or less than the negative value.  
	 * In other words, if you reject the null hypothesis that there is no correlation, then
	 * there is the p-value chance that you are wrong.  The one sided values are exactly half 
	 * the two-sided values.  I verified the p-values against an on-line calculator.
	 * @param list1
	 * @param list2
	 * @return double[2], where the first element is the correlation and the second is the p-value
	 */
	private double[] getCorrelationAndP_Value(ArrayList<Double> list1, ArrayList<Double> list2) {
		double[][] vals = new double[list1.size()][2];
		for(int i=0;i<list1.size();i++) {
			vals[i][0] = list1.get(i);
			vals[i][1] = list2.get(i);
		}
		PearsonsCorrelation corrCalc = new PearsonsCorrelation(vals);
		double[] result = new double[2];
		RealMatrix matrix;
		try {
			matrix = corrCalc.getCorrelationMatrix();
			result[0] = matrix.getEntry(0, 1);
			matrix = corrCalc.getCorrelationPValues();
			result[1] = matrix.getEntry(0, 1);
		} catch (MathException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	
	
	/**
	 * This compares all the computed magnitudes to those given on the input files 
	 * and writes out the maximum absolute difference.
	 */
	public void checkEventMagnitudes() {

		double maxMagDiff = 0;

		// loop over all events
		for(EQSIM_Event event:eventList) {
			if(event.hasElementSlipsAndIDs()) {

				double eventMag = event.getMagnitude();
				double moment =0;
				ArrayList<Double> slipList = new ArrayList<Double>();
				ArrayList<Integer> elementID_List = new ArrayList<Integer>();
				// collect slip and ID data from all event records
				for(EventRecord evRec: event) {
					slipList.addAll(evRec.getElementSlipList());
					elementID_List.addAll(evRec.getElementID_List());
				}
				// get average date of last event and average predicted date of next
				int numElements = slipList.size();
				for(int e=0;e<numElements;e++) {
					int index = elementID_List.get(e).intValue() -1;
					double area = rectElementsList.get(index).getGriddedSurface().getSurfaceArea();
					double slip = slipList.get(e); // this is in meters
					moment += FaultMomentCalc.getMoment(area*1e6, slip);	// convert area to meters squared
				}
				double computedMag = MomentMagCalc.getMag(moment);
				double diff = Math.abs(eventMag-computedMag);
				if(diff> maxMagDiff) maxMagDiff = diff;
			}
		}
		System.out.println("maximum abs(eventMag-computedMag) ="+maxMagDiff);
	}
	
	
	/**
	 * This computes the simulation duration from the times of the first and last event
	 * @return
	 */
	public double getSimulationDurationInYears() {
		double startTime=eventList.get(0).getTime();
		double endTime=eventList.get(eventList.size()-1).getTime();
		return (endTime-startTime)/SECONDS_PER_YEAR;
	}

	
	/**
	 * This compares observed slip rate (from events) with those imposed.
	 * @param fileName - set as null to not write the data out.
	 */
	public void checkElementSlipRates(String fileName, boolean makePlot) {

		FileWriter fw_slipRates;
		double[] obsAveSlipRate = new double[rectElementsList.size()];
		double[] imposedSlipRate = new double[rectElementsList.size()];
		int[] numEvents = new int[rectElementsList.size()];
		int eventNum=0;
		// loop over all events
		for(EQSIM_Event event:eventList) {
			eventNum++;
			if(event.hasElementSlipsAndIDs()) {
				ArrayList<Double> slipList = new ArrayList<Double>();
				ArrayList<Integer> elementID_List = new ArrayList<Integer>();
				// collect slip and ID data from all event records
				for(EventRecord evRec: event) {
					slipList.addAll(evRec.getElementSlipList());
					elementID_List.addAll(evRec.getElementID_List());
				}
				int numElements = slipList.size();
				for(int e=0;e<numElements;e++) {
					int index = elementID_List.get(e).intValue() -1;
					obsAveSlipRate[index] += slipList.get(e);
					numEvents[index] += 1;
					//						if(eventNum ==3) System.out.println("Test: el_ID="+elementID_List.get(e).intValue()+"\tindex="+index+"\tslip="+slipList.get(e));
				}
			}
		}

		// finish obs and get imposed slip rates:
		double simDurr = getSimulationDurationInYears();
		for(int i=0; i<obsAveSlipRate.length; i++) {
			obsAveSlipRate[i] /= simDurr;
			imposedSlipRate[i] = rectElementsList.get(i).getSlipRate();
		}

		PearsonsCorrelation corrCalc = new PearsonsCorrelation();
		double slipRateCorr = corrCalc.correlation(obsAveSlipRate, imposedSlipRate);
		System.out.println("Correlation between obs and imposed slip rate = "+(float)slipRateCorr);
		
		// make plot if desired
		if(makePlot) {
			XY_DataSet xy_data = new XY_DataSet(imposedSlipRate,obsAveSlipRate);
			xy_data.setName("Obs versus Imposed Slip Rate");
			xy_data.setInfo(" ");
			ArrayList funcs = new ArrayList();
			funcs.add(xy_data);
			GraphiWindowAPI_Impl graph = new GraphiWindowAPI_Impl(funcs, "Slip Rate Comparison");   
			graph.setX_AxisLabel("Imposed Slip Rate (m/s)");
			graph.setY_AxisLabel("Observed Slip Rate (m/s)");
			ArrayList<PlotCurveCharacterstics> curveCharacteristics = new ArrayList<PlotCurveCharacterstics>();
			curveCharacteristics.add(new PlotCurveCharacterstics(PlotColorAndLineTypeSelectorControlPanel.FILLED_CIRCLES, Color.BLUE, 4));
			graph.setPlottingFeatures(curveCharacteristics);
		}

		// write file if name is non null
		if(fileName != null) {
			try {
				fw_slipRates = new FileWriter(fileName);
				fw_slipRates.write("obsSlipRate\timposedSlipRate\tdiff\tnumEvents\n");
				//				System.out.println(endTime+"\t"+startTime);
				for(int i=0; i<obsAveSlipRate.length; i++) {
					double diff = obsAveSlipRate[i]-imposedSlipRate[i];
					fw_slipRates.write(obsAveSlipRate[i]+"\t"+imposedSlipRate[i]+"\t"+diff+"\t"+numEvents[i]+"\n");
				}
				fw_slipRates.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	
	public void plotScalingRelationships() {
		double[] slip = new double[eventList.size()];
		double[] mag = new double[eventList.size()];
		double[] area = new double[eventList.size()];
		double[] length = new double[eventList.size()];
		
		int index = -1;
		for(EQSIM_Event event:eventList) {
			index +=1;
			slip[index]=event.getMeanSlip();
			mag[index]=event.getMagnitude();
			area[index]=event.getArea()/1e6; 		// convert to km-sq
			length[index]=event.getLength()/1000; 	// convert to km
		}
		/**/
		
		// SLIP VS LENGTH PLOT
		XY_DataSet s_vs_l_data = new XY_DataSet(slip,length);
		s_vs_l_data.setName("Mean Slip vs Length");
		s_vs_l_data.setInfo(" ");
		ArrayList s_vs_l_funcs = new ArrayList();
		s_vs_l_funcs.add(s_vs_l_data);
		GraphiWindowAPI_Impl s_vs_l_graph = new GraphiWindowAPI_Impl(s_vs_l_funcs, "Mean Slip vs Length");   
		s_vs_l_graph.setX_AxisLabel("Mean Slip (m)");
		s_vs_l_graph.setY_AxisLabel("Length (km)");
		ArrayList<PlotCurveCharacterstics> s_vs_l_curveChar = new ArrayList<PlotCurveCharacterstics>();
		s_vs_l_curveChar.add(new PlotCurveCharacterstics(PlotColorAndLineTypeSelectorControlPanel.CIRCLES, Color.BLUE, 3));
		s_vs_l_graph.setPlottingFeatures(s_vs_l_curveChar);
		
		// MAG VS AREA PLOT
		XY_DataSet m_vs_a_data = new XY_DataSet(area,mag);
		m_vs_a_data.setName("Mag-Area data from simulation");
		m_vs_a_data.setInfo(" ");
		ArrayList m_vs_a_funcs = new ArrayList();
		Ellsworth_B_WG02_MagAreaRel elB = new Ellsworth_B_WG02_MagAreaRel();
		HanksBakun2002_MagAreaRel hb = new HanksBakun2002_MagAreaRel();
		WC1994_MagAreaRelationship wc = new WC1994_MagAreaRelationship();
		wc.setRake(0);
		Shaw_2007_MagAreaRel sh = new Shaw_2007_MagAreaRel();
		m_vs_a_funcs.add(elB.getMagAreaFunction(1, 10000, 101));
		m_vs_a_funcs.add(hb.getMagAreaFunction(1, 10000, 101));
		m_vs_a_funcs.add(wc.getMagAreaFunction(1, 10000, 101));
		m_vs_a_funcs.add(sh.getMagAreaFunction(1, 10000, 101));
		m_vs_a_funcs.add(m_vs_a_data);	// do this after the above so it plots underneath
		GraphiWindowAPI_Impl m_vs_a_graph = new GraphiWindowAPI_Impl(m_vs_a_funcs, "Mag vs Area");   
		m_vs_a_graph.setY_AxisLabel("Magnitude (Mw)");
		m_vs_a_graph.setX_AxisLabel("Area (km-sq)");
		ArrayList<PlotCurveCharacterstics> m_vs_a_curveChar = new ArrayList<PlotCurveCharacterstics>();
		m_vs_a_curveChar.add(new PlotCurveCharacterstics(PlotColorAndLineTypeSelectorControlPanel.SOLID_LINE, Color.BLACK, 3));
		m_vs_a_curveChar.add(new PlotCurveCharacterstics(PlotColorAndLineTypeSelectorControlPanel.SOLID_LINE, Color.BLUE, 3));
		m_vs_a_curveChar.add(new PlotCurveCharacterstics(PlotColorAndLineTypeSelectorControlPanel.SOLID_LINE, Color.GREEN, 3));
		m_vs_a_curveChar.add(new PlotCurveCharacterstics(PlotColorAndLineTypeSelectorControlPanel.SOLID_LINE, Color.MAGENTA, 3));
		m_vs_a_curveChar.add(new PlotCurveCharacterstics(PlotColorAndLineTypeSelectorControlPanel.CIRCLES, Color.RED, 3));
		m_vs_a_graph.setPlottingFeatures(m_vs_a_curveChar);
		m_vs_a_graph.setXLog(true);
		m_vs_a_graph.setY_AxisRange(4.5, 8.5);
	/**/
		// MAG VS LENGTH PLOT
		XY_DataSet m_vs_l_data = new XY_DataSet(length,mag);
		m_vs_l_data.setName("Mag vs Length");
		m_vs_l_data.setInfo(" ");
		ArrayList m_vs_l_funcs = new ArrayList();
		m_vs_l_funcs.add(m_vs_l_data);
		GraphiWindowAPI_Impl m_vs_l_graph = new GraphiWindowAPI_Impl(m_vs_l_funcs, "Mag vs Length");   
		m_vs_l_graph.setY_AxisLabel("Magnitude (Mw)");
		m_vs_l_graph.setX_AxisLabel("Length (km)");
		ArrayList<PlotCurveCharacterstics> m_vs_l_curveChar = new ArrayList<PlotCurveCharacterstics>();
		m_vs_l_curveChar.add(new PlotCurveCharacterstics(PlotColorAndLineTypeSelectorControlPanel.CIRCLES, Color.GREEN, 3));
		m_vs_l_graph.setPlottingFeatures(m_vs_l_curveChar);
		m_vs_l_graph.setXLog(true);
		m_vs_l_graph.setY_AxisRange(4.5, 8.5);

	}
	
	
	/**
	 * 
	 * @param elemID
	 * @param magThresh
	 * @return
	 */
	public double[] getRecurIntervalsForElement(int elemID, double magThresh) {
		ArrayList<Double> eventTimes = new ArrayList<Double>();
		for(EQSIM_Event event:eventList)
			if(event.getAllElementIDs().contains(elemID) && event.getMagnitude() >= magThresh)
				eventTimes.add(event.getTimeInYears());
		if (eventTimes.size()>0) {
			double[] intervals = new double[eventTimes.size()-1];
			for(int i=1;i<eventTimes.size();i++)
				intervals[i-1] = (eventTimes.get(i)-eventTimes.get(i-1));
			return intervals;
		}
		else return null;
	}
	
	
	/**
	 * 
	 * @param magThresh
	 */
	public void plotNormRecurIntsForAllSurfaceElements(double magThresh) {

		int totNum=0;

		// Make distribution function; bins for dist are 0.05, with 201 bins (from 0 to 10)
		double delta=0.05;
		int num = (int)Math.round(50/0.05)+1;
		EvenlyDiscretizedFunc riHist = new EvenlyDiscretizedFunc(delta/2, num, delta);
		riHist.setTolerance(10*delta);

		// Loop over elements
		for(RectangularElement elem:rectElementsList) {
			// check whether it's a surface element
			if(elem.getVertices()[0].getTraceFlag() != 0) {
//				System.out.println("trace vertex found");
				double[] recurInts = getRecurIntervalsForElement(elem.getID(), magThresh);
				if(recurInts != null) {
					double mean=0;
					for(int i=0;i<recurInts.length; i++) 
						mean += recurInts[i]/recurInts.length;
					for(int i=0;i<recurInts.length; i++) {
						riHist.add(recurInts[i]/mean, 1.0);
						totNum +=1;
					}					
				}
			}
		}
		ArrayList<EvenlyDiscretizedFunc> funcList = new ArrayList<EvenlyDiscretizedFunc>();
		funcList.add(riHist);
		GraphiWindowAPI_Impl graph = new GraphiWindowAPI_Impl(funcList, "Recurence Intervals for All Surface Elments"); 
		graph.setX_AxisLabel("RI (yrs)");
		graph.setY_AxisLabel("Number of Observations");
		ArrayList<PlotCurveCharacterstics> curveCharacteristics = new ArrayList<PlotCurveCharacterstics>();
		curveCharacteristics.add(new PlotCurveCharacterstics(PlotColorAndLineTypeSelectorControlPanel.HISTOGRAM, Color.RED, 2));
		graph.setPlottingFeatures(curveCharacteristics);

	}

	
	
	/**
	 * This one only includes events that utilize the nearest rectangular element (presumably the 
	 * one at the surface), which means non-surface rupturing events will not be included
	 * @param lat
	 * @param lon
	 * @param magThresh
	 * @param makePlot
	 * @return
	 */
	public double[] getRecurIntervalsForNearestLoc(double lat, double lon, double magThresh, boolean makePlot) {
		Location loc = new Location(lat,lon);
		double recurInt =0;
		double minDist= Double.MAX_VALUE;
		int elementIndex=-1;
		//Find nearest Element
		for(int i=0; i<rectElementsList.size(); i++) {
			double dist = DistanceRupParameter.getDistance(loc, rectElementsList.get(i).getGriddedSurface());
			if(dist<minDist){
				minDist=dist;
				elementIndex= i;
			}
		}
		Integer elemID = rectElementsList.get(elementIndex).getID();
		System.out.println("Closest Element to loc is rect elem ID "+elemID+
				" on "+rectElementsList.get(elementIndex).getSectionName()+" ("+minDist+" km away)");
		
		double[] intervals = getRecurIntervalsForElement(elemID, magThresh);
		double maxInterval=0;
		for(int i=1;i<intervals.length;i++) {
			if(intervals[i]>maxInterval) maxInterval = intervals[i];
		}
		
		System.out.println("number of RIs for loc is "+intervals.length);
		
		// calc num bins at 10-year intervals
		int numBins = (int)Math.ceil(maxInterval/10.0);
		EvenlyDiscretizedFunc riHist = new EvenlyDiscretizedFunc(5.0, numBins, 10.0);
		riHist.setTolerance(20.0);  // anything more than 10 should do it
		
		for(int i=0; i<intervals.length;i++)
			riHist.add(intervals[i], 1.0);
		
		if(makePlot){
			ArrayList<EvenlyDiscretizedFunc> funcList = new ArrayList<EvenlyDiscretizedFunc>();
			funcList.add(riHist);
			GraphiWindowAPI_Impl graph = new GraphiWindowAPI_Impl(funcList, "Recurence Intervals"); 
			graph.setX_AxisLabel("RI (yrs)");
			graph.setY_AxisLabel("Number of Observations");
			ArrayList<PlotCurveCharacterstics> curveCharacteristics = new ArrayList<PlotCurveCharacterstics>();
			curveCharacteristics.add(new PlotCurveCharacterstics(PlotColorAndLineTypeSelectorControlPanel.HISTOGRAM, Color.BLUE, 2));
			graph.setPlottingFeatures(curveCharacteristics);


		}

		return intervals;
	}
	
	
	/**
	 * This version includes events that pass anywhere below the site (by using DAS values the way Keith Richards-Dinger does it)
	 * @param lat
	 * @param lon
	 * @param magThresh
	 * @param makePlot
	 * @return
	 */
	public double[] getRecurIntervalsForNearestLoc2(double lat, double lon, double magThresh, boolean makePlot) {
		Location loc = new Location(lat,lon);
		double minDist= Double.MAX_VALUE;
		int vertexIndex=-1;
		//Find nearest Element
		for(int i=0; i<vertexList.size(); i++) {
			double dist = LocationUtils.linearDistance(loc, vertexList.get(i));
			if(dist<minDist){
				minDist=dist;
				vertexIndex= i;
			}
		}
		Vertex closestVertex = vertexList.get(vertexIndex);
		// Find 2nd closest vertex
		double secondMinDist= Double.MAX_VALUE;
		int secondClosestVertexIndex=-1;
		//Find nearest Element
		for(int i=0; i<vertexList.size(); i++) {
			double dist = LocationUtils.linearDistance(loc, vertexList.get(i));
			if(dist<secondMinDist && i != vertexIndex){
				secondMinDist=dist;
				secondClosestVertexIndex= i;
			}
		}
		Vertex secondClosestVertex = vertexList.get(secondClosestVertexIndex);

		
		double das = 1000*(closestVertex.getDAS()*minDist+secondClosestVertex.getDAS()*secondMinDist)/(minDist+secondMinDist); // convert to meters for comparisons below
		int sectIndex = -1;
		// find the section index for the closest vertex
		for(int i=0; i<vertexListForSections.size(); i++)
			if(vertexListForSections.get(i).contains(closestVertex))
				sectIndex = i;
		int sectID = sectIndex+1;
				
		System.out.println("RI PDF at site ("+lat+","+lon+"):\n\tClosest vertex ID is "+closestVertex.getID()+" & second closest vertex ID "+secondClosestVertex.getID()+
				", on section "+namesOfSections.get(sectIndex)+" at average DAS of "+(float)das+"; site is "+(float)minDist+" km away from closest vertex.");

		ArrayList<Double> eventTimes = new ArrayList<Double>();
//		System.out.println("Events Included:\n\t\teventID\teventMag\teventTime");
//		int num=0;
		for(EQSIM_Event event:eventList) {
			if(event.getMagnitude() >= magThresh && event.doesEventIncludeSectionAndDAS(sectID,das)) {
						eventTimes.add(event.getTimeInYears());
//						num+=1;
//						System.out.println("\t"+num+"\t"+event.getID()+"\t"+event.getMagnitude()+"\t"+event.getTime());
			}
		}
		double[] intervals = new double[eventTimes.size()-1];
		double maxInterval=0;
		for(int i=1;i<eventTimes.size();i++) {
			intervals[i-1] = (eventTimes.get(i)-eventTimes.get(i-1));
			if(intervals[i-1]>maxInterval) maxInterval = intervals[i-1];
		}
		
		System.out.println("\tnumber of RIs for loc is "+intervals.length);
		
		// calc num bins at 10-year intervals
		int numBins = (int)Math.ceil(maxInterval/10.0);
		EvenlyDiscretizedFunc riHist = new EvenlyDiscretizedFunc(5.0, numBins, 10.0);
		riHist.setTolerance(20.0);  // anything more than 10 should do it
		
		for(int i=0; i<intervals.length;i++)
			riHist.add(intervals[i], 1.0);
		
		if(makePlot){
			ArrayList<EvenlyDiscretizedFunc> funcList = new ArrayList<EvenlyDiscretizedFunc>();
			funcList.add(riHist);
			GraphiWindowAPI_Impl graph = new GraphiWindowAPI_Impl(funcList, "Recurence Intervals"); 
			graph.setX_AxisLabel("RI (yrs)");
			graph.setY_AxisLabel("Number Observed");
			ArrayList<PlotCurveCharacterstics> curveCharacteristics = new ArrayList<PlotCurveCharacterstics>();
			curveCharacteristics.add(new PlotCurveCharacterstics(PlotColorAndLineTypeSelectorControlPanel.HISTOGRAM, Color.BLUE, 2));
			graph.setPlottingFeatures(curveCharacteristics);
		}

		return null;
	}



	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		long startTime=System.currentTimeMillis();
		System.out.println("Starting");
		
		/*
		// this is for analysis of the Ward Results:
		String fullPath = "org/opensha/sha/simulators/eqsim_v04/WardsInputFile/test.txt";
		// I had to rename the file "NCAL(9/1/10)-elements.dat.txt" to test.txt to get this to work
		General_EQSIM_Tools test = new General_EQSIM_Tools(fullPath, 1);
		test.read_EQSIMv04_EventsFile("org/opensha/sha/simulators/eqsim_v04/ExamplesFromKeith/NCAL_Ward.out.txt");
		test.checkEventMagnitudes();
		test.checkElementSlipRates("testSlipRateFileForWard");
		test.testTimePredictability(6.5, "testTimePredFileForWard_M6pt5");
		 */
		
		// this is for analysis of the RQSim Results:
		String fullPath = "org/opensha/sha/simulators/eqsim_v04/ExamplesFromKeith/NCA_Ward_Geometry.dat.txt";
		General_EQSIM_Tools test = new General_EQSIM_Tools(fullPath);
		ArrayList<String> sectNames = test.getSectionsNameList();
//		System.out.println("Section Names (IDs)");
//		for(int s=0; s<sectNames.size();s++)	System.out.println("\t"+sectNames.get(s)+"("+(s+1)+")");
		test.read_EQSIMv04_EventsFile("org/opensha/sha/simulators/eqsim_v04/ExamplesFromKeith/eqs.NCA_RSQSim.barall.txt");
//		test.checkEventMagnitudes();
//		test.checkElementSlipRates("testSlipRateFileForEQSim", true);
		System.out.println("Simulation Duration is "+(float)test.getSimulationDurationInYears()+" years");
//		test.randomizeEventTimes();
//		test.plotYearlyEventRates();
//		test.plotScalingRelationships();
//		test.plotNormRecurIntsForAllSurfaceElements(6.5);
//		test.getRecurIntervalsForNearestLoc(36.9415,  -121.6729, 6.5, true);
		test.testTimePredictability(6.5, "testTimePredFileForEQSim_M6pt5_rand");
//		ArbIncrementalMagFreqDist mfd = test.computeTotalMagFreqDist(4.05,9.05,51,true);
//		ArrayList<ArbIncrementalMagFreqDist> funcs = test.computeMagFreqDistByFaultSection(4.05,9.05,51,true,false);
		
		/*
		ArbIncrementalMagFreqDist mfd = test.getMagFreqDist(4.0,9.0,51);
		ArrayList funcs = new ArrayList();
		funcs.add(mfd);
		
		XY_DataSet testScatter = new XY_DataSet();
		for(int i=0; i<mfd.getNum();i++)
			testScatter.set(mfd.getX(i), mfd.getY(i)*(Math.random()+0.5));
		testScatter.setName("Scatter Plot Name");
		testScatter.setInfo("Scatter Plot Info");
		funcs.add(testScatter);
		GraphiWindowAPI_Impl graph = new GraphiWindowAPI_Impl(funcs, "Mag Freq Dist");   
*/

		/*
		ArbitrarilyDiscretizedFunc func = new ArbitrarilyDiscretizedFunc();
		XY_DataSet scatter = new XY_DataSet();
		for (double x=0; x<getMaxX(); x++) {
			double y = x * 0.8;
			y += r.nextDouble() - 0.5d;
			System.out.println("Adding " + x + ", " + y);
			func.set(x, y);
			
			scatter.set(x, y + r.nextDouble()*0.5);
			scatter.set(x, y - r.nextDouble()*0.5);
		}
		
		funcs.add(func);
		funcs.add(scatter);
		*/

		
		/*  This writes an EQSIM file from a UCERF2 deformation model
		General_EQSIM_Tools test = new General_EQSIM_Tools(82, false, 4.0);
//		test.getElementsList();
		String writePath = "testEQSIM_Output.txt";
		try {
			test.writeTo_EQSIM_V04_GeometryFile(writePath, null, "test UCERF2 output", "Ned Field", "Aug 3, 2010");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/

		
		/*
		// THE FOLLOWING TEST LOOKS GOOD FROM A VISUAL INSPECTION
		String fullPath = "org/opensha/sha/simulators/eqsim_v04/ExamplesFromKeith/NCA_Ward_Geometry.dat.txt";
//		String fullPath = "org/opensha/sha/simulators/eqsim_v04/ALLCAL_Model_v04/ALLCAL_Ward_Geometry.dat";
		General_EQSIM_Tools test = new General_EQSIM_Tools(fullPath);
		test.read_EQSIMv04_EventsFile("org/opensha/sha/simulators/eqsim_v04/ExamplesFromKeith/NCAL_Ward.out.txt");
//		test.read_EQSIMv04_EventsFile("org/opensha/sha/simulators/eqsim_v04/ExamplesFromKeith/VC_norcal.d.txt");
//		test.read_EQSIMv04_EventsFile("org/opensha/sha/simulators/eqsim_v04/ExamplesFromKeith/eqs.NCA_RSQSim.barall.txt");


		System.out.println(test.getNumEventsWithElementSlipData()+" out of "+test.getEventsList().size()+" have slip on elements data");

		// find the mag cutoff for inclusion of element slip data
		double maxWithOut = 0;
		double minWith =10;
		for(EQSIM_Event event:test.getEventsList()) {
			if(event.hasElementSlipsAndIDs()) {
				if (event.getMagnitude()<minWith) minWith = event.getMagnitude();
			}
			else
				if(event.getMagnitude()>maxWithOut) maxWithOut = event.getMagnitude();
		}
		System.out.println("minWith="+minWith+";\tmaxWithOut="+maxWithOut);
		
		*/
						
		int runtime = (int)(System.currentTimeMillis()-startTime)/1000;
		System.out.println("This Run took "+runtime+" seconds");
	}
}
