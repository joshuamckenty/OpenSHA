package org.opensha.sha.cybershake.calc;

/**
 * This interface is for calculating curves for special cases where the UCERF probabilities
 * should be modified for a small subset of ruptures.
 * 
 * @author kevin
 *
 */
public interface RuptureProbabilityModifier {
	
	/**
	 * This method should return the modified probability for the given source/rup ID
	 * combination.
	 * 
	 * @param sourceID
	 * @param rupID
	 * @param origProb
	 * @return
	 */
	public double getModifiedProb(int sourceID, int rupID, double origProb);

}
