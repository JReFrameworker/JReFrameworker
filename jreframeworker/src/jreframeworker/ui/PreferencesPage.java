package jreframeworker.ui;

import jreframeworker.Activator;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	// allow phantom refs option name and label
	public static final String ALLOW_PHANTOM_REFERENCES_BOOLEAN = "ALLOW_PHANTOM_REFERENCES";
	public static final String ALLOW_PHANTOM_REFERENCES_DESCRIPTION = "Allow Phantom References";
	
	/**
	 * Returns the users preference for allowing phantom references in soot operations
	 * @return
	 */
	public static boolean getAllowPhantomReferencesPreference(){
		return Activator.getDefault().getPreferenceStore().getBoolean(ALLOW_PHANTOM_REFERENCES_BOOLEAN);
	}
	
	// allow phantom refs option name and label
	public static final String MERGE_RENAMING_PREFIX_STRING = "MERGE_RENAMING_PREFIX";
	public static final String MERGE_RENAME_PREFIX_DESCRIPTION = "Merge Renaming Prefix";
	public static final String MERGE_RENAME_PREFIX_DEFAULT_VALUE = "jref_";
	
	/**
	 * Returns the user preference for the merge renaming prefix
	 * @return
	 */
	public static String getMergeRenamingPrefix(){
		String mergeRenamingPrefix = Activator.getDefault().getPreferenceStore().getString(MERGE_RENAMING_PREFIX_STRING);
		if(mergeRenamingPrefix == null || mergeRenamingPrefix.equals("")){
			mergeRenamingPrefix = MERGE_RENAME_PREFIX_DEFAULT_VALUE;
		}
		return mergeRenamingPrefix;
	}
	
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
		StringFieldEditor mergeRenamingPrefixStringField = new StringFieldEditor(MERGE_RENAMING_PREFIX_STRING, "&" + MERGE_RENAME_PREFIX_DESCRIPTION, getFieldEditorParent());
		// class files do not have a defined max length, see http://stackoverflow.com/a/695959/475329
		// but if long prefixes becomes a problem use the setTextLimit(int) method to limit input length
		mergeRenamingPrefixStringField.setEmptyStringAllowed(false);
		mergeRenamingPrefixStringField.setStringValue(MERGE_RENAME_PREFIX_DEFAULT_VALUE);
		addField(mergeRenamingPrefixStringField);
	}

}