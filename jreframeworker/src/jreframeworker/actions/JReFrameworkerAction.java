package jreframeworker.actions;

import java.io.File;
import java.io.IOException;

import jreframeworker.builder.JReFrameworkerNature;
import jreframeworker.core.bytecode.identifiers.JREFAnnotationIdentifier;
import jreframeworker.core.bytecode.utils.BytecodeUtils;
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
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

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
						IJavaProject jProject = JavaCore.create(project);
						File binDirectory = project.getFolder("bin").getLocation().toFile();
						processDirectory(binDirectory, jProject);
						
//						File projectDirectory = new File(project.getLocation().toFile().getCanonicalPath() + File.separatorChar + project.getName()).getCanonicalFile();
//						File runtimesDirectory = new File(projectDirectory.getCanonicalPath() + File.separatorChar + "runtimes");
//						File runtimeJar = project.getFile(runtimesDirectory.getName() + File.separatorChar + "rt.jar").getLocation().toFile();
//						JimpleUtils.assemble(jProject,  project.getFile(runtimesDirectory.getName() + File.separatorChar + "rt.jar"));
						
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
	
	private void processDirectory(File root, IJavaProject jProject) throws IOException {
		File[] files = root.listFiles();
		for(File file : files){
			if(file.isFile()){
				if(file.getName().endsWith(".class")){
					// check to see if the class is annotated with 
					ClassNode classNode = BytecodeUtils.getClassNode(file);
					// TODO: address innerclasses
//					classNode.innerClasses
					if(classNode.invisibleAnnotations != null){
						for(Object o : classNode.invisibleAnnotations){
							AnnotationNode annotationNode = (AnnotationNode) o;
							JREFAnnotationIdentifier checker = new JREFAnnotationIdentifier();
							checker.visitAnnotation(annotationNode.desc, false);
							if(checker.isJREFAnnotation()){
								if(checker.isDefineTypeAnnotation()){
									// TODO: determine if its an insert or a replace
									Log.info("INSERT or REPLACE " + classNode.name);
								} else if(checker.isMergeTypeAnnotation()){
									// TODO: execute merge
//									Merge.mergeClasses(baseClass, classToMerge, outputClass);
									Log.info("MERGE " + classNode.name);
								}
							}
						}
					}
					
				}
			} else if(file.isDirectory()){
				processDirectory(file, jProject);
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