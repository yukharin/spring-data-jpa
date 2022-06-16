/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import org.springframework.beans.BeanUtils;
import org.springframework.data.jpa.repository.QueryPostProcessor;

/**
 * Provide a {@link QueryPostProcessor} based upon the {@link JpaQueryMethod}. {@code QueryPostProcessor} instances may
 * be contextual or plain objects that are not attached to a bean factory or CDI context.
 *
 * @author Greg Turnquist
 * @since 3.0
 * @see QueryPostProcessor
 */
public interface QueryPostProcessorProvider {

	/**
	 * Return a simple {@code QueryPostProcessorProvider} that uses
	 * {@link org.springframework.beans.BeanUtils#instantiateClass(Class)} to obtain a {@link QueryPostProcessor}
	 * instance.
	 *
	 * @return a simple {@link QueryPostProcessorProvider}.
	 */
	static QueryPostProcessorProvider simple() {

		return method -> {

			Class<? extends QueryPostProcessor> queryPostProcessor = method.getQueryPostProcessor();

			if (queryPostProcessor == QueryPostProcessor.IdentityQueryPostProcessor.class) {
				return QueryPostProcessor.IdentityQueryPostProcessor.INSTANCE;
			}

			return BeanUtils.instantiateClass(queryPostProcessor);
		};
	}

	/**
	 * Obtain an instance of {@link QueryPostProcessor} for a {@link JpaQueryMethod}.
	 *
	 * @param method the underlying JPA query method.
	 * @return a Java bean that implements {@link QueryPostProcessor}.
	 */
	QueryPostProcessor getQueryPostProcessor(JpaQueryMethod method);
}
