package jreframeworker.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import jreframeworker.core.JReFrameworker;
import jreframeworker.core.JReFrameworkerProject;
import jreframeworker.log.Log;

public class ResetClasspathHandler extends AbstractHandler {

	@SuppressWarnings("restriction")
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			// get the package explorer selection
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			ISelection selection = window.getSelectionService().getSelection("org.eclipse.jdt.ui.PackageExplorer");
			
			if(selection == null){
				Log.warning("Selection must be a project.");
				return null;
			}
			
			TreePath[] paths = ((TreeSelection) selection).getPaths();
			if(paths.length > 0){
				TreePath p = paths[0];
				Object last = p.getLastSegment();
				
				// locate the project handle for the selection
				IProject project = null;
				if(last instanceof IJavaProject){
					project = ((IJavaProject) last).getProject();
				} else if (last instanceof IResource) {
					project = ((IResource) last).getProject();
				} 
				
				if(project == null){
					Log.warning("Selection must be a project.");
					return null;
				}
				
				JReFrameworkerProject jrefProject = new JReFrameworkerProject(project);
				jrefProject.restoreOriginalClasspathEntries();
				jrefProject.refresh();
			} else {
				Log.warning("Selection must be a project.");
			}
		} catch (Exception e) {
			Log.error("Unable to reset project classpath", e);
		}
		
		return null;
	}

}
