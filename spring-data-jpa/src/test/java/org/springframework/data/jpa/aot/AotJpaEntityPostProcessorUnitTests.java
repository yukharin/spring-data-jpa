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

import static org.springframework.data.jpa.aot.RepositoryBeanContributionAssert.*;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.generator.BeanInstantiationContribution;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.aot.RepositoryBeanContribution;
import org.springframework.data.jpa.aot.AotJapRepositoryPostProcessorUnitTests.BeanContributionBuilder;
import org.springframework.data.jpa.aot.configs.SimpleJpaConfig;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
public class AotJpaEntityPostProcessorUnitTests {

	@Test
	@Disabled("nah - need a better solution - actually one that works")
	void x2() {

		BeanInstantiationContribution instantiationContribution = computeConfiguration(SimpleJpaConfig.class);

		BeanInstantiationContributionAssert.assertThat(instantiationContribution).codeContributionSatisfies(it -> {
			System.out.println("it: " + it);
		});

	}

	BeanInstantiationContribution computeConfiguration(Class<?> configuration) {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(configuration);
		ctx.refreshForAotProcessing();

		String[] repoBeanNames = ctx.getBeanNamesForType(EntityManager.class);

		String beanName = repoBeanNames[0];
		BeanDefinition beanDefinition = ctx.getBeanDefinition(beanName);

		AotJpaEntityPostProcessor postProcessor = new AotJpaEntityPostProcessor();
		postProcessor.setBeanFactory(ctx.getDefaultListableBeanFactory());

		try {
			return postProcessor.contribute((RootBeanDefinition) beanDefinition, ClassUtils.forName(beanDefinition.getBeanClassName(), ctx.getClassLoader()), beanName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
