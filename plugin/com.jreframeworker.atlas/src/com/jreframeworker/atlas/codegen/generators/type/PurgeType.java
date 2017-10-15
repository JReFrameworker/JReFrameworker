package com.jreframeworker.atlas.codegen.generators.type;

import com.ensoftcorp.atlas.core.query.Q;
import com.jreframeworker.core.JReFrameworkerProject;

public class PurgeType extends TypeGenerator {

	@Override
	public String getName() {
		return "Purge";
	}

	@Override
	public String getDescription() {
		return "Removes an existing type.";
	}

	@Override
	public void generateCode(JReFrameworkerProject jrefProject, Q input) {
		// TODO: implement
	}

}
