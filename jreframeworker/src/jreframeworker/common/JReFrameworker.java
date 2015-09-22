package jreframeworker.common;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jreframeworker.builder.JReFrameworkerNature;
import jreframeworker.log.Log;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.events.BuildCommand;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;

@SuppressWarnings("restriction")
public class JReFrameworker {

	// references: 
	// https://sdqweb.ipd.kit.edu/wiki/JDT_Tutorial:_Creating_Eclipse_Java_Projects_Programmatically
	// https://eclipse.org/articles/Article-Builders/builders.html
	// http://www.programcreek.com/java-api-examples/index.php?api=org.eclipse.core.internal.events.BuildCommand
	public static IStatus createProjectWithDefaultRuntime(String projectName, IPath projectPath, IProgressMonitor monitor) throws CoreException, IOException {
		IProject project = null;
		
		try {
			monitor.beginTask("Create JReFrameworker Runtime Project", 3);
			
			// create the empty eclipse project
			monitor.setTaskName("Creating Eclipse project...");
			project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			File projectDirectory = new File(projectPath.toFile().getCanonicalPath() + File.separatorChar + projectName).getCanonicalFile();
			File runtimesDirectory = new File(projectDirectory.getCanonicalPath() + File.separatorChar + "runtimes");
			runtimesDirectory.mkdirs();
			IJavaProject jProject = createProject(projectName, projectPath, monitor, project);
			monitor.worked(1);
			if (monitor.isCanceled()){
				return Status.CANCEL_STATUS;
			}
			
			// copy runtimes and configure project classpath
			monitor.setTaskName("Configuring project classpath...");
			configureProjectClasspath(project, projectDirectory, runtimesDirectory, jProject);
			monitor.worked(1);
			if (monitor.isCanceled()){
				return Status.CANCEL_STATUS;
			}

			// generate jimple for runtimes
			monitor.setTaskName("Disassembling runtimes...");
			// TODO: implement
			monitor.worked(1);
			if (monitor.isCanceled()){
				return Status.CANCEL_STATUS;
			}
			
			return Status.OK_STATUS;
		} finally {
			if (project != null && project.exists()){
				project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			}
			monitor.done();
		}
	}

	private static void configureProjectClasspath(IProject project, File projectDirectory, File runtimesDirectory, IJavaProject jProject) throws CoreException, JavaModelException, IOException {
		// create bin folder
		IFolder binFolder = project.getFolder("bin");
		binFolder.create(false, true, null);
		jProject.setOutputLocation(binFolder.getFullPath(), null);
		
		cloneDefaultRuntimeLibraries(jProject, projectDirectory, runtimesDirectory);
		
		// create source folder
		IFolder sourceFolder = project.getFolder("src");
		sourceFolder.create(false, true, null);
		
		// add source folder to project class entries
		addClasspathEntry(jProject, sourceFolder);
		
		Log.info("Successfully created JReFrameworker project [" + project.getName() + "]");
	}

	private static IJavaProject createProject(String projectName, IPath projectPath, IProgressMonitor monitor, IProject project) throws CoreException {
		IProjectDescription projectDescription = project.getWorkspace().newProjectDescription(project.getName());
		URI location = getProjectLocation(projectName, projectPath);
		projectDescription.setLocationURI(location);
		projectDescription.setNatureIds(new String[] { JavaCore.NATURE_ID, JReFrameworkerNature.NATURE_ID });

		BuildCommand javaBuildCommand = new BuildCommand();
		javaBuildCommand.setBuilderName(JavaCore.BUILDER_ID);
		projectDescription.setBuildSpec(new ICommand[]{ javaBuildCommand });

		// create and open the Eclipse project
		project.create(projectDescription, null);
		IJavaProject jProject = JavaCore.create(project);
		project.open(new NullProgressMonitor());
		return jProject;
	}

	private static void addClasspathEntry(IJavaProject jProject, IFolder sourceFolder) throws JavaModelException {
		IPackageFragmentRoot root = jProject.getPackageFragmentRoot(sourceFolder);
		IClasspathEntry[] oldEntries = jProject.getRawClasspath();
		IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
		newEntries[oldEntries.length] = JavaCore.newSourceEntry(root.getPath());
		jProject.setRawClasspath(newEntries, null);
	}
	
	private static void cloneDefaultRuntimeLibraries(IJavaProject jProject, File projectDirectory, File libDirectory) throws IOException, JavaModelException {
		// add the default JVM classpath (assuming translator uses the same jvm libraries)
		IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
		LinkedList<File> libraries = new LinkedList<File>();
		for (LibraryLocation element : JavaRuntime.getLibraryLocations(vmInstall)) {
			File library = JavaCore.newLibraryEntry(element.getSystemLibraryPath(), null, null).getPath().toFile().getCanonicalFile();
			File libraryCopy = new File(libDirectory.getCanonicalPath() + File.separatorChar + library.getName());
			RuntimeUtils.copyFile(library, libraryCopy);
			libraries.add(libraryCopy);
		}
		
		// add the project libraries in the WEB-INF folder to the project classpath
		List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
		for(File projectJar : libraries){
			String projectJarCanonicalPath = projectJar.getCanonicalPath();
			String projectCanonicalPath = projectDirectory.getCanonicalPath();
			String projectJarBasePath = projectJarCanonicalPath.substring(projectJarCanonicalPath.indexOf(projectCanonicalPath));
			String projectJarParentCanonicalPath = projectJar.getCanonicalPath();
			String projectJarParentBasePath = projectJarParentCanonicalPath.substring(projectJarParentCanonicalPath.indexOf(projectCanonicalPath));
			entries.add(JavaCore.newLibraryEntry(new Path(projectJarBasePath), null, new Path(projectJarParentBasePath)));
		}
		
		// set the class path
		jProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), null);
	}

//	private static void linkDefaultRuntimeLibraries(IJavaProject jProject) throws JavaModelException {
//		List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
//		IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
//		LibraryLocation[] locations = JavaRuntime.getLibraryLocations(vmInstall);
//		for (LibraryLocation element : locations) {
//		 entries.add(JavaCore.newLibraryEntry(element.getSystemLibraryPath(), null, null));
//		}
//		//add libs to project class path
//		jProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), null);
//	}

	private static URI getProjectLocation(String projectName, IPath projectPath) {
		URI location = null;
		if (projectPath != null){
			location = URIUtil.toURI(projectPath);
		}
		if (location != null && ResourcesPlugin.getWorkspace().getRoot().getLocationURI().equals(location)) {
			location = null;
		} else {
			location = URIUtil.toURI(URIUtil.toPath(location) + File.separator + projectName);
		}
		return location;
	}

}
