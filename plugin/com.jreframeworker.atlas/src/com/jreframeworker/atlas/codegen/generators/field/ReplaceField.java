package com.jreframeworker.atlas.codegen.generators.field;

import java.io.File;
import java.util.Set;

import com.ensoftcorp.atlas.core.query.Q;
import com.jreframeworker.atlas.projects.JReFrameworkerAtlasProject;
import com.jreframeworker.core.JReFrameworkerProject;

public class ReplaceField extends FieldGenerator {

	@Override
	public String getName() {
		return "Replace";
	}

	@Override
	public String getDescription() {
		return "Replaces an existing field.";
	}

	@Override
	public Set<File> generateCode(JReFrameworkerProject jrefProject, Q input) {
		JReFrameworkerAtlasProject atlasProject = new JReFrameworkerAtlasProject(jrefProject);
		return atlasProject.replaceFields(input);
	}

}
