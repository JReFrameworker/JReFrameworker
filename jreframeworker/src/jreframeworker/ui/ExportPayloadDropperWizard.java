package jreframeworker.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;

import jreframeworker.Activator;
import jreframeworker.core.JReFrameworker;
import jreframeworker.engine.utils.JarModifier;
import jreframeworker.log.Log;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.Bundle;

// TODO: this is extending the export project wizard...which is a bit overkill since we don't need to set a project name to export a jar file
public class ExportPayloadDropperWizard extends Wizard implements IExportWizard {

	public static final String PAYLOAD_DROPPER = "dropper.jar";
	public static final String EXPORT_PAYLOAD_DROPPER = "export" + File.separatorChar + PAYLOAD_DROPPER;
	
	private ExportPayloadDropperPage page;
	private File workspace = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().getAbsolutePath() + File.separatorChar + ".jreframeworker");
	private File dropperJar = new File(workspace.getAbsolutePath() + File.separatorChar + EXPORT_PAYLOAD_DROPPER);
	
	public ExportPayloadDropperWizard() throws Exception {
		page = new ExportPayloadDropperPage("Create Payload Dropper");
		this.setWindowTitle("Create Payload Dropper");

		if(!dropperJar.exists()){
			dropperJar.getParentFile().mkdirs();
			URL fileURL = Activator.getContext().getBundle().getEntry(JReFrameworker.EXPORT_DIRECTORY + "/" + PAYLOAD_DROPPER);
			URL resolvedFileURL = FileLocator.toFileURL(fileURL);
			// need to use the 3-arg constructor of URI in order to properly escape file system chars
			URI resolvedURI = new URI(resolvedFileURL.getProtocol(), resolvedFileURL.getPath(), null);
			InputStream dropperJarInputStream = resolvedURI.toURL().openConnection().getInputStream();
			if(dropperJarInputStream == null){
				throw new RuntimeException("Could not locate: " + PAYLOAD_DROPPER);
			}
			Files.copy(dropperJarInputStream, dropperJar.toPath());
		}
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {}
	
	@Override
	public void addPages() {
		this.addPage(page);
	}

	@Override
	public boolean performFinish() {
		final File dropperFile = new File(page.getJARPath());

		IRunnableWithProgress j = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) {
				try {
					JarModifier dropper = new JarModifier(dropperJar);
//					dropper.add("config", Files.readAllBytes(jProject.getProject().getFile(JReFrameworker.RUNTIMES_CONFIG).getLocation().toFile().toPath()), true);
					// TODO: add class file entries
					dropper.save(dropperFile);
				} catch (Throwable t) {
					String message = "Could not create JAR binary project. " + t.getMessage();
					UIJob uiJob = new ShowErrorDialogJob("Showing error dialog", message);
					uiJob.schedule();
					Log.error(message, t);
				} finally {
					monitor.done();
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
	
	private static class ExportPayloadDropperPage extends WizardNewProjectCreationPage {
		private String jarPath;
		
		public ExportPayloadDropperPage(String pageName, String startJARPath) {
			super(pageName);
			jarPath = startJARPath;
		}
		
		public ExportPayloadDropperPage(String pageName) {
			this(pageName, "");
		}
		
		public String getJARPath() {
			return jarPath;
		}
		
		@Override
		public void createControl(Composite parent) {
			super.createControl(parent);
			Composite composite = (Composite) this.getControl();
			
			final FileDialog fileChooser = new FileDialog(composite.getShell(), SWT.SAVE);
			fileChooser.setFilterExtensions(new String[] { "*.jar" });
			fileChooser.setFileName("dropper.jar");
			
			Composite row = new Composite(composite, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.numColumns = 3;
			row.setLayout(layout);
			
			GridData data = new GridData();
			data.horizontalAlignment = SWT.FILL;
			row.setLayoutData(data);
			
			Label labelJAR = new Label(row, SWT.NONE);
			labelJAR.setText("JAR:");
			
			final Text textJAR = new Text(row, SWT.SINGLE | SWT.BORDER);
			textJAR.setText(jarPath);
			data = new GridData();
			data.grabExcessHorizontalSpace = true;
			data.horizontalAlignment = SWT.FILL;
			textJAR.setLayoutData(data);
			
			textJAR.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					jarPath = textJAR.getText();
				}
			});
			
			Button buttonBrowseJAR = new Button(row, SWT.PUSH);
			buttonBrowseJAR.setText("     Browse...     ");
			buttonBrowseJAR.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (!jarPath.isEmpty()){
						fileChooser.setFileName(jarPath);
					}
					String path = fileChooser.open();
					if (path != null){
						jarPath = path;
					}
					textJAR.setText(jarPath);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {}
				
			});
		}
	}

	private static class ShowErrorDialogJob extends UIJob {

		private String message;
		
		public ShowErrorDialogJob(String name, String errorMessage) {
			super(name);
			this.message = errorMessage;
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
													"Could Not Create Payload Dropper", 
													icon, 
													message, 
													MessageDialog.ERROR,
													new String[] { "OK" }, 
													0);
			dialog.open();
			IStatus status = Status.OK_STATUS;
			
			if (icon != null){
				icon.dispose();
			}
			
			return status;
		}
		
	}

}
