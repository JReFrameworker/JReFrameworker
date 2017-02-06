package jreframeworker.engine.tests.inputs.a;

import jreframeworker.annotations.methods.MergeMethod;
import jreframeworker.annotations.types.MergeType;

@MergeType
public class MergeClass extends BaseClass {

	@Override
	@MergeMethod
	public String method(){
		return "merge-method";
	}
	
}
