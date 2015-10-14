package verifier;

import java.io.IOException;

public class ClassVerifier {

	public static void verify(String qualifiedClassName) throws IOException {
		org.apache.bcel.verifier.Verifier.main(new String[]{qualifiedClassName});
	}
	
	public static void main(String[] args){
		org.apache.bcel.verifier.GraphicalVerifier.main(new String[]{});
	}
	
}
