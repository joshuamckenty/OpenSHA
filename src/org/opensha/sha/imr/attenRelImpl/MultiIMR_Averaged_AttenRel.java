package org.opensha.sha.imr.attenRelImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

import org.opensha.commons.data.Site;
import org.opensha.commons.data.function.DiscretizedFuncAPI;
import org.opensha.commons.exceptions.ConstraintException;
import org.opensha.commons.exceptions.EditableException;
import org.opensha.commons.exceptions.IMRException;
import org.opensha.commons.exceptions.ParameterException;
import org.opensha.commons.geo.Location;
import org.opensha.commons.param.DoubleDiscreteConstraint;
import org.opensha.commons.param.ParamLinker;
import org.opensha.commons.param.ParameterAPI;
import org.opensha.commons.param.ParameterList;
import org.opensha.commons.param.StringConstraint;
import org.opensha.commons.param.StringParameter;
import org.opensha.commons.param.event.ParameterChangeWarningListener;
import org.opensha.commons.util.ClassUtils;
import org.opensha.sha.earthquake.EqkRupture;
import org.opensha.sha.earthquake.rupForecastImpl.PointEqkSource;
import org.opensha.sha.gui.beans.IMT_NewGuiBean;
import org.opensha.sha.imr.AttenuationRelationship;
import org.opensha.sha.imr.PropagationEffect;
import org.opensha.sha.imr.ScalarIntensityMeasureRelationshipAPI;
import org.opensha.sha.imr.param.IntensityMeasureParams.DampingParam;
import org.opensha.sha.imr.param.IntensityMeasureParams.IA_Param;
import org.opensha.sha.imr.param.IntensityMeasureParams.MMI_Param;
import org.opensha.sha.imr.param.IntensityMeasureParams.PGA_Param;
import org.opensha.sha.imr.param.IntensityMeasureParams.PGD_Param;
import org.opensha.sha.imr.param.IntensityMeasureParams.PGV_Param;
import org.opensha.sha.imr.param.IntensityMeasureParams.PeriodInterpolatedParam;
import org.opensha.sha.imr.param.IntensityMeasureParams.PeriodParam;
import org.opensha.sha.imr.param.IntensityMeasureParams.SA_InterpolatedParam;
import org.opensha.sha.imr.param.IntensityMeasureParams.SA_Param;
import org.opensha.sha.imr.param.OtherParams.ComponentParam;
import org.opensha.sha.imr.param.OtherParams.StdDevTypeParam;
import org.opensha.sha.imr.param.SiteParams.DepthTo1pt0kmPerSecParam;
import org.opensha.sha.imr.param.SiteParams.DepthTo2pt5kmPerSecParam;
import org.opensha.sha.imr.param.SiteParams.Vs30_Param;
import org.opensha.sha.imr.param.SiteParams.Vs30_TypeParam;

public class MultiIMR_Averaged_AttenRel extends AttenuationRelationship {
	
	public static final String NAME = "Averaged Multi IMR";
	public static final String SHORT_NAME = "MultiIMR";
	
	private static final String C = ClassUtils.getClassNameWithoutPackage(MultiIMR_Averaged_AttenRel.class);
	
	private static final boolean D = true;
	
	private ArrayList<ScalarIntensityMeasureRelationshipAPI> imrs;
	private ArrayList<Double> weights;
	
	
	public MultiIMR_Averaged_AttenRel(ArrayList<ScalarIntensityMeasureRelationshipAPI> imrs) {
		this(imrs, null);
	}
	
	public MultiIMR_Averaged_AttenRel(ArrayList<ScalarIntensityMeasureRelationshipAPI> imrs,
			ArrayList<Double> weights) {
		
		if (imrs == null)
			throw new NullPointerException("imrs cannot be null!");
		if (imrs.size() == 0)
			throw new IllegalArgumentException("imrs must contain at least one IMR");
		
		if (D) System.out.println(SHORT_NAME+": const called with " + imrs.size() + " imrs");
		if (D)
			for (ScalarIntensityMeasureRelationshipAPI imr : imrs)
				System.out.println(" * " + imr.getName());
		
		this.imrs = imrs;
		setWeights(weights);
		this.propEffect = new PropagationEffect();

		initSupportedIntensityMeasureParams();
		initEqkRuptureParams();
		initPropagationEffectParams();
		initSiteParams();
		initOtherParams();
		initIndependentParamLists(); // Do this after the above
	}
	
	public void setWeights(ArrayList<Double> weights) {
		if (weights == null) {
			weights = new ArrayList<Double>();
			double indWeight = 1d / (double)imrs.size();
			for (int i=0; i<imrs.size(); i++) {
				weights.add(indWeight);
			}
		}
		if (weights.size() != imrs.size())
			throw new IllegalArgumentException("There must be exactly one weight for each IMR!");
		double total = 0;
		for (Double weight : weights) {
			total += weight;
		}
		if (total != 1.0)
			throw new IllegalArgumentException("Weights must add up to exactly 1.0");
		this.weights = weights;
	}

	@Override
	protected void initEqkRuptureParams() {
		// do nothing // TODO validate this assumption
	}

	@Override
	protected void initPropagationEffectParams() {
		// do nothing // TODO validate this assumption
	}

	@Override
	protected void initSiteParams() {
		HashMap<String, ArrayList<ScalarIntensityMeasureRelationshipAPI>> paramNameIMRMap =
			new HashMap<String, ArrayList<ScalarIntensityMeasureRelationshipAPI>>();
		for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
			ListIterator<ParameterAPI<?>> siteParamsIt = imr.getSiteParamsIterator();
			while (siteParamsIt.hasNext()) {
				ParameterAPI<?> siteParam = siteParamsIt.next();
				String name = siteParam.getName();
				if (!paramNameIMRMap.containsKey(name))
					paramNameIMRMap.put(name, new ArrayList<ScalarIntensityMeasureRelationshipAPI>());
				ArrayList<ScalarIntensityMeasureRelationshipAPI> imrsForParam = paramNameIMRMap.get(name);
				imrsForParam.add(imr);
			}
		}
		siteParams.clear();
		
		for (String paramName : paramNameIMRMap.keySet()) {
			if (D) System.out.println(SHORT_NAME+": initializing site param: " + paramName);
			// if it's a special case, lets use the param we already have
			ArrayList<ScalarIntensityMeasureRelationshipAPI> imrs = paramNameIMRMap.get(paramName);
			Object defaultVal = imrs.get(0).getParameter(paramName).getDefaultValue();
			if (defaultVal == null)
				defaultVal = imrs.get(0).getParameter(paramName).getValue();
			ParameterAPI masterParam = null;
			if (paramName.equals(Vs30_Param.NAME)) {
				vs30Param = new Vs30_Param();
				masterParam = vs30Param;
			} else if (paramName.equals(Vs30_TypeParam.NAME)) {
				vs30_TypeParam = new Vs30_TypeParam();
				masterParam = vs30_TypeParam;
			} else if (paramName.equals(DepthTo2pt5kmPerSecParam.NAME)) {
				depthTo2pt5kmPerSecParam = new DepthTo2pt5kmPerSecParam();
				masterParam = depthTo2pt5kmPerSecParam;
			} else if (paramName.equals(DepthTo1pt0kmPerSecParam.NAME)) {
				depthTo1pt0kmPerSecParam = new DepthTo1pt0kmPerSecParam();
				masterParam = depthTo1pt0kmPerSecParam;
			} else {
				// it's a custom param not in the atten rel abstract class
				if (D) System.out.println(SHORT_NAME+": " + paramName + " is a custom param!");
			}
			
			for (int i=0; i<imrs.size(); i++) {
				ScalarIntensityMeasureRelationshipAPI imr = imrs.get(i);
				ParameterAPI imrParam = imr.getParameter(paramName);
				if (i == 0) {
					if (masterParam == null) {
						// in this case, we're using the first instance of this param as the master
						masterParam = imrParam;
						trySetDefault(defaultVal, masterParam);
						masterParam.setValue(defaultVal);
						continue;
					} else {
						trySetDefault(defaultVal, masterParam);
						masterParam.setValue(defaultVal);
					}
				} else {
					if (masterParam.getDefaultValue() != null)
						masterParam.setValueAsDefault();
				}
				trySetDefault(defaultVal, imrParam);
				// link the master param to this imr's param
				new ParamLinker(masterParam, imrParam);
			}
			siteParams.addParameter(masterParam);
		}
	}
	
	/**
	 * This will remove any parameters from the given ParameterList which do not exist in the given iterator.
	 * If params is null (such as the first call for this method), all params will be added to the list.
	 * 
	 * @param params
	 * @param it
	 * @return
	 */
	private static ParameterList removeNonCommonParams(ParameterList params, ListIterator<ParameterAPI<?>> it) {
		if (params == null) {
			params = new ParameterList();
			while (it.hasNext())
				params.addParameter(it.next());
			return params;
		}
		
		ParameterList paramsToKeep = new ParameterList();
		while (it.hasNext()) {
			ParameterAPI<?> param = it.next();
			paramsToKeep.addParameter(param);
		}
		ParameterList paramsToRemove = new ParameterList();
		
		for (ParameterAPI<?> param : params) {
			if (!paramsToKeep.containsParameter(param))
				paramsToRemove.addParameter(param);
		}
		
		for (ParameterAPI<?> param : paramsToRemove)
			params.removeParameter(param);
		
		return params;
	}

	@Override
	protected void initSupportedIntensityMeasureParams() {
		ParameterList imrTempList = null;
		for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
			imrTempList = removeNonCommonParams(imrTempList, imr.getSupportedIntensityMeasuresIterator());
		}
		
		saPeriodParam = null;
		saDampingParam = null;
		ArrayList<Double> commonPeriods = null;
		
		if (imrTempList.containsParameter(SA_Param.NAME) || imrTempList.containsParameter(SA_InterpolatedParam.NAME)) {
			saDampingParam = new DampingParam();
			saDampingParam.setNonEditable();
		}
		
		if (imrTempList.containsParameter(SA_Param.NAME)) {
			commonPeriods = IMT_NewGuiBean.getCommonPeriods(imrs);
			if (D) System.out.println(SHORT_NAME+": " + commonPeriods.size() + " common periods found!");
			if (commonPeriods.size() == 0) {
				System.err.println("WARNING: All IMRS have SA, but no common periods! Skipping SA.");
				imrTempList.removeParameter(SA_Param.NAME);
			} else {
				DoubleDiscreteConstraint periodList = new DoubleDiscreteConstraint(commonPeriods);
				Double defaultPeriod = 1.0;
				if (!periodList.isAllowed(defaultPeriod))
					defaultPeriod = periodList.getAllowedDoubles().get(0);
				saPeriodParam = new PeriodParam(periodList, defaultPeriod, false);
				saPeriodParam.setValueAsDefault();
			}
		}
		
		supportedIMParams.clear();
		// now init the params
		for (ParameterAPI<?> imrParam : imrTempList) {
			String name = imrParam.getName();
			if (D) System.out.println(SHORT_NAME+": initializing IM param: " + name);
			if (name.equals(PGA_Param.NAME)) {
				pgaParam = new PGA_Param();
				pgaParam.setNonEditable();
				supportedIMParams.addParameter(pgaParam);
			} else if (name.equals(PGV_Param.NAME)) {
				pgvParam = new PGV_Param();
				pgvParam.setNonEditable();
				supportedIMParams.addParameter(pgvParam);
			} else if (name.equals(PGD_Param.NAME)) {
				pgdParam = new PGD_Param();
				pgdParam.setNonEditable();
				supportedIMParams.addParameter(pgdParam);
			} else if (name.equals(MMI_Param.NAME)) {
				MMI_Param mmiParam = new MMI_Param();
				mmiParam.setNonEditable();
				supportedIMParams.addParameter(mmiParam);
			} else if (name.equals(IA_Param.NAME)) {
				IA_Param iaParam = new IA_Param();
				iaParam.setNonEditable();
				supportedIMParams.addParameter(iaParam);
			} else if (name.equals(SA_Param.NAME)) {
				saParam = new SA_Param(saPeriodParam, saDampingParam);
				saParam.setNonEditable();
				supportedIMParams.addParameter(saParam);
				for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
					ParameterAPI<Double> imrPeriodParam = imr.getParameter(PeriodParam.NAME);
					trySetDefault(saPeriodParam, imrPeriodParam);
					new ParamLinker<Double>(saPeriodParam, imrPeriodParam);
				}
			} else if (name.equals(SA_InterpolatedParam.NAME)) {
				double greatestMin = Double.MIN_VALUE;
				double smallestMax = Double.MAX_VALUE;
				for (ScalarIntensityMeasureRelationshipAPI imr: imrs) {
					SA_InterpolatedParam interParam = 
						(SA_InterpolatedParam)imr.getParameter(SA_InterpolatedParam.NAME);
					try {
						double min = interParam.getPeriodInterpolatedParam().getMin();
						double max = interParam.getPeriodInterpolatedParam().getMax();
						if (min > greatestMin)
							greatestMin = min;
						if (max < smallestMax)
							smallestMax = max;
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				if (smallestMax <= greatestMin)
					throw new RuntimeException("Period ranges don't overlap for interpolated SA");
				double defaultPeriod = 1.0;
				if (defaultPeriod < greatestMin || defaultPeriod > smallestMax)
					defaultPeriod = greatestMin;
				PeriodInterpolatedParam periodInterpParam =
					new PeriodInterpolatedParam(greatestMin, smallestMax,
							defaultPeriod, false);
				periodInterpParam.setValueAsDefault();
				for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
					ParameterAPI<Double> imrPeriodParam = imr.getParameter(PeriodInterpolatedParam.NAME);
					trySetDefault(periodInterpParam, imrPeriodParam);
					new ParamLinker<Double>(periodInterpParam, imrPeriodParam);
				}
				SA_InterpolatedParam saInterParam = new SA_InterpolatedParam(periodInterpParam, saDampingParam);
				supportedIMParams.addParameter(saInterParam);
			} else {
				throw new RuntimeException(SHORT_NAME+" cannot yet handle param of type '" + name + "'");
			}
		}
	}
	
	/**
	 * This creates the lists of independent parameters that the various dependent
	 * parameters (mean, standard deviation, exceedance probability, and IML at
	 * exceedance probability) depend upon. NOTE: these lists do not include anything
	 * about the intensity-measure parameters or any of thier internal
	 * independentParamaters.
	 */
	protected void initIndependentParamLists() {

		// params that the mean depends upon
		meanIndependentParams.clear();
		for (ParameterAPI<?> siteParam : siteParams) {
			meanIndependentParams.addParameter(siteParam);
		}
		if (componentParam != null)
			meanIndependentParams.addParameter(componentParam);

		// params that the stdDev depends upon
		stdDevIndependentParams.clear();
		if (stdDevTypeParam != null)
			stdDevIndependentParams.addParameter(stdDevTypeParam);
		if (componentParam != null)
			stdDevIndependentParams.addParameter(componentParam);

		// params that the exceed. prob. depends upon
		exceedProbIndependentParams.clear();
		for (ParameterAPI<?> siteParam : siteParams) {
			exceedProbIndependentParams.addParameter(siteParam);
		}
		if (componentParam != null)
			exceedProbIndependentParams.addParameter(componentParam);
		if (stdDevTypeParam != null)
			exceedProbIndependentParams.addParameter(stdDevTypeParam);
		if (sigmaTruncTypeParam != null)
			exceedProbIndependentParams.addParameter(sigmaTruncTypeParam);
		if (sigmaTruncLevelParam != null)
			exceedProbIndependentParams.addParameter(sigmaTruncLevelParam);

		// params that the IML at exceed. prob. depends upon
		imlAtExceedProbIndependentParams.addParameterList(
				exceedProbIndependentParams);
		imlAtExceedProbIndependentParams.addParameter(exceedProbParam);

	}
	
	private static void trySetDefault(ParameterAPI master, ParameterAPI child) {
		trySetDefault(master.getDefaultValue(), child);
	}
	
	private static void trySetDefault(Object defaultVal, ParameterAPI param) {
		try {
			param.setDefaultValue(defaultVal);
		} catch (EditableException e) {}
	}

	@Override
	protected void initOtherParams() {
		super.initOtherParams();
		// link up default params
		linkParams(otherParams);
		HashMap<String, ArrayList<ParameterAPI<?>>> newParams = new HashMap<String, ArrayList<ParameterAPI<?>>>();
		// now gather new params from IMRs
		for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
			for (ParameterAPI<?> param : imr.getOtherParamsList()) {
				if (otherParams.containsParameter(param))
					continue;
				if (!newParams.containsKey(param.getName()))
					newParams.put(param.getName(), new ArrayList<ParameterAPI<?>>());
				ArrayList<ParameterAPI<?>> params = newParams.get(param.getName());
				params.add(param);
				
//				if (componentParam == null && param instanceof ComponentParam) {
//					componentParam = (ComponentParam) param;
//				}
//				if (stdDevTypeParam == null && param instanceof StdDevTypeParam) {
//					stdDevTypeParam = (StdDevTypeParam) param;
//				}
//				newParams.addParameter(param);
//				otherParams.addParameter(param);
			}
		}
		for (String paramName : newParams.keySet()) {
			ArrayList<ParameterAPI<?>> params = newParams.get(paramName);
			StringConstraint sconst = null;
			String sDefault = null;
			ParameterAPI masterParam = params.get(0);
			if (params.size() > 1) {
				// this param is common to multiple IMRs
				if (params.get(0) instanceof StringParameter) {
					// hack to make string params consistant
					boolean allCommon = true;
					ArrayList<String> commonVals = null;
					for (ParameterAPI<?> param : params) {
						StringConstraint sconst_temp = (StringConstraint)param.getConstraint();
						ArrayList<String> myVals = sconst_temp.getAllowedValues();
						if (commonVals == null)
							commonVals = myVals;
						for (int i=commonVals.size()-1; i>=0; i--) {
							String commonVal = commonVals.get(i);
							if (!myVals.contains(commonVal)) {
								// this param isn't common after all
								allCommon = false;
								commonVals.remove(i);
							}
						}
						allCommon = allCommon && (commonVals.size() == myVals.size());
					}
					if (!allCommon) {
						if (D) System.out.println("Param '"+paramName+"' has "+commonVals.size()+" common vals");
						if (D)
							for (String val : commonVals)
								System.out.println(" * " + val);
						if (commonVals.size() == 0)
							continue;
						sconst = new StringConstraint(commonVals);
						sDefault = (String) masterParam.getDefaultValue();
						if (sDefault == null || !sconst.isAllowed(sDefault))
							sDefault = commonVals.get(0);
						if (D) System.out.println("NEW DEFAULT: " + sDefault);
					}
					// end string hack
				}
			}
			if (masterParam instanceof ComponentParam) {
				if (sconst != null)
					if (masterParam.isEditable())
						masterParam.setConstraint(sconst);
					else
						masterParam = new ComponentParam(sconst, sDefault);
				componentParam = (ComponentParam) masterParam;
			} else if (masterParam instanceof StdDevTypeParam) {
				if (sconst != null)
					if (masterParam.isEditable())
						masterParam.setConstraint(sconst);
					else
						masterParam = new StdDevTypeParam(sconst, sDefault);
				stdDevTypeParam = (StdDevTypeParam) masterParam;
			} else if (sconst != null) {
				if (masterParam.isEditable())
					masterParam.setConstraint(sconst);
				else {
					StringParameter newSParam = new StringParameter(masterParam.getName(), sconst,
						masterParam.getUnits(), sDefault);
					masterParam = newSParam;
				}
			}
			if (sDefault != null)
				masterParam.setValue(sDefault);
			masterParam.setValueAsDefault();
			for (ParameterAPI<?> param : params) {
				if (masterParam == param)
					continue;
				new ParamLinker(masterParam, param);
			}
			otherParams.addParameter(masterParam);
		}
	}
	
	private void linkParams(Iterable<ParameterAPI<?>> params) {
		for (ParameterAPI masterParam : params) {
			masterParam.setValueAsDefault();
			for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
				try {
					ParameterAPI imrParam = imr.getParameter(masterParam.getName());
					trySetDefault(masterParam, imrParam);
					// link them
					new ParamLinker(masterParam, imrParam);
				} catch (ParameterException e) {
					// this imr doesn't have it
					continue;
				}
			}
		}
	}

	@Override
	public void setEqkRupture(EqkRupture eqkRupture) {
		// Set the eqkRupture
		this.eqkRupture = eqkRupture;

		this.propEffect.setEqkRupture(eqkRupture);
		if (propEffect.getSite() != null) {
			for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
				// TODO speed this up to avoid redundant dist calculations
//				imr.setPropagationEffect(propEffect);
				imr.setEqkRupture(eqkRupture); // this is actually done above
			}
		}
	}

	@Override
	public void setSite(Site site) {
		propEffect.setSite(site);
		for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
			// TODO speed this up to avoid redundant dist calculations
			imr.setSite(site);
		}
	}

	@Override
	protected void setPropagationEffectParams() {
		// do nothing // TODO validate this assumption
		throw new UnsupportedOperationException("setPropagationEffectParams is not supported by "+C);
	}

	@Override
	public void setIntensityMeasure(ParameterAPI intensityMeasure)
			throws ParameterException, ConstraintException {
		super.setIntensityMeasure(intensityMeasure);
		for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
			imr.setIntensityMeasure(intensityMeasure);
		}
	}

	@Override
	public void setIntensityMeasure(String intensityMeasureName)
			throws ParameterException {
		super.setIntensityMeasure(intensityMeasureName);
		for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
			imr.setIntensityMeasure(intensityMeasureName);
		}
	}
	
	private double getWeightedValue(ArrayList<Double> vals) {
		if (vals.size() != weights.size())
			throw new RuntimeException("vals.size() != weights.size()");
		double weighted = 0;
		for (int i=0; i<vals.size(); i++) {
			double val = vals.get(i);
			double weight = weights.get(i);
			weighted += val * weight;
		}
		return weighted;
	}

	@Override
	public double getMean() {
		ArrayList<Double> means = new ArrayList<Double>();
		for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
			means.add(imr.getMean());
		}
		return getWeightedValue(means);
	}

	@Override
	public double getStdDev() {
		ArrayList<Double> std = new ArrayList<Double>();
		for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
			std.add(imr.getStdDev());
		}
		return getWeightedValue(std);
	}

	@Override
	public double getEpsilon() {
		ArrayList<Double> vals = new ArrayList<Double>();
		for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
			vals.add(imr.getEpsilon());
		}
		return getWeightedValue(vals);
	}

	@Override
	public double getEpsilon(double iml) {
		ArrayList<Double> vals = new ArrayList<Double>();
		for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
			vals.add(imr.getEpsilon(iml));
		}
		return getWeightedValue(vals);
	}

	@Override
	public DiscretizedFuncAPI getExceedProbabilities(
			DiscretizedFuncAPI intensityMeasureLevels)
			throws ParameterException {
		ArrayList<DiscretizedFuncAPI> funcs = new ArrayList<DiscretizedFuncAPI>();
		for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
			funcs.add(imr.getExceedProbabilities((DiscretizedFuncAPI)intensityMeasureLevels.deepClone()));
		}
		for (int i=0; i<intensityMeasureLevels.getNum(); i++) {
			ArrayList<Double> vals = new ArrayList<Double>();
			for (DiscretizedFuncAPI func : funcs) {
				vals.add(func.getY(i));
			}
			intensityMeasureLevels.set(i, getWeightedValue(vals));
		}
		return intensityMeasureLevels;
	}

	@Override
	public double getExceedProbability() throws ParameterException,
			IMRException {
		ArrayList<Double> vals = new ArrayList<Double>();
		for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
			vals.add(imr.getExceedProbability());
		}
		return getWeightedValue(vals);
	}

	@Override
	protected double getExceedProbability(double mean, double stdDev, double iml)
			throws ParameterException, IMRException {
		// TODO implement ??
		throw new UnsupportedOperationException("getExceedProbability(mean, stdDev, iml) is unsupported for "+C);
	}

	@Override
	public double getExceedProbability(double iml) throws ParameterException,
			IMRException {
		ArrayList<Double> vals = new ArrayList<Double>();
		for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
			vals.add(imr.getExceedProbability(iml));
		}
		return getWeightedValue(vals);
	}

	@Override
	public double getIML_AtExceedProb() throws ParameterException {
		ArrayList<Double> vals = new ArrayList<Double>();
		for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
			vals.add(imr.getIML_AtExceedProb());
		}
		return getWeightedValue(vals);
	}

	@Override
	public double getIML_AtExceedProb(double exceedProb)
			throws ParameterException {
		ArrayList<Double> vals = new ArrayList<Double>();
		for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
			vals.add(imr.getIML_AtExceedProb(exceedProb));
		}
		return getWeightedValue(vals);
	}

	@Override
	public DiscretizedFuncAPI getSA_ExceedProbSpectrum(double iml)
			throws ParameterException, IMRException {
		// TODO implement
		throw new UnsupportedOperationException("getSA_IML_AtExceedProbSpectrum is unsupported for "+C);
	}

	@Override
	public DiscretizedFuncAPI getSA_IML_AtExceedProbSpectrum(double exceedProb)
			throws ParameterException, IMRException {
		// TODO implement
		throw new UnsupportedOperationException("getSA_IML_AtExceedProbSpectrum is unsupported for "+C);
	}

	@Override
	public double getTotExceedProbability(PointEqkSource ptSrc, double iml) {
		throw new UnsupportedOperationException("getTotExceedProbability is unsupported for "+C);
	}

	@Override
	public void setIntensityMeasureLevel(Double iml) throws ParameterException {
		for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
			imr.setIntensityMeasureLevel(iml);
		}
	}

	@Override
	public void setIntensityMeasureLevel(Object iml) throws ParameterException {
		for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
			imr.setIntensityMeasureLevel(iml);
		}
	}

	@Override
	public void setSiteLocation(Location loc) {
		for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
			imr.setSiteLocation(loc);
		}
	}

	@Override
	public void setUserMaxDistance(double maxDist) {
		for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
			imr.setUserMaxDistance(maxDist);
		}
	}

	@Override
	public String getShortName() {
		return SHORT_NAME;
	}
	
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void setParamDefaults() {
		for (ScalarIntensityMeasureRelationshipAPI imr : imrs) {
			imr.setParamDefaults();
		}
	}

}
