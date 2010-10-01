package org.opensha.sha.cybershake.db;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DB_Utils {
	
	public static int getSingleInt(DBAccess db, String sql) {
		ResultSet rs = null;
		try {
			rs = db.selectData(sql);
		} catch (SQLException e1) {
//			TODO Auto-generated catch block
			e1.printStackTrace();
			return -1;
		}
		int id = -1;
		
		try {
			rs.first();
			if (rs.isAfterLast())
				return -1;
			id = rs.getInt(1);
			rs.close();
		} catch (SQLException e) {
//			e.printStackTrace();
			return -1;
		}
		return id;
	}

}
