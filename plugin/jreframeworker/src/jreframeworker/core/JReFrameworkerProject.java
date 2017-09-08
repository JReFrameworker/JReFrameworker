package jreframeworker.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileDeleteStrategy;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.xml.sax.SAXException;

import jreframeworker.builder.JReFrameworkerBuilder;
import jreframeworker.core.BuildFile.LibraryTarget;
import jreframeworker.log.Log;

@SuppressWarnings("restriction")
public class JReFrameworkerProject {

	private IProject project;
	private IJavaProject jProject;
	
	public JReFrameworkerProject(IProject project) {
		this.project = project;
		this.jProject = JavaCore.create(project);
	}
	
	public BuildFile getBuildFile(){
		return BuildFile.getOrCreateBuildFile(jProject);
	}
	
	public File getBuildDirectory() {
		return getProject().getFolder(JReFrameworker.BUILD_DIRECTORY).getLocation().toFile();
	}
	
	public File getBinaryDirectory(){
		return getProject().getFolder(JReFrameworker.BINARY_DIRECTORY).getLocation().toFile();
	}
	
	public File getSourceDirectory(){
		return getProject().getFolder(JReFrameworker.SOURCE_DIRECTORY).getLocation().toFile();
	}
	
	/**
	 * Returns the Eclipse project resource
	 * @return
	 */
	public IProject getProject(){
		return project;
	}
	
	/**
	 * Returns the Eclipse project resource
	 * @return
	 */
	public IJavaProject getJavaProject(){
		return jProject;
	}
	
	public void clean() throws CoreException {
		try {
			File buildDirectory = project.getFolder(JReFrameworker.BUILD_DIRECTORY).getLocation().toFile();

			// restore the classpath
			restoreOriginalClasspathEntries();

			if(buildDirectory.exists()) {
				clearProjectBuildDirectory(buildDirectory);
			}
		} catch (Exception e) {
			Log.error("Error cleaning " + project.getName(), e);
		}
		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	}

	private void clearProjectBuildDirectory(File buildDirectory) throws IOException {
		for(File file : buildDirectory.listFiles()){
			if(file.isDirectory()){
				File directory = file;
				clearProjectBuildDirectory(directory);
				directory.delete();
			} else {
				FileDeleteStrategy.FORCE.delete(file);
			}
		}
	}
	
	public void disableJavaBuilder() throws CoreException {
		IProjectDescription description = getProject().getDescription();
		ICommand[] commands = description.getBuildSpec();
		for (int i = 0; i < commands.length; ++i) {
			// if the command exists, remove it
			if (commands[i].getBuilderName().equals(JavaCore.BUILDER_ID)) {
				ICommand[] newCommands = new ICommand[commands.length - 1];
				System.arraycopy(commands, 0, newCommands, 0, i);
				System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
				description.setBuildSpec(newCommands);
				project.setDescription(description, null);			
				return;
			}
		}
	}
	
	public void enableJavaBuilder() throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();
		// if the command already exists, don't add it again
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(JavaCore.BUILDER_ID)) {
				return;
			}
		}
		ICommand[] newCommands = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, newCommands, 0, commands.length);
		ICommand command = desc.newCommand();
		command.setBuilderName(JavaCore.BUILDER_ID);
		newCommands[newCommands.length - 1] = command;
		
		if(!command1PreceedsCommand2(JavaCore.BUILDER_ID, JReFrameworkerBuilder.BUILDER_ID, newCommands)){
			swapBuildCommands(JavaCore.BUILDER_ID, JReFrameworkerBuilder.BUILDER_ID, newCommands);
		}
		
		desc.setBuildSpec(newCommands);
		project.setDescription(desc, null);
	}
	
	private boolean command1PreceedsCommand2(String command1, String command2, ICommand[] commands) {
		int command1Position = -1;
		int command2Position = -1;
		for (int i = 0; i < commands.length; ++i) {
			if(commands[i].getBuilderName().equals(command1)){
				command1Position = i;
			}
			if(commands[i].getBuilderName().equals(command2)){
				command2Position = i;
			}
		}
		if(command1Position != -1 && command2Position != -1 && command1Position != command2Position){
			if(command1Position < command2Position){
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	private void swapBuildCommands(String command1, String command2, ICommand[] commands) {
		int command1Position = -1;
		int command2Position = -1;
		for (int i = 0; i < commands.length; ++i) {
			if(commands[i].getBuilderName().equals(command1)){
				command1Position = i;
			}
			if(commands[i].getBuilderName().equals(command2)){
				command2Position = i;
			}
		}
		if(command1Position != -1 && command2Position != -1 && command1Position != command2Position){
			swapBuildCommands(command1Position, command2Position, commands);
		}
	}
	
	private void swapBuildCommands(int command1Position, int command2Position, ICommand[] commands) {
		ICommand swap = commands[command1Position];
		commands[command1Position] = commands[command2Position];
		commands[command2Position] = swap;
	}

	/**
	 * Lists the JReFrameworker project targets
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public Set<String> listTargets() throws SAXException, IOException, ParserConfigurationException {
		Set<String> targets = new HashSet<String>();
		for(BuildFile.Target target : getBuildFile().getTargets()){
			targets.add(target.getName());
		}
		return targets;
	}
	
	/**
	 * Adds a target from the JReFrameworker project
	 * @throws CoreException 
	 * @throws URISyntaxException 
	 */
	public void addTarget(File targetLibrary) throws TransformerException, ParserConfigurationException, SAXException, IOException, URISyntaxException, CoreException {
		String entry = addProjectLibrary(jProject, targetLibrary);
		
		// update the build file
		BuildFile buildFile = getBuildFile();
		buildFile.addLibraryTarget(targetLibrary.getName(), entry);
	}
	
	/**
	 * Adds a target with the given relative library directory
	 * @param targetLibrary
	 * @param relativeLibraryDirectory
	 * @throws TransformerException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws CoreException
	 */
	public void addTarget(File targetLibrary, String relativeLibraryDirectory) throws TransformerException, ParserConfigurationException, SAXException, IOException, URISyntaxException, CoreException {
		String entry = addProjectLibrary(jProject, targetLibrary, relativeLibraryDirectory);
		
		// update the build file
		BuildFile buildFile = getBuildFile();
		buildFile.addLibraryTarget(targetLibrary.getName(), entry);
	}
	
	/**
	 * Removes a target from the JReFrameworker project
	 */
	public void removeTarget(String target) throws TransformerException, ParserConfigurationException, SAXException, IOException {
		BuildFile.getOrCreateBuildFile(jProject).removeTarget(target);
	}
	
	public void refresh() throws CoreException {
		jProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	}
	
	/**
	 * Copies a library into the project root directory and updates the classpath
	 * @param jProject
	 * @param library
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 * @throws CoreException
	 */
	private static String addProjectLibrary(IJavaProject jProject, File library) throws IOException, URISyntaxException, MalformedURLException, CoreException {
		return addProjectLibrary(jProject, library, null);
	}
	
	/**
	 * Replaces the classpath jar entry with the given jar
	 * @param jarName
	 * @param updatedLibrary
	 * @throws IOException 
	 * @throws CoreException 
	 */
	public void updateProjectLibrary(String jarName, File updatedLibrary) throws IOException, CoreException {
		updatedLibrary = updatedLibrary.getCanonicalFile();
		String updatedLibraryPath = updatedLibrary.getCanonicalPath();
		File projectRoot = project.getLocation().toFile().getCanonicalFile();
		
		boolean isUpdatedLibraryContainedInProject = false;
		File parent = updatedLibrary.getParentFile();
		while(parent != null){
			if(parent.equals(projectRoot)){
				isUpdatedLibraryContainedInProject = true;
				break;
			} else {
				parent = parent.getParentFile();
			}
		}
		
		// if the updated library is inside the project, then make the path relative
		// otherwise we must use the absolution path
		if(isUpdatedLibraryContainedInProject){ // TODO: is this condition working correctly?
			String base = projectRoot.getCanonicalPath();
			String relativeFilePath = updatedLibrary.getCanonicalPath().substring(base.length());
			if(relativeFilePath.charAt(0) == File.separatorChar){
				relativeFilePath = relativeFilePath.substring(1);
			}
			updatedLibraryPath = relativeFilePath;
		}
		
		// create path to library
		IPath path;
		// TODO: figure out why this is causing "Illegal require library path" error during testing
//		if(isUpdatedLibraryContainedInProject){
//			updatedLibraryPath = updatedLibraryPath.replace(File.separator, "/");
//	    	// library is at some path relative to project root
//			path = new Path(updatedLibraryPath);
//	    } else {
	    	// library is outside the project, using absolute path
	    	path = jProject.getProject().getFile(updatedLibraryPath).getLocation();
//	    }
		
		// create a classpath entry for the library
		IClasspathEntry updatedLibraryEntry = new org.eclipse.jdt.internal.core.ClasspathEntry(
    	        IPackageFragmentRoot.K_BINARY,
    	        IClasspathEntry.CPE_LIBRARY, path,
    	        ClasspathEntry.INCLUDE_ALL, // inclusion patterns
    	        ClasspathEntry.EXCLUDE_NONE, // exclusion patterns
    	        null, null, null, // specific output folder
    	        false, // exported
    	        ClasspathEntry.NO_ACCESS_RULES, false, // no access rules to combine
    	        ClasspathEntry.NO_EXTRA_ATTRIBUTES);
		
		// search through the classpath's existing entries and replace the corresponding library entry
	    IClasspathEntry[] entries = jProject.getRawClasspath();
	    for(int i=0; i< entries.length; i++){
	    	if(entries[i].getPath().toFile().getName().equals(jarName)){
	    		entries[i] =  updatedLibraryEntry;
	    		// assuming there is only one library with the same name...
	    		break;
	    	}
	    }
	    jProject.setRawClasspath(entries, null);
	    
	    // refresh project
	 	jProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	}
	
	/**
	 * Copies a jar into the project at the specified relative path and updates the classpath
	 * @param jProject
	 * @param libraryToAdd
	 * @param relativeDirectoryPath
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 * @throws CoreException
	 */
	private static String addProjectLibrary(IJavaProject jProject, File libraryToAdd, String relativeDirectoryPath) throws IOException, URISyntaxException, MalformedURLException, CoreException {
		
		// only add the project library to the classpath if its not already there
		for(IClasspathEntry entry : jProject.getRawClasspath()){
			if(entry.getPath().toFile().getName().endsWith(libraryToAdd.getName())){
				return entry.getPath().toString();
			}
		}
		
	    // copy the jar file into the project (if its not already there)
	    InputStream libraryInputStream = new BufferedInputStream(new FileInputStream(libraryToAdd));
	    File libDirectory;
	    if(relativeDirectoryPath == null || relativeDirectoryPath.equals("")){
	    	libDirectory = new File(jProject.getProject().getLocation().toFile().getCanonicalPath());
	    } else {
	    	relativeDirectoryPath = relativeDirectoryPath.replace("/", File.separator).replace("\\", File.separator);
	    	libDirectory = new File(jProject.getProject().getLocation().toFile().getCanonicalPath() + File.separator + relativeDirectoryPath);
	    }
		libDirectory.mkdirs();
		File library = new File(libDirectory.getCanonicalPath() + File.separatorChar + libraryToAdd.getName());
		if(!library.exists()){
			Files.copy(libraryInputStream, library.toPath());
		}

		// refresh project
		jProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		
	    // create a classpath entry for the library
		IClasspathEntry relativeLibraryEntry;
		if(relativeDirectoryPath != null){
	    	relativeDirectoryPath = relativeDirectoryPath.replace(File.separator, "/");
	    	// library is at some path relative to project root
	    	relativeLibraryEntry = new org.eclipse.jdt.internal.core.ClasspathEntry(
	    	        IPackageFragmentRoot.K_BINARY,
	    	        IClasspathEntry.CPE_LIBRARY, jProject.getProject().getFile(relativeDirectoryPath).getLocation(),
	    	        ClasspathEntry.INCLUDE_ALL, // inclusion patterns
	    	        ClasspathEntry.EXCLUDE_NONE, // exclusion patterns
	    	        null, null, null, // specific output folder
	    	        false, // exported
	    	        ClasspathEntry.NO_ACCESS_RULES, false, // no access rules to combine
	    	        ClasspathEntry.NO_EXTRA_ATTRIBUTES);
	    } else {
	    	// library placed at project root
	    	relativeLibraryEntry = new org.eclipse.jdt.internal.core.ClasspathEntry(
	    	        IPackageFragmentRoot.K_BINARY,
	    	        IClasspathEntry.CPE_LIBRARY, jProject.getProject().getFile(libraryToAdd.getName()).getLocation(),
	    	        ClasspathEntry.INCLUDE_ALL, // inclusion patterns
	    	        ClasspathEntry.EXCLUDE_NONE, // exclusion patterns
	    	        null, null, null, // specific output folder
	    	        false, // exported
	    	        ClasspathEntry.NO_ACCESS_RULES, false, // no access rules to combine
	    	        ClasspathEntry.NO_EXTRA_ATTRIBUTES);
	    }

	    // add the new classpath entry to the project's existing entries
	    IClasspathEntry[] oldEntries = jProject.getRawClasspath();
	    IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
	    System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
	    newEntries[oldEntries.length] = relativeLibraryEntry;
	    jProject.setRawClasspath(newEntries, null);
	    
	    // refresh project
	 	jProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	 	
	 	return relativeLibraryEntry.getPath().toString();
	}

	/**
	 * Restores the original classpath entries of the project
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws CoreException
	 */
	public void restoreOriginalClasspathEntries() throws SAXException, IOException, ParserConfigurationException, CoreException {
		for(BuildFile.Target entry : getBuildFile().getTargets()){
			if(entry.isRuntime()){
				// TODO: implement
				throw new RuntimeException("Not Implemented");
//				File runtime = RuntimeUtils.getRuntimeJar(entry.getName());
//				updateProjectLibrary(runtime.getName(), runtime);
			} else {
				File library = new File(((LibraryTarget) entry).getLibraryPath());
				if(library.exists()){
					// absolute path
					updateProjectLibrary(library.getName(), library);
				} else {
					// relative path
					library = project.getFile(((LibraryTarget) entry).getLibraryPath()).getLocation().toFile();
					if(library.exists()){
						updateProjectLibrary(library.getName(), library);
					}
				}
			}
		}
	}

}
