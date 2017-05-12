package jreframeworker.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;

import jreframeworker.log.Log;

public class RuntimeUtils {

	public static File getDefaultRuntime() throws Exception {
		IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
		LinkedList<File> libraries = new LinkedList<File>();
		for (LibraryLocation element : JavaRuntime.getLibraryLocations(vmInstall)) {
			libraries.add(JavaCore.newLibraryEntry(element.getSystemLibraryPath(), null, null).getPath().toFile().getCanonicalFile());
		}
		Log.info(libraries.toString());
		return findJavaRuntimeJar(libraries.toArray(new File[libraries.size()]));
	}

	public static File findJavaRuntimeJar(File... files) throws Exception {
		for (File file : files) {
			if (file.getName().equals("rt.jar")) {
				return file;
			}
		}
		throw new Exception("Could not located default runtime!");
	}

	// modified from http://rosettacode.org/wiki/Find_common_directory_path#Java
	// a helper method for finding the common parent of a set of files
	public static File commonDirectory(File... files) throws IOException {
		String[] paths = new String[files.length];
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				paths[i] = files[i].getCanonicalPath();
			} else {
				paths[i] = files[i].getParentFile().getCanonicalPath();
			}
		}
		String commonPath = "";
		String[][] folders = new String[paths.length][];
		for (int i = 0; i < paths.length; i++) {
			folders[i] = paths[i].split(File.separator);
		}
		for (int j = 0; j < folders[0].length; j++) {
			String thisFolder = folders[0][j];
			boolean allMatched = true;
			for (int i = 1; i < folders.length && allMatched; i++) {
				if (folders[i].length < j) {
					allMatched = false;
					break;
				}
				// otherwise
				allMatched &= folders[i][j].equals(thisFolder);
			}
			if (allMatched) {
				commonPath += thisFolder + File.separatorChar;
			} else {
				break;
			}
		}
		return new File(commonPath);
	}
	
	// helper method to copy a file from source to destination
	public static void copyFile(File from, File to) throws IOException {
		Files.copy(from.toPath(), to.toPath());
	}

}
