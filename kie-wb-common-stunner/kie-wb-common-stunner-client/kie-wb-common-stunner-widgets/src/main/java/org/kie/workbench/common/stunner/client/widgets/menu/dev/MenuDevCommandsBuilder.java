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
package org.kie.workbench.common.stunner.client.widgets.menu.dev;

import java.util.LinkedList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.gwt.user.client.ui.IsWidget;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ButtonGroup;
import org.gwtbootstrap3.client.ui.DropDownMenu;
import org.gwtbootstrap3.client.ui.constants.ButtonSize;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.constants.Toggle;
import org.jboss.errai.ioc.client.api.LoadAsync;
import org.jboss.errai.ioc.client.api.ManagedInstance;
import org.kie.workbench.common.stunner.client.widgets.menu.MenuUtils;
import org.uberfire.async.UberfireActivityFragment;
import org.uberfire.workbench.model.menu.MenuItem;

/**
 * The menu builder for different Stunner's dev menu items.
 * By default this class is not enabled. Enable it in your @EntryPoint if necessary.
 */
@ApplicationScoped
@LoadAsync(UberfireActivityFragment.class)
public class MenuDevCommandsBuilder {

    private final ManagedInstance<MenuDevCommand> menuDevCommandManagedInstances;
    private final List<MenuDevCommand> devCommands = new LinkedList<>();
    private boolean enabled;

    @Inject
    public MenuDevCommandsBuilder(final ManagedInstance<MenuDevCommand> menuDevCommandManagedInstances) {
        this.menuDevCommandManagedInstances = menuDevCommandManagedInstances;
        this.enabled = false;
    }

    public void enable() {
        if (!enabled) {
            this.enabled = true;
            menuDevCommandManagedInstances.iterator().forEachRemaining(devCommands::add);
        }
    }

    public MenuItem build() {
        return enabled ? buildDevMenuItem() : null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private MenuItem buildDevMenuItem() {
        final DropDownMenu menu = new DropDownMenu() {{
            addStyleName("pull-right");
        }};
        for (final MenuDevCommand command : devCommands) {
            menu.add(new AnchorListItem(command.getText()) {{
                setIcon(command.getIcon());
                addClickHandler(event -> command.execute());
            }});
        }
        final IsWidget group = new ButtonGroup() {{
            add(new Button() {{
                setToggleCaret(false);
                setDataToggle(Toggle.DROPDOWN);
                setIcon(IconType.COG);
                setSize(ButtonSize.SMALL);
                setTitle("Development");
            }});
            add(menu);
        }};
        return MenuUtils.buildItem(group);
    }
}
