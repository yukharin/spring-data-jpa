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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aot.generator.CodeContribution;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.generator.AotContributingBeanPostProcessor;
import org.springframework.beans.factory.generator.BeanInstantiationContribution;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.aot.TypeScanner;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeName;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
public class AotPersistenceUnitPostProcessor implements AotContributingBeanPostProcessor, ResourceLoaderAware {

	private static final Log LOGGER = LogFactory.getLog(AotPersistenceUnitPostProcessor.class);

	private ResourceLoader resourceLoader;

	@Override
	public BeanInstantiationContribution contribute(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {

		if (!ClassUtils.isAssignable(AotBasePackageAwarePersistenceUnitPostProcessor.class, beanType)) {
			return null;
		}

		return new BeanInstantiationContribution() {

			@Override
			public void applyTo(CodeContribution contribution) {

				Set<String> packageNames = new LinkedHashSet<>();
				PropertyValue propertyValue = beanDefinition.getPropertyValues().getPropertyValue("packageNames");
				if (propertyValue.getValue() != null) {
					if (propertyValue.getValue() instanceof Collection col) {
						packageNames.addAll(col);
					}
				}

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(String.format("Preparing AotBasePackageAwarePersistenceUnitPostProcessor - scanning packages %s for managed types.", packageNames));
				}

				Set<String> managedTypeNames = lookupRelevantTypes(resourceLoader.getClassLoader(), packageNames).stream().map(Class::getName).collect(Collectors.toSet());

				ClassName set = ClassName.get("java.util", "LinkedHashSet");
				ClassName string = ClassName.get("java.lang", "String");
				TypeName setOfString = ParameterizedTypeName.get(set, string);

				contribution.statements().addStatement("$T managedTypes = new $T<>()", setOfString, set);
				for (String managedType : managedTypeNames) {
					contribution.statements().addStatement("managedTypes.add($S)", managedType);
				}
				contribution.statements().addStatement("bean.setManagedTypes(managedTypes)");
			}
		};
	}

	Set<Class<?>> lookupRelevantTypes(ClassLoader classLoader, Collection<String> packageNames) {

		return new TypeScanner(classLoader)
				.scanForTypesAnnotatedWith(jakarta.persistence.Entity.class, jakarta.persistence.MappedSuperclass.class, jakarta.persistence.Embeddable.class)
				.scanPackages(packageNames);
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}
}
