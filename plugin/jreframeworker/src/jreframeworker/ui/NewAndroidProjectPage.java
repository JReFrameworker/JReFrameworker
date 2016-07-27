package jreframeworker.ui;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.swt.layout.RowLayout;
import swing2swt.layout.FlowLayout;
import org.eclipse.swt.events.SelectionAdapter;

public class NewAndroidProjectPage extends WizardNewProjectCreationPage {
	
	private Text apkLocationField;
	private boolean validated = false;

	/**
	 * @wbp.parser.constructor
	 */
	public NewAndroidProjectPage(String pageName) {
		super(pageName);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite composite = (Composite) this.getControl();

		final FileDialog fileChooser = new FileDialog(composite.getShell(), SWT.OPEN);
		fileChooser.setFilterExtensions(new String[] { "*.apk" });

		Composite row = new Composite(composite, SWT.NONE);
		row.setLayout(new GridLayout(3, false));

		Label apkLabel = new Label(row, SWT.NONE);
		apkLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		apkLabel.setText("APK: ");

		apkLocationField = new Text(row, SWT.BORDER);
		apkLocationField.setEditable(false);
		GridData gd_apkLocationField = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_apkLocationField.widthHint = 435;
		apkLocationField.setLayoutData(gd_apkLocationField);

		Button browseButton = new Button(row, SWT.NONE);
		GridData browseButtonGrid = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		browseButtonGrid.widthHint = 90;
		browseButton.setLayoutData(browseButtonGrid);
		browseButton.setText("Browse...");
		
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!apkLocationField.getText().isEmpty()){
					fileChooser.setFileName(apkLocationField.getText());
				}
				String result = fileChooser.open();
				if (result != null){
					apkLocationField.setText(result);
				}
				validated = new File(apkLocationField.getText()).exists();
			}
		});
	}

	@Override
	public boolean validatePage() {
//		return validated; // TODO: fix
		return true;
	}

	public File getTargetAPK() {
		if(apkLocationField == null){
			return null;
		} else {
			return new File(apkLocationField.getText());
		}
	}
}
