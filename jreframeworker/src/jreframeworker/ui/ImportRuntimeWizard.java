package jreframeworker.ui;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import jreframeworker.Activator;
import jreframeworker.core.JReFrameworker;
import jreframeworker.log.Log;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

public class ImportRuntimeWizard extends Wizard implements IImportWizard {

	private NewRuntimeProjectPage page;
	
	public ImportRuntimeWizard(String startRuntimePath) {
		page = new NewRuntimeProjectPage("Create JReFrameworker Runtime Project", startRuntimePath);
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
	
	public ImportRuntimeWizard() {
		page = new NewRuntimeProjectPage("Create JReFrameworker Runtime Project");
		this.setWindowTitle("Create JReFrameworker Runtime Project");
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {}
	
	@Override
	public void addPages() {
		this.addPage(page);
	}

	@Override
	public boolean performFinish() {
		final String projectName = page.getProjectName();
		final IPath projectLocation = page.getLocationPath();
//		final File runtimeDirectory = new File(page.getRuntimePath());  // TODO: use this in project creation

		IRunnableWithProgress j = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) {
				IStatus result = null;
				try {
					if(page.isDefaultRuntime()){
						result = JReFrameworker.createProjectWithDefaultRuntime(projectName, projectLocation, monitor);
					} else {
						// TODO: implement
					}
				} catch (Throwable t) {
					String message = "Could not create JReFrameworker runtime project. " + t.getMessage();
					UIJob uiJob = new ImportWizardErrorDialog("Error importing runtime...", message, projectName);
					uiJob.schedule();
					Log.error(message, t);
				} finally {
					monitor.done();
				}
				if(result.equals(Status.CANCEL_STATUS)) {
					IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
					deleteProject(project);
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
	
}
