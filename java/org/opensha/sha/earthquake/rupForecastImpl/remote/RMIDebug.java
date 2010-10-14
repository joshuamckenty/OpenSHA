package org.opensha.sha.earthquake.rupForecastImpl.remote;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.opensha.sha.earthquake.rupForecastImpl.remoteERF_Clients.Frankel96_AdjustableEqkRupForecastClient;

public class RMIDebug {
	
	public static void main(String[] args) {
		int[] ports = null;
		if (args.length == 0) {
			ports = new int[2];
			ports[0] = 1099;
			ports[1] = 1098;
			if (System.currentTimeMillis() % 2l == 0l) {
				int port1 = ports[0];
				ports[0] = ports[1];
				ports[1] = port1;
			}
		} else {
			ports = new int[args.length];
			for (int i=0; i<args.length; i++) {
				ports[i] = Integer.parseInt(args[i]);
			}
		}
		for (int port : ports) {
			System.out.println("********************* Debugging port " + port + " *********************");
			try {
				Registry registry = LocateRegistry.getRegistry("opensha.usc.edu", port);
				System.out.println("Registry bindings: ---------");
				String[] list = registry.list();
				for (String binding : list) {
					System.out.println(binding);
				}
				System.out.println("----------------------------");
				try {
					String lname = RegisterRemoteERF_Factory.registrationName;
					System.out.println("Now attempting to lookup: "+ lname);
					RemoteERF_FactoryAPI r = (RemoteERF_FactoryAPI)
								registry.lookup(lname);
					String erfName = Frankel96_AdjustableEqkRupForecastClient.class.getName();
					System.out.println("Getting ERF: " + erfName);
					RemoteEqkRupForecastAPI erf =
						r.getRemoteERF(erfName);
					System.out.println("Loaded: " + erf.getName());
				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}
			try {
				String[] list = Naming.list("rmi://opensha.usc.edu:"+port+"/");
				System.out.println("Naming bindings: ---------");
				for (String binding : list) {
					System.out.println(binding);
				}
				System.out.println("----------------------------");
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}
		}
	}

}
