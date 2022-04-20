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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.aot.RepositoryBeanContribution;
import org.springframework.data.jpa.aot.configs.SimpleJpaConfig;

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

					System.out.println("contribution: " + contribution);
					//contribution.
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
			postProcessor.setBeanFactory(ctx.getDefaultListableBeanFactory());

			return postProcessor.contribute((RootBeanDefinition) beanDefinition, it, beanName);
		};
	}

	interface BeanContributionBuilder {

		RepositoryBeanContribution forRepository(Class<?> repositoryInterface);
	}
}
