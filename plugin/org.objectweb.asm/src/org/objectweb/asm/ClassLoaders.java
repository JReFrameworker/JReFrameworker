package org.objectweb.asm;

/**
 * A class used to specify an ordered list of class loaders to be used by ASM
 * 
 * @author Ben Holland
 */
public class ClassLoaders {
	
	 /**
     * A set of ordered class loaders to use when loading class definitions
     */
	private static ClassLoader[] classLoaders = new ClassLoader[]{ ClassLoaders.class.getClassLoader() };
	
	/**
	 * Returns an ordered set of class loaders to be used by ASM when loading class definitions
	 * @return
	 */
	public static ClassLoader[] getClassLoaders(){
		return classLoaders;
	}
	
	/**
	 * Sets an ordered set of class loaders to be used by ASM when loading class definitions
	 * @return
	 */
	public static void setClassLoaders(ClassLoader... classLoaders){
		ClassLoaders.classLoaders = classLoaders;
	}

}
