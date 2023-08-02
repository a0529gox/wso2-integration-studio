/*
 * Copyright (c) 2012-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.integrationstudio.esb.project.refactoring.delete;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.DeleteParticipant;
import org.wso2.integrationstudio.esb.core.utils.SynapseUtils;

public class ESBArtifactMetaDataDeleteParticipant extends DeleteParticipant {
	
	
	private static final String UPDATE_ESB_META_DATA_MODEL_STATUS_MESSAGE = "Update ESB meta-data model";
	private static final String ARTIFACT_XML_FILE = "artifact.xml";
	private static final String ESB_APRTIFACT_DELETE_CHANGE_OBJECT_NAME = "ESB Artifact Delete";
	private static final String UPDATE_ARTIFACT_XML_CHANGE_OBJECT_NAME = "Update arifact.xml";
	private static final String SYNAPSE_CONFIG_DIR_API = "api";
	private static final String SYNAPSE_CONFIG_DIR_PROXY_SERVICE = "proxy-services";
	private IFile originalFile;
	private static int numOfFiles;
	private static int currentFileNum;
	private static Map<IProject, List<IFile>> changeFileList;
	private static List<IProject> projectList;

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor progressMonitor,
			CheckConditionsContext context) throws OperationCanceledException {
		return RefactoringStatus
				.createInfoStatus(UPDATE_ESB_META_DATA_MODEL_STATUS_MESSAGE);
	}

	@Override
	public Change createChange(IProgressMonitor progressMonitor)
			throws CoreException, OperationCanceledException {
		CompositeChange emptychange = new CompositeChange(
				ESB_APRTIFACT_DELETE_CHANGE_OBJECT_NAME);
		currentFileNum++;
		if (numOfFiles == currentFileNum) {
			CompositeChange change = new CompositeChange(
					UPDATE_ARTIFACT_XML_CHANGE_OBJECT_NAME);
			for (IProject project : projectList) {
			    IContainer buildArtifactsLocation = project.getFolder("build-artifacts");
				List<IFile> fileList = changeFileList.get(project);
				List<IFile> metaFileList = new ArrayList<>();
                for (IFile file : fileList) {
                     IPath location = file.getLocation();
                     String[] locationSegments = location.segments();
                     if(locationSegments[location.segmentCount()-2].equals(SYNAPSE_CONFIG_DIR_API)) {
                         String APIXML = locationSegments[location.segmentCount()-1];
                         String APIName = APIXML.substring(0, APIXML.length() - 4);
                         IContainer metadataLocation = project.getFolder("src/main/resources/metadata");
                         IFile APIMetaFile = metadataLocation.getFile(new Path(APIName+"_metadata.yaml"));
                         IFile swaggerFile = metadataLocation.getFile(new Path(APIName+"_swagger.yaml"));
                         IFolder metadataBuildArtifactFile = buildArtifactsLocation.getFolder(
								 new Path("metadata" + File.separator + APIName + "_metadata"));
                         IFolder swaggerBuildArtifactFile = buildArtifactsLocation.getFolder(
								 new Path("metadata" + File.separator + APIName + "_swagger"));
                         metaFileList.add(APIMetaFile);
                         metaFileList.add(swaggerFile);
                         if(APIMetaFile.exists()) {
                             APIMetaFile.delete(true, null);
                         }
                         if (metadataBuildArtifactFile.exists()) {
							 deleteMetadataBuildArtifacts(APIName + "_metadata", buildArtifactsLocation);
                         }
                         if(swaggerFile.exists()) {
                             swaggerFile.delete(true, null);
                         }
                         if (swaggerBuildArtifactFile.exists()) {
							 deleteMetadataBuildArtifacts(APIName + "_swagger", buildArtifactsLocation);
                         }
                     } else if (locationSegments[location.segmentCount() - 2].equals(SYNAPSE_CONFIG_DIR_PROXY_SERVICE)) {
                         String proxyXML = locationSegments[location.segmentCount()-1];
                         String proxyName = proxyXML.substring(0, proxyXML.length() - 4);
                         IContainer metadataLocation = project.getFolder("src/main/resources/metadata");
                         IFile proxyMetaFile = metadataLocation.getFile(new Path(proxyName + "_proxy_metadata.yaml"));
                         IFolder metadataBuildArtifactFile = buildArtifactsLocation.getFolder(
								 new Path("metadata" + File.separator + proxyName + "_proxy_metadata"));
                         metaFileList.add(proxyMetaFile);
                         if(proxyMetaFile.exists()) {
                             proxyMetaFile.delete(true, null);
                         }
                         if (metadataBuildArtifactFile.exists()) {
							 deleteMetadataBuildArtifacts(proxyName + "_proxy_metadata", buildArtifactsLocation);
                         }
                     }
                 }
                
				deleteBuildArtifacts(fileList, buildArtifactsLocation);
                
                // adding to fileList to remove entries from artifact.xml file 
                for (IFile file : metaFileList) {
                    fileList.add(file);
                }
				change.add(new ESBMetaDataFileDeleteChange(project.getName(),
						project.getFile(ARTIFACT_XML_FILE), fileList));
			}
			resetStaticVariables();
			return change;
		}
		return emptychange;
	}

	private void deleteBuildArtifacts(List<IFile> fileList, IContainer buildArtifactsLocation) {
		String synapseConfigDirectory = "";
		for (IFile iFile : fileList) {
			IPath location = originalFile.getLocation();
			String[] locationSegments = location.segments();
			synapseConfigDirectory = locationSegments[location.segmentCount()-2];
			String originalFileFullName = iFile.getName();
			try {
				String originalFileName = originalFileFullName.substring(0, originalFileFullName.lastIndexOf("."));
                SynapseUtils.removeESBConfigBuildArtifacts(buildArtifactsLocation, synapseConfigDirectory,
						originalFileName);
			} catch (CoreException | IOException | XmlPullParserException e) {
                throw new OperationCanceledException("Error while deleting the build artifacts for "
						+ originalFileFullName + " " + synapseConfigDirectory + "synapse config");
            }
		}
	}

	private void deleteMetadataBuildArtifacts(String metadataFolderName, IContainer buildArtifactsLocation) {
		try {
            SynapseUtils.removeESBConfigBuildArtifacts(buildArtifactsLocation, "metadata", metadataFolderName);
        } catch (CoreException | IOException | XmlPullParserException e) {
            throw new OperationCanceledException("Error while deleting the build artifacts for "
                    + metadataFolderName + "metadata.");
        }
	}

	private void resetStaticVariables() {
		changeFileList.clear();
		projectList.clear();
		numOfFiles = 0;
		currentFileNum = 0;
	}

	@Override
	public String getName() {
		return "ESBArtifactDelete";
	}

	@Override
	protected boolean initialize(Object file) {
		if (file instanceof IFile) {
			numOfFiles++;
			originalFile = (IFile) file;
			if (numOfFiles == 1) {
				List<IFile> fileList = new ArrayList<>();
				projectList = new ArrayList<>();
				changeFileList = new HashMap<IProject, List<IFile>>();
				fileList.add(originalFile);
				projectList.add(originalFile.getProject());
				changeFileList.put(originalFile.getProject(), fileList);
			} else {
				if (changeFileList.containsKey(originalFile.getProject())) {
					changeFileList.get(originalFile.getProject()).add(
							originalFile);
				} else {
					List<IFile> fileList = new ArrayList<>();
					fileList.add(originalFile);
					projectList.add(originalFile.getProject());
					changeFileList.put(originalFile.getProject(), fileList);
				}
			}
			return true;
		}
		return false;
	}

}
