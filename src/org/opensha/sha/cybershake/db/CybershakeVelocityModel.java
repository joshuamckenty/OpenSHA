package org.opensha.sha.cybershake.db;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CybershakeVelocityModel {
	
	private int id;
	private String name;
	private String version;
	
	public CybershakeVelocityModel(int id, String name, String version) {
		this.id = id;
		this.name = name;
		this.version = version;
	}

	public int getID() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	@Override
	public String toString() {
		return id + ". " + name + " (" + version + ")";
	}
	
	public static CybershakeVelocityModel fromResultSet(ResultSet rs) throws SQLException {
		int id = rs.getInt("Velocity_Model_ID");
		String name = rs.getString("Velocity_Model_Name");
		String version = rs.getString("Velocity_Model_Version");
		
		return new CybershakeVelocityModel(id, name, version);
	}

}
