package org.opensha.sha.cybershake.timeDep;

import org.opensha.commons.data.TimeSpan;
import org.opensha.sha.cybershake.calc.RuptureProbabilityModifier;
import org.opensha.sha.cybershake.db.MeanUCERF2_ToDB;
import org.opensha.sha.earthquake.EqkRupForecastAPI;
import org.opensha.sha.earthquake.rupForecastImpl.WGCEP_UCERF_2_Final.UCERF2;
import org.opensha.sha.earthquake.rupForecastImpl.WGCEP_UCERF_2_Final.MeanUCERF2.MeanUCERF2;

public class TimeDependentRupProbMod implements RuptureProbabilityModifier {
	
	private EqkRupForecastAPI erf;
	
	public TimeDependentRupProbMod(int startYear) {
		erf = MeanUCERF2_ToDB.createUCERF2ERF();
		erf.getAdjustableParameterList().getParameter(UCERF2.PROB_MODEL_PARAM_NAME)
				.setValue(MeanUCERF2.PROB_MODEL_WGCEP_PREF_BLEND);
		TimeSpan ts = erf.getTimeSpan();
		ts.setStartTime(startYear);
		ts.setDuration(1.0, TimeSpan.YEARS);
		System.out.println("Duration: " + erf.getTimeSpan().getDuration(TimeSpan.YEARS) + " years");
		erf.updateForecast();
//		
//		System.out.println("86-0 prob: " + erf.getRupture(86, 0).getProbability());
//		System.exit(0);
	}

	public double getModifiedProb(int sourceID, int rupID, double origProb) {
		return erf.getRupture(sourceID, rupID).getProbability();
	}

}
