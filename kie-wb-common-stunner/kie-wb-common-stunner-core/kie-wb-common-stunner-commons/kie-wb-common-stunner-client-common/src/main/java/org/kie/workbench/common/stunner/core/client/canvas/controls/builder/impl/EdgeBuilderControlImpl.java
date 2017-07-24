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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.ioc.client.api.LoadAsync;
import org.kie.workbench.common.stunner.core.client.api.ClientDefinitionManager;
import org.kie.workbench.common.stunner.core.client.api.ShapeManager;
import org.kie.workbench.common.stunner.core.client.canvas.AbstractCanvasHandler;
import org.kie.workbench.common.stunner.core.client.canvas.Canvas;
import org.kie.workbench.common.stunner.core.client.canvas.controls.AbstractCanvasHandlerControl;
import org.kie.workbench.common.stunner.core.client.canvas.controls.builder.EdgeBuilderControl;
import org.kie.workbench.common.stunner.core.client.canvas.controls.builder.request.EdgeBuildRequest;
import org.kie.workbench.common.stunner.core.client.command.CanvasCommandFactory;
import org.kie.workbench.common.stunner.core.client.command.CanvasCommandManager;
import org.kie.workbench.common.stunner.core.client.command.CanvasViolation;
import org.kie.workbench.common.stunner.core.client.command.RequiresCommandManager;
import org.kie.workbench.common.stunner.core.client.shape.MutationContext;
import org.kie.workbench.common.stunner.core.client.shape.Shape;
import org.kie.workbench.common.stunner.core.client.shape.util.EdgeMagnetsHelper;
import org.kie.workbench.common.stunner.core.command.CommandResult;
import org.kie.workbench.common.stunner.core.command.impl.CompositeCommandImpl;
import org.kie.workbench.common.stunner.core.command.util.CommandUtils;
import org.kie.workbench.common.stunner.core.graph.Edge;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.content.view.Magnet;
import org.kie.workbench.common.stunner.core.graph.content.view.MagnetImpl;
import org.kie.workbench.common.stunner.core.graph.content.view.View;
import org.uberfire.async.UberfireActivityFragment;

@Dependent
@LoadAsync(UberfireActivityFragment.class)
public class EdgeBuilderControlImpl extends AbstractCanvasHandlerControl<AbstractCanvasHandler> implements EdgeBuilderControl<AbstractCanvasHandler> {

    private static Logger LOGGER = Logger.getLogger(EdgeBuilderControlImpl.class.getName());

    private final ClientDefinitionManager clientDefinitionManager;
    private final ShapeManager shapeManager;
    private final CanvasCommandFactory<AbstractCanvasHandler> commandFactory;
    private final EdgeMagnetsHelper magnetsHelper;
    private RequiresCommandManager.CommandManagerProvider<AbstractCanvasHandler> commandManagerProvider;

    protected EdgeBuilderControlImpl() {
        this(null,
             null,
             null,
             null);
    }

    @Inject
    public EdgeBuilderControlImpl(final ClientDefinitionManager clientDefinitionManager,
                                  final ShapeManager shapeManager,
                                  final CanvasCommandFactory<AbstractCanvasHandler> commandFactory,
                                  final EdgeMagnetsHelper magnetsHelper) {
        this.clientDefinitionManager = clientDefinitionManager;
        this.shapeManager = shapeManager;
        this.commandFactory = commandFactory;
        this.magnetsHelper = magnetsHelper;
    }

    @Override
    public void setCommandManagerProvider(final RequiresCommandManager.CommandManagerProvider<AbstractCanvasHandler> provider) {
        this.commandManagerProvider = provider;
    }

    @Override
    public boolean allows(final EdgeBuildRequest request) {
        final double x = request.getX();
        final double y = request.getY();
        final Edge<View<?>, Node> edge = request.getEdge();
        final AbstractCanvasHandler<?, ?> wch = canvasHandler;
        final Node<View<?>, Edge> inNode = request.getInNode();
        final Node<View<?>, Edge> outNode = request.getOutNode();
        boolean allowsSourceConn = true;
        if (null != inNode) {
            final CommandResult<CanvasViolation> cr1 = getCommandManager().allow(wch,
                                                                                 commandFactory.setSourceNode(inNode,
                                                                                                              edge,
                                                                                                              MagnetImpl.Builder.build(0d,
                                                                                                                                       0d),
                                                                                                              true));
            allowsSourceConn = isAllowed(cr1);
        }
        boolean allowsTargetConn = true;
        if (null != outNode) {
            final CommandResult<CanvasViolation> cr2 = getCommandManager().allow(wch,
                                                                                 commandFactory.setTargetNode(outNode,
                                                                                                              edge,
                                                                                                              MagnetImpl.Builder.build(0d,
                                                                                                                                       0d),
                                                                                                              true));
            allowsTargetConn = isAllowed(cr2);
        }
        return allowsSourceConn & allowsTargetConn;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void build(final EdgeBuildRequest request,
                      final BuildCallback buildCallback) {
        final double x = request.getX();
        final double y = request.getY();
        final Edge<View<?>, Node> edge = request.getEdge();
        final AbstractCanvasHandler<?, ?> wch = canvasHandler;
        final Node<View<?>, Edge> inNode = request.getInNode();
        final Node<View<?>, Edge> outNode = request.getOutNode();
        final Canvas canvas = canvasHandler.getCanvas();
        if (null == inNode) {
            throw new RuntimeException(" An edge must be into the outgoing edges list from a node.");
        }
        final Shape sourceShape = canvas.getShape(inNode.getUUID());
        final Shape targetShape = outNode != null ? canvas.getShape(outNode.getUUID()) : null;
        Magnet[] magnets = new Magnet[]{MagnetImpl.Builder.build(0d,
                                                                 0d), MagnetImpl.Builder.build(0d,
                                                                                               0d)};
        if (targetShape != null) {
            magnets = magnetsHelper.getDefaultMagnets(sourceShape.getShapeView(),
                                                      targetShape.getShapeView());
        }
        final Object edgeDef = edge.getContent().getDefinition();
        final String ssid = canvasHandler.getDiagram().getMetadata().getShapeSetId();
        final CompositeCommandImpl.CompositeCommandBuilder commandBuilder = new CompositeCommandImpl.CompositeCommandBuilder()
                .addCommand(commandFactory.addConnector(inNode,
                                                        edge,
                                                        magnets[0],
                                                        ssid));
        if (null != outNode) {
            commandBuilder.addCommand(commandFactory.setTargetNode(outNode,
                                                                   edge,
                                                                   magnets[1],
                                                                   true));
        }
        final CommandResult<CanvasViolation> results = getCommandManager().execute(wch,
                                                                                   commandBuilder.build());
        if (CommandUtils.isError(results)) {
            LOGGER.log(Level.WARNING,
                       results.toString());
        }
        canvasHandler.applyElementMutation(edge,
                                           MutationContext.STATIC);
        buildCallback.onSuccess(edge.getUUID());
    }

    @Override
    protected void doDisable() {
        commandManagerProvider = null;
    }

    private boolean isAllowed(CommandResult<CanvasViolation> result) {
        return !CommandResult.Type.ERROR.equals(result.getType());
    }

    private CanvasCommandManager<AbstractCanvasHandler> getCommandManager() {
        return commandManagerProvider.getCommandManager();
    }
}
