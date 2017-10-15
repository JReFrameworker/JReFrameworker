package com.jreframeworker.dropper.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.objectweb.asm.tree.ClassNode;
import org.xml.sax.SAXException;

import com.jreframeworker.core.BuildFile;
import com.jreframeworker.core.BuilderUtils;
import com.jreframeworker.core.JReFrameworker;
import com.jreframeworker.core.JReFrameworkerProject;
import com.jreframeworker.core.BuildFile.LibraryTarget;
import com.jreframeworker.core.BuildFile.RuntimeTarget;
import com.jreframeworker.core.BuildFile.Target;
import com.jreframeworker.dropper.Activator;
import com.jreframeworker.dropper.Dropper;
import com.jreframeworker.dropper.log.Log;
import com.jreframeworker.engine.utils.BytecodeUtils;
import com.jreframeworker.engine.utils.JarModifier;
import com.jreframeworker.preferences.JReFrameworkerPreferences;

public class ExportPayloadDropperWizard extends Wizard implements IExportWizard {

	public static final String PAYLOAD_DROPPER = "dropper.jar";
	public static final String EXPORT_PAYLOAD_DROPPER = "export" + File.separatorChar + PAYLOAD_DROPPER;
	
	private SelectJReFrameworkerProjectPage page1;
	private ExportPayloadDropperPage page2;
	
	private File jReFrameworkerWorkspace = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().getAbsolutePath() + File.separatorChar + ".jreframeworker");
	private File dropperJar = new File(jReFrameworkerWorkspace.getAbsolutePath() + File.separatorChar + PAYLOAD_DROPPER);
	
	public ExportPayloadDropperWizard() throws Exception {
		page1 = new SelectJReFrameworkerProjectPage("Select JReFrameworker Project");
		page2 = new ExportPayloadDropperPage("Create Payload Dropper");
		setWindowTitle("Create Payload Dropper");
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {}
	
	@Override
	public void addPages() {
		this.addPage(page1);
		this.addPage(page2);
	}
	
	@Override
	public boolean performFinish() {
		final File dropperFile = new File(page2.getJARPath());

		IRunnableWithProgress j = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) {
				try {
					// make sure we have a fresh copy of the base dropper
					if(dropperJar.exists()){
						dropperJar.delete();
					}
					
					dropperJar.getParentFile().mkdirs();
					URL fileURL = Activator.getDefault().getBundle().getEntry(JReFrameworker.EXPORT_DIRECTORY + "/" + PAYLOAD_DROPPER);
					URL resolvedFileURL = FileLocator.toFileURL(fileURL);
					// need to use the 3-arg constructor of URI in order to properly escape file system chars
					URI resolvedURI = new URI(resolvedFileURL.getProtocol(), resolvedFileURL.getPath(), null);
					InputStream dropperJarInputStream = resolvedURI.toURL().openConnection().getInputStream();
					if(dropperJarInputStream == null){
						throw new RuntimeException("Could not locate: " + PAYLOAD_DROPPER);
					}
					Files.copy(dropperJarInputStream, dropperJar.toPath());
					JarModifier dropper = new JarModifier(dropperJar);
					
					JReFrameworkerProject jrefProject = new JReFrameworkerProject(page1.getJReFrameworkerProject().getProject());
					
					// add payloads
					Set<String> payloadClassNames = new HashSet<String>();
					ICompilationUnit[] compilationUnits = BuilderUtils.getSourceCompilationUnits(jrefProject.getJavaProject());
					for(ICompilationUnit compilationUnit : compilationUnits){
						File sourceFile = compilationUnit.getCorrespondingResource().getLocation().toFile().getCanonicalFile();
						File classFile = BuilderUtils.getCorrespondingClassFile(jrefProject, sourceFile);
						if(classFile.exists()){
							if(!BuilderUtils.hasSevereProblems(compilationUnit)){
								ClassNode classNode = BytecodeUtils.getClassNode(classFile);
								if(BuilderUtils.hasTopLevelAnnotation(classNode)){
									payloadClassNames.add(classFile.getName());
									byte[] classFileBytes = Files.readAllBytes(classFile.toPath());
									dropper.add(Dropper.PAYLOAD_DIRECTORY + "/" + classFile.getName(), classFileBytes, true);
								}
							}
						}
					}
					
					// create dropper configuration file
					BuildFile buildFile = jrefProject.getBuildFile();
					Map<String,String> configurations = new HashMap<String,String>();
					configurations.put(Dropper.MERGE_RENAME_PREFIX, JReFrameworkerPreferences.getMergeRenamingPrefix());
					Dropper.Configuration dropperConfiguration = generateDropperConfiguration(buildFile, configurations, payloadClassNames);
					dropper.add(Dropper.CONFIG_FILE, dropperConfiguration.getXML().getBytes(), true);
					
					// set manifest
					byte[] manifest = "Manifest-Version: 1.0\nClass-Path: .\nMain-Class: com.jreframeworker.dropper.Dropper\n\n\n".getBytes();
					dropper.add("META-INF/MANIFEST.MF", manifest, true);
					
					// export the dropper jar
					dropper.save(dropperFile);
					
					if(JReFrameworkerPreferences.isVerboseLoggingEnabled()){
						Log.info("Exported payload dropper as " + dropperFile.getName());
					}
				} catch (Throwable t) {
					final String message = "Could not create dropper JAR. " + t.getMessage();
					Log.error(message, t);
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							int style = SWT.ICON_ERROR;
							MessageBox messageBox = new MessageBox(Display.getDefault().getActiveShell(), style);
							messageBox.setMessage(message);
							messageBox.open();
						}
					});
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
	
	private static Dropper.Configuration generateDropperConfiguration(BuildFile buildFile, Map<String,String> configurations, Set<String> payloadClassNames) throws TransformerException, ParserConfigurationException, SAXException, IOException {
		Dropper.Configuration result = new Dropper.Configuration();
		
		// add build targets
		for(Target target : buildFile.getTargets()){
			if(target instanceof RuntimeTarget){
				result.runtimes.add(target.getName());
			} else if(target instanceof LibraryTarget){
				result.libraries.add(target.getName());
			}
		}
		
		// add configurations
		result.configurations.putAll(configurations);
		
		// add the payload class names
		result.payloadClassNames.addAll(payloadClassNames);
		
		return result;
	}

}
