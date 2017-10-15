package com.jreframeworker.atlas.codegen.generators.method;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.ensoftcorp.atlas.core.query.Q;
import com.jreframeworker.core.JReFrameworkerProject;

public class PurgeMethod extends MethodGenerator {

	@Override
	public String getName() {
		return "Purge";
	}

	@Override
	public String getDescription() {
		return "Removes an existing method.";
	}

	@Override
	public Set<File> generateCode(JReFrameworkerProject jrefProject, Q input) {
		// TODO: implement
		return new HashSet<File>();
	}

}
