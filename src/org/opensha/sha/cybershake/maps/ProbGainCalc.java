package org.opensha.sha.cybershake.maps;

import java.util.ArrayList;

import org.opensha.commons.data.ArbDiscretizedXYZ_DataSet;
import org.opensha.commons.data.XYZ_DataSetAPI;

public class ProbGainCalc {

	public static ArbDiscretizedXYZ_DataSet calcProbGain(XYZ_DataSetAPI refXYZ, XYZ_DataSetAPI modXYZ) {
		ArbDiscretizedXYZ_DataSet gainXYZ = new ArbDiscretizedXYZ_DataSet();

		ArrayList<Double> refXVals = refXYZ.getX_DataSet();
		ArrayList<Double> refYVals = refXYZ.getY_DataSet();
		ArrayList<Double> refZVals = refXYZ.getZ_DataSet();

		ArrayList<Double> modXVals = modXYZ.getX_DataSet();
		ArrayList<Double> modYVals = modXYZ.getY_DataSet();
		ArrayList<Double> modZVals = modXYZ.getZ_DataSet();

		for (int refInd=0; refInd<refXVals.size(); refInd++) {
			double refX = refXVals.get(refInd);
			double refY = refYVals.get(refInd);
			double refZ = refZVals.get(refInd);
			for (int modInd=0; modInd<modXVals.size(); modInd++) {
				double modX = modXVals.get(modInd);
				double modY = modYVals.get(modInd);
				double modZ = modZVals.get(modInd);

				if (refX != modX || refY != modY)
					continue;

				double gain = modZ / refZ;

				gainXYZ.addValue(modX, modY, gain);
			}
		}

		return gainXYZ;
	}

}
