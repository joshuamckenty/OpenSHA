package org.opensha.sha.earthquake.rupForecastImpl.remoteERF_Clients;


import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

import org.junit.Before;
import org.junit.Test;
import org.opensha.commons.util.RMIUtils;
import org.opensha.commons.util.ServerPrefs;
import org.opensha.sha.earthquake.rupForecastImpl.remote.RegisterRemoteERF_Factory;
import org.opensha.sha.earthquake.rupForecastImpl.remote.RegisterRemoteERF_ListFactory;

import util.TestUtils;

public class Test_RMI_ERFs_Operational {

	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void testFrankel96() throws Throwable {
		TestUtils.runTestWithTimer("runFrankel96", this, 60);
	}
	
	@SuppressWarnings("unused")
	private void runFrankel96() {
		Frankel96_AdjustableEqkRupForecastClient erf = null;
		try {
			erf = new Frankel96_AdjustableEqkRupForecastClient();
		} catch (RemoteException e) {
			e.printStackTrace();
			fail("RemoteException: " + e.getMessage());
		} catch (MalformedURLException e) {
			e.printStackTrace();
			fail("MalformedURLException: " + e.getMessage());
		} catch (NotBoundException e) {
			e.printStackTrace();
			fail("NotBoundException: " + e.getMessage());
		}
		assertNotNull("ERF should not be null!", erf);
		erf.updateForecast();
	}
	
	@Test
	public void testWG02_Fortran() throws Throwable {
		TestUtils.runTestWithTimer("runWG02", this, 60);
	}
	
	@SuppressWarnings("unused")
	private void runWG02() {
		WG02_FortranWrappedERF_EpistemicListClient erf = null;
		try {
			erf = new WG02_FortranWrappedERF_EpistemicListClient();
		} catch (RemoteException e) {
			e.printStackTrace();
			fail("RemoteException: " + e.getMessage());
		}
		assertNotNull("ERF should not be null!", erf);
		try {
			erf.updateForecast();
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			fail("NullPointerException: " + e.getMessage());
		}
	}
	
	@Test
	public void testProdDevPorts() throws Throwable {
		TestUtils.runTestWithTimer("testProdPorts", this, 15);
		TestUtils.runTestWithTimer("testDevPorts", this, 15);
//		testPortsForPrefs(ServerPrefs.PRODUCTION_PREFS);
//		testPortsForPrefs(ServerPrefs.DEV_PREFS);
	}
	
	@SuppressWarnings("unused")
	private void testProdPorts() {
		testPortsForPrefs(ServerPrefs.PRODUCTION_PREFS);
	}
	
	@SuppressWarnings("unused")
	private void testDevPorts() {
		testPortsForPrefs(ServerPrefs.DEV_PREFS);
	}
	
	private boolean contains(String[] list, String testName) {
		for (String name : list) {
			if (name.equals(testName))
				return true;
		}
		return false;
	}
	
	private void testPortsForPrefs(ServerPrefs prefs) {
		try {
			Registry reg = RMIUtils.getRegistry(prefs);
			String[] list = reg.list();
			System.out.println("******* Naming list for "+prefs.name()+" *******");
			for (String name : list)
				System.out.println("- "+name);
			String[] testNames = { RegisterRemoteERF_Factory.registrationName,
					RegisterRemoteERF_ListFactory.registrationName };
			for (String testName : testNames) {
				assertTrue("ServerPrefs '"+prefs.name()+"' doesn't have RMI binding for '"+testName+"'", 
						contains(list, testName));
			}
		} catch (RemoteException e) {
			e.printStackTrace();
			fail("Remote exceptoin!");
		}
	}

}
