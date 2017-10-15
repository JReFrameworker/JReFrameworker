package com.jreframeworker.atlas.codegen.generators.method;

import java.io.File;
import java.util.Set;

import com.ensoftcorp.atlas.core.query.Q;
import com.jreframeworker.atlas.projects.JReFrameworkerAtlasProject;
import com.jreframeworker.core.JReFrameworkerProject;

public class MergeMethod extends MethodGenerator {

	@Override
	public String getName() {
		return "Merge";
	}

	@Override
	public String getDescription() {
		return "Merges functionality into an existing method.";
	}

	@Override
	public Set<File> generateCode(JReFrameworkerProject jrefProject, Q input) {
		JReFrameworkerAtlasProject atlasProject = new JReFrameworkerAtlasProject(jrefProject);
		return atlasProject.mergeMethods(input);
	}

}
