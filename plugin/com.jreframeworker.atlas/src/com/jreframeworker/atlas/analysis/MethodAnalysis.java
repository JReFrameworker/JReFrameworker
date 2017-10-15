package com.jreframeworker.atlas.analysis;

import java.util.ArrayList;

import javax.lang.model.element.Modifier;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.query.Attr;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.script.CommonQueries;
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
	
	// TODO: consider ordering
	// TODO: need to consider generics?
	public static Parameter[] getParameters(Node method){
		ArrayList<Parameter> parameters = new ArrayList<Parameter>();
		
		Q parameterNodes = Common.toQ(method).children().nodes(XCSG.Parameter);
		Q typeOfEdges = Common.universe().edges(XCSG.TypeOf);
		
		for(Node parameterNode : parameterNodes.eval().nodes()){
			Node parameterType = typeOfEdges.successors(Common.toQ(parameterNode)).eval().nodes().one();
			
			ArrayList<Modifier> modifiers = new ArrayList<Modifier>();
			if(parameterType.taggedWith(Attr.Node.IS_FINAL)){ // TODO: replace with XCSG equivalent
				modifiers.add(Modifier.FINAL);
			}
			Modifier[] parameterModifiers = new Modifier[modifiers.size()];
			modifiers.toArray(parameterModifiers);

			String parameterName = parameterNode.getAttr(XCSG.name).toString();

			// TODO: test this logic..
			if(parameterType.taggedWith(XCSG.ArrayType)){
				int arrayDimension = Integer.parseInt(parameterType.getAttr(XCSG.Java.arrayTypeDimension).toString());
				Q arrayElementTypeEdges = Common.universe().edges(XCSG.ArrayElementType);
				parameterType = arrayElementTypeEdges.successors(Common.toQ(parameterType)).eval().nodes().one();
				String qualifiedParameterType = getQualifiedType(parameterType);
				parameters.add(new Parameter(parameterModifiers, qualifiedParameterType, parameterName, arrayDimension));
			} else {
				String qualifiedParameterType = getQualifiedType(parameterType);
				parameters.add(new Parameter(parameterModifiers, qualifiedParameterType, parameterName));
			}
		}
		
		Parameter[] result = new Parameter[parameters.size()];
		parameters.toArray(result);
		return result;
	}

	private static String getPrimitiveType(Node parameterType) {
		if(parameterType.getAttr(XCSG.name).toString().equals("byte")){
			return (byte.class).getName();
		} else if(parameterType.getAttr(XCSG.name).toString().equals("char")){
			return (char.class).getName();
		} else if(parameterType.getAttr(XCSG.name).toString().equals("short")){
			return (short.class).getName();
		} else if(parameterType.getAttr(XCSG.name).toString().equals("int")){
			return (int.class).getName();
		} else if(parameterType.getAttr(XCSG.name).toString().equals("long")){
			return (long.class).getName();
		} else if(parameterType.getAttr(XCSG.name).toString().equals("float")){
			return (float.class).getName();
		} else if(parameterType.getAttr(XCSG.name).toString().equals("double")){
			return (double.class).getName();
		} else if(parameterType.getAttr(XCSG.name).toString().equals("boolean")){
			return (boolean.class).getName();
		} else if(parameterType.getAttr(XCSG.name).toString().equals("void")){
			return (void.class).getName();
		}
		throw new IllegalArgumentException("Not a primitive type");
	}
	
	@SuppressWarnings("rawtypes")
	public static class Parameter {
		private Modifier[] modifiers;
		private String type;
		private String name;
		private int arrayDimension = 0;
		private Class primitive = null;
		
		public Parameter(Modifier[] modifiers, String type, String name, int arrayDimension) {
			this(modifiers, type, name);
			if(arrayDimension > 0){
				this.arrayDimension = arrayDimension;
			}
		}
		
		public Parameter(Modifier[] modifiers, String type, String name) {
			this.modifiers = modifiers;
			this.type = type;
			this.name = name;
			if(type.equals((byte.class).getName())){
				this.primitive = byte.class;
			} else if(type.equals((char.class).getName())){
				this.primitive = char.class;
			} else if(type.equals((short.class).getName())){
				this.primitive = short.class;
			} else if(type.equals((int.class).getName())){
				this.primitive = int.class;
			} else if(type.equals((long.class).getName())){
				this.primitive = long.class;
			} else if(type.equals((float.class).getName())){
				this.primitive = float.class;
			} else if(type.equals((double.class).getName())){
				this.primitive = double.class;
			} else if(type.equals((boolean.class).getName())){
				this.primitive = boolean.class;
			} else if(type.equals((void.class).getName())){
				this.primitive = void.class;
			}
		}
		
		public Modifier[] getModifiers() {
			return modifiers;
		}
		
		public String getType() {
			return type;
		}
		
		public String getName() {
			return name;
		}
		
		public boolean isArray(){
			return arrayDimension != 0;
		}
		
		public boolean isPrimitive(){
			return primitive != null;
		}
		
		public Class getPrimitive(){
			return primitive;
		}
		
	}

	@SuppressWarnings("rawtypes")
	public static class Return {
		private String type;
		private int arrayDimension = 0;
		private Class primitive = null;
		
		public Return(String type, int arrayDimension) {
			this(type);
			if(arrayDimension > 0){
				this.arrayDimension = arrayDimension;
			}
		}
		
		public Return(String type) {
			this.type = type;
			if(type.equals((byte.class).getName())){
				this.primitive = byte.class;
			} else if(type.equals((char.class).getName())){
				this.primitive = char.class;
			} else if(type.equals((short.class).getName())){
				this.primitive = short.class;
			} else if(type.equals((int.class).getName())){
				this.primitive = int.class;
			} else if(type.equals((long.class).getName())){
				this.primitive = long.class;
			} else if(type.equals((float.class).getName())){
				this.primitive = float.class;
			} else if(type.equals((double.class).getName())){
				this.primitive = double.class;
			} else if(type.equals((boolean.class).getName())){
				this.primitive = boolean.class;
			} else if(type.equals((void.class).getName())){
				this.primitive = void.class;
			}
		}
		
		public boolean isPrimitive(){
			return primitive != null;
		}
		
		public Class getPrimitive(){
			return primitive;
		}
		
		public String getType() {
			return type;
		}

		public boolean isArray(){
			return arrayDimension != 0;
		}
	}
	
	public static Return getReturnType(Node method) {
		Q returnsEdges = Common.universe().edgesTaggedWithAny(XCSG.Returns).retainEdges();
		Q voidMethods = returnsEdges.predecessors(Common.types("void"));
		if(!CommonQueries.isEmpty(Common.toQ(method).intersection(voidMethods))){
			return new Return((void.class).getName());
		} else {
			// TODO: there should only ever be one right?
			Q returnTypes = returnsEdges.successors(Common.toQ(method));
			Q superTypeEdges = Common.universe().edges(XCSG.Supertype);
			// just being super safe here...might not even be necessary....
			// TODO: error logging if theres more than one
			Node returnType = returnTypes.induce(superTypeEdges).roots().eval().nodes().one();

			// TODO: test this logic..
			if(returnType.taggedWith(XCSG.ArrayType)){
				int arrayDimension = Integer.parseInt(returnType.getAttr(XCSG.Java.arrayTypeDimension).toString());
				Q arrayElementTypeEdges = Common.universe().edges(XCSG.ArrayElementType);
				returnType = arrayElementTypeEdges.successors(Common.toQ(returnType)).eval().nodes().one();
				String qualifiedReturnType = getQualifiedType(returnType);
				return new Return(qualifiedReturnType, arrayDimension);
			} else {
				String qualifiedReturnType = getQualifiedType(returnType);
				return new Return(qualifiedReturnType);
			}
		}
	}

	private static String getQualifiedType(Node returnType) {
		String qualifiedReturnType;
		if(returnType.taggedWith(XCSG.Primitive)){
			qualifiedReturnType = getPrimitiveType(returnType);
		} else {
			qualifiedReturnType = ClassAnalysis.getPackage(returnType) + "." + ClassAnalysis.getName(returnType);
		}
		return qualifiedReturnType;
	}

}
