package org.opensha.sha.cybershake.bombay;

import java.util.Date;

import org.opensha.sha.cybershake.calc.RuptureProbabilityModifier;
import org.opensha.sha.cybershake.calc.RuptureVariationProbabilityModifier;

public class UniformModRupConfig extends AbstractModProbConfig {
	
	private RuptureProbabilityModifier rupProbMod;
	
	public UniformModRupConfig(String name, int probModelID, int timeSpanID, double mod) {
		super(name, probModelID, timeSpanID);
		final double finMod = mod;
		rupProbMod = new RuptureProbabilityModifier() {
			
			@Override
			public double getModifiedProb(int sourceID, int rupID, double origProb) {
				// TODO Auto-generated method stub
				return origProb * finMod;
			}
		};
	}


	@Override
	public RuptureProbabilityModifier getRupProbModifier() {
		return rupProbMod;
	}

	@Override
	public RuptureVariationProbabilityModifier getRupVarProbModifier() {
		return null;
	}

}
