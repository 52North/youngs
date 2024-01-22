/*
 * Copyright 2015-2024 52°North Spatial Information Research GmbH
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
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class SinkException extends Exception {

    private static final long serialVersionUID = -4813801023326552215L;

    public SinkException() {
        //
    }

    public SinkException(Throwable cause, String format, Object... args) {
        super(String.format(format, args), cause);
    }

    public SinkException(String format, Object... args) {
        super(String.format(format, args));
    }

    public SinkException(Throwable exception) {
        super(exception);
    }

    public SinkException(String message, Throwable exception) {
        super(message, exception);
    }

}
