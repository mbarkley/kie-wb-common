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

package org.kie.workbench.common.screens.datamodeller.model;


import org.jboss.errai.common.client.api.annotations.Portable;

import java.util.ArrayList;
import java.util.List;

@Portable
public class AnnotationDefinitionTO {

    private String className;

    private boolean marker;

    private boolean objectAnnotation;

    private boolean propertyAnnotation;
    
    public static final String ROLE_ANNOTATION = "org.kie.api.definition.type.Role";
    
    public static final String POSITION_ANNOTATION = "org.kie.api.definition.type.Position";

    public static final String LABEL_ANNOTATION = "org.kie.api.definition.type.Label";

    public static final String DESCRIPTION_ANNOTATION = "org.kie.api.definition.type.Description";

    public static final String KEY_ANNOTATION = "org.kie.api.definition.type.Key";

    public static final String PROPERTY_REACTIVE_ANNOTATION = "org.kie.api.definition.type.PropertyReactive";

    public static final String CLASS_REACTIVE_ANNOTATION = "org.kie.api.definition.type.ClassReactive";

    public static final String TIMESTAMP_ANNOTATION = "org.kie.api.definition.type.Timestamp";

    public static final String DURATION_ANNOTATION = "org.kie.api.definition.type.Duration";

    public static final String EXPIRES_ANNOTATION = "org.kie.api.definition.type.Expires";

    public static final String TYPE_SAFE_ANNOTATION = "org.kie.api.definition.type.TypeSafe";

    public static final String REMOTABLE_ANNOTATION = "org.kie.api.remote.Remotable";


    public static final String JAVAX_PERSISTENCE_ENTITY_ANNOTATION = "javax.persistence.Entity";

    public static final String JAVAX_PERSISTENCE_TABLE_ANNOTATION = "javax.persistence.Table";

    public static final String JAVAX_PERSISTENCE_ID_ANNOTATION = "javax.persistence.Id";

    public static final String JAVAX_PERSISTENCE_GENERATED_VALUE_ANNOTATION = "javax.persistence.GeneratedValue";

    public static final String JAVAX_PERSISTENCE_SEQUENCE_GENERATOR_ANNOTATION = "javax.persistence.SequenceGenerator";

    public static final String JAVAX_PERSISTENCE_COLUMN_ANNOTATION = "javax.persistence.Column";

    public static final String JAVAX_PERSISTENCE_MANY_TO_ONE = "javax.persistence.ManyToOne";

    public static final String JAVAX_PERSISTENCE_MANY_TO_MANY = "javax.persistence.ManyToMany";

    public static final String JAVAX_PERSISTENCE_ONE_TO_MANY = "javax.persistence.OneToMany";

    public static final String JAVAX_PERSISTENCE_ONE_TO_ONE = "javax.persistence.OneToOne";

    public static final String VALUE_PARAM = "value";

    private List<AnnotationMemberDefinitionTO> annotationMembers = new ArrayList<AnnotationMemberDefinitionTO>();

    public AnnotationDefinitionTO() {
    }

    public AnnotationDefinitionTO(String className, boolean objectAnnotation, boolean propertyAnnotation) {
        this.className = className;
        this.objectAnnotation = objectAnnotation;
        this.propertyAnnotation = propertyAnnotation;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public boolean isMarker() {
        return annotationMembers == null || annotationMembers.size() == 0;
    }

    public void setMarker(boolean marker) {
        this.marker = marker;
    }

    public boolean getMarker() {
        return isMarker();
    }

    public List<AnnotationMemberDefinitionTO> getAnnotationMembers() {
        return annotationMembers;
    }

    public boolean isObjectAnnotation() {
        return objectAnnotation;
    }

    public void setObjectAnnotation(boolean objectAnnotation) {
        this.objectAnnotation = objectAnnotation;
    }

    public boolean isPropertyAnnotation() {
        return propertyAnnotation;
    }

    public void setPropertyAnnotation(boolean propertyAnnotation) {
        this.propertyAnnotation = propertyAnnotation;
    }

    public void addMember(AnnotationMemberDefinitionTO memberDefinitionTO) {
        annotationMembers.add(memberDefinitionTO);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AnnotationDefinitionTO that = (AnnotationDefinitionTO) o;

        if (marker != that.marker) {
            return false;
        }
        if (objectAnnotation != that.objectAnnotation) {
            return false;
        }
        if (propertyAnnotation != that.propertyAnnotation) {
            return false;
        }
        if (annotationMembers != null ? !annotationMembers.equals(that.annotationMembers) : that.annotationMembers != null) {
            return false;
        }
        if (className != null ? !className.equals(that.className) : that.className != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = className != null ? className.hashCode() : 0;
        result = ~~result;
        result = 31 * result + (marker ? 1 : 0);
        result = ~~result;
        result = 31 * result + (objectAnnotation ? 1 : 0);
        result = ~~result;
        result = 31 * result + (propertyAnnotation ? 1 : 0);
        result = ~~result;
        result = 31 * result + (annotationMembers != null ? annotationMembers.hashCode() : 0);
        result = ~~result;
        return result;
    }
}
