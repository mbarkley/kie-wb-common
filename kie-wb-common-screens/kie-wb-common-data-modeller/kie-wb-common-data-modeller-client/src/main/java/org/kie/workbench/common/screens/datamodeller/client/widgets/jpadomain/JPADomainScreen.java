/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.screens.datamodeller.client.widgets.jpadomain;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;

import org.jboss.errai.ioc.client.api.LoadAsync;
import org.uberfire.async.UberfireActivityFragment;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartTitleDecoration;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.client.annotations.WorkbenchScreen;

@ApplicationScoped
@WorkbenchScreen ( identifier = "JPADomainScreen")
@LoadAsync(UberfireActivityFragment.class)
public class JPADomainScreen {

    JPADomainScreenView view;

    public JPADomainScreen() {
    }

    @Inject
    public JPADomainScreen( JPADomainScreenView view ) {
        this.view = view;
    }

    @WorkbenchPartTitle
    public String getTitle() {
        return "Persistence";
    }

    @WorkbenchPartTitleDecoration
    public IsWidget getTitleDecoration() {
        return new Label( "Persistence" );
    }

    @WorkbenchPartView
    public IsWidget getView() {
        return view;
    }

}
