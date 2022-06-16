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
package org.springframework.data.jpa.repository.cdi;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import java.util.Iterator;

import org.springframework.beans.BeanUtils;
import org.springframework.data.jpa.repository.QueryPostProcessor;
import org.springframework.data.jpa.repository.query.DelegatingQueryPostProcessor;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.repository.query.QueryPostProcessorProvider;
import org.springframework.data.util.Lazy;

/**
 * A {@link BeanManager}-based {@link QueryPostProcessor}.
 *
 * @author Greg Turnquist
 * @since 3.0
 */
public class BeanManagerQueryPostProcessorProvider implements QueryPostProcessorProvider {

	private final BeanManager beanManager;

	public BeanManagerQueryPostProcessorProvider(BeanManager beanManager) {
		this.beanManager = beanManager;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryPostProcessor getQueryPostProcessor(JpaQueryMethod method) {

		Class<? extends QueryPostProcessor> queryPostProcessor = method.getQueryPostProcessor();
		if (queryPostProcessor == QueryPostProcessor.IdentityQueryPostProcessor.class) {
			return QueryPostProcessor.IdentityQueryPostProcessor.INSTANCE;
		}

		Iterator<Bean<?>> iterator = beanManager.getBeans(queryPostProcessor).iterator();

		if (iterator.hasNext()) {

			Bean<QueryPostProcessor> bean = (Bean<QueryPostProcessor>) iterator.next();
			CreationalContext<QueryPostProcessor> context = beanManager.createCreationalContext(bean);
			Lazy<QueryPostProcessor> postPrcessor = Lazy
					.of(() -> (QueryPostProcessor) beanManager.getReference(bean, queryPostProcessor, context));

			return new DelegatingQueryPostProcessor(postPrcessor);
		}

		return BeanUtils.instantiateClass(queryPostProcessor);
	}

}
