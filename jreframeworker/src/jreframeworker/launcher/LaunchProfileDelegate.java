package jreframeworker.launcher;

import jreframeworker.log.Log;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

/**
 * A launcher profile to set the modified runtime
 * References: 
 * 1. https://www.eclipse.org/articles/Article-Launch-Framework/launch.html
 * 2. https://eclipse.org/articles/Article-Java-launch/launching-java.html
 * 3. http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fdebug_launch_adding.htm
 * 
 * @author Ben Holland
 */
public class LaunchProfileDelegate extends AbstractJavaLaunchConfigurationDelegate {

	/**
	 * Return the <code>IType</code> referenced by the specified name and
	 * contained in the specified project or throw a <code>CoreException</code>
	 * whose message explains why this couldn't be done.
	 * 
	 * Original Source: https://www.cct.lsu.edu/~rguidry/ecl31docs/api/src-html/org/eclipse/jdt/internal/launching/JavaLaunchConfigurationUtils.html
	 */
	private IType getMainType(String mainTypeName, IJavaProject javaProject) throws CoreException {
		if ((mainTypeName == null) || (mainTypeName.trim().length() < 1)) {
			abort("Main type not specified", null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
		}
		IType mainType = null;
		try {
			mainType = findType(javaProject, mainTypeName);
		} catch (JavaModelException jme) {
		}
		if (mainType == null) {
			abort("Main type does not exist", null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
		}
		return mainType;
	}
	
	/**
	 * Find the specified (fully-qualified) type name in the specified java project.
	 * 
	 * Original Source: https://www.cct.lsu.edu/~rguidry/ecl31docs/api/src-html/org/eclipse/jdt/internal/launching/JavaLaunchConfigurationUtils.html
	 */
	private IType findType(IJavaProject javaProject, String mainTypeName) throws JavaModelException {
		String pathStr = mainTypeName.replace('.', '/') + ".java";
		IJavaElement javaElement = javaProject.findElement(new Path(pathStr));
		if (javaElement == null) {
			return null;
		} else if (javaElement instanceof IType) {
			return (IType) javaElement;
		} else if (javaElement.getElementType() == IJavaElement.COMPILATION_UNIT) {
			String simpleName = Signature.getSimpleName(mainTypeName);
			return ((ICompilationUnit) javaElement).getType(simpleName);
		} else if (javaElement.getElementType() == IJavaElement.CLASS_FILE) {
			return ((IClassFile) javaElement).getType();
		}
		return null;
	}
	
	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		String mainTypeName = verifyMainTypeName(configuration);
		IJavaProject javaProject = getJavaProject(configuration);

		Log.info("LAUNCH DELEGATE: " + mainTypeName);
		Log.info("LAUNCH DELEGATE: " + javaProject.getProject().getName());
		Log.info("LAUNCH DELEGATE: " + configuration.getName());
		
		
//		IType mainType = getMainType(mainTypeName, javaProject);
//		IVMInstall vm = verifyVMInstall(configuration);
//		IVMRunner runner = vm.getVMRunner(mode);
//
//		// create VM config
//		VMRunnerConfiguration runConfig = new VMRunnerConfiguration("sun.applet.AppletViewer", classpath);
//		runConfig.setProgramArguments(new String[]{});
//		String[] vmArgs = execArgs.getVMArgumentsArray();
//		String[] realArgs = new String[vmArgs.length + 1];
//		System.arraycopy(vmArgs, 0, realArgs, 1, vmArgs.length);
//		realArgs[0] = javaPolicy;
//		runConfig.setVMArguments(realArgs);
//
//		runConfig.setWorkingDirectory(workingDirName);
//		// bootpath
//		String[] bootpath = getBootpath(configuration);
//		runConfig.setBootClassPath(bootpath);
//
//		// Launch the configuration
//		runner.run(runConfig, launch, monitor);
	}

}
