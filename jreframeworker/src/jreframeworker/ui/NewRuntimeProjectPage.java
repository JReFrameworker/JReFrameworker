package jreframeworker.ui;

import java.io.File;

import jreframeworker.common.RuntimeUtils;
import jreframeworker.log.Log;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
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
import org.eclipse.wb.swt.SWTResourceManager;

public class NewRuntimeProjectPage extends WizardNewProjectCreationPage {
	private String runtimePath;
	private boolean defaultRuntime;
	
	public NewRuntimeProjectPage(String pageName, String startRuntimePath) {
		super(pageName);
		runtimePath = startRuntimePath;
	}
	
	/**
	 * @wbp.parser.constructor
	 */
	public NewRuntimeProjectPage(String pageName) {
		this(pageName, "");
	}
	
	public String getRuntimePath() {
		return runtimePath;
	}
	
	public boolean isDefaultRuntime(){
		return defaultRuntime;
	}
	
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite composite = (Composite) this.getControl();
		
		final FileDialog fileChooser = new FileDialog(composite.getShell(), SWT.OPEN);
		fileChooser.setFilterExtensions(new String[] { "*.jar" });
		
		Composite row1 = new Composite(composite, SWT.NONE);
		GridLayout row1Layout = new GridLayout();
		row1.setLayout(row1Layout);
		
		Label runtimeSelectionLabel = new Label(row1, SWT.NONE);
		runtimeSelectionLabel.setFont(SWTResourceManager.getFont(".Helvetica Neue DeskInterface", 11, SWT.BOLD));
		runtimeSelectionLabel.setText("Select runtime to modify (creates a project with a copy of the runtime)");
		
		Composite row2 = new Composite(composite, SWT.NONE);
		GridLayout row2Layout = new GridLayout();
		row2.setLayout(row2Layout);
		
		final Button useDefaultRuntimeButton = new Button(row2, SWT.CHECK);
		useDefaultRuntimeButton.setSelection(true);
		useDefaultRuntimeButton.setText("Use default runtime environment");
		
		Composite row3 = new Composite(composite, SWT.NONE);
		GridLayout row3Layout = new GridLayout();
		row3Layout.numColumns = 3;
		row3.setLayout(row3Layout);

		GridData data = new GridData();
		data.horizontalAlignment = SWT.FILL;
		row3.setLayoutData(data);
		
		Label runtimeLabel = new Label(row3, SWT.NONE);
		runtimeLabel.setText("Runtime:");
		
		try {
			defaultRuntime = true;
			runtimePath = RuntimeUtils.getDefaultRuntime().getCanonicalPath();
		} catch (Exception ex){
			Log.error("Could not located default runtime path.", ex);
		}
		
		final Text runtimeText = new Text(row3, SWT.SINGLE | SWT.BORDER);
		runtimeText.setEnabled(false);
		runtimeText.setText(runtimePath);
		data = new GridData();
		data.grabExcessHorizontalSpace = true;
		data.horizontalAlignment = SWT.FILL;
		runtimeText.setLayoutData(data);
		
		runtimeText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				runtimePath = runtimeText.getText();
				if(validatePage()){
					setPageComplete(true);
				}
			}
		});
		
		final Button runtimeBrowseButton = new Button(row3, SWT.PUSH);
		runtimeBrowseButton.setEnabled(false);
		runtimeBrowseButton.setText("     Browse...     ");
		runtimeBrowseButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String path = fileChooser.open();
				if (path != null){
					runtimePath = path;
				}
				runtimeText.setText(runtimePath);
				if(validatePage()){
					setPageComplete(true);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
			
		});
		
		useDefaultRuntimeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(useDefaultRuntimeButton.getSelection()){
					try {
						runtimePath = RuntimeUtils.getDefaultRuntime().getCanonicalPath();
						runtimeText.setText(runtimePath);
						runtimeBrowseButton.setEnabled(false);
						runtimeText.setEnabled(false);
					} catch (Exception ex){
						Log.error("Could not located default runtime path.", ex);
					}
				} else {
					runtimeBrowseButton.setEnabled(true);
					runtimeText.setEnabled(true);
				}
				defaultRuntime = !defaultRuntime;
				if(validatePage()){
					setPageComplete(true);
				}
			}
		});
	}
	
	@Override
	public boolean validatePage(){
		if(!getProjectName().equals("") && !runtimePath.equals("") && new File(runtimePath).exists()){
			return true;
		} else {
			return false;
		}
	}
}
