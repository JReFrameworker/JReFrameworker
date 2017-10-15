package com.jreframeworker.atlas.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.log.Log;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.ui.selection.IAtlasSelectionListener;
import com.ensoftcorp.atlas.ui.selection.SelectionUtil;
import com.ensoftcorp.atlas.ui.selection.event.IAtlasSelectionEvent;
import com.jreframeworker.atlas.codegen.CodeGenerator;
import com.jreframeworker.atlas.codegen.CodeGenerators;
import com.jreframeworker.builder.JReFrameworkerNature;
import com.jreframeworker.common.WorkspaceUtils;
import com.jreframeworker.core.JReFrameworkerProject;

public class CodeWizardView extends ViewPart {
	
	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "com.jreframeworker.atlas.ui.CodeWizardView";
	
	// the current Atlas selection
	private Graph selection = Common.empty().eval();
	private JReFrameworkerProject jrefProject = null;
	private Set<CodeGenerator> codeGenerators;
	private Map<CodeGenerator,Button> codeGeneratorButtons;
	
	public CodeWizardView() {}
	
	@Override
	public void setFocus() {}
	
	@Override
	public void createPartControl(Composite parent) {
		// initialize code generators
		CodeGenerators.loadCodeGeneratorContributions();
		codeGenerators = CodeGenerators.getRegisteredCodeGenerators();
		codeGeneratorButtons = new HashMap<CodeGenerator,Button>();
		
		parent.setLayout(new GridLayout(1, false));
		
		Group activeProjectGroup = new Group(parent, SWT.NONE);
		activeProjectGroup.setText("Project");
		activeProjectGroup.setLayout(new GridLayout(1, false));
		activeProjectGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		
		Composite activeProjectComposite = new Composite(activeProjectGroup, SWT.NONE);
		activeProjectComposite.setLayout(new GridLayout(2, false));
		activeProjectComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label activeProjectLabel = new Label(activeProjectComposite, SWT.NONE);
		activeProjectLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		activeProjectLabel.setText("Active Project: ");
		
		Combo activeProjectCombo = new Combo(activeProjectComposite, SWT.NONE);
		activeProjectCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		activeProjectCombo.removeAll();
		for(IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()){
			if(project.exists()){
				try {
					// open project if it is closed
					if(!project.isOpen()){
						project.open(new NullProgressMonitor());
					}
					// if the project is a JReFrameworker project
					if(project.hasNature(JReFrameworkerNature.NATURE_ID) && project.hasNature(JavaCore.NATURE_ID)){
						JReFrameworkerProject jrefProject = new JReFrameworkerProject(project);
						String projectName = jrefProject.getJavaProject().getProject().getName();
						activeProjectCombo.add(projectName);
						activeProjectCombo.setData(projectName, jrefProject);
					}
				} catch (CoreException e) {
					String message = "Error opening project.";
					Log.warning(message, new IllegalArgumentException(message));
				}
			}
		}
		
		if(activeProjectCombo.getItemCount() == 1){
			activeProjectCombo.select(0);
			jrefProject = (JReFrameworkerProject) activeProjectCombo.getData(activeProjectCombo.getText());
		}
		
		activeProjectCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				jrefProject = (JReFrameworkerProject) activeProjectCombo.getData(activeProjectCombo.getText());
			}
		});
		
		Group codeGenerationGroup = new Group(parent, SWT.NONE);
		codeGenerationGroup.setText("Code Generation");
		codeGenerationGroup.setLayout(new GridLayout(1, false));
		codeGenerationGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		ScrolledComposite codeGenerationScrolledComposite = new ScrolledComposite(codeGenerationGroup, SWT.BORDER | SWT.V_SCROLL);
		codeGenerationScrolledComposite.setExpandVertical(true);
		codeGenerationScrolledComposite.setExpandHorizontal(true);
		codeGenerationScrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		Composite codeGenerationScrolledContentComposite = new Composite(codeGenerationScrolledComposite, SWT.NONE);
		codeGenerationScrolledContentComposite.setLayout(new GridLayout(1, false));
		
		List<String> sortedCategories = new ArrayList<String>(CodeGenerators.getRegisteredCodeGeneratorCategories());
		Collections.sort(sortedCategories);
		for(String category : sortedCategories){
			Group codeGenerationCategoryGroup = new Group(codeGenerationScrolledContentComposite, SWT.NONE);
			codeGenerationCategoryGroup.setText(category);
			codeGenerationCategoryGroup.setLayout(new GridLayout(2, false));
			codeGenerationCategoryGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			
			List<CodeGenerator> sortedCodeGenerators = new ArrayList<CodeGenerator>(CodeGenerators.getCodeGeneratorsForCategory(category));
			Collections.sort(sortedCodeGenerators, new Comparator<CodeGenerator>(){
				@Override
				public int compare(CodeGenerator cg1, CodeGenerator cg2) {
					return cg1.getName().compareTo(cg2.getName());
				}
			});
			for(CodeGenerator codeGenerator : sortedCodeGenerators){
				Button codeGenerationButton = new Button(codeGenerationCategoryGroup, SWT.NONE);
				codeGenerationButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
				codeGenerationButton.setText(codeGenerator.getName());
				codeGenerationButton.setEnabled(false);
				codeGeneratorButtons.put(codeGenerator, codeGenerationButton);
				
				codeGenerationButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						Set<File> sourceFiles = codeGenerator.generateCode(jrefProject, Common.toQ(selection));
						for(File sourceFile : sourceFiles){
							WorkspaceUtils.openFileInEclipseEditor(sourceFile);
						}
					}
				});
				
				StyledText codeGenerationDescriptionText = new StyledText(codeGenerationCategoryGroup, SWT.BORDER);
				codeGenerationDescriptionText.setEditable(false);
				codeGenerationDescriptionText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
				codeGenerationDescriptionText.setText(codeGenerator.getDescription());
			}
		}

		codeGenerationScrolledComposite.setContent(codeGenerationScrolledContentComposite);
		codeGenerationScrolledComposite.setMinSize(codeGenerationScrolledContentComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		// setup the Atlas selection event listener
		IAtlasSelectionListener selectionListener = new IAtlasSelectionListener(){
			@Override
			public void selectionChanged(IAtlasSelectionEvent atlasSelection) {
				try {
					selection = atlasSelection.getSelection().eval();
				} catch (Exception e){
					selection = Common.empty().eval();
				}
				
				for(CodeGenerator codeGenerator : codeGenerators){
					Button codeGenerationButton = codeGeneratorButtons.get(codeGenerator);
					if(codeGenerationButton != null){
						if(jrefProject != null){
							codeGenerationButton.setEnabled(codeGenerator.isApplicableTo(Common.toQ(selection)));
						} else {
							codeGenerationButton.setEnabled(false);
						}
					}
				}
			}				
		};
		
		// add the selection listener
		SelectionUtil.addSelectionListener(selectionListener);
	}
}
