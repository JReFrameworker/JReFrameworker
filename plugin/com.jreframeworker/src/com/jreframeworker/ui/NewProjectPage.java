package com.jreframeworker.ui;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

public class NewProjectPage extends WizardNewProjectCreationPage {
	
	/**
	 * @wbp.parser.constructor
	 */
	public NewProjectPage(String pageName) {
		super(pageName);
	}
	
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
//		Composite composite = (Composite) this.getControl();
	}
	
	@Override
	public boolean validatePage(){
		return true;
	}
}
