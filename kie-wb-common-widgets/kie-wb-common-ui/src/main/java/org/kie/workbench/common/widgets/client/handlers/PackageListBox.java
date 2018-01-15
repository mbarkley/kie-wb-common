/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
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
package org.kie.workbench.common.widgets.client.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import org.guvnor.common.services.project.client.context.WorkspaceProjectContext;
import org.guvnor.common.services.project.model.Module;
import org.guvnor.common.services.project.model.Package;
import org.gwtbootstrap3.extras.select.client.ui.Option;
import org.gwtbootstrap3.extras.select.client.ui.Select;
import org.jboss.errai.common.client.api.Caller;
import org.kie.workbench.common.services.shared.project.KieModuleService;
import org.kie.workbench.common.widgets.client.resources.i18n.CommonConstants;
import org.uberfire.mvp.Command;

/**
 * A ListBox that shows a list of Packages from which the user can select
 */
@Dependent
public class PackageListBox extends Composite {

    private WorkspaceProjectContext projectContext;
    protected Caller<KieModuleService> moduleService;
    private SimplePanel panel = new SimplePanel();
    private Select select;
    private Map<String, Package> packages;

    @Inject
    public PackageListBox(final WorkspaceProjectContext projectContext,
                          final Caller<KieModuleService> moduleService) {
        this.projectContext = projectContext;
        this.moduleService = moduleService;
        initWidget(panel);
        getElement().getStyle().setMarginBottom(15,
                                                Style.Unit.PX);
        packages = new HashMap<String, Package>();
    }

    public void setUp(final boolean includeDefaultPackage) {
        setUp(includeDefaultPackage,
              null);
    }

    public void setUp(final boolean includeDefaultPackage,
                      final Command packagesLoadedCommand) {
        noPackage();
        packages.clear();

        // Disable and set default content if Module is not selected
        final Module activeModule = projectContext.getActiveModule();
        if (activeModule == null) {
            return;
        }

        // Otherwise show list of packages
        moduleService.call((Set<Package> pkgs) -> {
            moduleService.call((Package activePackage) -> {
                //Sort by caption
                final List<Package> sortedPackages = new ArrayList<Package>();
                sortedPackages.addAll(pkgs);
                Collections.sort(sortedPackages,
                                 (p1, p2) -> p1.getCaption().compareTo(p2.getCaption()));

                // Remove default package, if not required (after sorting it is guaranteed to be at index 0)
                if (!includeDefaultPackage) {
                    sortedPackages.remove(0);
                }

                // Disable and set default content if no Packages available
                if (sortedPackages.size() == 0) {
                    return;
                }

                addPackagesToSelect(sortedPackages,
                                    activePackage);

                if (packagesLoadedCommand != null) {
                    packagesLoadedCommand.execute();
                }
            }).resolveDefaultWorkspacePackage(activeModule);
        }).resolvePackages(activeModule);
    }

    private void addPackagesToSelect(final List<Package> sortedPackages,
                                     final Package activePackage) {
        clearSelect();

        for (Package pkg : sortedPackages) {
            addPackage(pkg,
                       activePackage);
        }

        refreshSelect();
    }

    public Package getSelectedPackage() {
        if (packages.size() == 0 || select == null) {
            return null;
        }
        return packages.get(select.getValue());
    }

    void addPackage(final Package pkg,
                    final Package activePackage) {
        final Option option = new Option();
        option.setText(pkg.getCaption());
        select.add(option);
        packages.put(pkg.getCaption(),
                     pkg);
        if (pkg.equals(activePackage)) {
            select.setValue(pkg.getCaption());
        }
    }

    void noPackage() {
        clearSelect();
        final Option option = new Option();
        option.setText(CommonConstants.INSTANCE.ItemUndefinedPath());

        select.add(option);
        select.setEnabled(false);
        refreshSelect();
    }

    void clearSelect() {
        if (select != null) {
            select.removeFromParent();
            removeSelect(select.getElement());
        }
        select = new Select();
        panel.setWidget(select);
    }

    void refreshSelect() {
        select.refresh();
    }

    private native void removeSelect(final Element e) /*-{
        $wnd.jQuery(e).selectpicker('destroy');
    }-*/;
}
