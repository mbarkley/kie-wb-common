/*
 * Copyright 2015 JBoss Inc
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

package org.kie.workbench.common.screens.datamodeller.client.widgets.common.domain;

import javax.enterprise.event.Observes;

import org.kie.workbench.common.screens.datamodeller.events.ChangeType;
import org.kie.workbench.common.screens.datamodeller.events.DataObjectFieldChangeEvent;
import org.kie.workbench.common.screens.datamodeller.events.DataObjectFieldDeletedEvent;
import org.kie.workbench.common.screens.datamodeller.events.DataObjectFieldSelectedEvent;
import org.kie.workbench.common.screens.datamodeller.events.DataObjectSelectedEvent;
import org.kie.workbench.common.services.datamodeller.core.DataObject;
import org.kie.workbench.common.services.datamodeller.core.ObjectProperty;

public abstract class FieldEditor extends BaseEditor {

    protected DataObject dataObject;

    protected ObjectProperty objectField;

    protected FieldEditor() {
    }

    public DataObject getDataObject() {
        return dataObject;
    }

    public ObjectProperty getObjectField() {
        return objectField;
    }

    protected abstract void loadDataObjectField( DataObject dataObject,
            ObjectProperty objectField );


    // Event observers

    protected void onDataObjectSelected( @Observes DataObjectSelectedEvent event ) {
        if ( event.isFromContext( context != null ? context.getContextId() : null ) ) {
            clean();
            this.dataObject = event.getCurrentDataObject();
            this.objectField = null;
        }
    }

    protected void onDataObjectFieldSelected( @Observes DataObjectFieldSelectedEvent event ) {
        if ( event.isFromContext( context != null ? context.getContextId() : null ) ) {
            loadDataObjectField( event.getCurrentDataObject(), event.getCurrentField() );
        }
    }

    protected void onDataObjectFieldDeleted( @Observes DataObjectFieldDeletedEvent event ) {
        // When all attributes from the current object have been deleted clean
        if ( event.isFromContext( context != null ? context.getContextId() : null ) ) {
            if ( getDataObject().getProperties().size() == 0 ) {
                clean();
                setReadonly( true );
            }
        }
    }

    protected void notifyFieldChange( ChangeType changeType,
            String memberName,
            Object oldValue,
            Object newValue ) {

        //TODO check if data model for the event is needed
        DataObjectFieldChangeEvent changeEvent = new DataObjectFieldChangeEvent( changeType,
                getContext().getContextId(),
                getName(),
                null,
                getDataObject(),
                getObjectField(),
                memberName, oldValue, newValue );
        //TODO, check if the helper is still needed.
        // Notify helper directly
        getContext().getHelper().dataModelChanged( changeEvent );
        dataModelerEvent.fire( changeEvent );
    }

}
