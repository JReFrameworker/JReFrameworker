package jreframeworker.launchers;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * A launcher profile to set the modified runtime
 * Reference: https://www.eclipse.org/articles/Article-Launch-Framework/launch.html
 * 
 * @author Ben Holland
 */
public class JavaLaunchProfileDelegate implements org.eclipse.debug.core.model.ILaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		
	}

}
