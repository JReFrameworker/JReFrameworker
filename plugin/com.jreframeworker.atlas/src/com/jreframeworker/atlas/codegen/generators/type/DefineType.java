package com.jreframeworker.atlas.codegen.generators.type;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

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
	public Set<File> generateCode(JReFrameworkerProject jrefProject, Q input) {
		// TODO: implement
		return new HashSet<File>();
	}
	
	@Override
	protected String[] getSupportedNodeTags() {
		return EVERYTHING;
	}

}
