package com.jreframeworker.dropper.log;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.jreframeworker.dropper.Activator;

/**
 * Centralized logging for Eclipse plugins.
 */
public class Log {
	private static ILog log;
	
	static {
		BundleContext context = Activator.getDefault().getBundle().getBundleContext();
		if (context != null) {
			Bundle bundle = context.getBundle();
			log = Platform.getLog(bundle);
		}
	}
	
	public static void error(String message, Throwable e) {
		log(Status.ERROR, message, e);
	}
	
	public static void warning(String message) {
		warning(message, null);
	}
	
	public static void warning(String message, Throwable e) {
		log(Status.WARNING, message, e);
	}
	
	public static void info(String message) {
		info(message, null);
	}
	
	public static void info(String message, Throwable e) {
		log(Status.INFO, message, e);
	}
	
	public static void log(int severity, String string, Throwable e) {
		if(log == null){
			System.err.println(string + "\n" + e);
		} else {
			IStatus status = new Status(severity, Activator.PLUGIN_ID, string, e);
			log.log(status);
		}
	}
}

