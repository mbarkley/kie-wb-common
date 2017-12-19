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

package org.kie.workbench.common.forms.editor.backend.service.impl;

import javax.enterprise.event.Event;

import org.guvnor.common.services.backend.util.CommentedOptionFactory;
import org.guvnor.common.services.shared.metadata.model.Metadata;
import org.guvnor.common.services.shared.metadata.model.Overview;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.forms.editor.model.FormModelerContent;
import org.kie.workbench.common.forms.editor.service.backend.FormModelHandlerManager;
import org.kie.workbench.common.forms.editor.service.shared.VFSFormFinderService;
import org.kie.workbench.common.forms.editor.type.FormResourceTypeDefinition;
import org.kie.workbench.common.forms.fields.test.TestFieldManager;
import org.kie.workbench.common.forms.model.FormDefinition;
import org.kie.workbench.common.forms.model.FormModel;
import org.kie.workbench.common.forms.serialization.FormDefinitionSerializer;
import org.kie.workbench.common.forms.service.shared.FieldManager;
import org.kie.workbench.common.services.shared.project.KieModule;
import org.kie.workbench.common.services.shared.project.KieModuleService;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;
import org.uberfire.backend.vfs.Path;
import org.uberfire.ext.editor.commons.service.DeleteService;
import org.uberfire.ext.editor.commons.service.RenameService;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.file.FileAlreadyExistsException;
import org.uberfire.rpc.SessionInfo;
import org.uberfire.workbench.events.ResourceOpenedEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FormEditorServiceImplTest {

    private static final String COMMIT_MESSAGE = "commit message";

    private static final String FORM_NAME = "formName";

    private static final String FULL_FORM_NAME = FORM_NAME + "." + FormResourceTypeDefinition.EXTENSION;

    private static final String NEW_FORM_NAME = "newFormName";

    private static final String COMMIT_COMMENT = "a comment";

    @Mock
    private Path path;

    @Mock
    private IOService ioService;

    @Mock
    private SessionInfo sessionInfo;

    @Mock
    private Event<ResourceOpenedEvent> resourceOpenedEvent;

    private FieldManager fieldManager = new TestFieldManager();

    @Mock
    private FormModelHandlerManager modelHandlerManager;

    @Mock
    KieModuleService moduleService;

    @Mock
    private KieModule module;

    @Mock
    private FormDefinitionSerializer formDefinitionSerializer;

    @Mock
    private VFSFormFinderService vfsFormFinderService;

    @Mock
    private DeleteService deleteService;

    @Mock
    private RenameService renameService;

    @Mock
    private CommentedOptionFactory commentedOptionFactory;

    private FormEditorServiceImpl formEditorService;

    @Before
    public void init() {
        formEditorService = spy(new FormEditorServiceImpl(ioService,
                                                          sessionInfo,
                                                          resourceOpenedEvent,
                                                          fieldManager,
                                                          modelHandlerManager,
                                                          moduleService,
                                                          formDefinitionSerializer,
                                                          vfsFormFinderService,
                                                          deleteService,
                                                          commentedOptionFactory,
                                                          renameService));

        when(path.toURI()).thenReturn("default:///src/main/resources/test.frm");
        when(moduleService.resolveModule(any())).thenReturn(module);
    }

    @Test
    public void testCreateForm() {
        when(formDefinitionSerializer.serialize(any())).thenAnswer(this::verifyNewForm);

        FormModel formModel = mock(FormModel.class);

        Path resultPath = formEditorService.createForm(path, FULL_FORM_NAME, formModel);
        assertNotNull(resultPath);
    }

    @Test
    public void testDeleteForm() {
        formEditorService.delete(path, COMMIT_MESSAGE);

        verify(moduleService).resolveModule(path);

        verify(deleteService).delete(path, COMMIT_MESSAGE);
    }

    @Test
    public void testDeleteFormWrongProject() {
        when(moduleService.resolveModule(any())).thenReturn(null);

        formEditorService.delete(path, COMMIT_MESSAGE);

        verify(moduleService).resolveModule(path);

        verify(deleteService, never()).delete(path, COMMIT_MESSAGE);
    }

    @Test
    public void testDeleteFormWithException() {
        when(moduleService.resolveModule(any())).thenThrow(new IllegalStateException("Testing exception handling"));

        formEditorService.delete(path, COMMIT_MESSAGE);

        verify(moduleService).resolveModule(path);

        verify(deleteService, never()).delete(path, COMMIT_MESSAGE);
    }

    protected String verifyNewForm(InvocationOnMock invocationOnMock) {
        FormDefinition form = (FormDefinition) invocationOnMock.getArguments()[0];
        assertNotNull(form);
        assertNotNull(form.getId());
        assertNotNull(form.getModel());
        assertEquals(FORM_NAME, form.getName());
        assertTrue(form.getFields().isEmpty());
        assertNotNull(form.getLayoutTemplate());
        return "";
    }

    @Test
    public void testCreateFormThatExistsOnVFS() {
        try {
            when(ioService.exists(any())).thenReturn(true);
            FormModel formModel = mock(FormModel.class);
            formEditorService.createForm(path, FULL_FORM_NAME, formModel);
            fail("If form exists we shouldn't be here");
        } catch (FileAlreadyExistsException ex) {

        }
    }

    @Test
    public void testSaveAndRename() {
        testRename(true);
    }

    @Test
    public void testRenameWithoutSaving() {
        testRename(false);
    }

    private void testRename(boolean saveBeforeRenaming) {
        FormModelerContent content = mock(FormModelerContent.class);
        doReturn(path).when(formEditorService).save(any(Path.class), any(FormModelerContent.class), any(Metadata.class), anyString());
        doReturn(content).when(formEditorService).constructContent(any(Path.class), any(Overview.class));
        when(content.getDefinition()).thenReturn(mock(FormDefinition.class));
        Metadata metadata = mock(Metadata.class);

        FormModelerContent resultContent = formEditorService.rename(path, NEW_FORM_NAME, COMMIT_COMMENT, saveBeforeRenaming, content, metadata);

        assertSame(content, resultContent);
        VerificationMode constructContentCalls = saveBeforeRenaming ? never() : times(1);
        verify(formEditorService, constructContentCalls).constructContent(eq(path), any(Overview.class));
        verify(formEditorService).save(eq(path), eq(content), eq(metadata), eq(COMMIT_COMMENT));
        verify(renameService).rename(eq(path), eq(NEW_FORM_NAME), eq(COMMIT_COMMENT));
    }
}
