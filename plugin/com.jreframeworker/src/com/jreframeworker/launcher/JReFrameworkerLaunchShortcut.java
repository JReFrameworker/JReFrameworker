package com.jreframeworker.launcher;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaApplicationLaunchShortcut;

/**
 * Launch shortcut for JReFrameworker launch profiles, which is just the same as a JavaApplicationLaunchShortcut
 * 
 * References: 
 * 1. http://grepcode.com/file_/repository.grepcode.com/java/eclipse.org/3.5.2/org.eclipse.jdt.debug/ui/3.4.1/org/eclipse/jdt/debug/ui/launchConfigurations/JavaApplicationLaunchShortcut.java/?v=source
 * 2. http://opensourcejavaphp.net/java/eclipse/org/eclipse/jdt/internal/debug/ui/launcher/JavaLaunchShortcut.java.html
 * 
 * @author Ben Holland
 */
public class JReFrameworkerLaunchShortcut extends JavaApplicationLaunchShortcut {
	
	/**
	 * Overrides the launch configuration type to a JReFrameworker configuration
	 */
	@Override
	protected ILaunchConfigurationType getConfigurationType() {
		return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(JReFrameworkerLaunchDelegate.JREFRAMEWORKER_LAUNCH_CONFIGURATION_TYPE);		
	}
	
}