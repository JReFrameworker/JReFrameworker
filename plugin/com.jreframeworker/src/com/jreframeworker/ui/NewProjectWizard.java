package com.jreframeworker.ui;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

import com.jreframeworker.core.BuildFile;
import com.jreframeworker.core.JReFrameworker;
import com.jreframeworker.log.Log;

public class NewProjectWizard extends Wizard implements INewWizard {

	private NewProjectPage page;

	public NewProjectWizard(String startRuntimePath) {
		page = new NewProjectPage("Create JReFrameworker Runtime Project");
		String projectName = new File(startRuntimePath).getName();
		projectName = projectName.substring(0, projectName.lastIndexOf('.'));
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (project.exists()) {
			// find a project name that doesn't collide
			int i = 2;
			while (project.exists()) {
				i++;
				project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName + "_" + i);
			}
			projectName = projectName + "_" + i;
		}
		page.setInitialProjectName(projectName);
		this.setWindowTitle("Create JReFrameworker Runtime Project");
	}

	public NewProjectWizard() {
		page = new NewProjectPage("Create JReFrameworker Runtime Project");
		this.setWindowTitle("Create JReFrameworker Runtime Project");
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public void addPages() {
		this.addPage(page);
	}

	@Override
	public boolean performFinish() {
		final String projectName = page.getProjectName();
		final IPath projectLocation = page.getLocationPath();

		IRunnableWithProgress j = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) {
				IStatus result = null;
				try {
					BuildFile.Target[] targets = new BuildFile.Target[]{};
					result = JReFrameworker.createProject(projectName, projectLocation, monitor, targets);
				} catch (Throwable t) {
					String message = "Could not create JReFrameworker runtime project. " + t.getMessage();
					UIJob uiJob = new WizardErrorDialog("Error creating project...", message, projectName);
					uiJob.schedule();
					Log.error(message, t);
				} finally {
					monitor.done();
				}
				if (result != null && result.equals(Status.CANCEL_STATUS)) {
					IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
					JReFrameworker.deleteProject(project);
				}
			}
		};

		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);

		try {
			dialog.run(true, true, j);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return true;
	}

}