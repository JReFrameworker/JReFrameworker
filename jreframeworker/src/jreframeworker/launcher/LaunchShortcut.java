package jreframeworker.launcher;

import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

/**
 * Launch shortcut for JReFrameworker launch profiles
 * 
 * @author Ben Holland
 */
public class LaunchShortcut implements ILaunchShortcut {

	@Override
	public void launch(IEditorPart editor, String mode) {
		// run shortcut from class
		IEditorInput input = editor.getEditorInput();
		IJavaElement javaElement = (IJavaElement) input.getAdapter(IJavaElement.class);
		if (javaElement != null) {
			searchAndLaunch(new Object[] { javaElement }, mode);
		}
	}

	@Override
	public void launch(ISelection selection, String mode) {
		// run shortcut from project
		if (selection instanceof IStructuredSelection) {
			searchAndLaunch(((IStructuredSelection) selection).toArray(), mode);
		}
	}

    protected void searchAndLaunch(Object[] search, String mode) {
    	// TODO: implement
    }

}