package org.wso2.integrationstudio.gmf.esb.diagram.edit.commands;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gmf.runtime.common.core.command.CommandResult;
import org.eclipse.gmf.runtime.common.core.command.ICommand;
import org.eclipse.gmf.runtime.emf.type.core.IElementType;
import org.eclipse.gmf.runtime.emf.type.core.commands.EditElementCommand;
import org.eclipse.gmf.runtime.emf.type.core.requests.ConfigureRequest;
import org.eclipse.gmf.runtime.emf.type.core.requests.CreateElementRequest;
import org.eclipse.gmf.runtime.notation.View;
import org.wso2.integrationstudio.gmf.esb.EsbFactory;
import org.wso2.integrationstudio.gmf.esb.SwitchDefaultBranchOutputConnector;
import org.wso2.integrationstudio.gmf.esb.SwitchMediator;

/**
 * @generated
 */
public class SwitchDefaultBranchOutputConnectorCreateCommand extends EditElementCommand {

    /**
     * @generated
     */
    public SwitchDefaultBranchOutputConnectorCreateCommand(CreateElementRequest req) {
        super(req.getLabel(), null, req);
    }

    /**
     * FIXME: replace with setElementToEdit()
     * 
     * @generated
     */
    protected EObject getElementToEdit() {
        EObject container = ((CreateElementRequest) getRequest()).getContainer();
        if (container instanceof View) {
            container = ((View) container).getElement();
        }
        return container;
    }

    /**
     * @generated
     */
    public boolean canExecute() {
        SwitchMediator container = (SwitchMediator) getElementToEdit();
        if (container.getDefaultBranch() != null) {
            return false;
        }
        return true;

    }

    /**
     * @generated
     */
    protected CommandResult doExecuteWithResult(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        SwitchDefaultBranchOutputConnector newElement = EsbFactory.eINSTANCE.createSwitchDefaultBranchOutputConnector();

        SwitchMediator owner = (SwitchMediator) getElementToEdit();
        owner.setDefaultBranch(newElement);

        doConfigure(newElement, monitor, info);

        ((CreateElementRequest) getRequest()).setNewElement(newElement);
        return CommandResult.newOKCommandResult(newElement);
    }

    /**
     * @generated
     */
    protected void doConfigure(SwitchDefaultBranchOutputConnector newElement, IProgressMonitor monitor, IAdaptable info)
            throws ExecutionException {
        IElementType elementType = ((CreateElementRequest) getRequest()).getElementType();
        ConfigureRequest configureRequest = new ConfigureRequest(getEditingDomain(), newElement, elementType);
        configureRequest.setClientContext(((CreateElementRequest) getRequest()).getClientContext());
        configureRequest.addParameters(getRequest().getParameters());
        ICommand configureCommand = elementType.getEditCommand(configureRequest);
        if (configureCommand != null && configureCommand.canExecute()) {
            configureCommand.execute(monitor, info);
        }
    }

}