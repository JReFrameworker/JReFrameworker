package jreframeworker.common;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class JarUtils {

	public static final String META_INF = "META-INF";

	/**
	 * Generates a Jar Manifest based on the given parameters
	 * @return
	 */
	public static Manifest generateEmptyManifest(){
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		return manifest;
	}
	
	/**
	 * Retrieves a Manifest object or creates an empty Manifest object 
	 * if the Manifest is not found in the given Jar file
	 * @param jarFile
	 * @return
	 * @throws IOException
	 */
	public static Manifest getManifest(File jarFile) throws IOException {
		JarFile jar = new JarFile(jarFile);
		String manifestPath = META_INF + "/MANIFEST.MF";
		JarEntry jarEntry = jar.getJarEntry(manifestPath);
		if (jarEntry != null) {
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				jarEntry = (JarEntry) entries.nextElement();
				if (manifestPath.equalsIgnoreCase(jarEntry.getName())){
					break;
				} else {
					jarEntry = null;
				}
			}
		}
		Manifest manifest = new Manifest();
		if (jarEntry != null){
			manifest.read(jar.getInputStream(jarEntry));
		}
		jar.close();
		return manifest;
	}
	
	public static Manifest getManifestFromFile(File manifestFile) throws IOException {
		Manifest manifest = new Manifest();
		manifest.read(new FileInputStream(manifestFile));
		return manifest;
	}
	
	/**
	 * Extracts a Jar file
	 * 
	 * @param inputJar
	 * @param outputPath
	 * @throws IOException
	 */
	public static void unjar(File inputJar, File outputPath) throws IOException {
		outputPath.mkdirs();
		JarFile jar = new JarFile(inputJar);
		Enumeration<JarEntry> jarEntries = jar.entries();
		while (jarEntries.hasMoreElements()) {
			JarEntry jarEntry = jarEntries.nextElement();
			File file = new File(outputPath.getAbsolutePath() + java.io.File.separator + jarEntry.getName());
			new File(file.getParent()).mkdirs();
			if (jarEntry.isDirectory()) {
				file.mkdir();
				continue;
			}
			InputStream is = jar.getInputStream(jarEntry);
			FileOutputStream fos = new FileOutputStream(file);
			while (is.available() > 0) {
				fos.write(is.read());
			}
			fos.close();
			is.close();
		}
		jar.close();
	}
	
	/**
	 * Creates a Jar file from a directory
	 * 
	 * @param extractedJarPath
	 * @param outputJar
	 * @throws IOException
	 */
	public static void jar(File extractedJarPath, File outputJar, Manifest manifest) throws IOException {
		JarOutputStream target = new JarOutputStream(new FileOutputStream(outputJar), manifest);
		addFileToJar(extractedJarPath, extractedJarPath, target);
		target.close();
	}

	// note this current implementation also updates the timestamps of Jar contents
	private static void addFileToJar(File fileOrDirectoryToAdd, File extractedJarPath, JarOutputStream target) throws IOException {
		String relPath = "";
		if(!fileOrDirectoryToAdd.getAbsolutePath().equals(extractedJarPath.getAbsolutePath())){
			String fileToAddCanonicalPath = fileOrDirectoryToAdd.getCanonicalPath();
			int relStart = extractedJarPath.getCanonicalPath().length() + 1;
			int relEnd = fileOrDirectoryToAdd.getCanonicalPath().length();
			String d = fileToAddCanonicalPath.substring(relStart,relEnd);
			relPath = d.replace("\\", "/");
		}
		BufferedInputStream in = null;
		try {
			if (fileOrDirectoryToAdd.isDirectory()) {
				if (!relPath.isEmpty()) {
					if (!relPath.endsWith("/")){
						relPath += "/";
					}
					JarEntry entry = new JarEntry(relPath);
					entry.setTime(fileOrDirectoryToAdd.lastModified());
					target.putNextEntry(entry);
					target.closeEntry();
				}
				for (File nestedFile : fileOrDirectoryToAdd.listFiles()){
					addFileToJar(nestedFile, extractedJarPath, target);
				}
				return;
			}

			JarEntry entry = new JarEntry(relPath);
			entry.setTime(fileOrDirectoryToAdd.lastModified());
			target.putNextEntry(entry);
			in = new BufferedInputStream(new FileInputStream(fileOrDirectoryToAdd));

			byte[] buffer = new byte[1024];
			while (true) {
				int count = in.read(buffer);
				if (count == -1){
					break;
				}
				target.write(buffer, 0, count);
			}
			target.closeEntry();
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}
	
	/**
	 * From: http://docs.oracle.com/javase/7/docs/technotes/tools/windows/jarsigner.html
	 * When jarsigner is used to sign a JAR file, the output signed JAR file is exactly the 
	 * same as the input JAR file, except that it has two additional files placed in the 
	 * META-INF directory: a signature file, with a .SF extension, and a signature block 
	 * file, with a .DSA, .RSA, or .EC extension.
	 * 
	 * The method deletes the jarsigner signature file and the signature block files.
	 */
	public static void unsign(File extractedJarDirectory){
		File metaInfDirectory = getMetaInfDirectory(extractedJarDirectory);
		File[] files = metaInfDirectory.listFiles();
		if(files != null){
			for(File file : files){
				if(file.getName().endsWith(".SF") 
					|| file.getName().endsWith(".DSA") 
					|| file.getName().endsWith(".RSA") 
					|| file.getName().endsWith(".EC")){
					file.delete();
				}
			}
		}
	}
	
	// TODO: finish implementing
	public static void sign(File jarFile, File keystore) throws IOException {
		
	}
	
	/**
	 * Deletes the entire META-INF directory.  This effectively unsigns a Jar as well.
	 * @throws IOException 
	 */
	public static void purgeMetaInf(File extractedJarDirectory) throws IOException {
		File metaInfDirectory = getMetaInfDirectory(extractedJarDirectory);
		delete(metaInfDirectory);
	}

	// given the root directory of the extracted Jar directory, return the META-INF directory
	private static File getMetaInfDirectory(File extractedJarDirectory) {
		return new File(extractedJarDirectory.getAbsolutePath() + File.separatorChar + META_INF);
	}
	
	// recursively deletes a file or directory
	public static void delete(File f) throws IOException {
		if (f.isDirectory()) {
			for (File c : f.listFiles()) {
				delete(c);
			}
		}
		if (!f.delete()) {
			throw new IOException("Failed to delete file: " + f);
		}
	}

}