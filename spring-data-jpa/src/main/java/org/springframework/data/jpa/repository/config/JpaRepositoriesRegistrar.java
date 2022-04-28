/*
 * Copyright 2012-2022 the original author or authors.
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
package org.springframework.data.jpa.repository.config;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.jpa.aot.AotBasePackageAwarePersistenceUnitPostProcessor;
import org.springframework.data.jpa.aot.AotPersistenceUnitPostProcessor;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationSource;

/**
 * {@link ImportBeanDefinitionRegistrar} to enable {@link EnableJpaRepositories} annotation.
 *
 * @author Oliver Gierke
 */
class JpaRepositoriesRegistrar extends RepositoryBeanDefinitionRegistrarSupport implements ResourceLoaderAware, EnvironmentAware {

	ResourceLoader resourceLoader;
	private Environment environment;

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableJpaRepositories.class;
	}

	@Override
	protected RepositoryConfigurationExtension getExtension() {
		return new JpaRepositoryConfigExtension();
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry, BeanNameGenerator generator) {
		super.registerBeanDefinitions(metadata, registry, generator);

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(AotBasePackageAwarePersistenceUnitPostProcessor.class);
		RepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(metadata, getAnnotation(), resourceLoader, environment, registry, generator);

		if (configurationSource.getBasePackages() != null) {

			Set<String> packageNames = configurationSource.getBasePackages().stream().collect(Collectors.toSet());
			builder.addPropertyValue("packageNames", packageNames);
		}

		AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
		String beanName = generator.generateBeanName(beanDefinition, registry);

		if (!registry.isBeanNameInUse(beanName)) {
			registry.registerBeanDefinition(beanName, beanDefinition);
			registry.registerBeanDefinition("aot.pui.pp", BeanDefinitionBuilder.rootBeanDefinition(AotPersistenceUnitPostProcessor.class).getBeanDefinition());
		}
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		super.setResourceLoader(resourceLoader);
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setEnvironment(Environment environment) {
		super.setEnvironment(environment);
		this.environment = environment;
	}
}
