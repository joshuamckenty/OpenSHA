package org.opensha.sha.cybershake.bombay;

import java.util.HashMap;

import org.opensha.commons.geo.Location;

public class ModProbConfigFactory {
	
	public static final HashMap<Integer, ModProbConfig> modProbConfigs = new HashMap<Integer, ModProbConfig>();
	
	static {
		modProbConfigs.put(new Integer(1), new UniformModRupConfig("Single Day", 1, 2, 1d/365d));
		modProbConfigs.put(new Integer(3), new ScenarioBasedModProbConfig("Bombay Beach", 3, 2,
				BombayBeachHazardCurveCalc.BOMBAY_LOC, 1000, 10d, "andreas", false));
		modProbConfigs.put(new Integer(4), new ScenarioBasedModProbConfig("Parkfield", 4, 2,
				BombayBeachHazardCurveCalc.PARKFIELD_LOC, 1000, 10d, "andreas", false));
		modProbConfigs.put(new Integer(5), new ScenarioBasedModProbConfig("Pico Rivera", 5, 2,
				BombayBeachHazardCurveCalc.PICO_RIVERA_LOC, 1000, 10d, null, false));
		modProbConfigs.put(new Integer(6), new ScenarioBasedModProbConfig("Yucaipa", 6, 2,
				BombayBeachHazardCurveCalc.YUCAIPA_LOC, 1000, 10d, null, false));
		modProbConfigs.put(new Integer(7), new ScenarioBasedModProbConfig("Coyote Creek", 7, 2,
				BombayBeachHazardCurveCalc.COYOTE_CREEK, 1000, 10d, null, false));
	}
	
	public static ModProbConfig getModProbConfig(int probModelID) {
		return modProbConfigs.get(probModelID);
	}
	
	public static ScenarioBasedModProbConfig getScenarioConfig(Location hypo) {
		for (ModProbConfig config : modProbConfigs.values()) {
			if (config instanceof ScenarioBasedModProbConfig) {
				ScenarioBasedModProbConfig scenConfig = (ScenarioBasedModProbConfig)config;
				if (hypo.equals(scenConfig.getHypocenter()))
					return scenConfig;
			}
		}
		throw new IllegalArgumentException("Hypocenter '"+hypo+"' doesn't match any scenarios!");
	}

}
