/*
 * Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.integrationstudio.artifact.synapse.api.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.json.simple.JSONValue;
import org.wso2.integrationstudio.artifact.synapse.api.Activator;
import org.wso2.integrationstudio.artifact.synapse.api.util.ArtifactConstants;
import org.wso2.integrationstudio.esb.core.utils.SynapseEntryType;
import org.wso2.integrationstudio.esb.core.utils.SynapseFileUtils;
import org.wso2.integrationstudio.esb.project.utils.ESBProjectUtils;
import org.wso2.integrationstudio.general.project.utils.GeneralProjectUtils;
import org.wso2.integrationstudio.gmf.esb.APIVersionType;
import org.wso2.integrationstudio.logging.core.IIntegrationStudioLog;
import org.wso2.integrationstudio.logging.core.Logger;
import org.wso2.integrationstudio.platform.core.exception.ObserverFailedException;
import org.wso2.integrationstudio.platform.core.project.model.ProjectDataModel;
import org.yaml.snakeyaml.Yaml;

import static org.wso2.integrationstudio.platform.core.utils.Constants.*;

/**
 * The model class for API artifact wizard specific objects.
 */
public class APIArtifactModel extends ProjectDataModel {
	private static IIntegrationStudioLog log=Logger.getLog(Activator.PLUGIN_ID);
	
	private String name;
	private String context;
	private String hostname;
	private int port;
	private String versionType = APIVersionType.NONE.getLiteral();
	private String version = "";
	private IContainer saveLocation;
	private IContainer swaggerRegistryLocation;
	private List<OMElement> availableAPIslist;
	private List<OMElement> selectedAPIsList;
	private String publishSwagger = "";
	private File importSwaggerFile;
	private String swaggerAPIName = "";

	private File apiWSDLFile;
    private String apiWSDLurl;
    private String apiWSDLType;
	
	public APIArtifactModel() {
		availableAPIslist = new ArrayList<OMElement>();
		selectedAPIsList = new ArrayList<OMElement>();
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getVersionType() {
		return versionType;
	}

	public void setVersionType(String versionType) {
		this.versionType = versionType;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public IContainer getSaveLocation() {
		return saveLocation;
	}

	public void setSaveLocation(IContainer saveLocation) {
		this.saveLocation = saveLocation;
	}
	
	public String getPublishSwagger() {
		return publishSwagger;
	}

	public void setPublishSwagger(String publishSwagger) {
		this.publishSwagger = publishSwagger;
	}
	
	public File getSwaggerFile() {
		return importSwaggerFile;
	}

	public void setSwaggerFile(File importedSwaggerFile) {
		this.importSwaggerFile = importedSwaggerFile;
	}
	
	public IContainer getSwaggerRegistryLocation() {
		return swaggerRegistryLocation;
	}
	
	public void setSwaggerRegistryLocation(IContainer swaggerRegistryLocation) {
		this.swaggerRegistryLocation = swaggerRegistryLocation;
	}
	
	public String getSwaggerAPIName() {
        return swaggerAPIName;
    }

    public void setSwaggerAPIName(String swaggerAPIName) {
        this.swaggerAPIName = swaggerAPIName;
    }

    @Override
	public Object getModelPropertyValue(String key) {
		Object modelPropertyValue = super.getModelPropertyValue(key);
		if (modelPropertyValue == null) {
			if (key.equals(ArtifactConstants.ID_API_NAME)) {
				modelPropertyValue = getName();
			} else if (key.equals(ArtifactConstants.ID_API_CONTEXT)) {
				modelPropertyValue = getContext();
			} else if (key.equals(ArtifactConstants.ID_API_PUBLISHSWAGGER)) {
				modelPropertyValue = getPublishSwagger();
			} else if (key.equals(ArtifactConstants.ID_API_HOSTNAME)) {
				modelPropertyValue = getHostname();
			} else if (key.equals(ArtifactConstants.ID_API_PORT)) {
				modelPropertyValue = (getPort() != 0) ? getPort() : null;
			} else if (key.equals(ArtifactConstants.ID_SAVE_LOCATION)) {
				IContainer container = getSaveLocation();
				if (container != null && container instanceof IFolder) {
					IFolder apiFolder = container.getProject().getFolder("src").getFolder("main")
							.getFolder("synapse-config").getFolder("api");
					modelPropertyValue = apiFolder;
				} else {
					modelPropertyValue = container;
				}
			} else if (key.equals("available.apis")) {
				if (selectedAPIsList != null) {
					modelPropertyValue = selectedAPIsList.toArray();
				}
			} else if (key.equals(ArtifactConstants.ID_API_VERSIONTYPE)) {
				modelPropertyValue = getVersionType();
			} else if (key.equals(ArtifactConstants.ID_API_VERSION)) {
				modelPropertyValue = getVersion();
			} else if (key.equals(ArtifactConstants.ID_SWAGGER_FILE_REGISTRY_LOCATION)) {
				modelPropertyValue = getSwaggerRegistryLocation();
			}  else if (key.equals(ArtifactConstants.ID_SWAGGER_API_NAME)) {
                modelPropertyValue = getSwaggerAPIName();
            }
		}
	 
		return modelPropertyValue;
	}
	
	@Override
	public boolean setModelPropertyValue(String key, Object data) throws ObserverFailedException {
		boolean result = super.setModelPropertyValue(key, data);
		if (key.equals(ArtifactConstants.ID_API_NAME)) {
			setName(data.toString());
		}if (key.equals("import.file")) {
			if (getImportFile() != null && !getImportFile().toString().equals("")) {
				try {
					List<OMElement> availableAPIs = new ArrayList<OMElement>();
					if (SynapseFileUtils.isSynapseConfGiven(getImportFile(),
					                                        SynapseEntryType.API)) {
						availableAPIs =
						        SynapseFileUtils.synapseFileProcessing(getImportFile().getPath(),
						                                               SynapseEntryType.API);
						setAvailableAPIslist(availableAPIs);
					} else {
						setAvailableAPIslist(new ArrayList<OMElement>());
					}
					result = false;
				} catch (OMException e) {
					log.error("Error reading object model", e);
				} catch (XMLStreamException e) {
					log.error("XML stream error", e);
				} catch (IOException e) {
					log.error("I/O error has occurred", e);
				} catch (Exception e) {
					log.error("An unexpected error has occurred", e);
				}
			}
		} else if(key.equals("available.apis")){
			selectedAPIsList.clear();
			Object[] selectedAPIs = (Object[])data;
			for (Object object : selectedAPIs) {
				if(object instanceof OMElement){
					if(!selectedAPIsList.contains((OMElement)object)){
						selectedAPIsList.add((OMElement)object);
					}
				}
			}
		} else if (key.equals(ArtifactConstants.ID_API_CONTEXT)) {
			setContext(data.toString());
		} else if (key.equals(ArtifactConstants.ID_API_PUBLISHSWAGGER)) {
			setPublishSwagger(data.toString());
		} else if (key.equals(ArtifactConstants.ID_API_HOSTNAME)) {
			setHostname(data.toString());
		} else if (key.equals(ArtifactConstants.ID_API_PORT)) {
			try {
				setPort(Integer.parseInt(data.toString()));
			} catch (NumberFormatException e) {
				/*ignore*/
			}
		} else if (key.equals(ArtifactConstants.ID_SAVE_LOCATION)) {
			IContainer container=(IContainer) data;
			if(container != null && container instanceof IFolder){
				IFolder apiFolder = container.getProject().getFolder("src").getFolder("main").getFolder("synapse-config").getFolder("api");
				setSaveLocation(apiFolder);
			}else{
				setSaveLocation(container);
			}
		} else if (key.contains(ArtifactConstants.ID_CREATE_PRJ)){
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			IProject esbProject = ESBProjectUtils.createESBProject(shell,getLocation());
			if(esbProject!=null){
				setSaveLocation(esbProject);
			}
		} else if (key.contains(ArtifactConstants.ID_CREATE_REG_PRJ)) {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			IProject regProject = GeneralProjectUtils.createGeneralProject(shell, getLocation());
			if (regProject != null) {
				setSwaggerRegistryLocation(regProject);
			}
		} else if (key.equals(ArtifactConstants.ID_API_VERSIONTYPE)) {
			setVersionType(data.toString());
		} else if (key.equals(ArtifactConstants.ID_API_VERSION)) {
			setVersion(data.toString());
		} else if (key.equals(ArtifactConstants.ID_API_SWAGGER_FILE)) {
			setSwaggerFile((File) data);
			setSwaggerAPIName(readAPINameFromSwaggerFile((File) data));
		} else if (key.equals(ArtifactConstants.ID_SWAGGER_FILE_REGISTRY_LOCATION)) {
			setSwaggerRegistryLocation((IProject)data);
		} else if (key.equals(ArtifactConstants.ID_SWAGGER_API_NAME)) {
            setSwaggerAPIName(data.toString());
        }  else if (key.equals(ArtifactConstants.API_WSDL_FILE)) {
            if (data instanceof File && ((File) data).exists()) {
                setAPIWSDLFile((File) data);
            }

        } else if (key.equals(ArtifactConstants.API_WSDL_URL)) {
            String url = (String) data;
            if (StringUtils.isNotBlank(url)) {
                setAPIWSDLurl(url);
            }

        } else if (key.equals(ArtifactConstants.API_WSDL_TYPE)) {
            if (data instanceof String) {
                setAPIWSDLType((String) data);
            }
        }
		
		return result;
	}
	
	@Override
	public void setLocation(File location) {
		super.setLocation(location);
		File absolutionPath = getLocation();
		if (getSaveLocation() == null && absolutionPath != null) {
			IContainer newSaveLocation =
			        getContainer(absolutionPath, ESB_PROJECT_NATURE);
			setSaveLocation(newSaveLocation);
		}
	}
	
	public static IContainer getContainer(File absolutionPath, String projectNature) {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		int length = 0;
		IProject currentSelection = null;
		for (IProject project : projects) {
			try {
				if (project.isOpen() && project.hasNature(projectNature)) {
					File projectLocation = project.getLocation().toFile();
					int projectLocationLength = projectLocation.toString().length();
					if (projectLocationLength > length &&
					    projectLocationLength <= absolutionPath.toString().length()) {
						if (absolutionPath.toString().startsWith(projectLocation.toString())) {
							length = projectLocationLength;
							currentSelection = project;
						}
					}
				}
			} catch (CoreException e) {
				log.error("An unexpected error has occurred", e);
			}
		}
		IContainer newAPIsSaveLocation = null;
		if (currentSelection != null) {
			String path =
			        absolutionPath.toString().substring(
			                                            currentSelection.getLocation().toFile()
			                                                    .toString().length());

			if (path.equals("")) {
				newAPIsSaveLocation = currentSelection;
			} else {
				newAPIsSaveLocation = currentSelection.getFolder(path);
			}
		}
		return newAPIsSaveLocation;
	}
	
	private String readAPINameFromSwaggerFile(File swaggerFile) {
	    if (swaggerFile.exists()){
            try {
                String content = FileUtils.readFileToString(swaggerFile, "UTF-8").trim();
                JSONObject jsonObject;
                String fileExtension = FilenameUtils.getExtension((swaggerFile).getAbsolutePath());
                if (fileExtension.equals("yaml") || fileExtension.equals("yml")) {
                    content = convertYamlToJson(content);
                    jsonObject = new JSONObject(content);
                } else {
                    jsonObject = new JSONObject(content);
                }
                return jsonObject.getJSONObject("info").getString("title").replaceAll("[^a-zA-Z0-9_-]+", "");
            } catch (FileNotFoundException er) {
                log.error("Exception while reading given swagger file", er);
            } catch (JSONException e) {
                log.error("Exception while reading given swagger file", e);
            } catch (IOException e) {
                log.error("Exception while reading given swagger file", e);
            } catch (Exception e) {
                log.error("Exception while reading given swagger file", e);
            }
        }
	    
	    return "";
	}
	
	/**
     * Converts a given YAML content to JSON.
     * 
     * @param yamlSource yaml content
     * @return converted JSON content
     */
    public static String convertYamlToJson(String yamlSource) {
        Yaml yaml = new Yaml();
        Object object = yaml.load(yamlSource);
        return JSONValue.toJSONString(object);
    }
	
	public void setAvailableAPIslist(List<OMElement> availableAPIslist) {
		this.availableAPIslist = availableAPIslist;
	}
	public List<OMElement> getAvailableAPIslist() {
		return availableAPIslist;
	}
	public void setSelectedAPIsList(List<OMElement> selectedAPIsList) {
		this.selectedAPIsList = selectedAPIsList;
	}
	public List<OMElement> getSelectedAPIsList() {
		return selectedAPIsList;
	}


    public File getAPIWSDLFile() {
        return apiWSDLFile;
    }

    public void setAPIWSDLFile(File wsdlFile) {
        this.apiWSDLFile = wsdlFile;
    }

    public String getAPIWSDLurl() {
        return apiWSDLurl;
    }

    public void setAPIWSDLurl(String wsdlUrl) {
        this.apiWSDLurl = wsdlUrl;
    }

    public String getAPIWSDLType() {
        return apiWSDLType;
    }

    public void setAPIWSDLType(String wsdlType) {
        this.apiWSDLType = wsdlType;
    }

}
