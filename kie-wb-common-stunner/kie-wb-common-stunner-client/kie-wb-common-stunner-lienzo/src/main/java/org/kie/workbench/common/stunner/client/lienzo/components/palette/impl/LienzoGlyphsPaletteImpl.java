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

package org.kie.workbench.common.stunner.client.lienzo.components.palette.impl;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.ioc.client.api.LoadAsync;
import org.kie.workbench.common.stunner.client.lienzo.components.glyph.LienzoGlyphRenderers;
import org.kie.workbench.common.stunner.client.lienzo.components.palette.AbstractLienzoGlyphItemsPalette;
import org.kie.workbench.common.stunner.client.lienzo.components.palette.LienzoGlyphsPalette;
import org.kie.workbench.common.stunner.client.lienzo.components.palette.view.LienzoPaletteViewImpl;
import org.kie.workbench.common.stunner.core.client.api.ShapeManager;
import org.kie.workbench.common.stunner.core.client.components.palette.model.GlyphPaletteItem;
import org.kie.workbench.common.stunner.core.client.components.palette.model.HasPaletteItems;
import org.kie.workbench.common.stunner.core.client.components.views.CanvasDefinitionTooltip;
import org.uberfire.async.UberfireActivityFragment;

@Dependent
@LoadAsync(UberfireActivityFragment.class)
public class LienzoGlyphsPaletteImpl
        extends AbstractLienzoGlyphItemsPalette<HasPaletteItems<? extends GlyphPaletteItem>, LienzoPaletteViewImpl>
        implements LienzoGlyphsPalette {

    protected LienzoGlyphsPaletteImpl() {
        this(null,
             null,
             null,
             null);
    }

    @Inject
    public LienzoGlyphsPaletteImpl(final ShapeManager shapeManager,
                                   final CanvasDefinitionTooltip definitionGlyphTooltip,
                                   final LienzoGlyphRenderers glyphRenderer,
                                   final LienzoPaletteViewImpl view) {
        super(shapeManager,
              definitionGlyphTooltip,
              glyphRenderer,
              view);
    }

    @PostConstruct
    public void init() {
        super.doInit();
    }
}
