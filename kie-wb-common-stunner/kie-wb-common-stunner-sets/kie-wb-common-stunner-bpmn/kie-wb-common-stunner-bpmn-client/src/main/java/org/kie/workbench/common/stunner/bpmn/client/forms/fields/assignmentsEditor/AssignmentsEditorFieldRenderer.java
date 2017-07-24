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

package org.kie.workbench.common.stunner.bpmn.client.forms.fields.assignmentsEditor;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.google.gwt.user.client.ui.IsWidget;

import org.jboss.errai.ioc.client.api.LoadAsync;
import org.kie.workbench.common.forms.dynamic.client.rendering.FieldRenderer;
import org.kie.workbench.common.stunner.bpmn.client.forms.util.ContextUtils;
import org.kie.workbench.common.stunner.bpmn.definition.BPMNDefinition;
import org.kie.workbench.common.stunner.bpmn.forms.model.AssignmentsEditorFieldDefinition;
import org.uberfire.async.UberfireActivityFragment;

@Dependent
@LoadAsync(UberfireActivityFragment.class)
public class AssignmentsEditorFieldRenderer extends FieldRenderer<AssignmentsEditorFieldDefinition> {

    private AssignmentsEditorWidget assignmentsEditor;

    @Inject
    public AssignmentsEditorFieldRenderer(final AssignmentsEditorWidget assignmentsEditor) {
        this.assignmentsEditor = assignmentsEditor;
    }

    @Override
    public String getName() {
        return AssignmentsEditorFieldDefinition.FIELD_TYPE.getTypeName();
    }

    @Override
    public void initInputWidget() {
        assignmentsEditor.setBPMNModel(null);
        Object model = ContextUtils.getModel(renderingContext);
        if (model instanceof BPMNDefinition) {
            assignmentsEditor.setBPMNModel((BPMNDefinition) model);
        }
    }

    @Override
    public IsWidget getPrettyViewWidget() {
        initInputWidget();
        return getInputWidget();
    }

    @Override
    protected void setReadOnly(final boolean readOnly) {

    }

    @Override
    public IsWidget getInputWidget() {
        return assignmentsEditor;
    }

    @Override
    public String getSupportedCode() {
        return AssignmentsEditorFieldDefinition.FIELD_TYPE.getTypeName();
    }
}
