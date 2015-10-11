package jreframeworker.actions;

import java.io.File;
import java.io.IOException;

import jreframeworker.builder.JReFrameworkerNature;
import jreframeworker.common.JimpleUtils;
import jreframeworker.log.Log;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Our sample action implements workbench action delegate.
 * The action proxy will be created by the workbench and
 * shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be 
 * delegated to it.
 * @see IWorkbenchWindowActionDelegate
 */
public class JReFrameworkerAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
	
	/**
	 * The constructor.
	 */
	public JReFrameworkerAction() {}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		if(ResourcesPlugin.getWorkspace().getRoot().getProjects().length == 0){
			MessageDialog.openInformation(window.getShell(),
					"JReFrameworker",
					"No projects to build.");
		} else {
			for(IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()){
				try {
					if(project.isOpen() && project.hasNature(JReFrameworkerNature.NATURE_ID)){
						File projectDirectory = new File(project.getLocation().toFile().getCanonicalPath() + File.separatorChar + project.getName()).getCanonicalFile();
						File runtimesDirectory = new File(projectDirectory.getCanonicalPath() + File.separatorChar + "runtimes");
						IJavaProject jProject = JavaCore.create(project);
						JimpleUtils.assemble(jProject,  project.getFile(runtimesDirectory.getName() + File.separatorChar + "rt.jar"));
						project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
						MessageDialog.openInformation(window.getShell(),
								"JReFrameworker",
								"Successfully built runtime.");
					}
				} catch (CoreException | IOException e) {
					Log.error("Error iterating workspace projects.", e);
					MessageDialog.openInformation(window.getShell(),
							"JReFrameworker",
							"Could not build runtime.");
				}
			}
		}
	}

	/**
	 * Selection in the workbench has been changed. We 
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after 
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {}

	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}