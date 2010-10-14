package org.opensha.commons.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.opensha.commons.util.ApplicationVersion;
import org.opensha.commons.util.FileUtils;

public class DisclaimerDialog extends JDialog implements ActionListener {
	
	private static boolean D = false;
	
	private String appName;
	private String shortName;
	private ApplicationVersion version;
	
	private String license;
	
	private JTextArea textArea;
	private JScrollPane textScroll;
	
	private JButton acceptButton = new JButton("Accept");
	private JButton exitButton = new JButton("Exit");
	private JCheckBox hideCheck = new JCheckBox("Don't show this again", false);
	
	private boolean accepted = false;
	
	private static Preferences prefs;
	protected static Preferences getPrefs() {
		if (prefs == null)
			prefs = Preferences.userNodeForPackage(DisclaimerDialog.class);
		return prefs;
	}
	
	private static final String disclaimerPrefPrefix = "disc_accpted_";
	
	public DisclaimerDialog(String appName, String shortName, ApplicationVersion version) throws IOException {
		this.appName = appName;
		this.shortName = shortName;
		this.version = version;
		
		getPrefs(); // init the prefs
		if (canSkipDisclaimer()) {
			// just return, never show it
			if (D) System.out.println("can skip disclaimer!");
			return;
		}
		if (D) System.out.println("displaying disclaimer!");
		try {
			license = loadLicense();
		} catch (Exception e) {
			e.printStackTrace();
			license = "Could not load License/Disclaimer. Please see http://www.opensha.org to obtain" +
					" the latest License/Disclaimer.";
		}
		
		init();
		if (!accepted)
			System.exit(0);
		if (hideCheck.isSelected()) {
			storeAcceptedVersion(shortName, version);
			flushPrefs();
		}
	}
	
	private void init() {
		this.setModalityType(ModalityType.TOOLKIT_MODAL);
		this.getContentPane().setLayout(new BorderLayout());
		
		textArea = new JTextArea(license);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setEditable(false);
		textScroll = new JScrollPane(textArea);
		textScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		this.getContentPane().add(textScroll, BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add(acceptButton);
		buttonPanel.add(exitButton);
		buttonPanel.add(hideCheck);
		
		acceptButton.addActionListener(this);
		exitButton.addActionListener(this);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		
		this.setTitle("License/Disclaimer: "+appName+" ("+version+")");
		this.setSize(700, 600);
		this.setLocationRelativeTo(null);
		if (D) System.out.println("Setting visible");
		this.setVisible(true);
		if (D) System.out.println("Set visible done (accepted = " + accepted + ")");
	}
	
	private static String loadLicense() throws IOException {
		String license = "";
		
		String fileName = "LICENSE.txt";
		ArrayList<String> lines = null;
		if (new File(fileName).exists()) {
			// this will work from a working copy
			lines = FileUtils.loadFile(fileName, false);
		} else {
			// this will work from a jar
			URL url = DisclaimerDialog.class.getResource("/"+fileName);
			if (url == null)
				throw new FileNotFoundException("could not load license!");
			lines = FileUtils.loadFile(url);
		}
		for (String line : lines) {
			line = line.trim();
			license += line + "\n";
		}
		
		if (D) System.out.println("----- Loaded license -----\n"+license);
		
		return license;
	}
	
	private String getPrefKey() {
		return getPrefKey(shortName);
	}
	
	private static String getPrefKey(String shortName) {
		return disclaimerPrefPrefix + shortName;
	}
	
	private ApplicationVersion getAcceptedVersion() {
		String version = prefs.get(getPrefKey(), null);
		if (D) System.out.println("getAcceptedVersion(): prefVal=" + version);
		if (version != null && !version.isEmpty() && version.contains(".")) {
			return ApplicationVersion.fromString(version);
		}
		return null;
	}
	
	private boolean canSkipDisclaimer() {
		ApplicationVersion accepted = getAcceptedVersion();
		
		// we can skip it if the user has already accepted version >= current for this app
		if (D) System.out.println("Comparing my version ("+version+") to pref version ("+accepted+")");
		if (accepted != null && !accepted.isLessThan(version)) {
			return true;
		}
		return false;
	}
	
	private static void storeAcceptedVersion(String shortName, ApplicationVersion version) {
		if (version == null) {
			clearAcceptedVersion(shortName);
			return;
		}
		String key = getPrefKey(shortName);
		if (D) System.out.println("setting accepted version for '"+key+"' to: " + version);
		getPrefs().put(key, version.toString());
	}
	
	private static void clearAcceptedVersion(String shortName) {
		String key = getPrefKey(shortName);
		if (D) System.out.println("clearing accepted version for '"+key+"'");
		try {
			getPrefs().remove(key);
		} catch (NullPointerException e) {
			if (D) System.out.println("key '" + key + "' cannot be cleared as it doesn't exist!");
		}
	}
	
	private static void flushPrefs() {
		try {
			getPrefs().flush();
		} catch (Throwable t) {
			t.printStackTrace();
			System.err.println("WARNING: Couldn't write to preferences!");
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(acceptButton)) {
			accepted = true;
			setVisible(false);
		} else if (e.getSource().equals(exitButton)) {
			accepted = false;
			setVisible(false);
		}
	}
	
	public static void main(String[] args) {
		String name = "Test Application";
		String shortName = "TestApp";
		ApplicationVersion version = ApplicationVersion.fromString("0.2.3");
		ApplicationVersion accepted = ApplicationVersion.fromString("0.2.1");
//		storeAcceptedVersion(shortName, accepted);
		try {
			new DisclaimerDialog(name, shortName, version);
		} catch (Throwable t) {
			System.out.println("Caught an exception!");
			t.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}

}
