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
import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.jpa.repository.QueryPostProcessor;
import org.springframework.data.util.Lazy;

/**
 * A {@link BeanFactory}-based {@link QueryPostProcessor}.
 *
 * @author Greg Turnquist
 * @since 3.0
 */
public class BeanFactoryQueryPostProcessorProvider implements QueryPostProcessorProvider {

	private final BeanFactory beanFactory;

	public BeanFactoryQueryPostProcessorProvider(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryPostProcessor getQueryPostProcessor(JpaQueryMethod method) {

		Class<? extends QueryPostProcessor> queryPostProcessor = method.getQueryPostProcessor();
		if (queryPostProcessor == QueryPostProcessor.IdentityQueryPostProcessor.class) {
			return QueryPostProcessor.IdentityQueryPostProcessor.INSTANCE;
		}

		Lazy<QueryPostProcessor> postProcessor = Lazy
				.of(() -> beanFactory.getBeanProvider((Class<QueryPostProcessor>) queryPostProcessor)
						.getIfAvailable(() -> BeanUtils.instantiateClass(queryPostProcessor)));

		return new DelegatingQueryPostProcessor(postProcessor);
	}
}
