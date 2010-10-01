package org.opensha.sha.imr.attenRelImpl.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ListIterator;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.opensha.commons.data.Site;
import org.opensha.commons.geo.Location;
import org.opensha.commons.param.ParameterAPI;
import org.opensha.commons.util.DataUtils;
import org.opensha.sha.earthquake.EqkRupForecast;
import org.opensha.sha.earthquake.ProbEqkRupture;
import org.opensha.sha.earthquake.ProbEqkSource;
import org.opensha.sha.earthquake.rupForecastImpl.Frankel96.Frankel96_AdjustableEqkRupForecast;
import org.opensha.sha.gui.infoTools.AttenuationRelationshipsInstance;
import org.opensha.sha.imr.PropagationEffect;
import org.opensha.sha.imr.ScalarIntensityMeasureRelationshipAPI;
import org.opensha.sha.imr.attenRelImpl.test.MultiIMR_CalcTest.IMR_PROP;
import org.opensha.sha.imr.param.IntensityMeasureParams.PGA_Param;
import org.opensha.sha.imr.param.IntensityMeasureParams.PGV_Param;
import org.opensha.sha.imr.param.IntensityMeasureParams.SA_Param;

@RunWith(Parameterized.class)
public class SetPropEffectTest {
	
	private ScalarIntensityMeasureRelationshipAPI imr1;
	private ScalarIntensityMeasureRelationshipAPI imr2;
	
	private static EqkRupForecast erf;
	private static Site site;
	private static ArrayList<Object[]> props;
	
	@BeforeClass
	public static void setUpBeforeClass() {
		erf = new Frankel96_AdjustableEqkRupForecast();
		erf.updateForecast();
		
		props = new ArrayList<Object[]>();
		
		Object[] val1 = { SA_Param.NAME, 1.0, 0.2 };
		Object[] val2 = { SA_Param.NAME, 1.0, 0.5 };
		Object[] val3 = { SA_Param.NAME, 0.1, 0.2 };
		Object[] val4 = { SA_Param.NAME, 0.1, 0.5 };
		Object[] val5 = { PGA_Param.NAME, -1.0, 0.2 };
		Object[] val6 = { PGA_Param.NAME, -1.0, 0.5 };
		Object[] val7 = { PGV_Param.NAME, -1.0, 100.0 };
		Object[] val8 = { PGV_Param.NAME, -1.0, 200.0 };
		
		props.add(val1);
		props.add(val2);
		props.add(val3);
		props.add(val4);
		props.add(val5);
		props.add(val6);
		props.add(val7);
		props.add(val8);
	}

	@Parameters
	public static Collection<ScalarIntensityMeasureRelationshipAPI[]> data() {
		AttenuationRelationshipsInstance inst = new AttenuationRelationshipsInstance();
		ArrayList<ScalarIntensityMeasureRelationshipAPI> imrs1 = inst.createIMRClassInstance(null);
		ArrayList<ScalarIntensityMeasureRelationshipAPI> imrs2 = inst.createIMRClassInstance(null);
		ArrayList<ScalarIntensityMeasureRelationshipAPI[]> ret = new ArrayList<ScalarIntensityMeasureRelationshipAPI[]>();
		
		site = new Site(new Location(34, -118));
		for (int i=0; i<imrs1.size(); i++) {
			ScalarIntensityMeasureRelationshipAPI imr1 = imrs1.get(i);
			imr1.setParamDefaults();
			ScalarIntensityMeasureRelationshipAPI imr2 = imrs2.get(i);
			imr2.setParamDefaults();
			ScalarIntensityMeasureRelationshipAPI[] array = { imr1, imr2 };
			ret.add(array);
			
			ListIterator<ParameterAPI<?>> siteParamIt = imr1.getSiteParamsIterator();
			while (siteParamIt.hasNext()) {
				ParameterAPI<?> param = siteParamIt.next();
				if (!site.containsParameter(param))
					site.addParameter(param);
			}
		}
		
		return ret;
	}
	
	public SetPropEffectTest(ScalarIntensityMeasureRelationshipAPI imr1,
			ScalarIntensityMeasureRelationshipAPI imr2) {
		this.imr1 = imr1;
		this.imr2 = imr2;
	}
	
	@Test
	public void testEpsilon() {
		doTest(IMR_PROP.EPSILON);
	}
	
	@Test
	public void testExceedProb() {
		doTest(IMR_PROP.EXCEED_PROB);
	}
	
	@Test
	public void testMean() {
		doTest(IMR_PROP.MEAN);
	}
	
	@Test
	public void testStdDev() {
		doTest(IMR_PROP.STD_DEV);
	}
	
	private void doTest(IMR_PROP prop) {
		for (Object[] vals : props) {
			String imt = (String)vals[0];
			double period = (Double)vals[1];
			double iml = (Double)vals[2];
			
			doTest(prop, imt, period, iml);
		}
	}
	
	private void doTest(IMR_PROP prop, String imt, double period, double iml) {
		System.out.println("testing imr: " + imr1.getName() +  " imt: "+imt + " period: " + period + " iml: " + iml);
		if (!imr1.isIntensityMeasureSupported(imt))
			return;
		
		MultiIMR_CalcTest.setIMT(imr1, imt, period);
		MultiIMR_CalcTest.setIMT(imr2, imt, period);
		
		double logIML = Math.log(iml);
		imr1.setIntensityMeasureLevel(logIML);
		imr2.setIntensityMeasureLevel(logIML);
		
		imr1.setSite(site);
		imr2.setSite(site);
		PropagationEffect propEffect = new PropagationEffect();
		propEffect.setSite(site);
		for (int sourceID=0; sourceID<erf.getNumSources(); sourceID++) {
			ProbEqkSource source = erf.getSource(sourceID);
			for (int rupID=0; rupID<source.getNumRuptures(); rupID++) {
				ProbEqkRupture rup = source.getRupture(rupID);
				
				double val1=0;
				double val2=0;
				
				imr1.setEqkRupture(rup);
				propEffect.setEqkRupture(rup);
				imr2.setPropagationEffect(propEffect);
				
				if (prop == IMR_PROP.EPSILON) {
					val1 = imr1.getEpsilon();
					val2 = imr2.getEpsilon();
				} else if (prop == IMR_PROP.EXCEED_PROB) {
					val1 = imr1.getExceedProbability();
					val2 = imr2.getExceedProbability();
				} else if (prop == IMR_PROP.MEAN) {
					val1 = imr1.getMean();
					val2 = imr2.getMean();
				} else if (prop == IMR_PROP.STD_DEV) {
					val1 = imr1.getStdDev();
					val2 = imr2.getStdDev();
				}
				
				double pDiff = DataUtils.getPercentDiff(val1, val2);
				
				String message = imr1.getShortName() + "["+prop+"] doesn't match:\n";
				message += "imt: "+imt+" period: "+period+" iml: "+iml+"\n";
				message += "setRupVal: "+val1 + ", setPropVal: "+val2 + ", pDiff: " + pDiff;
				assertTrue(message, pDiff < 0.05);
			}
		}
	}

}
