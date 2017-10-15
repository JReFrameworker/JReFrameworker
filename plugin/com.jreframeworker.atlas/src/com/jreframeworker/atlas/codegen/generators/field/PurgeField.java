package com.jreframeworker.atlas.codegen.generators.field;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

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
	public Set<File> generateCode(JReFrameworkerProject jrefProject, Q input) {
		// TODO: implement
		return new HashSet<File>();
	}

}
