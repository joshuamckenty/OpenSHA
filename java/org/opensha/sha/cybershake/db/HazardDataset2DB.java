package org.opensha.sha.cybershake.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class HazardDataset2DB {
	
	private DBAccess db;
	
	public HazardDataset2DB(DBAccess db) {
		this.db = db;
	}
	
	public int getDefaultProbModelID(int erfID) {
		return getERF_Field("Default_Prob_Model_ID", erfID);
	}
	
	public int getDefaultTimeSpanID(int erfID) {
		return getERF_Field("Default_Time_Span_ID", erfID);
	}
	
	private int getERF_Field(String field, int erfID) {
		String sql = "SELECT "+field;
		sql += " FROM ERF_IDs WHERE ERF_ID="+erfID;
		
//		System.out.println(sql);
		
		return DB_Utils.getSingleInt(db, sql);
	}
	
	public int getDefaultDatasetID(CybershakeRun run) {
		int probModelID = getDefaultProbModelID(run.getERFID());
		int timeSpanID = getDefaultTimeSpanID(run.getERFID());
		
		return getDatasetID(run.getERFID(), run.getRupVarScenID(), run.getSgtVarID(),
				run.getVelModelID(), probModelID, timeSpanID, null);
	}
	
	public int getDatasetID(int erfID, int rvScenID, int sgtVarID,
					int velModelID, int probModelID, int timeSpanID, Date timeSpanStart) {
		String dateStr;
		if (timeSpanStart != null) {
			dateStr = "='"+DBAccess.SQL_DATE_FORMAT.format(timeSpanStart)+"'";
		} else {
			dateStr = " IS NULL";
		}
		
		System.out.println("DATE: " + timeSpanStart + " DATE_STR: " + dateStr);
		
		String sql = "SELECT Hazard_Dataset_ID";
		sql += " FROM Hazard_Datasets";
		sql += " WHERE ERF_ID="+erfID;
		sql += " AND Rup_Var_Scenario_ID="+rvScenID;
		sql += " AND SGT_Variation_ID="+sgtVarID;
		sql += " AND Velocity_Model_ID="+velModelID;
		sql += " AND Prob_Model_ID="+probModelID;
		sql += " AND Time_Span_ID="+timeSpanID;
		sql += " AND Time_Span_Start_Date"+dateStr;
		
//		System.out.println(sql);
		
		return DB_Utils.getSingleInt(db, sql);
	}
	
	public int addNewDataset(int erfID, int rvScenID, int sgtVarID,
					int velModelID, int probModelID, int timeSpanID, Date timeSpanStart) {
		String dateField;
		String dateStr;
		if (timeSpanStart != null) {
			dateField = ",Time_Span_Start_Date";
			dateStr = ",'"+DBAccess.SQL_DATE_FORMAT.format(timeSpanStart)+"'";
		} else {
			dateField = "";
			dateStr = "";
		}
		String sql = "INSERT INTO Hazard_Datasets" + 
				"(ERF_ID,Rup_Var_Scenario_ID,SGT_Variation_ID,Velocity_Model_ID," +
				"Prob_Model_ID,Time_Span_ID"+dateField+")"+
				"VALUES("+erfID+","+rvScenID+","+sgtVarID+","+velModelID
				+","+probModelID+","+timeSpanID+dateStr+")";
		
		try {
			db.insertUpdateOrDeleteData(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		
		return getDatasetID(erfID, rvScenID, sgtVarID, velModelID, probModelID, timeSpanID, timeSpanStart);
	}
	
	public static void main(String[] args) {
		DBAccess db = Cybershake_OpenSHA_DBApplication.db;
		Runs2DB r2db = new Runs2DB(db);
		CybershakeRun run = r2db.getRun(571);
		HazardDataset2DB hd2db = new HazardDataset2DB(db);
		System.out.println("Dataset: " + hd2db.getDefaultDatasetID(run));
		
		db.destroy();
		
		System.exit(0);
	}

}
