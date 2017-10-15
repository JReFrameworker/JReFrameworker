package com.jreframeworker.atlas.analysis;

import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.script.Common;
import com.ensoftcorp.atlas.core.xcsg.XCSG;

public class ClassAnalysis {

	/**
	 * Returns true if the given class is marked final
	 * @param clazz
	 * @return
	 */
	public static boolean isFinal(Node clazz){
		return clazz.taggedWith(XCSG.Java.finalClass);
	}
	
	/**
	 * Returns true if the given class is marked public
	 * @param clazz
	 * @return
	 */
	public static boolean isPublic(Node clazz){
		return clazz.taggedWith(XCSG.publicVisibility);
	}
	
	/**
	 * Returns the name of the given class
	 * @param clazz
	 * @return
	 */
	public static String getName(Node clazz){
		return clazz.getAttr(XCSG.name).toString();
	}
	
	/**
	 * Returns the package name that contains the given class 
	 * @param clazz
	 * @return
	 */
	public static String getPackage(Node clazz){
		AtlasSet<Node> pkgs = Common.toQ(clazz).containers().nodesTaggedWithAny(XCSG.Package).eval().nodes();
		if(pkgs.isEmpty()){
			throw new IllegalArgumentException("Class is not contained in a package!");
		} else if(pkgs.size() > 1){
			throw new IllegalArgumentException("Class is not contained in multiple packages!");
		} else {
			return pkgs.one().getAttr(XCSG.name).toString();
		}
	}
	
}
