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

package org.opensha.sha.earthquake.rupForecastImpl.remote;

import java.rmi.registry.Registry;

import org.opensha.commons.util.RMIUtils;
import org.opensha.commons.util.ServerPrefUtils;

/**
 *
 * <p>Title: RegisterRemoteERF_Factory.java </p>
 * <p>Description: This class registers the RemoteERF Factory object with the
 * naming service. Remote ERF factory acts as a resource for clients
 * for getting references to remote ERFs </p>
 * @author Nitin Gupta, Vipin Gupta
 * @version 1.0
 */
public class RegisterRemoteERF_Factory {
	public final static String registrationName ="ERF_FactoryServer";
	private final static String registrationURL =
		ServerPrefUtils.SERVER_PREFS.getRMIBaseURL()+registrationName;
	public static void main(String[] args) {
		try {
			System.out.println("Creating custom socket factory");
			RMIUtils.initSocketFactory();
			// register the ERF Factory with the naming service
			System.out.println("Starting ERF Factory Server");
			RemoteERF_FactoryAPI erfServer = new RemoteERF_FactoryImpl();
			Registry registry = RMIUtils.getCreateRegistry();
			registry.rebind(registrationName, erfServer);
			System.out.println("Registered "+registrationName+" as " + registrationURL);
			System.out.println("CWD: " + System.getProperty("user.dir"));
			System.out.println("Registry bindings: ---------");
			for (String binding : registry.list()) {
				System.out.println(binding);
			}
			System.out.println("----------------------------");
		}
		catch (Exception e) {
			System.out.println("exception in starting server");
			e.printStackTrace();
			e.getMessage();
			return;
		}

	}
}
