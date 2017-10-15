package com.jreframeworker.atlas.codegen.generators.method;

import java.io.File;
import java.util.Set;

import com.ensoftcorp.atlas.core.query.Q;
import com.jreframeworker.atlas.projects.JReFrameworkerAtlasProject;
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
	public Set<File> generateCode(JReFrameworkerProject jrefProject, Q input) {
		JReFrameworkerAtlasProject atlasProject = new JReFrameworkerAtlasProject(jrefProject);
		return atlasProject.addPostExecutionMethodHooks(input);
	}

}
