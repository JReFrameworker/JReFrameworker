package jreframeworker.atlas.analysis;

import java.util.ArrayList;

import javax.lang.model.element.Modifier;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.query.Attr;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;

public class MethodAnalysis {

	/**
	 * Returns the class that owns the given method
	 * @param method
	 * @return
	 */
	public static Node getOwnerClass(Node method) {
		return Common.toQ(method).parent().eval().nodes().one();
	}
	
	/**
	 * Returns true if the given method is marked final
	 * @param clazz
	 * @return
	 */
	public static boolean isFinal(Node method){
		return method.taggedWith(XCSG.Java.finalMethod);
	}
	
	/**
	 * Returns true if the given method is marked public
	 * @param clazz
	 * @return
	 */
	public static boolean isPublic(Node method){
		return method.taggedWith(XCSG.publicVisibility);
	}
	
	/**
	 * Returns true if the given method is marked protected
	 * @param clazz
	 * @return
	 */
	public static boolean isProtected(Node method){
		return method.taggedWith(XCSG.protectedPackageVisibility);
	}
	
	/**
	 * Returns true if the given method is marked private
	 * @param clazz
	 * @return
	 */
	public static boolean isPrivate(Node method){
		return method.taggedWith(XCSG.privateVisibility);
	}
	
	/**
	 * Returns true if the given method is marked static
	 * @param clazz
	 * @return
	 */
	public static boolean isStatic(Node method){
		return method.taggedWith(Attr.Node.IS_STATIC); // TODO: figure out XCSG equivalent
	}
	
	/**
	 * Returns the name of the given method
	 * @param clazz
	 * @return
	 */
	public static String getName(Node method){
		return method.getAttr(XCSG.name).toString();
	}

	public static Modifier[] getModifiers(Node method){
		ArrayList<Modifier> modifiers = new ArrayList<Modifier>();
		
		if(isPublic(method)){
			modifiers.add(Modifier.PUBLIC);
		}
		
		if(isProtected(method)){
			modifiers.add(Modifier.PROTECTED);
		}
		
		if(isPrivate(method)){
			modifiers.add(Modifier.PRIVATE);
		}
		
		if(isStatic(method)){
			modifiers.add(Modifier.STATIC);
		}
		
		// TODO: consider other modifiers...
		
		Modifier[] result = new Modifier[modifiers.size()];
		modifiers.toArray(result);
		return result;
	}
	
	public static Parameter[] getParameters(Node method){
		ArrayList<Parameter> parameters = new ArrayList<Parameter>();
		
		// TODO: implement
		
		Parameter[] result = new Parameter[parameters.size()];
		parameters.toArray(result);
		return result;
	}
	
	@SuppressWarnings("rawtypes")
	public static class Parameter {
		private Modifier[] modifiers;
		private Class type;
		private String name;
		
		public Parameter(Modifier[] modifiers, Class type, String name) {
			this.modifiers = modifiers;
			this.type = type;
			this.name = name;
		}
		
		public Modifier[] getModifiers() {
			return modifiers;
		}
		
		public Class getType() {
			return type;
		}
		
		public String getName() {
			return name;
		}
		
	}

	@SuppressWarnings("rawtypes")
	public static Class getReturnType(Node targetMethod) {
		// TODO: implement
		return void.class;
	}

}
