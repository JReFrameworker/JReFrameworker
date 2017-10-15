package com.jreframeworker.atlas.codegen.generators.method;

import com.ensoftcorp.atlas.core.query.Q;
import com.jreframeworker.core.JReFrameworkerProject;

public class DefineMethod extends MethodGenerator {

	@Override
	public String getName() {
		return "Define";
	}

	@Override
	public String getDescription() {
		return "Defines a new method.";
	}

	@Override
	public void generateCode(JReFrameworkerProject jrefProject, Q input) {
		// TODO: implement
	}
	
	@Override
	protected String[] getSupportedNodeTags() {
		return EVERYTHING;
	}

}
