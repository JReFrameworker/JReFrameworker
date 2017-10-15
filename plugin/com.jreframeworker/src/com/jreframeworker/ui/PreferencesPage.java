package com.jreframeworker.ui;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.jreframeworker.Activator;
import com.jreframeworker.preferences.JReFrameworkerPreferences;

public class PreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	public static final String MERGE_RENAME_PREFIX_DESCRIPTION = "Merge Renaming Prefix";
	public static final String VERBOSE_LOGGING_DESCRIPTION = "Verbose Logging";
	
	public PreferencesPage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Configure preferences for the JReFrameworker.");
	}

	@Override
	protected void createFieldEditors() {
		StringFieldEditor mergeRenamingPrefixStringField = new StringFieldEditor(JReFrameworkerPreferences.MERGE_RENAMING_PREFIX, "&" + MERGE_RENAME_PREFIX_DESCRIPTION, getFieldEditorParent());
		// class files do not have a defined max length, see http://stackoverflow.com/a/695959/475329
		// but if long prefixes becomes a problem use the setTextLimit(int) method to limit input length
		mergeRenamingPrefixStringField.setEmptyStringAllowed(false);
		String mergeRenamingPrefix = JReFrameworkerPreferences.getMergeRenamingPrefix();
		if(mergeRenamingPrefix == null || mergeRenamingPrefix.equals("")){
			mergeRenamingPrefixStringField.setStringValue(JReFrameworkerPreferences.MERGE_RENAMING_PREFIX_DEFAULT);
		}
		addField(mergeRenamingPrefixStringField);
		addField(new BooleanFieldEditor(JReFrameworkerPreferences.VERBOSE_LOGGING, "&" + VERBOSE_LOGGING_DESCRIPTION, getFieldEditorParent()));
	}

}