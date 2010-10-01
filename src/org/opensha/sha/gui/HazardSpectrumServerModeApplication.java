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
import java.rmi.RemoteException;
import java.util.ArrayList;

import org.opensha.commons.gui.DisclaimerDialog;
import org.opensha.commons.util.FileUtils;
import org.opensha.sha.calc.SpectrumCalculator;
import org.opensha.sha.calc.remoteCalc.RemoteResponseSpectrumClient;
import org.opensha.sha.earthquake.EqkRupForecastBaseAPI;
import org.opensha.sha.earthquake.rupForecastImpl.remoteERF_Clients.FloatingPoissonFaultERF_Client;
import org.opensha.sha.earthquake.rupForecastImpl.remoteERF_Clients.Frankel02_AdjustableEqkRupForecastClient;
import org.opensha.sha.earthquake.rupForecastImpl.remoteERF_Clients.Frankel96_AdjustableEqkRupForecastClient;
import org.opensha.sha.earthquake.rupForecastImpl.remoteERF_Clients.PEER_AreaForecastClient;
import org.opensha.sha.earthquake.rupForecastImpl.remoteERF_Clients.PEER_LogicTreeERF_ListClient;
import org.opensha.sha.earthquake.rupForecastImpl.remoteERF_Clients.PEER_MultiSourceForecastClient;
import org.opensha.sha.earthquake.rupForecastImpl.remoteERF_Clients.PEER_NonPlanarFaultForecastClient;
import org.opensha.sha.earthquake.rupForecastImpl.remoteERF_Clients.Point2MultVertSS_FaultERF_Client;
import org.opensha.sha.earthquake.rupForecastImpl.remoteERF_Clients.Point2MultVertSS_FaultERF_ListClient;
import org.opensha.sha.earthquake.rupForecastImpl.remoteERF_Clients.PoissonFaultERF_Client;
import org.opensha.sha.earthquake.rupForecastImpl.remoteERF_Clients.STEP_AlaskanPipeForecastClient;
import org.opensha.sha.earthquake.rupForecastImpl.remoteERF_Clients.STEP_EqkRupForecastClient;
import org.opensha.sha.earthquake.rupForecastImpl.remoteERF_Clients.WG02_FortranWrappedERF_EpistemicListClient;
import org.opensha.sha.earthquake.rupForecastImpl.remoteERF_Clients.WGCEP_UCERF1_EqkRupForecastClient;
import org.opensha.sha.gui.beans.ERF_GuiBean;
import org.opensha.sha.gui.beans.EqkRupSelectorGuiBean;
import org.opensha.sha.gui.infoTools.ApplicationVersionInfoWindow;
import org.opensha.sha.gui.infoTools.ExceptionWindow;
import org.opensha.sha.gui.util.IconFetcher;

/**
 * <p>Title: HazardSpectrumServerModeApplication </p>
 *
 * <p>Description: This class allows the  </p>
 *
 * <p>Copyright: Copyright (c) 2002</p>
 *
 * <p>Company: </p>
 *
 * @author Nitin Gupta
 * @version 1.0
 */
public class HazardSpectrumServerModeApplication
extends HazardSpectrumLocalModeApplication {
	
	public static final String APP_NAME = "Hazard Spectrum Server Mode Application";
	public static final String APP_SHORT_NAME = "HazardSpectrumServer";

	/**
	 * Initialize the ERF Gui Bean
	 */
	protected void initERF_GuiBean() {

		if (erfGuiBean == null) {
			try {
				// create the ERF Gui Bean object
				ArrayList<String> erf_Classes = new ArrayList<String>();
				//adding the RMI based ERF's to the application
				erf_Classes.add(Frankel96_AdjustableEqkRupForecastClient.class.getName());
				erf_Classes.add(WGCEP_UCERF1_EqkRupForecastClient.class.getName());
				// erf_Classes.add(RMI_STEP_FORECAST_CLASS_NAME);
				erf_Classes.add(STEP_AlaskanPipeForecastClient.class.getName());
				erf_Classes.add(FloatingPoissonFaultERF_Client.class.getName());
				erf_Classes.add(Frankel02_AdjustableEqkRupForecastClient.class.getName());
				erf_Classes.add(PEER_AreaForecastClient.class.getName());
				erf_Classes.add(PEER_MultiSourceForecastClient.class.getName());
				erf_Classes.add(PEER_NonPlanarFaultForecastClient.class.getName());
				erf_Classes.add(PoissonFaultERF_Client.class.getName());
				erf_Classes.add(Point2MultVertSS_FaultERF_Client.class.getName());
				erf_Classes.add(WG02_FortranWrappedERF_EpistemicListClient.class.getName());
				erf_Classes.add(PEER_LogicTreeERF_ListClient.class.getName());
				erf_Classes.add(Point2MultVertSS_FaultERF_ListClient.class.getName());

				erfGuiBean = new ERF_GuiBean(erf_Classes);
				erfGuiBean.getParameter(erfGuiBean.ERF_PARAM_NAME).
				addParameterChangeListener(this);
			}
			catch (InvocationTargetException e) {
				ExceptionWindow bugWindow = new ExceptionWindow(this, e,
						"ERF's Initialization problem. Rest all parameters are default");
				bugWindow.setVisible(true);
				bugWindow.pack();
				//e.printStackTrace();
				//throw new RuntimeException("Connection to ERF's failed");
			}
		}
		else {
			boolean isCustomRupture = erfRupSelectorGuiBean.isCustomRuptureSelected();
			if (!isCustomRupture) {
				EqkRupForecastBaseAPI eqkRupForecast = erfRupSelectorGuiBean.
				getSelectedEqkRupForecastModel();
				erfGuiBean.setERF(eqkRupForecast);
			}
		}
		//    erfPanel.removeAll(); TODO clean
		//    erfPanel.add(erfGuiBean, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
		//        GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
		//
		//    erfPanel.updateUI();

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
			ArrayList erf_Classes = new ArrayList();

			/**
			 *  The object class names for all the supported Eqk Rup Forecasts
			 */
			erf_Classes.add(PoissonFaultERF_Client.class.getName());
			erf_Classes.add(Frankel96_AdjustableEqkRupForecastClient.class.getName());
			erf_Classes.add(WGCEP_UCERF1_EqkRupForecastClient.class.getName());
			erf_Classes.add(STEP_EqkRupForecastClient.class.getName());
			erf_Classes.add(STEP_AlaskanPipeForecastClient.class.getName());
			erf_Classes.add(FloatingPoissonFaultERF_Client.class.getName());
			erf_Classes.add(Frankel02_AdjustableEqkRupForecastClient.class.getName());
			erf_Classes.add(PEER_AreaForecastClient.class.getName());
			erf_Classes.add(PEER_NonPlanarFaultForecastClient.class.getName());
			erf_Classes.add(PEER_MultiSourceForecastClient.class.getName());
			erf_Classes.add(WG02_FortranWrappedERF_EpistemicListClient.class.getName());

			try {
				erfRupSelectorGuiBean = new EqkRupSelectorGuiBean(erf,erf_Classes);
			}
			catch (InvocationTargetException e) {
				throw new RuntimeException("Connection to ERF's failed");
			}
		}
		//    erfPanel.removeAll(); TODO clean
		//    //erfGuiBean = null;
		//    erfPanel.add(erfRupSelectorGuiBean,
		//                 new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
		//                                        GridBagConstraints.CENTER,
		//                                        GridBagConstraints.BOTH, new Insets(0,0,0,0), 0,
		//                                        0));
		//    erfPanel.updateUI();
	}

	/**
	 * This method creates the SpectrumCalc s.
	 * If the internet connection is available then it creates a remote instances of
	 * the calculators on server where the calculations take place, else
	 * calculations are performed on the user's own machine.
	 */
	protected void createCalcInstance() {
		try{
			if (calc == null && isProbabilisticCurve) {
				calc = (new RemoteResponseSpectrumClient()).getRemoteSpectrumCalc();
				if(this.calcParamsControl != null)
					try {
						calc.setAdjustableParams(calcParamsControl.getAdjustableCalcParams());
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			else if(calc == null && !isProbabilisticCurve) {
				calc = new SpectrumCalculator();
				calc.setAdjustableParams(calcParamsControl.getAdjustableCalcParams());
			}
		}catch (Exception ex) {
			ExceptionWindow bugWindow = new ExceptionWindow(this,
					ex, this.getParametersInfoAsString());
			bugWindow.setVisible(true);
			bugWindow.pack();
		}
	}

	public static void main(String[] args) throws IOException {
		new DisclaimerDialog(APP_NAME, APP_SHORT_NAME, getAppVersion());
		HazardSpectrumServerModeApplication applet = new
		HazardSpectrumServerModeApplication();
		applet.init();
		applet.setIconImages(IconFetcher.fetchIcons(APP_SHORT_NAME));
		applet.setVisible(true);
	}
}
