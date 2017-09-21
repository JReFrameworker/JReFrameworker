package jreframeworker.atlas.ui;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class CodeWizardView extends ViewPart {
	
	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "jreframeworker.atlas.ui.CodeWizardView";
	private Text createProjectText;
	
	public CodeWizardView() {}
	
	@Override
	public void setFocus() {}

	@Override
	public void createPartControl(Composite composite) {
		composite.setLayout(new GridLayout(1, false));
		
		SashForm sashForm = new SashForm(composite, SWT.NONE);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		Composite composite_1 = new Composite(sashForm, SWT.NONE);
		composite_1.setLayout(new GridLayout(1, false));
		
		Composite createProjectComposite = new Composite(composite_1, SWT.NONE);
		createProjectComposite.setLayout(new GridLayout(2, false));
		createProjectComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		createProjectText = new Text(createProjectComposite, SWT.BORDER);
		createProjectText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Button createProjectButton = new Button(createProjectComposite, SWT.NONE);
		createProjectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
			}
		});
		createProjectButton.setText("Create Project");
		
		ScrolledComposite activeProjectScrolledComposite = new ScrolledComposite(composite_1, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		activeProjectScrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		activeProjectScrolledComposite.setExpandHorizontal(true);
		activeProjectScrolledComposite.setExpandVertical(true);
		
		Group activeProjectGroup = new Group(activeProjectScrolledComposite, SWT.NONE);
		activeProjectGroup.setText("Active Project");
		activeProjectGroup.setLayout(new GridLayout(1, false));
		
		Button btnRadioButton = new Button(activeProjectGroup, SWT.RADIO);
		btnRadioButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		btnRadioButton.setText("Radio Button");
		activeProjectScrolledComposite.setContent(activeProjectGroup);
		activeProjectScrolledComposite.setMinSize(activeProjectGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		Composite composite_2 = new Composite(sashForm, SWT.NONE);
		composite_2.setLayout(new GridLayout(1, false));
		
		Group selectionOperationsGroup = new Group(composite_2, SWT.NONE);
		selectionOperationsGroup.setLayout(new GridLayout(1, false));
		selectionOperationsGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		selectionOperationsGroup.setText("Selection Operations");
		
		Group typeOperationsGroup = new Group(selectionOperationsGroup, SWT.NONE);
		typeOperationsGroup.setLayout(new RowLayout(SWT.HORIZONTAL));
		typeOperationsGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		typeOperationsGroup.setText("Type Operations");
		
		Button defineTypeButton = new Button(typeOperationsGroup, SWT.NONE);
		defineTypeButton.setText("Define");
		
		Button replaceTypeButton = new Button(typeOperationsGroup, SWT.NONE);
		replaceTypeButton.setText("Replace");
		
		Button mergeTypeButton = new Button(typeOperationsGroup, SWT.NONE);
		mergeTypeButton.setText("Merge");
		
		Button purgeTypeButton = new Button(typeOperationsGroup, SWT.NONE);
		purgeTypeButton.setText("Purge");
		
		Group methodOperationsGroup = new Group(selectionOperationsGroup, SWT.NONE);
		methodOperationsGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		methodOperationsGroup.setText("Method Operations");
		methodOperationsGroup.setLayout(new RowLayout(SWT.HORIZONTAL));
		
		Button defineMethodButton = new Button(methodOperationsGroup, SWT.NONE);
		defineMethodButton.setText("Define");
		
		Button replaceMethodButton = new Button(methodOperationsGroup, SWT.NONE);
		replaceMethodButton.setText("Replace");
		
		Button mergeMethodButton = new Button(methodOperationsGroup, SWT.NONE);
		mergeMethodButton.setText("Merge");
		
		Button hookBeforeButton = new Button(methodOperationsGroup, SWT.NONE);
		hookBeforeButton.setText("Hook Before");
		
		Button hookAfterButton = new Button(methodOperationsGroup, SWT.NONE);
		hookAfterButton.setText("Hook After");
		
		Button purgeMethodButton = new Button(methodOperationsGroup, SWT.NONE);
		purgeMethodButton.setText("Purge");
		
		Group fieldOperationsGroup = new Group(selectionOperationsGroup, SWT.NONE);
		fieldOperationsGroup.setLayout(new RowLayout(SWT.HORIZONTAL));
		fieldOperationsGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		fieldOperationsGroup.setText("Field Operations");
		
		Button defineFieldButton = new Button(fieldOperationsGroup, SWT.NONE);
		defineFieldButton.setText("Define");
		
		Button replaceFieldButton = new Button(fieldOperationsGroup, SWT.NONE);
		replaceFieldButton.setText("Replace");
		
		Button purgeFieldButton = new Button(fieldOperationsGroup, SWT.NONE);
		purgeFieldButton.setText("Purge");
		sashForm.setWeights(new int[] {1, 1});
		
	}
}
