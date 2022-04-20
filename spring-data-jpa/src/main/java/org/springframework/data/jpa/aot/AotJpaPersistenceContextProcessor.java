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
package org.springframework.data.jpa.aot;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.aot.generator.CodeContribution;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.generator.AotContributingBeanPostProcessor;
import org.springframework.beans.factory.generator.BeanInstantiationContribution;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.aot.TypeUtils;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
public class AotJpaPersistenceContextProcessor implements AotContributingBeanPostProcessor {

	private static final String JPA_PERSISTENCE_CONTEXT = "jakarta.persistence.PersistenceContext";

	@Override
	public BeanInstantiationContribution contribute(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {

		if (!TypeUtils.hasAnnotatedField(beanType, JPA_PERSISTENCE_CONTEXT)) {
			return null;
		}

		Set<InjectionPoint> injectionPoints = TypeUtils.getAnnotatedField(beanType, JPA_PERSISTENCE_CONTEXT)
				.stream()
				.map(InjectionPoint::new)
				.collect(Collectors.toSet());

		return new PersistenceContextContribution(injectionPoints);
	}

	@Override
	public int getOrder() {
		return 0;
	}

	record PersistenceContextContribution(
			Collection<InjectionPoint> injectionPoints) implements BeanInstantiationContribution {

		@Override
		public void applyTo(CodeContribution contribution) {

			for (InjectionPoint injectionPoint : injectionPoints) {
				contribution.runtimeHints().reflection()
						.registerType(injectionPoint.getDeclaredType(), hint -> {

							if (injectionPoint.getField() != null) {
								hint.withField(injectionPoint.getField().getName(), it -> it.allowWrite(true));
							}
							// TODO: do we need additional ones?
						});
			}
		}
	}
}
