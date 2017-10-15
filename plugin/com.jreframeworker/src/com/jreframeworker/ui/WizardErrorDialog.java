package com.jreframeworker.ui;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.Bundle;

import com.jreframeworker.Activator;
import com.jreframeworker.core.JReFrameworker;
import com.jreframeworker.log.Log;

public class WizardErrorDialog extends UIJob {

	private String message, projectName;
	
	public WizardErrorDialog(String name, String errorMessage, String projectName) {
		super(name);
		this.message = errorMessage;
		this.projectName = projectName;
	}

	@Override
	public IStatus runInUIThread(IProgressMonitor monitor) {
		Path iconPath = new Path("icons" + File.separator + "JReFrameworker.gif");
		Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
		Image icon = null;
		try {
			icon = new Image(PlatformUI.getWorkbench().getDisplay(), FileLocator.find(bundle, iconPath, null).openStream());
		} catch (IOException e) {
			Log.error("JReFrameworker.gif icon is missing.", e);
		};
		MessageDialog dialog = new MessageDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), 
												"Could Not Create JReFrameworker Runtime Project", 
												icon, 
												message, 
												MessageDialog.ERROR,
												new String[] { "Delete Project", "Cancel" }, 
												0);
		int response = dialog.open();

		IStatus status = Status.OK_STATUS;
		if (response == 0) {
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			status = JReFrameworker.deleteProject(project);
		}
		
		if (icon != null){
			icon.dispose();
		}
		
		return status;
	}
	
}
