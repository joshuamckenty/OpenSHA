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
import java.awt.GridBagConstraints;
import java.awt.Toolkit;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;

import org.opensha.commons.exceptions.RegionConstraintException;
import org.opensha.commons.gui.DisclaimerDialog;
import org.opensha.commons.util.FileUtils;
import org.opensha.sha.earthquake.rupForecastImpl.FloatingPoissonFaultERF;
import org.opensha.sha.earthquake.rupForecastImpl.PoissonFaultERF;
import org.opensha.sha.earthquake.rupForecastImpl.Frankel02.Frankel02_AdjustableEqkRupForecast;
import org.opensha.sha.earthquake.rupForecastImpl.Frankel96.Frankel96_AdjustableEqkRupForecast;
import org.opensha.sha.earthquake.rupForecastImpl.GEM.TestGEM_ERF;
import org.opensha.sha.earthquake.rupForecastImpl.GEM.TestSubductionZoneERF;
import org.opensha.sha.earthquake.rupForecastImpl.WG02.WG02_EqkRupForecast;
import org.opensha.sha.earthquake.rupForecastImpl.WGCEP_UCERF1.WGCEP_UCERF1_EqkRupForecast;
import org.opensha.sha.earthquake.rupForecastImpl.WGCEP_UCERF_2_Final.MeanUCERF2.MeanUCERF2;
import org.opensha.sha.gui.beans.EqkRupSelectorGuiBean;
import org.opensha.sha.gui.infoTools.ApplicationVersionInfoWindow;
import org.opensha.sha.gui.util.IconFetcher;

/**
 * <p>Title: ScenarioShakeMapLocalModeCalcApplication</p>
 *
 * <p>Description: This application allows user to run this application
 * without having to open non standard ports to get the Earthquake Rupture
 * Forecast(ERF). All the ERF's are generated on the user's machine and
 * so all the ScenarioShakemap calculations are done on the user's machine.</p>
 *
 * @author Nitin Gupta
 * @version 1.0
 */
public class ScenarioShakeMapLocalModeCalcApp
extends ScenarioShakeMapApp {
	
	public static final String APP_NAME = "Scenario ShakeMap Local Mode Application";
	public static final String APP_SHORT_NAME = "ScenarioShakeMapLocal";

	/**
	 * Initialize the ERF Gui Bean
	 */
	protected void initERFSelector_GuiBean() {
		// create the ERF Gui Bean object
		ArrayList<String> erf_Classes = new ArrayList<String>();

		/**
		 *  The object class names for all the supported Eqk Rup Forecasts
		 */
//		erf_Classes.add(TestGEM_ERF.class.getName());
		erf_Classes.add(PoissonFaultERF.class.getName());
		erf_Classes.add(MeanUCERF2.class.getName());
		erf_Classes.add(Frankel96_AdjustableEqkRupForecast.class.getName());
		//erf_Classes.add(STEP_FORECAST_CLASS_NAME);
		//   erf_Classes.add(STEP_ALASKA_ERF_CLASS_NAME);
		erf_Classes.add(FloatingPoissonFaultERF.class.getName());
		erf_Classes.add(Frankel02_AdjustableEqkRupForecast.class.getName());
		//   erf_Classes.add(PEER_AREA_FORECAST_CLASS_NAME);
		//   erf_Classes.add(PEER_NON_PLANAR_FAULT_FORECAST_CLASS_NAME);
		//   erf_Classes.add(PEER_MULTI_SOURCE_FORECAST_CLASS_NAME);
		erf_Classes.add(WG02_EqkRupForecast.class.getName());
		erf_Classes.add(WGCEP_UCERF1_EqkRupForecast.class.getName());
		erf_Classes.add(TestSubductionZoneERF.class.getName());

		try {
			erfGuiBean = new EqkRupSelectorGuiBean(erf_Classes);
		}
		catch (InvocationTargetException e) {
			throw new RuntimeException("Connection to ERF's failed");
		}
		eqkRupPanel.add(erfGuiBean, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, defaultInsets, 0, 0));
		calculationFromServer = false;
	}

	//Main method
	public static void main(String[] args) throws IOException {
		new DisclaimerDialog(APP_NAME, APP_SHORT_NAME, getAppVersion());
		ScenarioShakeMapLocalModeCalcApp applet = new ScenarioShakeMapLocalModeCalcApp();
		applet.init();
		applet.setIconImages(IconFetcher.fetchIcons(APP_SHORT_NAME));
		applet.setVisible(true);
	}

	/**
	 * This function sets the Gridded region Sites and the type of plot user wants to see
	 * IML@Prob or Prob@IML and it value.
	 * This function also gets the selected AttenuationRelationships in a ArrayList and their
	 * corresponding relative wts.
	 * This function also gets the mode of map calculation ( on server or on local machine)
	 */
	public void getGriddedSitesMapTypeAndSelectedAttenRels() throws
	RegionConstraintException, RuntimeException {
		//gets the IML or Prob selected value
		getIMLorProb();

		//get the site values for each site in the gridded region
		getGriddedRegionSites();

		//selected IMRs Wts
		attenRelWts = imrGuiBean.getSelectedIMR_Weights();
		//selected IMR's
		attenRel = imrGuiBean.getSelectedIMRs();
	}

}
