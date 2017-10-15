package com.jreframeworker.engine.log;

public class Log {
	
	public static void error(String message, Throwable e) {
		log("Error", message, e);
	}
	
	public static void warning(String message) {
		warning(message, null);
	}
	
	public static void warning(String message, Throwable e) {
		log("Warning", message, e);
	}
	
	public static void info(String message) {
		info(message, null);
	}
	
	public static void info(String message, Throwable e) {
		log("Info", message, e);
	}
	
	public static void log(String severity, String string, Throwable e) {
		// TODO: enable logging after the metasploit module is made more robust
//		if(e != null){
//			System.out.println(severity + ": " + string);
//			e.printStackTrace();
//		} else {
//			System.out.println(severity + ": " + string);
//		}
	}
}
