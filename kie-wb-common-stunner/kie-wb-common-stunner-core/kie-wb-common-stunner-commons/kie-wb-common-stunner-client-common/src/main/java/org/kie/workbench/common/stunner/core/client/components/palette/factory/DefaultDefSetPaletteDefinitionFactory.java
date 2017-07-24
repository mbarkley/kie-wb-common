/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.stunner.core.client.components.palette.factory;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.ioc.client.api.LoadAsync;
import org.kie.workbench.common.stunner.core.client.api.ShapeManager;
import org.kie.workbench.common.stunner.core.client.components.palette.model.definition.DefinitionSetPaletteBuilder;
import org.uberfire.async.UberfireActivityFragment;

/**
 * The default PaletteDefinition factory for a DefinitionSetPalette model.
 * It does not accepts any identifier, it's purpose is for being injected where necessary.
 */
@Dependent
@LoadAsync(UberfireActivityFragment.class)
public class DefaultDefSetPaletteDefinitionFactory extends AbstractPaletteDefinitionFactory<DefinitionSetPaletteBuilder>
        implements DefSetPaletteDefinitionFactory {

    @Inject
    public DefaultDefSetPaletteDefinitionFactory(final ShapeManager shapeManager,
                                                 final DefinitionSetPaletteBuilder paletteBuilder) {
        super(shapeManager,
              paletteBuilder);
    }

    @Override
    public boolean accepts(final String defSetId) {
        return false;
    }
}
