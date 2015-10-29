package java.io;

import jreframeworker.annotations.methods.MergeMethod;
import jreframeworker.annotations.types.MergeType;

@MergeType
public class BackwardsPrintStream extends PrintStream {

	public BackwardsPrintStream(OutputStream os) {
		super(os);
	}
	
	@MergeMethod
	@Override
	public void println(String str){
		StringBuilder sb = new StringBuilder(str);
		super.println(sb.reverse().toString());
	}

}
