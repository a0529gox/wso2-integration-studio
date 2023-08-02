/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.integrationstudio.artifact.connector.ui.wizard;

import java.io.File;
import java.io.IOException;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.wso2.integrationstudio.artifact.connector.Activator;
import org.wso2.integrationstudio.artifact.connector.artifact.ConnectorProjectArtifact;
import org.wso2.integrationstudio.artifact.connector.model.ConnectorModel;
import org.wso2.integrationstudio.artifact.connector.utils.ConnectorImageUtils;
import org.wso2.integrationstudio.esb.core.utils.SynapseConstants;
import org.wso2.integrationstudio.esb.core.utils.SynapseUtils;
import org.wso2.integrationstudio.logging.core.IIntegrationStudioLog;
import org.wso2.integrationstudio.logging.core.Logger;
import org.wso2.integrationstudio.maven.util.MavenUtils;
import org.wso2.integrationstudio.platform.core.model.MavenInfo;
import org.wso2.integrationstudio.platform.ui.wizard.AbstractWSO2ProjectCreationWizard;
import org.wso2.integrationstudio.platform.ui.wizard.pages.ProjectOptionsDataPage;
import org.wso2.integrationstudio.utils.project.ProjectUtils;

public class ConnectorCreationWizard extends AbstractWSO2ProjectCreationWizard {

	private static final String PROJECT_WIZARD_WINDOW_TITLE = "New Connector Exporter Project";
	private static final String CONNECTOR_PROJECT_NATURE = "org.wso2.developerstudio.eclipse.artifact.connector.project.nature";
	private ConnectorModel customMediatorModel;
	private IProject project;
	private File pomfile;
	private static IIntegrationStudioLog log = Logger.getLog(Activator.PLUGIN_ID);

	public ConnectorCreationWizard() {
		setCustomMediatorModel(new ConnectorModel());
		setModel(customMediatorModel);
		setWindowTitle(PROJECT_WIZARD_WINDOW_TITLE);
		setDefaultPageImageDescriptor(ConnectorImageUtils.getInstance().getImageDescriptor("new-mediator-wizard.png"));
	}

	public IWizardPage getNextPage(IWizardPage page) {
		IWizardPage nextPage = super.getNextPage(page);
		if (page instanceof ProjectOptionsDataPage) {
			if (getModel().getSelectedOption().equalsIgnoreCase("import.mediator")) {
				IProject temp = customMediatorModel.getMediatorProject();
				File pomfile = temp.getFile("pom.xml").getLocation().toFile();
				if (!pomfile.exists()) {
					try {
						getModel().setMavenInfo(
								new MavenInfo("org.wso2.carbon." + temp.getName(), temp.getName(), "1.0.0"));
						getModel().setProjectName(temp.getName());
					} catch (Exception e) {
						log.error("project update fail", e);
					}
				} else {
					nextPage = null;
				}
			}
		}
		return nextPage;
	}

	public boolean performFinish() {
		try {
//			setProjectNature(CONNECTOR_PROJECT_NATURE);
			project = createNewProject();
			pomfile = project.getFile("pom.xml").getLocation().toFile();
			createPOM(pomfile, "pom");
			updatePom();
			IFolder buildArtifactsFolder = ProjectUtils.getWorkspaceFolder(project, SynapseConstants.BUILD_ARTIFACTS_FOLDER);
			ProjectUtils.createFolder(buildArtifactsFolder);
			SynapseUtils.createBuildArtifactsModulePom(project, buildArtifactsFolder, "../pom.xml");
			ProjectUtils.addNatureToProject(project, false, CONNECTOR_PROJECT_NATURE);
			MavenUtils
					.updateWithMavenEclipsePlugin(pomfile, new String[] {}, new String[] { CONNECTOR_PROJECT_NATURE });

			// Creating the metadata file artifact.xml while creating the Connector project. It will be hidden and users
			// won't be able to see it via Eclipse.
			ConnectorProjectArtifact artifact = new ConnectorProjectArtifact();
			IFile file = project.getFile("artifact.xml");
			artifact.setSource(file.getLocation().toFile());
			artifact.toFile();
			getModel().addToWorkingSet(project);
			// Refresh the project to show the changes. But still won't see the newly created project.
			project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			refreshDistProjects();
			// Setting the created file to be hidden so that users won't see it.
//			if (file.exists()) {
//				file.setHidden(true);
//			}
		} catch (Exception e) {
			MessageDialog.openError(getShell(), "Error while creating the project", e.getMessage());
			return false;

		}
		return true;
	}


	public void updatePom() throws IOException, XmlPullParserException {
		File mavenProjectPomLocation = project.getFile("pom.xml").getLocation().toFile();
		MavenProject mavenProject = MavenUtils.getMavenProject(mavenProjectPomLocation);
		mavenProject.getModel().getProperties().put("CApp.type", "synapse/lib");
		MavenUtils.saveMavenProject(mavenProject, mavenProjectPomLocation);
	}

	public void setCustomMediatorModel(ConnectorModel customMediatorModel) {
		this.customMediatorModel = customMediatorModel;
	}

	public ConnectorModel getCustomMediatorModel() {
		return customMediatorModel;
	}

	public IResource getCreatedResource() {
		return project;
	}
}
