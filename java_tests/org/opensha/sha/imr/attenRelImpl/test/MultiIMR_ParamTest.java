package org.opensha.sha.imr.attenRelImpl.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.ListIterator;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opensha.commons.exceptions.ParameterException;
import org.opensha.commons.param.ParameterAPI;
import org.opensha.sha.cybershake.openshaAPIs.CyberShakeIMR;
import org.opensha.sha.gui.infoTools.AttenuationRelationshipsInstance;
import org.opensha.sha.imr.ScalarIntensityMeasureRelationshipAPI;
import org.opensha.sha.imr.attenRelImpl.MultiIMR_Averaged_AttenRel;
import org.opensha.sha.imr.param.OtherParams.SigmaTruncLevelParam;
import org.opensha.sha.imr.param.OtherParams.SigmaTruncTypeParam;
import org.opensha.sha.imr.param.OtherParams.StdDevTypeParam;
import org.opensha.sha.imr.param.SiteParams.DepthTo2pt5kmPerSecParam;
import org.opensha.sha.imr.param.SiteParams.Vs30_Param;

public class MultiIMR_ParamTest {
	
	private static ArrayList<ScalarIntensityMeasureRelationshipAPI> imrs;
	private static ArrayList<ArrayList<ScalarIntensityMeasureRelationshipAPI>> bundles;
	
	private static int imrs_per_bundle = 3;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		AttenuationRelationshipsInstance inst = new AttenuationRelationshipsInstance();
		imrs = inst.createIMRClassInstance(null);
		
		for (int i=imrs.size()-1; i>=0; i--) {
			ScalarIntensityMeasureRelationshipAPI imr = imrs.get(i);
			if (imr instanceof MultiIMR_Averaged_AttenRel)
				imrs.remove(i);
			else if (imr instanceof CyberShakeIMR)
				imrs.remove(i);
			
			imr.setParamDefaults();
		}
		
//		Collections.shuffle(imrs);
		
		bundles = new ArrayList<ArrayList<ScalarIntensityMeasureRelationshipAPI>>();
		
		ArrayList<ScalarIntensityMeasureRelationshipAPI> mimrs = null;
		for (int i=0; i<imrs.size(); i++) {
			if (i % imrs_per_bundle == 0) {
				if (mimrs != null) {
					bundles.add(mimrs);
				}
				mimrs = new ArrayList<ScalarIntensityMeasureRelationshipAPI>();
			}
			mimrs.add(imrs.get(i));
		}
		if (mimrs.size() > 0)
			bundles.add(mimrs);
		System.out.println("created " + bundles.size() + " bundles!");
	}
	
	@Test
	public void testSetIMTs() {
		for (ArrayList<ScalarIntensityMeasureRelationshipAPI> bundle : bundles) {
			MultiIMR_Averaged_AttenRel multi = new MultiIMR_Averaged_AttenRel(bundle);
			
			for (ParameterAPI<?> imt : multi.getSupportedIntensityMeasuresList()) {
				for (ScalarIntensityMeasureRelationshipAPI imr : bundle) {
					assertTrue("IMT '"+imt.getName()+"' is included but not supported by imr '"+imr.getName()+"'",
							imr.isIntensityMeasureSupported(imt));
				}
				multi.setIntensityMeasure(imt);
				for (ScalarIntensityMeasureRelationshipAPI imr : bundle) {
					assertEquals("IMT not set correctly!", imt.getName(), imr.getIntensityMeasure().getName());
				}
			}
		}
	}
	
	@Test
	public void testInitialConsist() {
		for (ArrayList<ScalarIntensityMeasureRelationshipAPI> bundle : bundles) {
			MultiIMR_Averaged_AttenRel multi = new MultiIMR_Averaged_AttenRel(bundle);
			testParamConsistancy(multi, bundle);
		}
	}
	
	@Test
	public void testChangeProp() {
		for (ArrayList<ScalarIntensityMeasureRelationshipAPI> bundle : bundles) {
			MultiIMR_Averaged_AttenRel multi = new MultiIMR_Averaged_AttenRel(bundle);
			trySet(multi, StdDevTypeParam.NAME, StdDevTypeParam.STD_DEV_TYPE_INTRA);
			testParamConsistancy(multi, bundle);
			trySet(multi, StdDevTypeParam.NAME, StdDevTypeParam.STD_DEV_TYPE_INTER);
			testParamConsistancy(multi, bundle);
			trySet(multi, SigmaTruncTypeParam.NAME, SigmaTruncTypeParam.SIGMA_TRUNC_TYPE_1SIDED);
			testParamConsistancy(multi, bundle);
			trySet(multi, SigmaTruncLevelParam.NAME, new Double(3.0));
			testParamConsistancy(multi, bundle);
			trySet(multi, SigmaTruncLevelParam.NAME, new Double(2.0));
			testParamConsistancy(multi, bundle);
			trySet(multi, Vs30_Param.NAME, new Double(200.0));
			testParamConsistancy(multi, bundle);
			trySet(multi, DepthTo2pt5kmPerSecParam.NAME, new Double(1.33));
			testParamConsistancy(multi, bundle);
		}
	}
	
	private void trySet(MultiIMR_Averaged_AttenRel multi, String paramName, Object value) {
		try {
			ParameterAPI param = multi.getParameter(paramName);
			if (param.isAllowed(value))
				param.setValue(value);
			else
				System.err.println("Multi imr can't set param '" + paramName + "' to '"+value+"'");
		} catch (ParameterException e) {
			System.err.println("Multi imr doesn't have param '" + paramName + "'");
		}
	}
	
	private void testParamConsistancy(MultiIMR_Averaged_AttenRel multi,
			ArrayList<ScalarIntensityMeasureRelationshipAPI> bundle) {
		for (ScalarIntensityMeasureRelationshipAPI imr : bundle) {
			testParamConsistancy(multi.getSiteParamsIterator(), imr);
			testParamConsistancy(multi.getOtherParamsIterator(), imr);
			testParamConsistancy(multi.getEqkRuptureParamsIterator(), imr);
		}
	}
	
	private void testParamConsistancy(ListIterator<ParameterAPI<?>> it, ScalarIntensityMeasureRelationshipAPI imr2) {
		while (it.hasNext()) {
			ParameterAPI param1 = it.next();
			try {
				ParameterAPI param2 = imr2.getParameter(param1.getName());
				// this imr has it also
				assertEquals("Param '"+param1.getName() +"' not propogated correctly!", param1.getValue(), param2.getValue());
			} catch (ParameterException e) {}
		}
	}

}
