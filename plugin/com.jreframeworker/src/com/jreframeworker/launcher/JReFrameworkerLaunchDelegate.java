package com.jreframeworker.launcher;

import java.io.File;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

import com.jreframeworker.core.JReFrameworker;
import com.jreframeworker.log.Log;
import com.jreframeworker.preferences.JReFrameworkerPreferences;

/**
 * A basic Java application launcher that adds in a boot classpath to the modified runtime
 * 
 * References: 
 * 1. https://www.eclipse.org/articles/Article-Launch-Framework/launch.html
 * 2. http://alvinalexander.com/java/jwarehouse/eclipse/org.eclipse.jdt.launching/launching/org/eclipse/jdt/internal/launching/JavaAppletLaunchConfigurationDelegate.java.shtml
 * 3. https://eclipse.org/articles/Article-Java-launch/launching-java.html
 * 
 * Note: You can see the command line used to initiate a launch by right-clicking the 
 * resulting process in the Debug View and selecting Properties. This is useful for 
 * debugging a delegate.
 * 
 * @author Ben Holland
 */
public class JReFrameworkerLaunchDelegate extends JavaLaunchDelegate {

	public static final String JREFRAMEWORKER_LAUNCH_CONFIGURATION_TYPE = "com.jreframeworker.launchConfigurationType";
	
	@Override
	public synchronized void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		super.launch(configuration, mode, launch, monitor);
		String mainTypeName = verifyMainTypeName(configuration);
		IJavaProject jProject = getJavaProject(configuration);
		if(JReFrameworkerPreferences.isVerboseLoggingEnabled()) {
			Log.info("Launching... [Project: " + jProject.getProject().getName() + ", Main Class: " + mainTypeName + "]"
				+ "\nClasspath: " + Arrays.toString(this.getClasspath(configuration))
				+ "\nBootpath: " + Arrays.toString(this.getBootpath(configuration))
				+ "\nProgram Args: " + this.getProgramArguments(configuration) 
				+ "\nVM Args: " + this.getVMArguments(configuration));
		}
	}

	/**
	 * Prepends the modified runtime jar to the boot classpath
	 */
	@Override
	public String getVMArguments(ILaunchConfiguration configuration) throws CoreException {
		IJavaProject jProject = getJavaProject(configuration);
		String bootClasspath = "-Xbootclasspath/p:" + 
								jProject.getProject().getFolder(JReFrameworker.BUILD_DIRECTORY)
								.getLocation().toFile().getAbsolutePath() + File.separatorChar + "rt.jar";
		return bootClasspath + super.getVMArguments(configuration);
	}
	
}