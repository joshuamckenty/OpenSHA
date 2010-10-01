package org.opensha.sha.cybershake.bombay;

import java.util.Date;

import org.opensha.sha.cybershake.calc.RuptureProbabilityModifier;
import org.opensha.sha.cybershake.calc.RuptureVariationProbabilityModifier;
import org.opensha.sha.cybershake.db.Cybershake_OpenSHA_DBApplication;
import org.opensha.sha.cybershake.db.DBAccess;
import org.opensha.sha.cybershake.db.HazardDataset2DB;

public abstract class AbstractModProbConfig implements ModProbConfig {
	
	protected final DBAccess db = Cybershake_OpenSHA_DBApplication.db;
	
	private int probModelID;
	private int timeSpanID;
	
	private String name;
	
	private HazardDataset2DB hd2db;
	
	public AbstractModProbConfig(String name, int probModelID, int timeSpanID) {
		this.name = name;
		this.probModelID = probModelID;
		this.timeSpanID = timeSpanID;
		
		hd2db = new HazardDataset2DB(db);
	}

	@Override
	public int getHazardDatasetID(int erfID, int rvScenID, int sgtVarID,
			int velModelID, Date timeSpanDate) {
		return hd2db.getDatasetID(erfID, rvScenID, sgtVarID, velModelID, probModelID, timeSpanID, timeSpanDate);
	}

	@Override
	public int getProbModelID() {
		return probModelID;
	}

	@Override
	public int getTimeSpanID() {
		return timeSpanID;
	}

	@Override
	public String getName() {
		return name;
	}

}
