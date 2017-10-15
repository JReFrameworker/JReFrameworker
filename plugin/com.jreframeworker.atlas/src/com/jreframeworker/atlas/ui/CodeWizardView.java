package com.jreframeworker.atlas.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

public class CodeWizardView extends ViewPart {
	
	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "com.jreframeworker.atlas.ui.CodeWizardView";
	
	public CodeWizardView() {}
	
	@Override
	public void setFocus() {}

	@Override
	public void createPartControl(Composite composite) {
		composite.setLayout(new GridLayout(1, false));
		
		Group activeProjectGroup = new Group(composite, SWT.NONE);
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
		
		Group codeGenerationGroup = new Group(composite, SWT.NONE);
		codeGenerationGroup.setText("Code Generation");
		codeGenerationGroup.setLayout(new GridLayout(1, false));
		codeGenerationGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		ScrolledComposite codeGenerationScrolledComposite = new ScrolledComposite(codeGenerationGroup, SWT.BORDER | SWT.V_SCROLL);
		codeGenerationScrolledComposite.setExpandVertical(true);
		codeGenerationScrolledComposite.setExpandHorizontal(true);
		codeGenerationScrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		Composite codeGenerationScrolledContentComposite = new Composite(codeGenerationScrolledComposite, SWT.NONE);
		codeGenerationScrolledContentComposite.setLayout(new GridLayout(1, false));
		
		Group codeGenerationCategoryGroup = new Group(codeGenerationScrolledContentComposite, SWT.NONE);
		codeGenerationCategoryGroup.setText("Category");
		codeGenerationCategoryGroup.setLayout(new GridLayout(2, false));
		codeGenerationCategoryGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Button codeGenerationButton = new Button(codeGenerationCategoryGroup, SWT.NONE);
		codeGenerationButton.setText("New Button");
		
		StyledText codeGenerationDescriptionText = new StyledText(codeGenerationCategoryGroup, SWT.BORDER);
		codeGenerationDescriptionText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		codeGenerationScrolledComposite.setContent(codeGenerationScrolledContentComposite);
		codeGenerationScrolledComposite.setMinSize(codeGenerationScrolledContentComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
	}
}
