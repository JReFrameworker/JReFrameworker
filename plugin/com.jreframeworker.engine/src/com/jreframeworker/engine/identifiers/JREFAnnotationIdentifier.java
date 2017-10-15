package com.jreframeworker.engine.identifiers;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class JREFAnnotationIdentifier extends ClassVisitor {

	private static final String DEFINE_TYPE_ANNOTATION = "Lcom/jreframeworker/annotations/types/DefineType;";
	private static final String DEFINE_FIELD_ANNOTATION = "Lcom/jreframeworker/annotations/fields/DefineField;";
	private static final String DEFINE_METHOD_ANNOTATION = "Lcom/jreframeworker/annotations/methods/DefineMethod;";
	
	private static final String MERGE_TYPE_ANNOTATION = "Lcom/jreframeworker/annotations/types/MergeType;";
	private static final String MERGE_METHOD_ANNOTATION = "Lcom/jreframeworker/annotations/methods/MergeMethod;";
	
	private static final String PURGE_TYPE_ANNOTATION = "Lcom/jreframeworker/annotations/types/PurgeType;";
	private static final String PURGE_TYPES_ANNOTATION = "Lcom/jreframeworker/annotations/types/PurgeTypes;";
	private static final String PURGE_FIELD_ANNOTATION = "Lcom/jreframeworker/annotations/fields/PurgeField;";
	private static final String PURGE_FIELDS_ANNOTATION = "Lcom/jreframeworker/annotations/fields/PurgeFields;";
	private static final String PURGE_METHOD_ANNOTATION = "Lcom/jreframeworker/annotations/methods/PurgeMethod;";
	private static final String PURGE_METHODS_ANNOTATION = "Lcom/jreframeworker/annotations/methods/PurgeMethods;";
	
	private static final String DEFINE_TYPE_FINALITY_ANNOTATION = "Lcom/jreframeworker/annotations/types/DefineTypeFinality;";
	private static final String DEFINE_TYPE_FINALITIES_ANNOTATION = "Lcom/jreframeworker/annotations/types/DefineTypeFinalities;";
	private static final String DEFINE_FIELD_FINALITY_ANNOTATION = "Lcom/jreframeworker/annotations/fields/DefineFieldFinality;";
	private static final String DEFINE_FIELD_FINALITIES_ANNOTATION = "Lcom/jreframeworker/annotations/fields/DefineFieldFinalities;";
	private static final String DEFINE_METHOD_FINALITY_ANNOTATION = "Lcom/jreframeworker/annotations/methods/DefineMethodFinality;";
	private static final String DEFINE_METHOD_FINALITIES_ANNOTATION = "Lcom/jreframeworker/annotations/methods/DefineMethodFinalities;";
	
	private static final String DEFINE_TYPE_VISIBILITY_ANNOTATION = "Lcom/jreframeworker/annotations/types/DefineTypeVisibility;";
	private static final String DEFINE_TYPE_VISIBILITIES_ANNOTATION = "Lcom/jreframeworker/annotations/types/DefineTypeVisibilities;";
	private static final String DEFINE_FIELD_VISIBILITY_ANNOTATION = "Lcom/jreframeworker/annotations/fields/DefineFieldVisibility;";
	private static final String DEFINE_FIELD_VISIBILITIES_ANNOTATION = "Lcom/jreframeworker/annotations/fields/DefineFieldVisibilities;";
	private static final String DEFINE_METHOD_VISIBILITY_ANNOTATION = "Lcom/jreframeworker/annotations/methods/DefineMethodVisibility;";
	private static final String DEFINE_METHOD_VISIBILITIES_ANNOTATION = "Lcom/jreframeworker/annotations/methods/DefineMethodVisibilities;";
	
	private boolean isDefineTypeAnnotation = false;
	private boolean isDefineFieldAnnotation = false;
	private boolean isDefineMethodAnnotation = false;
	
	private boolean isMergeTypeAnnotation = false;
	private boolean isMergeMethodAnnotation = false;
	
	private boolean isPurgeTypeAnnotation = false;
	private boolean isPurgeTypesAnnotation = false;
	private boolean isPurgeFieldAnnotation = false;
	private boolean isPurgeFieldsAnnotation = false;
	private boolean isPurgeMethodAnnotation = false;
	private boolean isPurgeMethodsAnnotation = false;
	
	private boolean isDefineTypeFinalityAnnotation = false;
	private boolean isDefineTypeFinalitiesAnnotation = false;
	private boolean isDefineFieldFinalityAnnotation = false;
	private boolean isDefineFieldFinalitiesAnnotation = false;
	private boolean isDefineMethodFinalityAnnotation = false;
	private boolean isDefineMethodFinalitiesAnnotation = false;
	
	private boolean isDefineTypeVisibilityAnnotation = false;
	private boolean isDefineTypeVisibilitiesAnnotation = false;
	private boolean isDefineFieldVisibilityAnnotation = false;
	private boolean isDefineFieldVisibilitiesAnnotation = false;
	private boolean isDefineMethodVisibilityAnnotation = false;
	private boolean isDefineMethodVisibilitiesAnnotation = false;
	
	public JREFAnnotationIdentifier() {
		super(Opcodes.ASM5);
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String name, boolean visible) {
		// System.out.println("Annotation: " + name);
		if (name.equals(DEFINE_TYPE_ANNOTATION)) {
			isDefineTypeAnnotation = true;
		} else if (name.equals(DEFINE_FIELD_ANNOTATION)) {
			isDefineFieldAnnotation = true;
		} else if (name.equals(DEFINE_METHOD_ANNOTATION)) {
			isDefineMethodAnnotation = true;
		}
		
		else if (name.equals(MERGE_TYPE_ANNOTATION)) {
			isMergeTypeAnnotation = true;
		} else if (name.equals(MERGE_METHOD_ANNOTATION)) {
			isMergeMethodAnnotation = true;
		}
		
		else if (name.equals(PURGE_TYPE_ANNOTATION)) {
			isPurgeTypeAnnotation = true;
		} else if (name.equals(PURGE_TYPES_ANNOTATION)) {
			isPurgeTypesAnnotation = true;
		} else if (name.equals(PURGE_FIELD_ANNOTATION)) {
			isPurgeFieldAnnotation = true;
		} else if (name.equals(PURGE_FIELDS_ANNOTATION)) {
			isPurgeFieldsAnnotation = true;
		} else if (name.equals(PURGE_METHOD_ANNOTATION)) {
			isPurgeMethodAnnotation = true;
		} else if (name.equals(PURGE_METHODS_ANNOTATION)) {
			isPurgeMethodsAnnotation = true;
		}
		
		else if (name.equals(DEFINE_TYPE_FINALITY_ANNOTATION)) {
			isDefineTypeFinalityAnnotation = true;
		} else if (name.equals(DEFINE_TYPE_FINALITIES_ANNOTATION)) {
			isDefineTypeFinalitiesAnnotation = true;
		} else if (name.equals(DEFINE_FIELD_FINALITY_ANNOTATION)) {
			isDefineFieldFinalityAnnotation = true;
		} else if (name.equals(DEFINE_FIELD_FINALITIES_ANNOTATION)) {
			isDefineFieldFinalitiesAnnotation = true;
		} else if (name.equals(DEFINE_METHOD_FINALITY_ANNOTATION)) {
			isDefineMethodFinalityAnnotation = true;
		} else if (name.equals(DEFINE_METHOD_FINALITIES_ANNOTATION)) {
			isDefineMethodFinalitiesAnnotation = true;
		}
		
		else if (name.equals(DEFINE_TYPE_VISIBILITY_ANNOTATION)) {
			isDefineTypeVisibilityAnnotation = true;
		} else if (name.equals(DEFINE_TYPE_VISIBILITIES_ANNOTATION)) {
			isDefineTypeVisibilitiesAnnotation = true;
		} else if (name.equals(DEFINE_FIELD_VISIBILITY_ANNOTATION)) {
			isDefineFieldVisibilityAnnotation = true;
		} else if (name.equals(DEFINE_FIELD_VISIBILITIES_ANNOTATION)) {
			isDefineFieldVisibilitiesAnnotation = true;
		} else if (name.equals(DEFINE_METHOD_VISIBILITY_ANNOTATION)) {
			isDefineMethodVisibilityAnnotation = true;
		} else if (name.equals(DEFINE_METHOD_VISIBILITIES_ANNOTATION)) {
			isDefineMethodVisibilitiesAnnotation = true;
		}
		
		return null;
	}
	
	public boolean isDefineTypeAnnotation() {
		return isDefineTypeAnnotation;
	}

	public boolean isDefineFieldAnnotation() {
		return isDefineFieldAnnotation;
	}

	public boolean isDefineMethodAnnotation() {
		return isDefineMethodAnnotation;
	}
	
	public boolean isMergeTypeAnnotation() {
		return isMergeTypeAnnotation;
	}

	public boolean isMergeMethodAnnotation() {
		return isMergeMethodAnnotation;
	}
	
	public boolean isPurgeTypeAnnotation() {
		return isPurgeTypeAnnotation;
	}
	
	public boolean isPurgeTypesAnnotation() {
		return isPurgeTypesAnnotation;
	}

	public boolean isPurgeFieldAnnotation() {
		return isPurgeFieldAnnotation;
	}
	
	public boolean isPurgeFieldsAnnotation() {
		return isPurgeFieldsAnnotation;
	}

	public boolean isPurgeMethodAnnotation() {
		return isPurgeMethodAnnotation;
	}
	
	public boolean isPurgeMethodsAnnotation() {
		return isPurgeMethodsAnnotation;
	}
	
	public boolean isDefineTypeFinalityAnnotation() {
		return isDefineTypeFinalityAnnotation;
	}
	
	public boolean isDefineTypeFinalitiesAnnotation() {
		return isDefineTypeFinalitiesAnnotation;
	}

	public boolean isDefineFieldFinalityAnnotation() {
		return isDefineFieldFinalityAnnotation;
	}

	public boolean isDefineFieldFinalitiesAnnotation() {
		return isDefineFieldFinalitiesAnnotation;
	}
	
	public boolean isDefineMethodFinalityAnnotation() {
		return isDefineMethodFinalityAnnotation;
	}
	
	public boolean isDefineMethodFinalitiesAnnotation() {
		return isDefineMethodFinalitiesAnnotation;
	}
	
	public boolean isDefineTypeVisibilityAnnotation() {
		return isDefineTypeVisibilityAnnotation;
	}
	
	public boolean isDefineTypeVisibilitiesAnnotation() {
		return isDefineTypeVisibilitiesAnnotation;
	}

	public boolean isDefineFieldVisibilityAnnotation() {
		return isDefineFieldVisibilityAnnotation;
	}
	
	public boolean isDefineFieldVisibilitiesAnnotation() {
		return isDefineFieldVisibilitiesAnnotation;
	}

	public boolean isDefineMethodVisibilityAnnotation() {
		return isDefineMethodVisibilityAnnotation;
	}
	
	public boolean isDefineMethodVisibilitiesAnnotation() {
		return isDefineMethodVisibilitiesAnnotation;
	}
	
	public boolean isPurgeAnnotation(){
		return isPurgeTypeAnnotation
			|| isPurgeTypesAnnotation
			|| isPurgeMethodAnnotation
			|| isPurgeMethodsAnnotation
			|| isPurgeFieldAnnotation
			|| isPurgeFieldsAnnotation;
	}
	
	public boolean isFinalityAnnotation(){
		return isDefineTypeFinalityAnnotation
			|| isDefineTypeFinalitiesAnnotation
			|| isDefineFieldFinalityAnnotation
			|| isDefineFieldFinalitiesAnnotation
			|| isDefineMethodFinalityAnnotation
			|| isDefineMethodFinalitiesAnnotation;
	}
	
	public boolean isVisibilityAnnotation(){
		return isDefineTypeVisibilityAnnotation
			|| isDefineTypeVisibilitiesAnnotation
			|| isDefineFieldVisibilityAnnotation
			|| isDefineFieldVisibilitiesAnnotation
			|| isDefineMethodVisibilityAnnotation
			|| isDefineMethodVisibilitiesAnnotation;
	}
	
	public boolean isJREFAnnotation(){
		return isDefineTypeAnnotation 
			|| isDefineFieldAnnotation 
			|| isDefineMethodAnnotation 
			
			|| isMergeTypeAnnotation 
			|| isMergeMethodAnnotation
			
			|| isPurgeTypeAnnotation 
			|| isPurgeTypesAnnotation 
			|| isPurgeFieldAnnotation
			|| isPurgeFieldsAnnotation
			|| isPurgeMethodAnnotation
			|| isPurgeMethodsAnnotation
			
			|| isDefineTypeFinalityAnnotation
			|| isDefineTypeFinalitiesAnnotation
			|| isDefineFieldFinalityAnnotation
			|| isDefineFieldFinalitiesAnnotation
			|| isDefineMethodFinalityAnnotation
			|| isDefineMethodFinalitiesAnnotation
			
			|| isDefineTypeVisibilityAnnotation
			|| isDefineTypeVisibilitiesAnnotation
			|| isDefineFieldVisibilityAnnotation
			|| isDefineFieldVisibilitiesAnnotation
			|| isDefineMethodVisibilityAnnotation
			|| isDefineMethodVisibilitiesAnnotation;
	}
	
}
