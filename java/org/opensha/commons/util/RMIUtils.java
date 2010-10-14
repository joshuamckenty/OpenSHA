package org.opensha.commons.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.util.Random;

public class RMIUtils {
	
	public static void initSocketFactory() throws IOException {
		initSocketFactory(ServerPrefUtils.SERVER_PREFS);
	}
	
	public static void initSocketFactory(ServerPrefs prefs) throws IOException {
		RMISocketFactory.setSocketFactory(new FixedPortRMISocketFactory(prefs));
	}

	public static Registry getRegistry() throws RemoteException {
		return getRegistry(ServerPrefUtils.SERVER_PREFS);
	}

	public static Registry getRegistry(ServerPrefs prefs) throws RemoteException {
		int port = prefs.getRMIPort();
		String host = prefs.getHostName();
		return LocateRegistry.getRegistry(host, port);
	}

	public static Registry getCreateRegistry() throws RemoteException {
		return getCreateRegistry(ServerPrefUtils.SERVER_PREFS);
	}

	public static Registry getCreateRegistry(ServerPrefs prefs) throws RemoteException {
		int port = prefs.getRMIPort();
		Registry registry = null;
		try {
			registry = LocateRegistry.getRegistry(port);
			registry.list(); // make sure this is real
		} catch (Exception e) {
			//			e.printStackTrace();
			// if we're here, then we need to create a registry
			System.out.println("RMIUtils: creating registry on port "+port); 
			registry = LocateRegistry.createRegistry(port);
		}
		return registry;
	}

	public static class FixedPortRMISocketFactory extends RMISocketFactory {
		
		private ServerPrefs prefs;
		private Random r = new Random();
		private String debugName;
		
		public FixedPortRMISocketFactory(ServerPrefs prefs) {
			this.prefs = prefs;
			debugName = "FixedPortRMISocketFactory[" + prefs.getBuildType() + "]: ";
		}
		
		private int getPort() throws IOException {
			int min = prefs.getMinRMISocketPort();
			int max = prefs.getMaxRMISocketPort();
			int delta = max - min;
			
			int port = 0;
			int cnt = 0;
			while (port == 0 || isPortInUse(port)) {
				port = min + r.nextInt(delta);
				cnt++;
				if (cnt == 100)
					throw new IOException("Couldn't find an open port after 100 tries!");
			}
			return port;
		}
		
		private boolean isPortInUse(int port) {
			try {
				ServerSocket socket = new ServerSocket(port);
				socket.close();
			} catch (Exception e) {
				System.out.println(debugName+"port '"+port+"' already in use!");
				return true;
			}
			return false;
		}

		@Override
		public ServerSocket createServerSocket(int port) throws IOException {
			port = (port == 0 ? getPort() : port);
			System.out.println(debugName+"creating ServerSocket on port " + port);
			return new ServerSocket(port);
		}

		@Override
		public Socket createSocket(String host, int port) throws IOException {
			System.out.println(debugName+"creating socket to host : " + host + " on port " + port);
			return new Socket(host, port);
		}

	}

}
