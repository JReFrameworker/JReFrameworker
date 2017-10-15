package com.jreframeworker.handlers;

import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.jreframeworker.core.JReFrameworkerProject;
import com.jreframeworker.log.Log;

public class AddTargetHandler extends AbstractHandler {

	@SuppressWarnings("restriction")
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			// get the package explorer selection
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			ISelection selection = window.getSelectionService().getSelection("org.eclipse.jdt.ui.PackageExplorer");
			
			if(selection == null){
				Log.warning("Selection must be a library file.");
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
				
				if(last instanceof org.eclipse.core.internal.resources.File){
					File library = ((org.eclipse.core.internal.resources.File)last).getLocation().toFile();
					JReFrameworkerProject jrefProject = new JReFrameworkerProject(project);
					jrefProject.addTarget(library);
				} else {
					Log.warning("Selection must be a library file.");
				}
			} else {
				Log.warning("Selection must be a library file.");
			}
		} catch (Exception e) {
			Log.error("Unable to add target", e);
		}
		
		return null;
	}

	
}
