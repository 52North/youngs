/*
 * Copyright 2015-2022 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.youngs.exception;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class SinkError extends Error {

    private static final long serialVersionUID = -5813801023326552216L;

    public SinkError() {
        //
    }

    public SinkError(Throwable cause, String format, Object... args) {
        super(String.format(format, args), cause);
    }

    public SinkError(String format, Object... args) {
        super(String.format(format, args));
    }

    public SinkError(Throwable exception) {
        super(exception);
    }

    public SinkError(String message, Throwable exception) {
        super(message, exception);
    }

}
