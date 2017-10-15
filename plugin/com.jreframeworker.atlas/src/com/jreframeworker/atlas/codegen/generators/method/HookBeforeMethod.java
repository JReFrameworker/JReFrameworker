package com.jreframeworker.atlas.codegen.generators.method;

import com.ensoftcorp.atlas.core.query.Q;
import com.jreframeworker.core.JReFrameworkerProject;

public class HookBeforeMethod extends MethodGenerator {

	@Override
	public String getName() {
		return "Hook After";
	}

	@Override
	public String getDescription() {
		return "Inserts functionality after a method invocation.";
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
