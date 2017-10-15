package com.jreframeworker.atlas.codegen;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import com.ensoftcorp.atlas.core.query.Q;
import com.jreframeworker.atlas.Activator;
import com.jreframeworker.atlas.log.Log;

public class CodeGenerators {

	private static Set<CodeGenerator> ALL_CODE_GENERATORS = Collections.synchronizedSet(new HashSet<CodeGenerator>());
	private static Map<String,Set<CodeGenerator>> CATEGORIZED_CODE_GENERATORS = Collections.synchronizedMap(new HashMap<String,Set<CodeGenerator>>());
	
	/**
	 * Returns a copy of the currently registered code generators
	 * 
	 * @return
	 */
	public static Set<CodeGenerator> getRegisteredCodeGenerators() {
		HashSet<CodeGenerator> codeGenerators = new HashSet<CodeGenerator>();
		for (CodeGenerator codeGenerator : ALL_CODE_GENERATORS) {
			codeGenerators.add(codeGenerator);
		}
		return codeGenerators;
	}
	
	/**
	 * Returns the registered code generator categories
	 * @return
	 */
	public static Set<String> getRegisteredCodeGeneratorCategories() {
		Set<String> categories = new HashSet<String>();
		for(CodeGenerator codeGenerator : getRegisteredCodeGenerators()){
			categories.add(codeGenerator.getCategory());
		}
		return categories;
	}
	
	/**
	 * Returns the registered code generators for a given category
	 * @param category
	 * @return
	 */
	public static Set<CodeGenerator> getCodeGeneratorsForCategory(String category) {
		Set<CodeGenerator> codeGenerators = CATEGORIZED_CODE_GENERATORS.get(category);
		if(codeGenerators != null){
			return new HashSet<CodeGenerator>(codeGenerators);
		} else {
			return new HashSet<CodeGenerator>();
		}
	}
	
	public static Set<CodeGenerator> getApplicableCodeGenerators(Q input) {
		Set<CodeGenerator> applicableCodeGenerators = new HashSet<CodeGenerator>();
		// find the applicable code generators
		for(CodeGenerator codeGenerator : CodeGenerators.getRegisteredCodeGenerators()){
			if(codeGenerator.isApplicableTo(input)){
				applicableCodeGenerators.add(codeGenerator);
			}
		}
		return applicableCodeGenerators;
	}

	/**
	 * Registers the contributed plugin code generator definitions
	 */
	public static void loadCodeGeneratorContributions() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] config = registry.getConfigurationElementsFor(Activator.PLUGIN_CODE_GENERATOR_EXTENSION_ID);
		try {
			for (IConfigurationElement element : config) {
				final Object o = element.createExecutableExtension("class");
				if (o instanceof CodeGenerator) {
					CodeGenerator codeGenerator = (CodeGenerator) o;
					registerCodeGenerator(codeGenerator);
				}
			}
		} catch (CoreException e) {
			Log.error("Error loading code generators.", e);
		}
	}

	/**
	 * Registers a new codeGenerator
	 * 
	 * @param codeGenerator
	 */
	private static synchronized void registerCodeGenerator(CodeGenerator codeGenerator) {
		ALL_CODE_GENERATORS.add(codeGenerator);
		if(CATEGORIZED_CODE_GENERATORS.containsKey(codeGenerator.getCategory())){
			CATEGORIZED_CODE_GENERATORS.get(codeGenerator.getCategory()).add(codeGenerator);
		} else {
			Set<CodeGenerator> codeGenerators = new HashSet<CodeGenerator>();
			codeGenerators.add(codeGenerator);
			CATEGORIZED_CODE_GENERATORS.put(codeGenerator.getCategory(), codeGenerators);
		}
	}

	/**
	 * Unregisters a codeGenerator
	 * 
	 * @param codeGenerator
	 */
	@SuppressWarnings("unused")
	private static synchronized void unregisterCodeGenerator(CodeGenerator codeGenerator) {
		ALL_CODE_GENERATORS.remove(codeGenerator);
		if(CATEGORIZED_CODE_GENERATORS.containsKey(codeGenerator.getCategory())){
			CATEGORIZED_CODE_GENERATORS.get(codeGenerator.getCategory()).remove(codeGenerator);
		}
	}
	
}
