package jreframeworker.ui;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

public class NewApplicationProjectPage extends WizardNewProjectCreationPage {
	
	/**
	 * @wbp.parser.constructor
	 */
	public NewApplicationProjectPage(String pageName) {
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

	public String getTargetJarName() {
		// TODO: Implement UI to enter custom jar
		return "application.jar";
	}
}
