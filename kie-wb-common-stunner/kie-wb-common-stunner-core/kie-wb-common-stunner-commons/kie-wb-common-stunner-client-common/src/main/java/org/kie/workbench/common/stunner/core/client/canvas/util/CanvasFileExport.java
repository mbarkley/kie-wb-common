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

package org.kie.workbench.common.stunner.core.client.canvas.util;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.errai.ioc.client.api.AsyncBeanLoader;
import org.kie.workbench.common.stunner.core.client.canvas.AbstractCanvasHandler;
import org.kie.workbench.common.stunner.core.client.canvas.CanvasExport;
import org.kie.workbench.common.stunner.core.client.canvas.Layer;
import org.uberfire.ext.editor.commons.client.file.exports.FileExport;
import org.uberfire.ext.editor.commons.client.file.exports.ImageDataUriContent;
import org.uberfire.ext.editor.commons.client.file.exports.PdfDocument;
import org.uberfire.ext.editor.commons.file.exports.FileExportsPreferences;
import org.uberfire.ext.editor.commons.file.exports.PdfExportPreferences;

/**
 * A helper client side bean that allows
 * exporting the canvas into different file types.
 */
@ApplicationScoped
public class CanvasFileExport {

    private static Logger LOGGER = Logger.getLogger(CanvasFileExport.class.getName());

    private static final String EXT_PNG = "png";
    private static final String EXT_JPG = "jpg";
    private static final String EXT_PDF = "pdf";

    private final CanvasExport<AbstractCanvasHandler> canvasExport;
    private final AsyncBeanLoader<FileExport<ImageDataUriContent>> imageFileExport;
    private final AsyncBeanLoader<FileExport<PdfDocument>> pdfFileExport;
    private final FileExportsPreferences preferences;

    protected CanvasFileExport() {
        this(null,
             null,
             null,
             null);
    }

    @Inject
    public CanvasFileExport(final CanvasExport<AbstractCanvasHandler> canvasExport,
                            final AsyncBeanLoader<FileExport<ImageDataUriContent>> imageFileExport,
                            final AsyncBeanLoader<FileExport<PdfDocument>> pdfFileExport,
                            final FileExportsPreferences preferences) {
        this.canvasExport = canvasExport;
        this.imageFileExport = imageFileExport;
        this.pdfFileExport = pdfFileExport;
        this.preferences = preferences;
    }

    public void exportToJpg(final AbstractCanvasHandler canvasHandler,
                            final String fileName) {
        exportImage(canvasHandler,
                    Layer.URLDataType.JPG,
                    fileName);
    }

    public void exportToPng(final AbstractCanvasHandler canvasHandler,
                            final String fileName) {
        exportImage(canvasHandler,
                    Layer.URLDataType.PNG,
                    fileName);
    }

    public void exportToPdf(final AbstractCanvasHandler canvasHandler,
                            final String fileName) {
        loadFileExportPreferences(prefs -> exportToPdf(canvasHandler,
                                                       fileName,
                                                       prefs.getPdfPreferences()));
    }

    private void exportToPdf(final AbstractCanvasHandler canvasHandler,
                             final String fileName,
                             final PdfExportPreferences pdfPreferences) {
        final String dataUrl = toDataImageURL(canvasHandler,
                                              Layer.URLDataType.JPG);
        final String title = canvasHandler.getDiagram().getMetadata().getTitle();
        final PdfDocument content = PdfDocument.create(PdfExportPreferences.create(PdfExportPreferences.Orientation.LANDSCAPE,
                                                                             pdfPreferences.getUnit(),
                                                                             pdfPreferences.getFormat()));
        content.addText(title,
                        5,
                        15);
        content.addImage(dataUrl,
                         EXT_JPG,
                         5,
                         40,
                         290,
                         150);
        pdfFileExport.call(exporter -> exporter.export(content, fileName + "." + EXT_PDF));
    }

    private void exportImage(final AbstractCanvasHandler canvasHandler,
                             final Layer.URLDataType type,
                             final String fileName) {
        final String dataUrl = toDataImageURL(canvasHandler,
                                              type);
        final ImageDataUriContent content = ImageDataUriContent.create(dataUrl);
        imageFileExport.call( exporter -> exporter.export(content, fileName + "." + getFileExtension(type)));
    }

    private String toDataImageURL(final AbstractCanvasHandler canvasHandler,
                                  final Layer.URLDataType urlDataType) {
        return canvasExport.toImageData(canvasHandler,
                                        urlDataType);
    }

    private static String getFileExtension(final Layer.URLDataType type) {
        switch (type) {
            case JPG:
                return EXT_JPG;
            case PNG:
                return EXT_PNG;
        }
        throw new UnsupportedOperationException("No mimeType supported for " + type);
    }

    private void loadFileExportPreferences(final Consumer<FileExportsPreferences> preferencesConsumer) {
        preferences.load(preferencesConsumer::accept,
                         error -> {
                             LOGGER.log(Level.SEVERE,
                                        "Cannot load preferences.",
                                        error);
                         });
    }
}
