package jreframeworker.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.xml.sax.SAXException;

@SuppressWarnings("restriction")
public class JReFrameworkerProject {

	private IProject project;
	private IJavaProject jProject;
	
	public JReFrameworkerProject(IProject project) {
		this.project = project;
		this.jProject = JavaCore.create(project);
	}
	
	/**
	 * Returns the Eclipse project resource
	 * @return
	 */
	public IProject getProject(){
		return project;
	}
	
	/**
	 * Lists the JReFrameworker project targets
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public Set<String> listTargets() throws SAXException, IOException, ParserConfigurationException {
		return BuildFile.getOrCreateBuildFile(project).getTargets();
	}
	
	/**
	 * Adds a target from the JReFrameworker project
	 * @throws CoreException 
	 * @throws URISyntaxException 
	 */
	public void addTarget(File targetLibrary) throws TransformerException, ParserConfigurationException, SAXException, IOException, URISyntaxException, CoreException {
		addProjectLibrary(jProject, targetLibrary);
		BuildFile.getOrCreateBuildFile(project).addTarget(targetLibrary.getName());
	}
	
	public void addTarget(File targetLibrary, String relativeLibraryDirectory) throws TransformerException, ParserConfigurationException, SAXException, IOException, URISyntaxException, CoreException {
		addProjectLibrary(jProject, targetLibrary, relativeLibraryDirectory);
		BuildFile.getOrCreateBuildFile(project).addTarget(targetLibrary.getName());
	}
	
	/**
	 * Removes a target from the JReFrameworker project
	 */
	public void removeTarget(String target) throws TransformerException, ParserConfigurationException, SAXException, IOException {
		BuildFile.getOrCreateBuildFile(project).removeTarget(target);
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
	private static void addProjectLibrary(IJavaProject jProject, File library) throws IOException, URISyntaxException, MalformedURLException, CoreException {
		addProjectLibrary(jProject, library, null);
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
	private static void addProjectLibrary(IJavaProject jProject, File libraryToAdd, String relativeDirectoryPath) throws IOException, URISyntaxException, MalformedURLException, CoreException {
	    // copy the jar file into the project
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
		Files.copy(libraryInputStream, library.toPath());

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
	}

}
