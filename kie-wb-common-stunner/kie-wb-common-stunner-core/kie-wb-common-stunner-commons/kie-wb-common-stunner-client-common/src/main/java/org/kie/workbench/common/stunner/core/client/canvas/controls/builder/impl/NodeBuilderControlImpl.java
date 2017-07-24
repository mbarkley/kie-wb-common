/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.stunner.core.client.canvas.controls.builder.impl;

import java.util.List;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.jboss.errai.ioc.client.api.LoadAsync;
import org.kie.workbench.common.stunner.core.client.api.ClientDefinitionManager;
import org.kie.workbench.common.stunner.core.client.api.ShapeManager;
import org.kie.workbench.common.stunner.core.client.canvas.AbstractCanvasHandler;
import org.kie.workbench.common.stunner.core.client.canvas.controls.AbstractCanvasHandlerControl;
import org.kie.workbench.common.stunner.core.client.canvas.controls.builder.NodeBuilderControl;
import org.kie.workbench.common.stunner.core.client.canvas.controls.builder.request.ElementBuildRequest;
import org.kie.workbench.common.stunner.core.client.canvas.controls.builder.request.ElementBuildRequestImpl;
import org.kie.workbench.common.stunner.core.client.canvas.controls.builder.request.NodeBuildRequest;
import org.kie.workbench.common.stunner.core.client.command.CanvasCommandFactory;
import org.kie.workbench.common.stunner.core.client.command.CanvasViolation;
import org.kie.workbench.common.stunner.core.client.command.RequiresCommandManager;
import org.kie.workbench.common.stunner.core.client.service.ClientRuntimeError;
import org.kie.workbench.common.stunner.core.client.shape.EdgeShape;
import org.kie.workbench.common.stunner.core.client.shape.MutationContext;
import org.kie.workbench.common.stunner.core.client.shape.Shape;
import org.kie.workbench.common.stunner.core.client.shape.util.EdgeMagnetsHelper;
import org.kie.workbench.common.stunner.core.command.Command;
import org.kie.workbench.common.stunner.core.command.CommandResult;
import org.kie.workbench.common.stunner.core.command.impl.CompositeCommandImpl;
import org.kie.workbench.common.stunner.core.command.util.CommandUtils;
import org.kie.workbench.common.stunner.core.graph.Edge;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.content.view.Magnet;
import org.kie.workbench.common.stunner.core.graph.content.view.Point2D;
import org.kie.workbench.common.stunner.core.graph.content.view.View;
import org.kie.workbench.common.stunner.core.graph.content.view.ViewConnector;
import org.uberfire.async.UberfireActivityFragment;

@Dependent
@LoadAsync(UberfireActivityFragment.class)
public class NodeBuilderControlImpl extends AbstractCanvasHandlerControl<AbstractCanvasHandler> implements NodeBuilderControl<AbstractCanvasHandler> {

    private final ClientDefinitionManager clientDefinitionManager;
    private final ShapeManager shapeManager;
    private final CanvasCommandFactory<AbstractCanvasHandler> commandFactory;
    private final AbstractElementBuilderControl elementBuilderControl;
    private final EdgeMagnetsHelper magnetsHelper;

    protected NodeBuilderControlImpl() {
        this(null,
             null,
             null,
             null,
             null);
    }

    @Inject
    public NodeBuilderControlImpl(final ClientDefinitionManager clientDefinitionManager,
                                  final ShapeManager shapeManager,
                                  final CanvasCommandFactory<AbstractCanvasHandler> commandFactory,
                                  final @Default @Element AbstractElementBuilderControl elementBuilderControl,
                                  final EdgeMagnetsHelper magnetsHelper) {
        this.clientDefinitionManager = clientDefinitionManager;
        this.shapeManager = shapeManager;
        this.commandFactory = commandFactory;
        this.elementBuilderControl = elementBuilderControl;
        this.magnetsHelper = magnetsHelper;
    }

    @Override
    public void enable(final AbstractCanvasHandler canvasHandler) {
        super.enable(canvasHandler);
        this.elementBuilderControl.enable(canvasHandler);
    }

    @Override
    protected void doDisable() {
        this.elementBuilderControl.disable();
    }

    @Override
    public void setCommandManagerProvider(final RequiresCommandManager.CommandManagerProvider<AbstractCanvasHandler> provider) {
        this.elementBuilderControl.setCommandManagerProvider(provider);
    }

    @Override
    public boolean allows(final NodeBuildRequest request) {
        final double x = request.getX();
        final double y = request.getY();
        final Node<View<?>, Edge> node = request.getNode();
        if (null != node) {
            final ElementBuildRequest<AbstractCanvasHandler> request1 = new ElementBuildRequestImpl(x,
                                                                                                    y,
                                                                                                    node.getContent().getDefinition());
            return elementBuilderControl.allows(request1);
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void build(final NodeBuildRequest request,
                      final BuildCallback buildCallback) {
        final double x = request.getX();
        final double y = request.getY();
        final Node<View<?>, Edge> node = request.getNode();
        final Edge<View<?>, Node> inEdge = request.getInEdge();
        final Magnet sourceMagnet = request.getSourceMagnet();
        final Magnet targetMagnet = request.getTargetMagnet();
        if (null != node) {
            final Object nodeDef = node.getContent().getDefinition();
            final String nodeId = clientDefinitionManager.adapters().forDefinition().getId(nodeDef);
            final ElementBuilderControlImpl ebc = getElementBuilderControl();
            final Node<View<?>, Edge> parent = ebc.getParent(x,
                                                             y);
            final Point2D childCoordinates = ebc.getChildCoordinates(parent,
                                                                     x,
                                                                     y);
            final String ssid = canvasHandler.getDiagram().getMetadata().getShapeSetId();
            ebc.getElementCommands(node,
                                   parent,
                                   childCoordinates.getX(),
                                   childCoordinates.getY(),
                                   new AbstractElementBuilderControl.CommandsCallback() {
                                       @Override
                                       public void onComplete(final String uuid,
                                                              final List<Command<AbstractCanvasHandler, CanvasViolation>> commands) {
                                           final CompositeCommandImpl.CompositeCommandBuilder commandBuilder = new CompositeCommandImpl.CompositeCommandBuilder().addCommands(commands);
                                           if (inEdge != null) {
                                               final Object edgeDef = inEdge.getContent().getDefinition();
                                               final String edgeId = clientDefinitionManager.adapters().forDefinition().getId(edgeDef);
                                               // The commands to batch for the edge that connects both nodes.
                                               commandBuilder.addCommand(commandFactory.addConnector(inEdge.getSourceNode(),
                                                                                                     inEdge,
                                                                                                     sourceMagnet,
                                                                                                     ssid));
                                               commandBuilder.addCommand(commandFactory.setTargetNode(node,
                                                                                                      inEdge,
                                                                                                      targetMagnet,
                                                                                                      true));
                                           }
                                           final CommandResult<CanvasViolation> results = elementBuilderControl.getCommandManager().execute(canvasHandler,
                                                                                                                                            commandBuilder.build());
                                           if (!CommandUtils.isError(results)) {
                                               updateConnectorShape(inEdge,
                                                                    node,
                                                                    sourceMagnet,
                                                                    targetMagnet);
                                           }
                                           buildCallback.onSuccess(uuid);
                                       }

                                       @Override
                                       public void onError(final ClientRuntimeError error) {
                                           buildCallback.onError(error);
                                       }
                                   });
        }
    }

    @SuppressWarnings("unchecked")
    protected void updateConnectorShape(final Edge<View<?>, Node> inEdge,
                                        final Node targetNode,
                                        final Magnet sourceMagnet,
                                        final Magnet targetMagnet) {
        final ViewConnector connectorContent = (ViewConnector) inEdge.getContent();
        canvasHandler.applyElementMutation(inEdge,
                                           MutationContext.STATIC);
        final EdgeShape edgeShape = (EdgeShape) canvasHandler.getCanvas().getShape(inEdge.getUUID());
        final Node source = inEdge.getSourceNode();
        if (null != source && null != targetNode) {
            final Shape<?> sShape = canvasHandler.getCanvas().getShape(source.getUUID());
            final Shape<?> tShape = canvasHandler.getCanvas().getShape(targetNode.getUUID());
            connectorContent.setSourceMagnet(sourceMagnet);
            connectorContent.setTargetMagnet(targetMagnet);
            edgeShape.applyConnections(inEdge,
                                       sShape.getShapeView(),
                                       tShape.getShapeView(),
                                       MutationContext.STATIC);
        }
    }

    protected ElementBuilderControlImpl getElementBuilderControl() {
        return (ElementBuilderControlImpl) elementBuilderControl;
    }
}
