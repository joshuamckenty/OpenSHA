package org.opensha.sha.cybershake.calc;

import java.util.ArrayList;


/**
 * This interface is for calculating curves for special cases where the UCERF probabilities
 * should be modified for a small subset of rupture variations within a rupture.
 * 
 * It was originally created for a calculation concerning a temporary increase in hazard
 * for southern-nucleating SAF ruptures during the Bombay Beach swarm in Spring, 2009. 
 * 
 * @author kevin
 *
 */
public interface RuptureVariationProbabilityModifier extends RuptureProbabilityModifier {
	
	public ArrayList<Integer> getModVariations(int sourceID, int rupID);
}
