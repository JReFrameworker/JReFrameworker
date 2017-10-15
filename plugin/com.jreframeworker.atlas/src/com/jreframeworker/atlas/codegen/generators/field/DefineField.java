package com.jreframeworker.atlas.codegen.generators.field;

import java.io.File;
import java.util.Set;

import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.jreframeworker.atlas.projects.JReFrameworkerAtlasProject;
import com.jreframeworker.core.JReFrameworkerProject;

public class DefineField extends FieldGenerator {

	@Override
	public String getName() {
		return "Define";
	}

	@Override
	public String getDescription() {
		return "Defines a new field.";
	}

	@Override
	public Set<File> generateCode(JReFrameworkerProject jrefProject, Q input) {
		JReFrameworkerAtlasProject atlasProject = new JReFrameworkerAtlasProject(jrefProject);
		
		// TODO: finish implementation
		
		return atlasProject.defineFields(input);
	}
	
	@Override
	protected String[] getSupportedNodeTags() {
		return new String[]{ XCSG.Java.Class };
	}

}
