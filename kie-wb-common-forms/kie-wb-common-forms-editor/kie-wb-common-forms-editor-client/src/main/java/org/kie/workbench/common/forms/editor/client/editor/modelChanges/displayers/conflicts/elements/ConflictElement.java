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

package org.kie.workbench.common.forms.editor.client.editor.modelChanges.displayers.conflicts.elements;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.common.client.api.IsElement;
import org.jboss.errai.common.client.dom.HTMLElement;
import org.jboss.errai.ioc.client.api.LoadAsync;
import org.jboss.errai.ui.client.local.spi.TranslationService;
import org.kie.workbench.common.forms.editor.client.resources.i18n.FormEditorConstants;
import org.uberfire.async.UberfireActivityFragment;

@Dependent
@LoadAsync(UberfireActivityFragment.class)
public class ConflictElement implements IsElement,
                                        ConflictElementView.Presenter {

    boolean isShowMorePressed = false;

    private String firstMessage;
    private String fullMessage;

    private ConflictElementView view;
    private TranslationService translationService;

    @Inject
    public ConflictElement(ConflictElementView view,
                           TranslationService translationService) {
        this.view = view;
        this.translationService = translationService;
        this.view.init(this);
    }

    public void showConflict(String field,
                             String firstMessage,
                             String secondMessage) {
        this.firstMessage = firstMessage;
        this.fullMessage = firstMessage + " " + secondMessage;
        view.showConflict(field,
                          firstMessage);
    }

    @Override
    public void onShowMoreClick() {
        if (isShowMorePressed) {
            view.setMessage(firstMessage);
            view.setShowMoreText(translationService.getTranslation(FormEditorConstants.ConflictElementViewImplShowMore));
        } else {
            view.setMessage(fullMessage);
            view.setShowMoreText(translationService.getTranslation(FormEditorConstants.ConflictElementViewImplShowLess));
        }
        isShowMorePressed = !isShowMorePressed;
    }

    @Override
    public HTMLElement getElement() {
        return view.getElement();
    }
}
