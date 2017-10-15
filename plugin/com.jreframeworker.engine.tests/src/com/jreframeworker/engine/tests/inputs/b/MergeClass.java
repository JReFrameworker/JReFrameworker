package com.jreframeworker.engine.tests.inputs.b;

import com.jreframeworker.annotations.methods.MergeMethod;
import com.jreframeworker.annotations.types.MergeType;

@MergeType
public class MergeClass extends BaseClass {

	@Override
	@MergeMethod
	public String method(){
		return "merged-" + super.method();
	}
	
}
