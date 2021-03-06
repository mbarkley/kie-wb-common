/**
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.screens.datamodeller.events;


import org.jboss.errai.common.client.api.annotations.Portable;
import org.kie.workbench.common.services.datamodeller.core.DataModel;
import org.kie.workbench.common.services.datamodeller.core.DataObject;

@Portable
public class DataObjectChangeEvent extends DataModelerValueChangeEvent {

    public DataObjectChangeEvent() {
    }

    public DataObjectChangeEvent( ChangeType changeType,
            String contextId,
            String source,
            DataModel currentModel,
            DataObject currentDataObject,
            String propertyName,
            Object oldValue,
            Object newValue ) {
        super( changeType, contextId, source, currentModel, currentDataObject, propertyName, oldValue, newValue );
    }
}
