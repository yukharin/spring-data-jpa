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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.jpa.aot.RepositoryBeanContributionAssert.*;

import java.util.function.Predicate;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.ManagedTypes;
import org.springframework.data.aot.RepositoryBeanContribution;
import org.springframework.data.aot.TypeUtils;
import org.springframework.data.jpa.aot.configs.AuditableEntityJpaConfig;
import org.springframework.data.jpa.aot.configs.SimpleJpaConfig;
import org.springframework.data.jpa.domain.sample.AuditableEmbeddable;
import org.springframework.data.jpa.domain.sample.AuditableEntity;
import org.springframework.data.jpa.domain.sample.Customer;
import org.springframework.data.jpa.domain.sample.Order;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
public class AotJapRepositoryPostProcessorUnitTests {

	@Test
	void contributesProxiesForDataAnnotations() {

		RepositoryBeanContribution repositoryBeanContribution = computeConfiguration(SimpleJpaConfig.class)
				.forRepository(SimpleJpaConfig.OrderRepository.class);

		assertThatContribution(repositoryBeanContribution) //
				.codeContributionSatisfies(contribution -> {

					contribution.contributesReflectionFor(Order.class, Customer.class);
					contribution.contributesReflectionFor(Table.class, Entity.class, Id.class, ManyToOne.class);
				});
	}

	@Test
	void contributesEntityListeners() {

		RepositoryBeanContribution repositoryBeanContribution = computeConfiguration(AuditableEntityJpaConfig.class)
				.forRepository(AuditableEntityJpaConfig.AuditableEntityRepository.class);

		assertThatContribution(repositoryBeanContribution) //
				.codeContributionSatisfies(contribution -> {

					contribution.contributesReflectionFor(AuditableEntity.class, AuditableEmbeddable.class);
					contribution.contributesReflectionFor(Entity.class, Id.class, GeneratedValue.class, Embedded.class, EntityListeners.class);
					contribution.contributesReflectionFor(AuditingEntityListener.class);
				});
	}


	BeanContributionBuilder computeConfiguration(Class<?> configuration) {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(configuration);
		ctx.refreshForAotProcessing();

		return it -> {

			String[] repoBeanNames = ctx.getBeanNamesForType(it);
			assertThat(repoBeanNames).describedAs("Unable to find repository %s in configuration %s.", it, configuration)
					.hasSize(1);

			String beanName = repoBeanNames[0];
			BeanDefinition beanDefinition = ctx.getBeanDefinition(beanName);

			AotJapRepositoryPostProcessor postProcessor = ctx.getBean(AotJapRepositoryPostProcessor.class);
			// need to modify filter to not skip the org.springframework.data namespace
			postProcessor.filter = type -> !type.isPrimitive() && !ClassUtils.isPrimitiveArray(type) && !TypeUtils.type(type).isPartOf("java", "org.aopalliance");
			postProcessor.setBeanFactory(ctx.getDefaultListableBeanFactory());

			ctx.getBean(ManagedTypes.class).forEach(System.out::println);
			return postProcessor.contribute((RootBeanDefinition) beanDefinition, it, beanName);
		};
	}

	interface BeanContributionBuilder {

		RepositoryBeanContribution forRepository(Class<?> repositoryInterface);
	}
}
