package org.opensha.sha.cybershake.bombay;

import java.util.Date;

import org.opensha.commons.data.NamedObjectAPI;
import org.opensha.sha.cybershake.calc.RuptureProbabilityModifier;
import org.opensha.sha.cybershake.calc.RuptureVariationProbabilityModifier;

public interface ModProbConfig extends NamedObjectAPI {
	
	public int getProbModelID();
	
	public int getTimeSpanID();
	
	public RuptureProbabilityModifier getRupProbModifier();
	
	public RuptureVariationProbabilityModifier getRupVarProbModifier();
	
	public int getHazardDatasetID(int erfID, int rvScenID, int sgtVarID,
			int velModelID, Date timeSpanDate);

}
