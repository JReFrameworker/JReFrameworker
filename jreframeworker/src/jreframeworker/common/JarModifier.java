package jreframeworker.common;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarException;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * A wrapper around the Java zip utilities to add, overwrite, or remove files
 * from archives.
 * 
 * @author Ben Holland
 */
public class JarModifier {

	/**
	 * The directory separator character for archive files as a string
	 */
	public static final String SEPERATOR = "/";
	
	/**
	 * The directory that stores the manifest and jar signatures
	 */
	public static final String META_INF = "META-INF";
	
	private HashMap<String,JarEntry> jarEntries = new HashMap<String,JarEntry>();
	private HashMap<String,File> jarEntriesToAdd = new HashMap<String,File>();
	private File jarFile;
	private Manifest manifest;
	
	/**
	 * Creates a new JarModifier with the given archive to be modified
	 * 
	 * @param jarFile The archive to be modified.
	 * 
	 * @throws JarException
	 * @throws IOException
	 */
	public JarModifier(File jarFile) throws JarException, IOException {
		this.jarFile = jarFile;
		JarFile jar = new JarFile(jarFile);
		// get references to all the archive file entries
		Enumeration<? extends JarEntry> enumerator = jar.entries();
		while(enumerator.hasMoreElements()){
			JarEntry currentEntry = (JarEntry) enumerator.nextElement();
			// need to create a new entry to reset properties that will need to be recomputed automatically
//			JarEntry resetEntry = resetEntry(currentEntry); // TODO: Fix
			JarEntry resetEntry = new JarEntry(currentEntry.getName());
			jarEntries.put(currentEntry.getName(), resetEntry);
		}
		
		String manifestPath = META_INF + SEPERATOR + "MANIFEST.MF";
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
		this.manifest = manifest;
		
		jar.close();
	}
	
	public File getJarFile(){
		return jarFile;
	}
	
	public void extractEntry(String entry, File outputFile) throws IOException {
		FileOutputStream fout = new FileOutputStream(outputFile);
		JarInputStream zin = new JarInputStream(new BufferedInputStream(new FileInputStream(jarFile)));
		JarEntry currentEntry = null;
		while ((currentEntry = zin.getNextJarEntry()) != null) {
			if (currentEntry.getName().equals(entry)) {
				byte[] buffer = new byte[1024];
				int len;
				while ((len = zin.read(buffer)) != -1) {
					fout.write(buffer, 0, len);
				}
				fout.close();
				break;
			}
		}
		zin.close();
	}
	
	public Manifest getManifest(){
		return manifest;
	}
	
	public HashSet<String> getJarEntrySet(){
		HashSet<String> entryList = new HashSet<String>();
		entryList.addAll(jarEntries.keySet());
		return entryList;
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
	public void unsign(){
		LinkedList<String> entriesToRemove = new LinkedList<String>();
		for(Entry<String,JarEntry> jarEntry : jarEntries.entrySet()){
			if(jarEntry.getKey().endsWith(".SF") 
					|| jarEntry.getKey().endsWith(".DSA") 
					|| jarEntry.getKey().endsWith(".RSA") 
					|| jarEntry.getKey().endsWith(".EC")){
				entriesToRemove.add(jarEntry.getKey());
			}
		}
		for(String entryToRemove : entriesToRemove){
			jarEntries.remove(entryToRemove);
		}
	}
	
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
	 * Adds (or optionally overwrites) an archive entry
	 * 
	 * @param entry
	 *            The entry path (example a/b/c/test.txt)
	 * @param file
	 *            The contents of the file to add
	 * @param overwrite
	 *            True if an existing entry should be overwritten
	 * @throws IOException
	 *             Thrown if overwrite is false and the archive already contains
	 *             the specified entry
	 */
	public void add(String entry, File file, boolean overwrite) throws IOException {
		JarEntry newEntry = new JarEntry(entry);
		if(jarEntries.containsKey(entry) && !overwrite){
			throw new IOException("Archive already contains entry: " + entry);
		} else {
			 // remove an entry if one already exists
			jarEntries.remove(entry);
			jarEntriesToAdd.remove(entry);
			// add a new entry
			jarEntries.put(entry, newEntry);
			jarEntriesToAdd.put(entry, file);
		}
	}
	
	/**
	 * Adds (or optionally overwrites) an archive entry with the specified entry
	 * properties.  
	 * 
	 * @param entry
	 *            JarEntry with the properties to add or overwrite
	 * @param file
	 *            The contents of the file to add
	 * @param overwrite
	 *            True if an existing entry should be overwritten
	 * @throws IOException
	 *             Thrown if overwrite is false and the archive already contains
	 *             the specified entry
	 */
	public void add(JarEntry entry, File file, boolean overwrite) throws IOException {
//		JarEntry newEntry = resetEntry(entry); // TODO: fix
		JarEntry newEntry = new JarEntry(entry.getName());
		newEntry.setSize(file.length());
		if(jarEntries.containsKey(entry.getName()) && !overwrite){
			throw new IOException("Archive already contains entry: " + entry);
		} else {
			 // remove an entry if one already exists
			jarEntries.remove(entry.getName());
			jarEntriesToAdd.remove(entry.getName());
			// add a new entry
			jarEntries.put(entry.getName(), newEntry);
			jarEntriesToAdd.put(entry.getName(), file);
		}
	}
	
	/**
	 * Removes the specified entry if one exits (example: a/b/c/test.txt)
	 * 
	 * @param entry
	 */
	public void remove(String entry){
		remove(new JarEntry(entry));
	}
	
	/**
	 * Removes the specified entry if one exits (example: a/b/c/test.txt)
	 * 
	 * @param entry
	 */
	public void remove(JarEntry entry){
		jarEntries.remove(entry.getName());
		jarEntriesToAdd.remove(entry.getName());
	}
	
	/**
	 * Removes any entries with the matching file name prefix
	 * 
	 * @param directory
	 */
	public void removeSubdirectory(String directory){
		// clear the entries that may have already existed in the archive
		LinkedList<String> entriesToRemove = new LinkedList<String>();
		for(Entry<String,JarEntry> JarEntry : jarEntries.entrySet()){
			if(JarEntry.getKey().startsWith(directory)){
				entriesToRemove.add(JarEntry.getKey());
			}
		}
		for(String entryToRemove : entriesToRemove){
			jarEntries.remove(entryToRemove);
		}	
	}
	
	/**
	 * Removes any entries with a matching file name (example: test.txt)
	 * 
	 * @param filename The filename to match
	 */
	public void removeFilesWithName(String filename){
		// clear the entries that may have already existed in the archive
		LinkedList<String> entriesToRemove = new LinkedList<String>();
		for(Entry<String,JarEntry> JarEntry : jarEntries.entrySet()){
			if(JarEntry.getKey().endsWith(filename)){
				entriesToRemove.add(JarEntry.getKey());
			}
		}
		for(String entryToRemove : entriesToRemove){
			jarEntries.remove(entryToRemove);
		}
		entriesToRemove.clear();
		
		// clear the entries that may have queued to be added
		for(Entry<String,File> JarEntry : jarEntriesToAdd.entrySet()){
			if(JarEntry.getKey().endsWith(filename)){
				entriesToRemove.add(JarEntry.getKey());
			}
		}
		for(String entryToRemove : entriesToRemove){
			jarEntriesToAdd.remove(entryToRemove);
		}
	}
	
	/**
	 * Writes the modified output archive to a file
	 * 
	 * @param outputArchive
	 * @throws IOException  
	 */
	public void save(File outputArchiveFile) throws IOException {
		JarInputStream zin = null;
	    JarOutputStream zout = null;
	    try {
	    	byte[] buf = new byte[1024];
	    	zin = new JarInputStream(new FileInputStream(jarFile));
		    zout = new JarOutputStream(new FileOutputStream(outputArchiveFile));
	    	JarEntry entry = zin.getNextJarEntry();
		    while (entry != null) {
		        // write the file to the zip depending on where it is located
		    	// entries from files will be added later so skip those now
		        if(jarEntries.containsKey(entry.getName()) && !jarEntriesToAdd.containsKey(entry.getName())){
		            // transfer the bytes from the old archive to the output archive
		        	zout.putNextEntry(jarEntries.get(entry.getName()));
		            int len;
		            while ((len = zin.read(buf)) > 0) {
		                zout.write(buf, 0, len);
		            }
		            // complete the entry
	                zout.closeEntry();
		        }
		        // get the next zip entry to examine
		        entry = zin.getNextJarEntry();
		    }
		    // transfer the bytes from the saved files to the output archive
		    for(Entry<String,File> jarEntryToAdd : jarEntriesToAdd.entrySet()){
		    	String entryName = jarEntryToAdd.getKey();
		    	File file = jarEntryToAdd.getValue();
	        	InputStream fin = null;
	        	try {
	        		fin = new FileInputStream(file);
	        		zout.putNextEntry(jarEntries.get(entryName));
	        		int len;
	                while ((len = fin.read(buf)) > 0) {
	                    zout.write(buf, 0, len);
	                }
	                // complete the entry
	                zout.closeEntry();
	        	} finally {
	        		if(fin != null){
	        			fin.close();
	        		}
	        	}
		    }
	    } finally {
	    	// close the streams        
		    zin.close();
		    zout.close();
	    } 
	}
	
	/**
	 * Prints the contents of the archive file if it were written to disk
	 */
	@Override
	public String toString(){
		StringBuilder result = new StringBuilder();
		
		// sort the entries
		ArrayList<String> allEntries = new ArrayList<String>(jarEntries.keySet().size());
		allEntries.addAll(jarEntries.keySet());
		Collections.sort(allEntries);
		
		for(String entry : allEntries) {
			result.append(entry);
			result.append(" [");
			if(jarEntriesToAdd.containsKey(entry)){
				result.append(jarEntriesToAdd.get(entry).getAbsolutePath());
			} else {
				result.append(jarFile.getAbsolutePath());
			}
			result.append("]\n");
		}
		
		return result.toString();
	}
	
	// TODO: fix this...
//	/**
//	 * Resets a zip entry. Copies over the time, comments, extras, and compression method.
//	 * 
//	 * File sizes and other properties are left to be recomputed automatically.
//	 * 
//	 * @param entry
//	 * @return
//	 */
//	private JarEntry resetEntry(JarEntry entry) {
//		JarEntry resetEntry = new JarEntry(entry.getName());
//		// copy over entry properties
//		resetEntry.setTime(entry.getTime());
//		resetEntry.setComment(entry.getComment());
//		resetEntry.setExtra(entry.getExtra());
//		resetEntry.setMethod(entry.getMethod());
//		return resetEntry;
//	}
	
}
