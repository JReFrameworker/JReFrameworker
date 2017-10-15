package com.jreframeworker.atlas.codegen.generators.type;

import com.ensoftcorp.atlas.core.query.Q;
import com.jreframeworker.core.JReFrameworkerProject;

public class DefineType extends TypeGenerator {

	@Override
	public String getName() {
		return "Define";
	}

	@Override
	public String getDescription() {
		return "Defines a new type.";
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
