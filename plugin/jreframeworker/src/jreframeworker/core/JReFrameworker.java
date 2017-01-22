package jreframeworker.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.events.BuildCommand;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
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
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import jreframeworker.Activator;
import jreframeworker.builder.JReFrameworkerBuilder;
import jreframeworker.builder.JReFrameworkerNature;
import jreframeworker.log.Log;

@SuppressWarnings("restriction")
public class JReFrameworker {

	public static final String APPLICATION_DIRECTORY = "applications";
	public static final String RUNTIMES_DIRECTORY = "runtimes";
	public static final String RUNTIMES_CONFIG = "runtimes/config";
	public static final String ANNOTATIONS_DIRECTORY = "annotations";
	public static final String EXPORT_DIRECTORY = "export";
	public static final String SOURCE_DIRECTORY = "src";
	public static final String BINARY_DIRECTORY = "bin";
	public static final String LIBRARY_DIRECTORY = "lib";
	public static final String JRE_FRAMEWORKER_ANNOTATIONS_JAR = "JReFrameworkerAnnotations.jar";
	public static final String ANNOTATIONS_JAR_PATH = ANNOTATIONS_DIRECTORY + "/" + JRE_FRAMEWORKER_ANNOTATIONS_JAR;
	public static final String XML_BUILD_FILENAME = "jref-build.xml";
	
	public static LinkedList<IJavaProject> getJReFrameworkerProjects(){
		LinkedList<IJavaProject> projects = new LinkedList<IJavaProject>();
		for(IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()){
			try {
				if(project.isOpen() && project.hasNature(JavaCore.NATURE_ID) && project.hasNature(JReFrameworkerNature.NATURE_ID)){
					IJavaProject jProject = JavaCore.create(project);
					if(jProject.exists()){
						projects.add(jProject);
					}
				}
			} catch (CoreException e) {}
		}
		return projects;
	}
	
	public static IStatus deleteProject(IProject project) {
		if (project != null && project.exists())
			try {
				project.delete(true, true, new NullProgressMonitor());
			} catch (CoreException e) {
				Log.error("Could not delete project", e);
				return new Status(Status.ERROR, Activator.PLUGIN_ID, "Could not delete project", e);
			}
		return Status.OK_STATUS;
	}
	
	// references: 
	// https://sdqweb.ipd.kit.edu/wiki/JDT_Tutorial:_Creating_Eclipse_Java_Projects_Programmatically
	// https://eclipse.org/articles/Article-Builders/builders.html
	// http://www.programcreek.com/java-api-examples/index.php?api=org.eclipse.core.internal.events.BuildCommand
	public static IStatus createProject(String projectName, IPath projectPath, IProgressMonitor monitor, String targetJar, boolean isRuntime) throws CoreException, IOException, URISyntaxException {
		IProject project = null;
		
		try {
			monitor.beginTask("Create JReFrameworker Runtime Project", 2);
			
			// create the empty eclipse project
			monitor.setTaskName("Creating Eclipse project...");
			project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			File projectDirectory = new File(projectPath.toFile().getCanonicalPath() + File.separatorChar + project.getName()).getCanonicalFile();
			if(isRuntime){
				File runtimesDirectory = new File(projectDirectory.getCanonicalPath() + File.separatorChar + RUNTIMES_DIRECTORY);
				runtimesDirectory.mkdirs();
			}
			
			IJavaProject jProject = createProject(projectName, projectPath, monitor, project);
			monitor.worked(1);
			if (monitor.isCanceled()){
				return Status.CANCEL_STATUS;
			}
			
			createBuildFile(projectDirectory, targetJar, isRuntime);
			
			// copy runtimes and configure project classpath
			monitor.setTaskName("Configuring project classpath...");
			configureProjectClasspath(jProject);
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

	private static File createBuildFile(File projectDirectory, String targetJar, boolean isRuntime) throws IOException {
		File buildXMLFile = new File(projectDirectory + File.separator + XML_BUILD_FILENAME);
		Log.info("Created Build XML File: " + buildXMLFile.getAbsolutePath());
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// root elements
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("build");
			doc.appendChild(rootElement);

			Element target = doc.createElement("target");
			rootElement.appendChild(target);

			target.setAttribute("name", targetJar);
			target.setAttribute("type", isRuntime ? "runtime" : "application");

			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(buildXMLFile);
			transformer.transform(source, result);
		} catch (ParserConfigurationException pce) {
			Log.error("ParserConfigurationException", pce);
		} catch (TransformerException tfe) {
			Log.error("TransformerException", tfe);
		}
		return buildXMLFile;
	}

	private static void configureProjectClasspath(IJavaProject jProject) throws CoreException, JavaModelException, IOException, URISyntaxException {
		// create bin folder
		try {
			IFolder binFolder = jProject.getProject().getFolder(BINARY_DIRECTORY);
			binFolder.create(false, true, null);
			jProject.setOutputLocation(binFolder.getFullPath(), null);
		} catch (Exception e){
			Log.warning("Could not created bin folder.", e);
		}
		
		// create a set of classpath entries
		List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>(); 
		
		// adds classpath entry of: <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.8"/>
		String path = org.eclipse.jdt.launching.JavaRuntime.JRE_CONTAINER + "/" + org.eclipse.jdt.internal.launching.StandardVMType.ID_STANDARD_VM_TYPE + "/" + "JavaSE-1.8";
		entries.add(JavaCore.newContainerEntry(new Path(path)));
		
		//  add the jreframeworker annotations jar 
		addProjectAnnotationsLibrary(jProject);

		// have to create this manually instead of using JavaCore.newLibraryEntry because JavaCore insists the path be absolute
		IClasspathEntry relativeAnnotationsLibraryEntry = new ClasspathEntry(IPackageFragmentRoot.K_BINARY,
				IClasspathEntry.CPE_LIBRARY, new Path(ANNOTATIONS_JAR_PATH), ClasspathEntry.INCLUDE_ALL, // inclusion patterns
				ClasspathEntry.EXCLUDE_NONE, // exclusion patterns
				null, null, null, // specific output folder
				false, // exported
				ClasspathEntry.NO_ACCESS_RULES, false, // no access rules to combine
				ClasspathEntry.NO_EXTRA_ATTRIBUTES);
		entries.add(relativeAnnotationsLibraryEntry);
		
		// create source folder and add it to the classpath
		IFolder sourceFolder = jProject.getProject().getFolder(SOURCE_DIRECTORY);
		sourceFolder.create(false, true, null);
		IPackageFragmentRoot sourceFolderRoot = jProject.getPackageFragmentRoot(sourceFolder);
		entries.add(JavaCore.newSourceEntry(sourceFolderRoot.getPath()));
		
		// set the class path
		jProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), null);
		
		Log.info("Successfully created JReFrameworker project [" + jProject.getProject().getName() + "]");
	}

	private static IJavaProject createProject(String projectName, IPath projectPath, IProgressMonitor monitor, IProject project) throws CoreException {
		IProjectDescription projectDescription = project.getWorkspace().newProjectDescription(project.getName());
		URI location = getProjectLocation(projectName, projectPath);
		projectDescription.setLocationURI(location);
		
		// make this a JReFrameworker project
		projectDescription.setNatureIds(new String[] { JavaCore.NATURE_ID, JReFrameworkerNature.NATURE_ID });

		// build first with Java compiler then JReFramewoker bytecode operations
		BuildCommand javaBuildCommand = new BuildCommand();
		javaBuildCommand.setBuilderName(JavaCore.BUILDER_ID);
		BuildCommand jrefBuildCommand = new BuildCommand();
		jrefBuildCommand.setBuilderName(JReFrameworkerBuilder.BUILDER_ID);
		projectDescription.setBuildSpec(new ICommand[]{ javaBuildCommand, jrefBuildCommand});

		// create and open the Eclipse project
		project.create(projectDescription, null);
		IJavaProject jProject = JavaCore.create(project);
		project.open(new NullProgressMonitor());
		return jProject;
	}

//	private static void addClasspathResourceEntry(IJavaProject jProject, IResource resource) throws JavaModelException {
//		IPackageFragmentRoot root = jProject.getPackageFragmentRoot(resource);
//		IClasspathEntry[] oldEntries = jProject.getRawClasspath();
//		IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
//		System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
//		newEntries[oldEntries.length] = JavaCore.newSourceEntry(root.getPath());
//		jProject.setRawClasspath(newEntries, null);
//	}
	
//	/**
//	 * Searches for the default virtual machine
//	 * Note: this methods creates a project that is hardcoded to the path of the installed VM
//	 * @param jProject
//	 * @throws IOException
//	 * @throws JavaModelException
//	 * @throws URISyntaxException
//	 */
//	@SuppressWarnings("unused")
//	private static void setAbsoluteProjectClasspath(IJavaProject jProject) throws IOException, JavaModelException, URISyntaxException {
//		List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
//		
//		// add the default JVM classpath (assuming translator uses the same jvm libraries)
//		IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
//		LibraryLocation[] locations = JavaRuntime.getLibraryLocations(vmInstall);
//		for (LibraryLocation library : locations) {
//			entries.add(JavaCore.newLibraryEntry(library.getSystemLibraryPath(), null, null));
//		}
//		
//		// add the jreframeworker operations jar to project and the classpath
//		setAnnotationsClasspath(jProject, entries);
//		
//		// set the class path
//		jProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), null);
//	}

	private static File addProjectAnnotationsLibrary(IJavaProject jProject) throws IOException, URISyntaxException, MalformedURLException {
		// see http://stackoverflow.com/q/23825933/475329 for logic of getting bundle resource
		URL fileURL = Activator.getDefault().getBundle().getEntry(ANNOTATIONS_JAR_PATH);
		URL resolvedFileURL = FileLocator.toFileURL(fileURL);
		// need to use the 3-arg constructor of URI in order to properly escape file system chars
		URI resolvedURI = new URI(resolvedFileURL.getProtocol(), resolvedFileURL.getPath(), null);
		InputStream annotationsJarInputStream = resolvedURI.toURL().openConnection().getInputStream();
		if(annotationsJarInputStream == null){
			throw new RuntimeException("Could not locate: " + ANNOTATIONS_JAR_PATH);
		}
		File annotationsLibDirectory = new File(jProject.getProject().getLocation().toFile().getCanonicalPath() + File.separatorChar + ANNOTATIONS_DIRECTORY);
		annotationsLibDirectory.mkdirs();
		File annotationsJar = new File(annotationsLibDirectory.getCanonicalPath() + File.separatorChar + JRE_FRAMEWORKER_ANNOTATIONS_JAR);
		Files.copy(annotationsJarInputStream, annotationsJar.toPath());
		return annotationsJar;
	}

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

	public static String getTargetJar(IProject project) throws SAXException, IOException, ParserConfigurationException {
		File buildXMLFile = project.getFile(XML_BUILD_FILENAME).getLocation().toFile();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(buildXMLFile);
		doc.getDocumentElement().normalize();
		NodeList targets = doc.getElementsByTagName("target");
		String result = ""; // TODO: support multiple targets
		for (int i = 0; i < targets.getLength(); i++) {
			Element target = (Element) targets.item(i);
			result = target.getAttribute("name");
		}
		return result;
	}
	
	public static boolean isTargetJarRuntime(IProject project) throws SAXException, IOException, ParserConfigurationException {
		File buildXMLFile = project.getFile(XML_BUILD_FILENAME).getLocation().toFile();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(buildXMLFile);
		doc.getDocumentElement().normalize();
		NodeList targets = doc.getElementsByTagName("target");
		String result = ""; // TODO: support multiple targets
		for (int i = 0; i < targets.getLength(); i++) {
			Element target = (Element) targets.item(i);
			result = target.getAttribute("type");
		}
		return result.equals("runtime") ? true : false;
	}

}
