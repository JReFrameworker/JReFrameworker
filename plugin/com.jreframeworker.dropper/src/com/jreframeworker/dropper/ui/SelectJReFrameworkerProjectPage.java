package com.jreframeworker.dropper.ui;

import java.util.LinkedList;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wb.swt.SWTResourceManager;

import com.jreframeworker.core.JReFrameworker;

public class SelectJReFrameworkerProjectPage extends WizardPage {
	
	private IJavaProject jProject;
	
	public IJavaProject getJReFrameworkerProject(){
		return jProject;
	}
	
	protected SelectJReFrameworkerProjectPage(String pageName) {
		super(pageName);
		setTitle("Select JReFrameworker Project");
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		container.setLayout(new GridLayout(1, false));
		
		Group projectGroup = new Group(container, SWT.SHADOW_IN);
		projectGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		projectGroup.setText("JReFrameworker Projects");
		projectGroup.setLayout(new RowLayout(SWT.VERTICAL));
		
		Label errorLabel = new Label(container, SWT.NONE);
	    errorLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_DARK_RED));
	    errorLabel.setFont(SWTResourceManager.getFont(".SF NS Text", 11, SWT.BOLD));
	    errorLabel.setLayoutData(new GridData(SWT.CENTER, SWT.BOTTOM, true, true, 1, 1));
		
		final LinkedList<IJavaProject> projects = JReFrameworker.getJReFrameworkerProjects();
		if(projects.isEmpty()){
			errorLabel.setText("No JReFrameworker Projects in Workspace!");
		} else {
			boolean first = true;
			for(final IJavaProject project : projects){
				Button projectButton = new Button(projectGroup, SWT.RADIO);
				projectButton.setText(project.getProject().getName());
				if(first){
					jProject = project;
					projectButton.setSelection(true);
					setPageComplete(true);
					first = false;
				}
				projectButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						jProject = project;
					}
				});
			}
		}
	}

}