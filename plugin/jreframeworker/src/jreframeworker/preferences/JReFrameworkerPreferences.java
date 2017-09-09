package jreframeworker.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import jreframeworker.Activator;
import jreframeworker.log.Log;

public class JReFrameworkerPreferences extends AbstractPreferenceInitializer {

	private static boolean initialized = false;
	
	/**
	 * Merge renaming prefix
	 */
	public static final String MERGE_RENAMING_PREFIX = "MERGE_RENAMING_PREFIX";
	public static final String MERGE_RENAMING_PREFIX_DEFAULT = "jref_";
	private static String mergeRenamePrefixValue = MERGE_RENAMING_PREFIX_DEFAULT;
	
	/**
	 * Configures merge renaming prefix
	 */
	public static void setMergeRenamingPrefix(String prefix){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setValue(MERGE_RENAMING_PREFIX, prefix);
		loadPreferences();
	}
	
	/**
	 * Returns the merge renaming prefix
	 * @return
	 */
	public static String getMergeRenamingPrefix(){
		if(!initialized){
			loadPreferences();
		}
		return mergeRenamePrefixValue;
	}
	
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setDefault(MERGE_RENAMING_PREFIX, MERGE_RENAMING_PREFIX_DEFAULT);
	}
	
	/**
	 * Restores the default preferences
	 */
	public static void restoreDefaults(){
		IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
		preferences.setValue(MERGE_RENAMING_PREFIX, MERGE_RENAMING_PREFIX_DEFAULT);
		loadPreferences();
	}
	
	/**
	 * Loads or refreshes current preference values
	 */
	public static void loadPreferences() {
		try {
			IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
			mergeRenamePrefixValue = preferences.getString(MERGE_RENAMING_PREFIX);
		} catch (Exception e){
			Log.warning("Error accessing JReFrameworker preferences, using defaults...", e);
		}
		initialized = true;
	}
}