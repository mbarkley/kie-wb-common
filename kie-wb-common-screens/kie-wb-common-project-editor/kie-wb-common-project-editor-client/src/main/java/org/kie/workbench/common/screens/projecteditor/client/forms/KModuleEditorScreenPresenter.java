/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.workbench.common.screens.projecteditor.client.forms;

import javax.inject.Inject;

import com.google.gwt.user.client.ui.IsWidget;

import org.jboss.errai.ioc.client.api.LoadAsync;
import org.kie.workbench.common.screens.defaulteditor.client.editor.KieTextEditorPresenter;
import org.kie.workbench.common.screens.defaulteditor.client.editor.KieTextEditorView;
import org.kie.workbench.common.screens.projecteditor.client.type.KModuleResourceType;
import org.uberfire.async.UberfireActivityFragment;
import org.uberfire.backend.vfs.ObservablePath;
import org.uberfire.client.annotations.WorkbenchEditor;
import org.uberfire.client.annotations.WorkbenchMenu;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartTitleDecoration;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.ext.widgets.common.client.ace.AceEditorMode;
import org.uberfire.lifecycle.OnStartup;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.workbench.model.menu.Menus;

@WorkbenchEditor(identifier = "kmoduleScreen", supportedTypes = { KModuleResourceType.class })
@LoadAsync(UberfireActivityFragment.class)
public class KModuleEditorScreenPresenter
        extends KieTextEditorPresenter {

    @Inject
    public KModuleEditorScreenPresenter( KieTextEditorView baseView ) {
        super( baseView );
    }

    @Override
    @OnStartup
    public void onStartup( final ObservablePath path,
                           final PlaceRequest place ) {
        super.onStartup( path, place );
    }

    @Override
    @WorkbenchMenu
    public Menus getMenus() {
        return super.getMenus();
    }

    @Override
    @WorkbenchPartTitleDecoration
    public IsWidget getTitle() {
        return super.getTitle();
    }

    @Override
    @WorkbenchPartTitle
    public String getTitleText() {
        return super.getTitleText();
    }

    @WorkbenchPartView
    public IsWidget asWidget() {
        return super.getWidget();
    }

    @Override
    public AceEditorMode getAceEditorMode() {
        return AceEditorMode.XML;
    }
}



