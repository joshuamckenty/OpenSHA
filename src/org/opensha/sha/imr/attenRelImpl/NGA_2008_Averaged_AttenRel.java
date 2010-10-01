package org.opensha.sha.imr.attenRelImpl;

import java.util.ArrayList;

import org.opensha.commons.param.event.ParameterChangeWarningListener;
import org.opensha.sha.imr.ScalarIntensityMeasureRelationshipAPI;

public class NGA_2008_Averaged_AttenRel extends MultiIMR_Averaged_AttenRel {
	
	public static final String NAME = "NGA 2008 Averaged Attenuation Relationship (unverified!)";
	public static final String SHORT_NAME = "NGA_2008";
	
	private static ArrayList<ScalarIntensityMeasureRelationshipAPI> buildIMRs(ParameterChangeWarningListener listener) {
		ArrayList<ScalarIntensityMeasureRelationshipAPI> imrs = new ArrayList<ScalarIntensityMeasureRelationshipAPI>();
		imrs.add(new CB_2008_AttenRel(listener));
		imrs.add(new BA_2008_AttenRel(listener));
		imrs.add(new CY_2008_AttenRel(listener));
		imrs.add(new AS_2008_AttenRel(listener));
		return imrs;
	}
	
	public NGA_2008_Averaged_AttenRel(ParameterChangeWarningListener listener) {
		super(buildIMRs(listener));
	}

	@Override
	public String getShortName() {
		return SHORT_NAME;
	}

	@Override
	public String getName() {
		return NAME;
	}

}
