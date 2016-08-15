/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.forms.dynamic.client;

import com.google.gwtmockito.GwtMock;
import com.google.gwtmockito.GwtMockitoTestRunner;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.forms.dynamic.client.rendering.FieldLayoutComponent;
import org.kie.workbench.common.forms.dynamic.client.rendering.FieldRenderer;
import org.kie.workbench.common.forms.dynamic.service.FormRenderingContext;
import org.kie.workbench.common.forms.dynamic.service.FormRenderingContextGeneratorService;
import org.kie.workbench.common.forms.dynamic.test.model.Employee;
import org.kie.workbench.common.forms.dynamic.test.util.TestFormGenerator;
import org.kie.workbench.common.forms.model.FieldDefinition;
import org.kie.workbench.common.forms.processing.engine.handling.FieldChangeHandler;
import org.kie.workbench.common.forms.processing.engine.handling.FormHandler;
import org.kie.workbench.common.forms.dynamic.client.rendering.renderers.relations.subform.widget.SubFormWidget;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.uberfire.mocks.CallerMock;
import org.uberfire.mvp.Command;

import static org.mockito.Mockito.*;

@RunWith(GwtMockitoTestRunner.class)
public class DynamicFormRendererTest extends TestCase {

    private FieldLayoutComponent component;

    private FieldRenderer fieldRenderer;

    @GwtMock
    private SubFormWidget widget;

    @Mock
    private FieldChangeHandler changeHandler;

    @Mock
    private FormHandler formHandler;

    private DynamicFormRenderer.DynamicFormRendererView view;

    private FormRenderingContextGeneratorService formRenderingContextGeneratorService;

    private CallerMock<FormRenderingContextGeneratorService> transformer;

    private DynamicFormRenderer renderer;

    private Employee employee = new Employee();

    @Before
    public void initTest() {
        component = mock(FieldLayoutComponent.class);
        view = mock( DynamicFormRenderer.DynamicFormRendererView.class );
        fieldRenderer = mock( FieldRenderer.class );
        formRenderingContextGeneratorService = mock( FormRenderingContextGeneratorService.class );
        transformer = new CallerMock<FormRenderingContextGeneratorService>( formRenderingContextGeneratorService );

        when( formRenderingContextGeneratorService.createContext( any( Employee.class ) ) ).thenAnswer( new Answer<FormRenderingContext>() {
            @Override
            public FormRenderingContext answer( InvocationOnMock invocationOnMock ) throws Throwable {
                return TestFormGenerator.getContextForEmployee( employee );
            }
        } );

        when( view.getFieldLayoutComponentForField( any( FieldDefinition.class) ) ).thenReturn( component );

        when( component.getFieldRenderer() ).thenReturn( fieldRenderer );
        when( fieldRenderer.getInputWidget() ).thenReturn( widget );

        renderer = new DynamicFormRendererMock( view, transformer, formHandler );
        renderer.init();
        verify( view ).setPresenter( renderer );
        renderer.asWidget();
        verify( view ).asWidget();
    }

    @Test
    public void testBaseBinding() {
        doBind();

        unBind();
    }

    @Test
    public void testBindingAddingFieldChangeHandler() {
        doBind();

        renderer.addFieldChangeHandler( changeHandler );

        renderer.addFieldChangeHandler( "name", changeHandler );

        renderer.addFieldChangeHandler( "address", changeHandler );

        verify( formHandler ).addFieldChangeHandler( any() );
        verify( formHandler, times(2) ).addFieldChangeHandler( anyString(), any() );

        unBind();
    }

    protected void doBind() {
        renderer.renderDefaultForm( employee, new Command() {
            @Override
            public void execute() {
                verify( view ).render( any() );
                verify( view ).bind();
                verify( formHandler ).setUp( any( Employee.class ) );
            }
        } );
    }

    protected void unBind() {
        renderer.isValid();
        renderer.unBind();
        verify( formHandler ).clear();
    }
}
