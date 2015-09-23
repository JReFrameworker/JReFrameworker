package jreframeworker.ui;

import jreframeworker.Activator;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	// allow phantom refs option name and label
	public static final String ALLOW_PHANTOM_REFERENCES_BOOLEAN = "ALLOW_PHANTOM_REFERENCES";
	public static final String ALLOW_PHANTOM_REFERENCES_DESCRIPTION = "Allow Phantom References";
	
	public PreferencesPage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Configure preferences for the WAR Binary Processing plugin.");
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(ALLOW_PHANTOM_REFERENCES_BOOLEAN, "&" + ALLOW_PHANTOM_REFERENCES_DESCRIPTION, getFieldEditorParent()));
	}

}