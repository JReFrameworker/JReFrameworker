package java.io;

import jreframeworker.annotations.methods.MergeMethod;
import jreframeworker.annotations.types.MergeType;

@MergeType
public class HiddenFile extends File {

	private static final long serialVersionUID = 1L;

	public HiddenFile(String name) {
		super(name);
	}
	
	@MergeMethod
	@Override
	public boolean exists(){
		if(isFile() && getName().equals("secretFile")){
			return false;
		} else {
			return super.exists();
		}
	}

}
