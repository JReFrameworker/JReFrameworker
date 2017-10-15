package com.jreframeworker.atlas.codegen.generators.method;

import java.io.File;
import java.util.Set;

import com.ensoftcorp.atlas.core.query.Q;
import com.jreframeworker.atlas.projects.JReFrameworkerAtlasProject;
import com.jreframeworker.core.JReFrameworkerProject;

public class HookAfterMethod extends MethodGenerator {

	@Override
	public String getName() {
		return "Hook Before";
	}

	@Override
	public String getDescription() {
		return "Inserts functionality before a method invocation.";
	}

	@Override
	public Set<File> generateCode(JReFrameworkerProject jrefProject, Q input) {
		JReFrameworkerAtlasProject atlasProject = new JReFrameworkerAtlasProject(jrefProject);
		return atlasProject.addPreExecutionMethodHooks(input);
	}

}
