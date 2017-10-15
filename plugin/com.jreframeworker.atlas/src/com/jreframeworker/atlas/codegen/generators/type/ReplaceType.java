package com.jreframeworker.atlas.codegen.generators.type;

import java.io.File;
import java.util.Set;

import com.ensoftcorp.atlas.core.query.Q;
import com.jreframeworker.atlas.projects.JReFrameworkerAtlasProject;
import com.jreframeworker.core.JReFrameworkerProject;

public class ReplaceType extends TypeGenerator {

	@Override
	public String getName() {
		return "Replace";
	}

	@Override
	public String getDescription() {
		return "Replaces an existing type.";
	}

	@Override
	public Set<File> generateCode(JReFrameworkerProject jrefProject, Q input) {
		JReFrameworkerAtlasProject atlasProject = new JReFrameworkerAtlasProject(jrefProject);
		return atlasProject.replaceTypes(input);
	}

}
