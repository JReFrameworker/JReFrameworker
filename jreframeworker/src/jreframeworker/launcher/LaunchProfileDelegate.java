package jreframeworker.launcher;

import java.io.File;

import jreframeworker.core.JReFrameworker;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

/**
 * A basic Java application launcher that adds in a boot classpath to the modified runtime
 * 
 * References: 
 * 1. https://www.eclipse.org/articles/Article-Launch-Framework/launch.html
 * 2. http://alvinalexander.com/java/jwarehouse/eclipse/org.eclipse.jdt.launching/launching/org/eclipse/jdt/internal/launching/JavaAppletLaunchConfigurationDelegate.java.shtml
 * 3. https://eclipse.org/articles/Article-Java-launch/launching-java.html
 * 
 * @author Ben Holland
 */
public class LaunchProfileDelegate extends JavaLaunchDelegate {

	@Override
	public synchronized void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		super.launch(configuration, mode, launch, monitor);
	}

	/**
	 * Prepends the modified runtime jar to the boot classpath
	 */
	@Override
	public String getVMArguments(ILaunchConfiguration configuration) throws CoreException {
		IJavaProject jProject = getJavaProject(configuration);
		String bootClasspath = "-Xbootclasspath/p:" + 
								jProject.getProject().getFolder(JReFrameworker.RUNTIMES_DIRECTORY)
								.getLocation().toFile().getAbsolutePath() + File.separatorChar + "rt.jar";
		return bootClasspath + super.getVMArguments(configuration);
	}
	
}