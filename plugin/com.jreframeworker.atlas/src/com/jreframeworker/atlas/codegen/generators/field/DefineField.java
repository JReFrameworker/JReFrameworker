package com.jreframeworker.atlas.codegen.generators.field;

import com.ensoftcorp.atlas.core.query.Q;
import com.jreframeworker.core.JReFrameworkerProject;

public class DefineField extends FieldGenerator {

	@Override
	public String getName() {
		return "Define";
	}

	@Override
	public String getDescription() {
		return "Defines a new field.";
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
