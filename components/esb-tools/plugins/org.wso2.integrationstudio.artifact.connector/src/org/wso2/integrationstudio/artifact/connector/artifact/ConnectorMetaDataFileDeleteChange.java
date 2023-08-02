/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.integrationstudio.artifact.connector.artifact;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.wso2.integrationstudio.maven.util.MavenUtils;
import org.wso2.integrationstudio.platform.core.utils.Constants;

public class ConnectorMetaDataFileDeleteChange extends TextFileChange {
    private static final String ARTIFACT_XML_FILE = "artifact.xml";

    private IFile metaDataFile;
    private String originalFileName;
    private String version;

    public ConnectorMetaDataFileDeleteChange(String name, IFile file, String originalFileFullName) throws IOException {
        super(name, file);
        this.metaDataFile = file;
        this.originalFileName = originalFileFullName.substring(0, originalFileFullName.lastIndexOf("-"));
        String fileNameWithoutExtension = originalFileFullName.substring(0, originalFileFullName.lastIndexOf("."));
        this.version = fileNameWithoutExtension.substring(fileNameWithoutExtension.lastIndexOf("-") + 1,
                fileNameWithoutExtension.length());

        addTextEdits();
        identyfyAndRemoveChangingItemFromCompositePOM(metaDataFile);
    }
    
    private void identyfyAndRemoveChangingItemFromCompositePOM(IFile changingFile) {
        
        try {
            IFile pomIFile = changingFile.getProject().getFile("pom.xml");
            File pomFile = pomIFile.getLocation().toFile();
            MavenProject mvnProject = MavenUtils.getMavenProject(pomFile);
            String chandingFileGroupId = mvnProject.getGroupId() + ".lib";
            
            IWorkspace workspace = ResourcesPlugin.getPlugin().getWorkspace();
            IProject [] projects = workspace.getRoot().getProjects();
            for (IProject project : projects) {
                if (project.hasNature(Constants.DISTRIBUTION_PROJECT_NATURE)) {
                    IFile compositePOMIFile = project.getFile("pom.xml");
                    File compositePOMFile = compositePOMIFile.getLocation().toFile();
                    MavenProject compositeMvnProject = MavenUtils.getMavenProject(compositePOMFile);
                    List<Dependency> dependencies = compositeMvnProject.getDependencies();
                    int dependencyIndex = -1;
                    for (int x = 0; x < dependencies.size(); x++) {
                        if (dependencies.get(x).getGroupId().equalsIgnoreCase(chandingFileGroupId) 
                                && dependencies.get(x).getArtifactId().equalsIgnoreCase(originalFileName)) {
                            dependencyIndex = x;
                            break;
                        }
                    }
                    if (dependencyIndex != -1) {
                        dependencies.remove(dependencyIndex);
                        compositeMvnProject.getProperties().remove(chandingFileGroupId.concat("_._").concat(originalFileName));
                        compositeMvnProject.setDependencies(dependencies);
                        MavenUtils.saveMavenProject(compositeMvnProject, compositePOMFile);
                    }
                }
            }
        } catch (Exception e) {
            //ignore
        } 
    }

    private void addTextEdits() throws IOException {
        if (metaDataFile.getName().equalsIgnoreCase(ARTIFACT_XML_FILE)) {
            setEdit(new MultiTextEdit());

            String artifactsStart = "<artifacts>";
            String artifactsEnd = "</artifacts>";
            String artifactStart = "<artifact";
            String artifactEnd = "</artifact>";
            String nameProperty = "name=\"";
            String versionProperty = "version=\"";

            List<String> artifactEntry = new ArrayList<String>();
            boolean isArtifact = false;
            boolean isArtifacts = false;
            boolean isArtifactMatch = false;
            boolean isArtifactLine = false;

            int fullIndex = 0;
            int startIndex = 0;
            BufferedReader reader = new BufferedReader(new FileReader(metaDataFile.getLocation().toFile()));

            String line = reader.readLine();
            while (line != null) {
                if (!isArtifacts && line.contains(artifactsStart)) {
                    isArtifacts = true;
                }

                if (isArtifacts && line.contains(artifactsEnd)) {
                    isArtifacts = false;
                }

                if (isArtifacts) {
                    isArtifactLine = false;
                    if (!isArtifact && line.trim().startsWith(artifactStart)) {
                        int artifactTagIndex = line.indexOf(artifactStart);
                        startIndex = fullIndex + artifactTagIndex;
                        if (line.contains(nameProperty + originalFileName + "\"") && line.contains(versionProperty + version + "\"")) {
                            isArtifact = true;
                            artifactEntry.add(line.substring(artifactTagIndex));
                            isArtifactLine = true;
                        } else {
                            isArtifact = false;
                            artifactEntry.clear();
                            startIndex = 0;
                        }
                    }

                    if (isArtifact) {
                        if (!isArtifactLine && !artifactEntry.contains(line)) {
                            artifactEntry.add(line);
                        }
                        if (line.trim().startsWith(artifactEnd)) {
                            isArtifact = false;
                            isArtifactMatch = true;
                        }
                    }

                    if (isArtifactMatch) {
                        int length = 0;
                        for (String string : artifactEntry) {
                            length += charsOnTheLine(string);
                        }
                        addEdit(new DeleteEdit(startIndex, length));
                        break;
                    }

                }

                fullIndex += charsOnTheLine(line);
                line = reader.readLine();
            }
            reader.close();
        }
    }

    private int charsOnTheLine(String line) {
        line += System.lineSeparator();
        return line.length();
    }

}
