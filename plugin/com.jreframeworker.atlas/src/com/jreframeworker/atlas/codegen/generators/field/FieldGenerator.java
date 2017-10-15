package com.jreframeworker.atlas.codegen.generators.field;

import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.jreframeworker.atlas.codegen.CodeGenerator;

public abstract class FieldGenerator extends CodeGenerator {

	@Override
	public String getCategory() {
		return "Field";
	}
	
	@Override
	protected String[] getSupportedNodeTags() {
		return new String[]{ XCSG.ClassVariable, XCSG.InstanceVariable };
	}

	@Override
	protected String[] getSupportedEdgeTags() {
		return NOTHING;
	}

}
