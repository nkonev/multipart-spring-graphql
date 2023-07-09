/*
 * Copyright 2020-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package name.nkonev.multipart.spring.graphql.client;


import org.springframework.graphql.client.ClientGraphQlRequest;
import org.springframework.graphql.support.DefaultGraphQlRequest;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MultipartClientGraphQlRequest extends DefaultGraphQlRequest implements ClientGraphQlRequest {

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    private final Map<String, Object> fileVariables = new ConcurrentHashMap<>();

    public MultipartClientGraphQlRequest(
			String document, @Nullable String operationName,
			Map<String, Object> variables, Map<String, Object> extensions,
			Map<String, Object> attributes,
            Map<String, Object> fileVariables) {

        super(document, operationName, variables, extensions);
        this.attributes.putAll(attributes);
        this.fileVariables.putAll(fileVariables);
	}

    public Map<String, Object> getFileVariables() {
        return this.fileVariables;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

}
