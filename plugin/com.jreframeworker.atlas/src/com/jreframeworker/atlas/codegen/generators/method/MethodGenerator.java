package com.jreframeworker.atlas.codegen.generators.method;

import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.jreframeworker.atlas.codegen.CodeGenerator;

public abstract class MethodGenerator extends CodeGenerator {

	@Override
	public String getCategory() {
		return "Method";
	}
	
	@Override
	protected String[] getSupportedNodeTags() {
		return new String[]{ XCSG.Method };
	}

	@Override
	protected String[] getSupportedEdgeTags() {
		return NOTHING;
	}

}
