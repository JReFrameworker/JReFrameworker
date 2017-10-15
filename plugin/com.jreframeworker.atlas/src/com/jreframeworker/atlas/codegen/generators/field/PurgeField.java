package com.jreframeworker.atlas.codegen.generators.field;

import com.ensoftcorp.atlas.core.query.Q;
import com.jreframeworker.core.JReFrameworkerProject;

public class PurgeField extends FieldGenerator {

	@Override
	public String getName() {
		return "Purge";
	}

	@Override
	public String getDescription() {
		return "Removes an existing field.";
	}

	@Override
	public void generateCode(JReFrameworkerProject jrefProject, Q input) {
		// TODO: implement
	}

}
