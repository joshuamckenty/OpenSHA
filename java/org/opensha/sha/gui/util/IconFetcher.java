package org.opensha.sha.gui.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.opensha.commons.util.ServerPrefUtils;
import org.opensha.commons.util.ServerPrefs;

public class IconFetcher {
	
	private static final boolean D = false;
	
	public static ArrayList<BufferedImage> fetchIcons(String appShortName) {
		return fetchIcons(appShortName, ServerPrefUtils.SERVER_PREFS);
	}
	
	public static ArrayList<BufferedImage> fetchIcons(String appShortName, ServerPrefs prefs) {
		ArrayList<BufferedImage> images = new ArrayList<BufferedImage>();
		String localBase = "/resources/images/icons/";
		for (int size : JNLPGen.icon_sizes) {
			URL url = null;
			String fileName = JNLPGen.getIconName(appShortName, size);
			try {
				// first try local images
				url = IconFetcher.class.getResource(localBase+fileName);
			} catch (Throwable t) {}
			if (url == null) {
				// then try internet
				try {
					String addy = JNLPGen.webRoot+"/"+appShortName+"/"+prefs.getBuildType()+"/icons/"+fileName;
					url = new URL(addy);
				} catch (Throwable t) {}
			}
			if (url == null)
				// couldn't load from local or internet
				continue;
			try {
				if (D) System.out.println("loading icon from: " + url);
				images.add(ImageIO.read(url));
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Error reading icon from: "+url);
			}
		}
		if (images.size() == 0)
			return null;
		return images;
	}
	
	public static void main(String[] args) throws IOException {
		ArrayList<BufferedImage> icons = new ArrayList<BufferedImage>();
		icons.add(ImageIO.read(new URL("http://opensha.usc.edu/apps/opensha/HazardCurveLocal/nightly/icons/HazardCurveLocal_16x16.png")));
		icons.add(ImageIO.read(new URL("http://opensha.usc.edu/apps/opensha/HazardCurveLocal/nightly/icons/HazardCurveLocal_32x32.png")));
		icons.add(ImageIO.read(new URL("http://opensha.usc.edu/apps/opensha/HazardCurveLocal/nightly/icons/HazardCurveLocal_48x48.png")));
		icons.add(ImageIO.read(new URL("http://opensha.usc.edu/apps/opensha/HazardCurveLocal/nightly/icons/HazardCurveLocal_128x128.png")));
		JFrame frame = new JFrame();
		frame.setSize(400, 400);
		frame.setIconImages(icons);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

}
