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

package org.opensha.sha.gui;


import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;

import org.opensha.commons.gui.DisclaimerDialog;
import org.opensha.commons.util.FileUtils;
import org.opensha.gem.GEM1.scratch.marco.testParsers.GEM1ERF;
import org.opensha.sha.calc.HazardCurveCalculator;
import org.opensha.sha.calc.disaggregation.DisaggregationCalculator;
import org.opensha.sha.cybershake.openshaAPIs.CyberShakeUCERFWrapper_ERF;
import org.opensha.sha.earthquake.EqkRupForecastBaseAPI;
import org.opensha.sha.earthquake.rupForecastImpl.FloatingPoissonFaultERF;
import org.opensha.sha.earthquake.rupForecastImpl.PointSourceERF;
import org.opensha.sha.earthquake.rupForecastImpl.PoissonFaultERF;
import org.opensha.sha.earthquake.rupForecastImpl.Frankel02.Frankel02_AdjustableEqkRupForecast;
import org.opensha.sha.earthquake.rupForecastImpl.Frankel96.Frankel96_AdjustableEqkRupForecast;
import org.opensha.sha.earthquake.rupForecastImpl.Frankel96.Frankel96_EqkRupForecast;
import org.opensha.sha.earthquake.rupForecastImpl.GEM.TestGEM_ERF;
import org.opensha.sha.earthquake.rupForecastImpl.GEM1.GEM1SouthAmericaERF;
import org.opensha.sha.earthquake.rupForecastImpl.GEM1.GEM1_CEUS_ERF;
import org.opensha.sha.earthquake.rupForecastImpl.GEM1.GEM1_GSHAP_Africa_ERF;
import org.opensha.sha.earthquake.rupForecastImpl.GEM1.GEM1_GSHAP_SE_Asia_ERF;
import org.opensha.sha.earthquake.rupForecastImpl.GEM1.GEM1_NSHMP_SE_Asia_ERF;
import org.opensha.sha.earthquake.rupForecastImpl.GEM1.GEM1_WEUS_ERF;
import org.opensha.sha.earthquake.rupForecastImpl.PEER_TestCases.PEER_AreaForecast;
import org.opensha.sha.earthquake.rupForecastImpl.PEER_TestCases.PEER_LogicTreeERF_List;
import org.opensha.sha.earthquake.rupForecastImpl.PEER_TestCases.PEER_MultiSourceForecast;
import org.opensha.sha.earthquake.rupForecastImpl.PEER_TestCases.PEER_NonPlanarFaultForecast;
import org.opensha.sha.earthquake.rupForecastImpl.Point2MultVertSS_Fault.Point2MultVertSS_FaultERF;
import org.opensha.sha.earthquake.rupForecastImpl.Point2MultVertSS_Fault.Point2MultVertSS_FaultERF_List;
import org.opensha.sha.earthquake.rupForecastImpl.WG02.WG02_ERF_Epistemic_List;
import org.opensha.sha.earthquake.rupForecastImpl.WG02.WG02_EqkRupForecast;
import org.opensha.sha.earthquake.rupForecastImpl.WGCEP_UCERF1.WGCEP_UCERF1_EqkRupForecast;
import org.opensha.sha.earthquake.rupForecastImpl.WGCEP_UCERF_2_Final.UCERF2;
import org.opensha.sha.earthquake.rupForecastImpl.WGCEP_UCERF_2_Final.UCERF2_TimeIndependentEpistemicList;
import org.opensha.sha.earthquake.rupForecastImpl.WGCEP_UCERF_2_Final.MeanUCERF2.MeanUCERF2;
import org.opensha.sha.earthquake.rupForecastImpl.YuccaMountain.YuccaMountainERF;
import org.opensha.sha.earthquake.rupForecastImpl.YuccaMountain.YuccaMountainERF_List;
import org.opensha.sha.earthquake.rupForecastImpl.step.STEP_AlaskanPipeForecast;
import org.opensha.sha.gui.beans.ERF_GuiBean;
import org.opensha.sha.gui.beans.EqkRupSelectorGuiBean;
import org.opensha.sha.gui.infoTools.ApplicationVersionInfoWindow;
import org.opensha.sha.gui.infoTools.ExceptionWindow;
import org.opensha.sha.gui.util.IconFetcher;

import scratch.christine.URS.URS_MeanUCERF2;



/**
 * <p>Title: HazardCurveLocalModeApplication</p>
 * <p>Description: This application is extension of HazardCurveApplication, where
 * everything take place on the user's own machine. This version of application
 * does not require any internet connection, the only difference between this
 * application and its parent class that it uses user's system memory for doing
 * any computation. Whereas , in the HazardCurve application all calculations
 * take place on the users machine.</p>
 * @author : Nitin Gupta and Vipin Gupta
 * @version 1.0
 */

public class HazardCurveLocalModeApplication extends HazardCurveServerModeApplication {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final String APP_NAME = "Hazard Curve Local Mode Application";
	public static final String APP_SHORT_NAME = "HazardCurveLocal";

	public static ArrayList<String> getLocalERFClasses() {
		ArrayList<String> erf_Classes = new ArrayList<String>();

		//adding the client based ERF's to the application
		erf_Classes.add(Frankel96_AdjustableEqkRupForecast.class.getName());
		erf_Classes.add(GEM1ERF.class.getName());
//		erf_Classes.add(POINT_SRC_TO_LINE_ERF_CLASS_NAME);
//		erf_Classes.add(POINT_SRC_TO_LINE_ERF_LIST_TEST_CLASS_NAME);
		erf_Classes.add(URS_MeanUCERF2.class.getName());
		erf_Classes.add(Frankel96_EqkRupForecast.class.getName());
		erf_Classes.add(Frankel02_AdjustableEqkRupForecast.class.getName());
		//      erf_Classes.add(NSHMP08_CEUS_ERF_CLASS_NAME);
		erf_Classes.add(WG02_ERF_Epistemic_List.class.getName());
		erf_Classes.add(WGCEP_UCERF1_EqkRupForecast.class.getName());
		erf_Classes.add(PEER_AreaForecast.class.getName());
		erf_Classes.add(PEER_NonPlanarFaultForecast.class.getName());
		erf_Classes.add(PEER_MultiSourceForecast.class.getName());
		erf_Classes.add(PEER_LogicTreeERF_List.class.getName());
		//erf_Classes.add(STEP_FORECAST_CLASS_NAME);
		erf_Classes.add(STEP_AlaskanPipeForecast.class.getName());
//		erf_Classes.add(OLD_POISSON_FAULT_ERF_CLASS_NAME);
		erf_Classes.add(FloatingPoissonFaultERF.class.getName());
		erf_Classes.add(PoissonFaultERF.class.getName());
		erf_Classes.add(PointSourceERF.class.getName());
		erf_Classes.add(Point2MultVertSS_FaultERF.class.getName());
		erf_Classes.add(Point2MultVertSS_FaultERF_List.class.getName());
		
		erf_Classes.add(UCERF2.class.getName());
		erf_Classes.add(UCERF2_TimeIndependentEpistemicList.class.getName());
		erf_Classes.add(MeanUCERF2.class.getName());
		
		erf_Classes.add(YuccaMountainERF.class.getName());
		erf_Classes.add(YuccaMountainERF_List.class.getName());
		
		erf_Classes.add(CyberShakeUCERFWrapper_ERF.class.getName());
		erf_Classes.add(TestGEM_ERF.class.getName());
		erf_Classes.add(GEM1SouthAmericaERF.class.getName());
		erf_Classes.add(GEM1_CEUS_ERF.class.getName());
		erf_Classes.add(GEM1_WEUS_ERF.class.getName());
		erf_Classes.add(GEM1_GSHAP_Africa_ERF.class.getName());
		erf_Classes.add(GEM1_GSHAP_SE_Asia_ERF.class.getName());
		erf_Classes.add(GEM1_NSHMP_SE_Asia_ERF.class.getName());
//		erf_Classes.add(GEM1_SS_ERF_CLASS_NAME);
		//      erf_Classes.add(CYBERSHAKE_ERF_LIST_CLASS_NAME);
		//      erf_Classes.add(CYBERSHAKE_ERF_WRAPPER_LIST_CLASS_NAME);
//		erf_Classes.add(NZ_ERF0909_CLASS_NAME);
		
		return erf_Classes;
	}


	/**
	 * Initialize the ERF Gui Bean
	 */
	protected void initERF_GuiBean() {

		if(erfGuiBean == null){
			// create the ERF Gui Bean object
			ArrayList<String> erf_Classes = getLocalERFClasses();

			try {
				erfGuiBean = new ERF_GuiBean(erf_Classes);
				erfGuiBean.getParameter(ERF_GuiBean.ERF_PARAM_NAME).
				addParameterChangeListener(this);
			}
			catch (InvocationTargetException e) {

				ExceptionWindow bugWindow = new ExceptionWindow(this, e,
						"Problem occured " +
				"during initialization the ERF's. All parameters are set to default.");
				bugWindow.setVisible(true);
				bugWindow.pack();
				//e.printStackTrace();
				//throw new RuntimeException("Connection to ERF's failed");
			}
		}
		else{
			boolean isCustomRupture = erfRupSelectorGuiBean.isCustomRuptureSelected();
			if(!isCustomRupture){
				EqkRupForecastBaseAPI eqkRupForecast = erfRupSelectorGuiBean.getSelectedEqkRupForecastModel();
				erfGuiBean.setERF(eqkRupForecast);
			}
		}
//		erfPanel.removeAll(); TODO clean
//		erfPanel.add(erfGuiBean, new GridBagConstraints( 0, 0, 1, 1, 1.0, 1.0,
//				GridBagConstraints.CENTER,GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0 ));
//		erfPanel.updateUI();
	}


	/**
	 * Initialize the ERF Rup Selector Gui Bean
	 */
	protected void initERFSelector_GuiBean() {

		EqkRupForecastBaseAPI erf = null;
		try {
			erf = erfGuiBean.getSelectedERF();
		}
		catch (InvocationTargetException ex) {
			ex.printStackTrace();
		}
		if(erfRupSelectorGuiBean == null){
			// create the ERF Gui Bean object
			ArrayList<String> erf_Classes = new ArrayList<String>();

			/**
			 *  The object class names for all the supported Eqk Rup Forecasts
			 */
			erf_Classes.add(PoissonFaultERF.class.getName());
			erf_Classes.add(Frankel96_AdjustableEqkRupForecast.class.getName());
			erf_Classes.add(UCERF2.class.getName());
			erf_Classes.add(UCERF2_TimeIndependentEpistemicList.class.getName());
			erf_Classes.add(MeanUCERF2.class.getName());
			//erf_Classes.add(STEP_FORECAST_CLASS_NAME);
			erf_Classes.add(STEP_AlaskanPipeForecast.class.getName());
			erf_Classes.add(FloatingPoissonFaultERF.class.getName());
			erf_Classes.add(Frankel02_AdjustableEqkRupForecast.class.getName());
			erf_Classes.add(PEER_AreaForecast.class.getName());
			erf_Classes.add(PEER_NonPlanarFaultForecast.class.getName());
			erf_Classes.add(PEER_MultiSourceForecast.class.getName());
			erf_Classes.add(WG02_EqkRupForecast.class.getName());


			try {

				erfRupSelectorGuiBean = new EqkRupSelectorGuiBean(erf,erf_Classes);
			}
			catch (InvocationTargetException e) {
				throw new RuntimeException("Connection to ERF's failed");
			}
		}
		else
			erfRupSelectorGuiBean.setEqkRupForecastModel(erf);
//		erfPanel.removeAll(); TODO clean
//		//erfGuiBean = null;
//		erfPanel.add(erfRupSelectorGuiBean, new GridBagConstraints( 0, 0, 1, 1, 1.0, 1.0,
//				GridBagConstraints.CENTER,GridBagConstraints.BOTH, defaultInsets, 0, 0 ));
//		erfPanel.updateUI();
	}

	/**
	 * This method creates the HazardCurveCalc and Disaggregation Calc(if selected) instances.
	 * Calculations are performed on the user's own machine, no internet connection
	 * is required for it.
	 */
	protected void createCalcInstance(){
		try{
			if(calc == null) {
				calc = new HazardCurveCalculator();
				if(this.calcParamsControl != null)
					calc.setAdjustableParams(calcParamsControl.getAdjustableCalcParams());
//System.out.println("Created new calc from LocalModeApp");
			}
			if(disaggregationFlag)
				if(disaggCalc == null)
					disaggCalc = new DisaggregationCalculator();
		}catch(Exception e){
			e.printStackTrace();
			ExceptionWindow bugWindow = new ExceptionWindow(this,e,this.getParametersInfoAsString());
			bugWindow.setVisible(true);
			bugWindow.pack();
			//     e.printStackTrace();
		}
		
	}

	public static void main(String[] args) throws IOException {
		new DisclaimerDialog(APP_NAME, APP_SHORT_NAME, getAppVersion());
		HazardCurveLocalModeApplication applet = new HazardCurveLocalModeApplication();
		applet.init();
		applet.setTitle("Hazard Curve Local mode Application "+"("+getAppVersion()+")" );
		applet.setIconImages(IconFetcher.fetchIcons(APP_SHORT_NAME));
		applet.setVisible(true);
		applet.createCalcInstance();
	}
}
