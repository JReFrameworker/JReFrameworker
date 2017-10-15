package com.jreframeworker.atlas.codegen;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.script.CommonQueries;
import com.jreframeworker.core.JReFrameworkerProject;

public abstract class CodeGenerator {

	private static String SUPPORTS_NOTHING = "SUPPORTS_NOTHING";
	private static String SUPPORTS_EVERYTHING = "SUPPORTS_EVERYTHING";
	
	protected static String[] EVERYTHING = { SUPPORTS_EVERYTHING };
	protected static String[] NOTHING = { SUPPORTS_NOTHING };
	
	/**
	 * Returns the name of the code generator
	 * @return
	 */
	public abstract String getName();
	
	/**
	 * Returns the code generator category
	 * @return
	 */
	public abstract String getCategory();
	
	/**
	 * Returns a description of the code generator
	 * @return
	 */
	public abstract String getDescription();
	
	/**
	 * The set of supported node tags that this filter can operate on
	 * @return
	 */
	protected abstract String[] getSupportedNodeTags();
	
	/**
	 * The set of supported edge tags that this filter can operate on
	 * @return
	 */
	protected abstract String[] getSupportedEdgeTags();
	
	/**
	 * Generates code for the given input
	 * Returns the set of generate source code files
	 * @param input
	 */
	public abstract Set<File> generateCode(JReFrameworkerProject jrefProject, Q input);
	
	/**
	 * Returns true if the input contains supported edges or nodes
	 * @param input
	 * @return
	 */
	public boolean isApplicableTo(Q input){
		return !CommonQueries.isEmpty(getSupportedInput(input));
	}
	
	/**
	 * Returns the supported edges and nodes
	 * @param input
	 * @return
	 */
	public Q getSupportedInput(Q input){
		String[] supportedEdgeTags = getSupportedEdgeTags();
		ArrayList<String> edgeTagsToKeep = new ArrayList<String>();
		if(supportedEdgeTags != null){
			for(String tag : supportedEdgeTags){
				if(tag.equals(SUPPORTS_EVERYTHING)){
					edgeTagsToKeep.clear();
					break;
				} else if(tag.equals(SUPPORTS_NOTHING)){
					input = input.retainNodes();
				} else {
					edgeTagsToKeep.add(tag);
				}
			}
			if(!edgeTagsToKeep.isEmpty()){
				String[] tags = new String[edgeTagsToKeep.size()];
				edgeTagsToKeep.toArray(tags);
				Q edgesWithTags = input.edgesTaggedWithAny(tags);
				Q edgesWithoutTags = input.difference(edgesWithTags);
				input = input.difference(edgesWithoutTags);
			}
		}
		
		String[] supportedNodeTags = getSupportedNodeTags();
		ArrayList<String> nodeTagsToKeep = new ArrayList<String>();
		if(supportedNodeTags != null){
			for(String tag : supportedNodeTags){
				if(tag.equals(SUPPORTS_EVERYTHING)){
					nodeTagsToKeep.clear();
					break;
				} else if(tag.equals(SUPPORTS_NOTHING)){
					input = input.retainEdges();
				} else {
					nodeTagsToKeep.add(tag);
				}
			}
			if(!nodeTagsToKeep.isEmpty()){
				String[] tags = new String[nodeTagsToKeep.size()];
				nodeTagsToKeep.toArray(tags);
				Q nodesWithTags = input.nodesTaggedWithAny(tags);
				Q nodesWithoutTags = input.difference(nodesWithTags);
				input = input.difference(nodesWithoutTags);
			}
		}
		
		return input;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CodeGenerator other = (CodeGenerator) obj;
		if (getName() == null) {
			if (other.getName() != null)
				return false;
		} else if (!getName().equals(other.getName()))
			return false;
		return true;
	}
}
