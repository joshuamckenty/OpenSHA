package org.opensha.commons.util;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerPrefUtils {
	
	public static final DateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
	
	/**
	 * Hostname for all production services
	 */
	protected static final String OPENSHA_SERVER_PRODUCTION_HOST = "opensha.usc.edu";
	
	/**
	 * Hostname for all production services
	 */
	protected static final String OPENSHA_SERVER_DEV_HOST = "opensha.usc.edu";
	
	/**
	 * This is the URL to the production OpenSHA servlets.
	 */
	protected static final String OPENSHA_SERVLET_PRODUCTION_URL =
		"http://"+OPENSHA_SERVER_PRODUCTION_HOST+":8080/OpenSHA/";
	
	/**
	 * This is the URL to the development OpenSHA servlets
	 */
	protected static final String OPENSHA_SERVLET_DEV_URL =
		"http://"+OPENSHA_SERVER_DEV_HOST+":8080/OpenSHA_dev/";
	
	/**
	 * This is the production port for RMI
	 */
	protected static final int OPENSHA_RMI_PRODUCTION_PORT = 1099;
	
	/**
	 * This is the development port for RMI
	 */
	protected static final int OPENSHA_RMI_DEV_PORT = 1098;
	
	/**
	 * This is the RMI port range min
	 */
	protected static final int OPENSHA_RMI_SOCKET_MIN_PORT = 40000;
	/**
	 * This is the RMI port range max
	 */
	protected static final int OPENSHA_RMI_SOCKET_MAX_PORT = 40500;
	
	protected static final String BUILD_TYPE_NIGHTLY = "nightly";
	protected static final String BUILD_TYPE_PRODUCTION = "dist";
	
	/**
	 * This is the preferences enum for OpenSHA...it should always be link to the production prefs
	 * when applications are final and being distributed, the the development prefs should be used when
	 * changes are being made that would break the currently released apps.
	 * 
	 * In practice, this means that it should be development prefs on trunk, and production prefs
	 * on release branches
	 */
	public static final ServerPrefs SERVER_PREFS = ServerPrefs.DEV_PREFS;
	
	public static void debug(String debugName, String message) {
		String date = "[" + df.format(new Date()) + "]";
		System.out.println(debugName + " " + date + ": " + message);
	}
	
	public static void fail(ObjectOutputStream out, String debugName, String message) throws IOException {
		debug(debugName, "Failing: " + message);
		out.writeObject(new Boolean(false));
		out.writeObject(message);
		out.flush();
		out.close();
	}

}
