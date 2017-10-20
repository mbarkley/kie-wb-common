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

package org.guvnor.ala.services.api.itemlist;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.guvnor.ala.runtime.providers.Provider;
import org.guvnor.ala.services.api.ItemList;

public class ProviderList implements ItemList<Provider> {

    private Provider[] provider;

    /*
     * No-args constructor for enabling marshalling to work, please do not remove. 
    */
    public ProviderList() {
    }

    public ProviderList(List<Provider> providers) {
        this.provider = providers.toArray(new Provider[providers.size()]);
    }

    public ProviderList(Provider[] providers) {
        this.provider = providers;
    }

    public Provider[] getProviderTypes() {
        return provider;
    }

    public void setProviderTypes(Provider[] providers) {
        this.provider = providers;
    }

    @Override
    @JsonIgnore
    public List<Provider> getItems() {
        if (provider == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(provider);
    }
}
