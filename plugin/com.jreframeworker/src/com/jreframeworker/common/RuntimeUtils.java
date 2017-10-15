package com.jreframeworker.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;

import com.jreframeworker.core.JReFrameworkerProject;

public class RuntimeUtils {

//	public static File getDefaultRuntime() throws Exception {
//		IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
//		LinkedList<File> libraries = new LinkedList<File>();
//		for (LibraryLocation element : JavaRuntime.getLibraryLocations(vmInstall)) {
//			libraries.add(JavaCore.newLibraryEntry(element.getSystemLibraryPath(), null, null).getPath().toFile().getCanonicalFile());
//		}
//		Log.info(libraries.toString());
//		return findJavaRuntimeJar(libraries.toArray(new File[libraries.size()]));
//	}
//
//	public static File findJavaRuntimeJar(File... files) throws Exception {
//		for (File file : files) {
//			if (file.getName().equals("rt.jar")) {
//				return file;
//			}
//		}
//		throw new Exception("Could not located default runtime!");
//	}
	
	public static File getClasspathJar(String targetJarName, JReFrameworkerProject jrefProject) throws IOException, JavaModelException {
		for(IClasspathEntry classpathEntry : jrefProject.getJavaProject().getRawClasspath()){
			if(classpathEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY){
				File jar = classpathEntry.getPath().toFile().getCanonicalFile();
				if(jar.getName().equals(targetJarName)){
					if(!jar.exists()){
						// path may have been relative, so try again to resolve path relative to project directory
						String relativePath = jar.getAbsolutePath();
						String projectName = jrefProject.getProject().getName();
						String projectRoot = File.separator + projectName;
						relativePath = relativePath.substring(relativePath.indexOf(projectRoot) + projectRoot.length());
						jar = jrefProject.getProject().getFile(relativePath).getLocation().toFile();
						if(!jar.exists()){
							// if jar still doesn't exist match any jar in the project with the same name
							jar = jrefProject.getProject().getFile(targetJarName).getLocation().toFile();
						}
					}
					return jar;
				}
			}
		}
		return getRuntimeJar(targetJarName);
	}

	public static boolean isRuntimeJar(File jar) throws IOException {
		IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
		LibraryLocation[] locations = JavaRuntime.getLibraryLocations(vmInstall);
		for (LibraryLocation library : locations) {
			File runtime = JavaCore.newLibraryEntry(library.getSystemLibraryPath(), null, null).getPath().toFile().getCanonicalFile();
			if(runtime.equals(jar.getCanonicalFile())){
				return true;
			}
		}
		return false;
	}
	
	public static File getRuntimeJar(String jarName) throws IOException {
		IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
		LibraryLocation[] locations = JavaRuntime.getLibraryLocations(vmInstall);
		for (LibraryLocation library : locations) {
			File runtime = JavaCore.newLibraryEntry(library.getSystemLibraryPath(), null, null).getPath().toFile().getCanonicalFile();
			if(runtime.getName().equals(jarName)){
				return runtime;
			}
		}
		return null;
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
