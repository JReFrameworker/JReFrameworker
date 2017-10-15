package com.jreframeworker.atlas.projects;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.xml.sax.SAXException;

import com.jreframeworker.builder.JReFrameworkerNature;
import com.jreframeworker.core.BuildFile;
import com.jreframeworker.core.JReFrameworker;
import com.jreframeworker.core.JReFrameworkerProject;

public class JREF {

	/**
	 * Creates or opens a JReFrameworker project
	 * @param projectName
	 * @return
	 * @throws CoreException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws TransformerException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public static JReFrameworkerAtlasProject create(String projectName) throws CoreException, IOException, URISyntaxException, TransformerException, ParserConfigurationException, SAXException {
		try {
			return open(projectName);
		} catch (Exception e) {
			BuildFile.Target[] targets = new BuildFile.Target[]{};
			IStatus status = JReFrameworker.createProject(projectName, ResourcesPlugin.getWorkspace().getRoot().getLocation(), new NullProgressMonitor(), targets);
			if(status.isOK()){
				return open(projectName);
			} else {
				throw new IllegalArgumentException("Project could not be created.");
			}
		}
	}
	
	/**
	 * Opens a JReFrameworker project
	 * @param projectName
	 * @return
	 * @throws CoreException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws TransformerException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public static JReFrameworkerAtlasProject open(String projectName) throws CoreException, IOException, URISyntaxException, TransformerException, ParserConfigurationException, SAXException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if(project.exists()){
			try {
				// open project if it is closed
				if(!project.isOpen()){
					project.open(new NullProgressMonitor());
				}
				// if the project is a JReFrameworker project
				if(project.hasNature(JReFrameworkerNature.NATURE_ID) && project.hasNature(JavaCore.NATURE_ID)){
					return new JReFrameworkerAtlasProject(new JReFrameworkerProject(project));
				} else {
					throw new IllegalArgumentException("Project is not a valid JReFrameworker project.");
				}
			} catch (CoreException e) {
				throw new IllegalArgumentException("Error opening project.");
			}
		} else {
			throw new IllegalArgumentException("Project does not exist.");
		}
	}
	
}
