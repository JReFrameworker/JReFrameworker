package com.jreframeworker.atlas.codegen.generators.type;

import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.jreframeworker.atlas.codegen.CodeGenerator;

public abstract class TypeGenerator extends CodeGenerator {

	@Override
	public String getCategory() {
		return "Type";
	}
	
	@Override
	protected String[] getSupportedNodeTags() {
		return new String[]{ XCSG.Java.Class };
	}

	@Override
	protected String[] getSupportedEdgeTags() {
		return NOTHING;
	}

}
