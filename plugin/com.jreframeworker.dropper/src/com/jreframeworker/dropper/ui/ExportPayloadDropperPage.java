package com.jreframeworker.dropper.ui;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ExportPayloadDropperPage extends WizardPage {
	private String jarPath;
	private Text dropperJarText;
	
	/**
	 * @wbp.parser.constructor
	 */
	public ExportPayloadDropperPage(String pageName) {
		super(pageName);
		setTitle("Create Payload Dropper");
	}
	
	public String getJARPath() {
		return jarPath;
	}
	
	@Override
	public void createControl(Composite parent) {
		
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		container.setLayout(new GridLayout(3, false));
		
		Label dropperJarLabel = new Label(container, SWT.NONE);
		dropperJarLabel.setText("Payload Dropper Jar: ");
		
		dropperJarText = new Text(container, SWT.BORDER);
		dropperJarText.setEditable(false);
		dropperJarText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		final FileDialog fileChooser = new FileDialog(container.getShell(), SWT.SAVE);
		fileChooser.setFilterExtensions(new String[] { "*.jar" });
		fileChooser.setFileName("dropper.jar");

		Button browseButton = new Button(container, SWT.NONE);
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String path = fileChooser.open();
				if (path != null){
					jarPath = path;
					setPageComplete(true);
				}
				dropperJarText.setText(jarPath);
			}
		});
		browseButton.setText("Browse...");
		
		setPageComplete(false);
	}
}
