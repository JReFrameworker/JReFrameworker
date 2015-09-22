package jreframeworker.ui;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import jreframeworker.Activator;
import jreframeworker.common.JReFrameworker;
import jreframeworker.log.Log;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.Bundle;

public class ImportRuntimeWizard extends Wizard implements IImportWizard {

	private NewJReFrameworkerRuntimeProjectPage page;
	
	public ImportRuntimeWizard(String startRuntimePath) {
		page = new NewJReFrameworkerRuntimeProjectPage("Create JReFrameworker Runtime Project", startRuntimePath);
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
		page = new NewJReFrameworkerRuntimeProjectPage("Create JReFrameworker Runtime Project");
		this.setWindowTitle("Create JReFrameworker runtime Project");
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
		final File runtimeDirectory = new File(page.getRuntimePath());

		IRunnableWithProgress j = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) {
				IStatus result = null;
				try {
					result = JReFrameworker.createJReFrameworkerProject(projectName, projectLocation, runtimeDirectory, monitor);
				} catch (Throwable t) {
					String message = "Could not create JReFrameworker runtime project. " + t.getMessage();
					UIJob uiJob = new ShowErrorDialogJob("Showing error dialog", message, projectName);
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
	
	private static class NewJReFrameworkerRuntimeProjectPage extends WizardNewProjectCreationPage {
		private String runtimePath;
		
		public NewJReFrameworkerRuntimeProjectPage(String pageName, String startRuntimePath) {
			super(pageName);
			runtimePath = startRuntimePath;
		}
		
		public NewJReFrameworkerRuntimeProjectPage(String pageName) {
			this(pageName, "");
		}
		
		public String getRuntimePath() {
			return runtimePath;
		}
		
		@Override
		public void createControl(Composite parent) {
			super.createControl(parent);
			Composite composite = (Composite) this.getControl();
			
			final DirectoryDialog directoryChooser = new DirectoryDialog(composite.getShell(), SWT.OPEN);
			
			Composite row = new Composite(composite, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.numColumns = 3;
			row.setLayout(layout);
			
			GridData data = new GridData();
			data.horizontalAlignment = SWT.FILL;
			row.setLayoutData(data);
			
			Label runtimeLabel = new Label(row, SWT.NONE);
			runtimeLabel.setText("Runtime:");
			
			final Text runtimeText = new Text(row, SWT.SINGLE | SWT.BORDER);
			runtimeText.setText(runtimePath);
			data = new GridData();
			data.grabExcessHorizontalSpace = true;
			data.horizontalAlignment = SWT.FILL;
			runtimeText.setLayoutData(data);
			
			runtimeText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					runtimePath = runtimeText.getText();
				}
			});
			
			Button runtimeBrowseButton = new Button(row, SWT.PUSH);
			runtimeBrowseButton.setText("     Browse...     ");
			runtimeBrowseButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					String path = directoryChooser.open();
					if (path != null){
						runtimePath = path;
					}
					runtimeText.setText(runtimePath);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {}
				
			});
		}
	}
	
	private static class ShowErrorDialogJob extends UIJob {

		private String message, projectName;
		
		public ShowErrorDialogJob(String name, String errorMessage, String projectName) {
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
				status = deleteProject(project);
			}
			
			if (icon != null){
				icon.dispose();
			}
			
			return status;
		}
		
	}
}
