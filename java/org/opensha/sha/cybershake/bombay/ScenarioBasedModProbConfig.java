package org.opensha.sha.cybershake.bombay;

import java.util.Date;

import org.opensha.commons.geo.Location;
import org.opensha.sha.cybershake.calc.RuptureProbabilityModifier;
import org.opensha.sha.cybershake.calc.RuptureVariationProbabilityModifier;
import org.opensha.sha.cybershake.db.Cybershake_OpenSHA_DBApplication;
import org.opensha.sha.cybershake.db.DBAccess;
import org.opensha.sha.cybershake.db.HazardDataset2DB;

public class ScenarioBasedModProbConfig extends AbstractModProbConfig {
	
	private Location hypocenter;
	private int increaseMulti;
	private double hypoDistCutoff;
	private String sourceNameConstr;
	private boolean useDepth;
	
	private RuptureProbabilityModifier rupProbMod;
	private RuptureVariationProbabilityModifier rvProbMod;
	
	public ScenarioBasedModProbConfig(String name, int probModelID, int timeSpanID,
			Location hypocenter, int increaseMulti, double hypoDistCutoff,
			String sourceNameConstr, boolean useDepth) {
		super(name, probModelID, timeSpanID);
		this.hypocenter = hypocenter;
		this.increaseMulti = increaseMulti;
		this.hypoDistCutoff = hypoDistCutoff;
		this.sourceNameConstr = sourceNameConstr;
		this.useDepth = useDepth;
		
		rupProbMod = new Div365ProbModifier();
	}

	@Override
	public RuptureProbabilityModifier getRupProbModifier() {
		return rupProbMod;
	}

	@Override
	public RuptureVariationProbabilityModifier getRupVarProbModifier() {
		if (rvProbMod == null) {
			rvProbMod = new BombayBeachHazardCurveCalc(db,
					increaseMulti, hypocenter, hypoDistCutoff, sourceNameConstr, useDepth);
		}
		return rvProbMod;
	}

	public Location getHypocenter() {
		return hypocenter;
	}

	public int getIncreaseMulti() {
		return increaseMulti;
	}

	public double getHypoDistCutoff() {
		return hypoDistCutoff;
	}

	public String getSourceNameConstr() {
		return sourceNameConstr;
	}

	public boolean isUseDepth() {
		return useDepth;
	}

}
