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

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.aot.generator.CodeContribution;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.generator.AotContributingBeanPostProcessor;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.data.aot.AotContext;
import org.springframework.data.aot.AotManagedTypesPostProcessor;
import org.springframework.data.aot.TypeUtils;
import org.springframework.data.util.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Christoph Strobl
 */
public class AotJpaEntityPostProcessor extends AotManagedTypesPostProcessor implements AotContributingBeanPostProcessor, BeanFactoryAware {

	private static final Collection<JpaImplementation> JPA_IMPLEMENTATIONS = Arrays.asList(new HibernateJpaImplementation());

	public AotJpaEntityPostProcessor() {
		setModulePrefix("jpa");
	}

	@Nullable
	private BeanFactory beanFactory;

	@Nullable
	private Collection<String> packageNames;

	private Set<String> processedPackageNames = new LinkedHashSet<>();

	private Set<Class<?>> managedTypes = Collections.emptySet();

	@Override
	protected void contributeType(ResolvableType type, CodeContribution contribution) {
		processManagedType(type.toClass(), contribution, AotContext.context(beanFactory));
	}


	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		super.setBeanFactory(beanFactory);
		this.beanFactory = beanFactory;
	}

	private static boolean isJavaOrPrimitiveType(Class<?> type) {
		if (TypeUtils.type(type).isPartOf("java") || type.isPrimitive() || ClassUtils.isPrimitiveArray(type)) {
			return true;
		}
		return false;
	}

	private static Collection<JpaImplementation> availableJpaImplementation(ClassLoader classLoader) {
		return JPA_IMPLEMENTATIONS.stream().filter(it -> it.isAvailable(classLoader)).collect(Collectors.toSet());
	}

	static void processManagedType(Class<?> type, CodeContribution contribution, AotContext ctx) {

		if (type.isAnnotation()) {
			contribution.runtimeHints().reflection().registerType(type, hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));
		} else {
			contribution.runtimeHints().reflection().registerType(type, hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS));

			ReflectionUtils.doWithFields(type, field -> {
				contribution.runtimeHints().reflection().registerField(field, hint -> hint.allowWrite(true).allowUnsafeAccess(true));
			}, field -> Modifier.isFinal(field.getModifiers()));
		}

		if (!ObjectUtils.isEmpty(type.getDeclaredClasses())) { // TODO: should we have this in commons?
			contribution.runtimeHints().reflection()
					.registerType(type, hint -> hint.withMembers(MemberCategory.DECLARED_CLASSES));
		}

		invokeJpaImplementationSpecificTreatment(type, contribution, ctx);

		TypeUtils.resolveUsedAnnotations(type)
				.forEach(it -> {
					if (TypeUtils.isAnnotationFromOrMetaAnnotated(it.getType(), "jakarta.persistence")) {
						processPersistenceAnnotation(it, contribution, ctx);
					}
					invokeJpaImplementationSpecificTreatment(it, contribution, ctx);
				});
	}

	static void processPersistenceAnnotation(MergedAnnotation<Annotation> annotation, CodeContribution contribution, AotContext ctx) {

		contribution.runtimeHints().reflection().registerType(annotation.getType(), hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));
		if (annotation.isMetaPresent()) {
			// TODO: do we need extra handling here
		}

		annotation.asMap().entrySet().forEach(entry -> {
			if (entry.getValue() instanceof Class) {
				Class<?> attributeValue = (Class<?>) entry.getValue();
				if (!isJavaOrPrimitiveType(attributeValue)) {
					processManagedType(attributeValue, contribution, ctx);
				}
			} else if (entry.getValue() instanceof Class[]) {
				for (Class<?> attributeValue : (Class<?>[]) entry.getValue()) {
					if (!isJavaOrPrimitiveType(attributeValue)) {
						processManagedType(attributeValue, contribution, ctx);
					}
				}
			}
		});
	}

	static void invokeJpaImplementationSpecificTreatment(Class<?> type, CodeContribution contribution, AotContext ctx) {
		availableJpaImplementation(ctx.getClassLoader()).stream()
				.forEach(it -> it.processManagedType(type, contribution, ctx));
	}

	static void invokeJpaImplementationSpecificTreatment(MergedAnnotation<Annotation> annotation, CodeContribution contribution, AotContext ctx) {
		availableJpaImplementation(ctx.getClassLoader()).stream()
				.filter(it -> TypeUtils.isAnnotationFromOrMetaAnnotated(annotation.getType(), it.getNamespace()))
				.forEach(it -> it.processPersistenceAnnotation(annotation, contribution, ctx));
	}

	private interface JpaImplementation {

		String getNamespace();

		boolean isAvailable(ClassLoader classLoader);

		void processManagedType(Class<?> type, CodeContribution contribution, AotContext ctx);

		void processPersistenceAnnotation(MergedAnnotation<Annotation> annotation, CodeContribution contribution, AotContext ctx);
	}

	private static class HibernateJpaImplementation implements JpaImplementation {

		Lazy<Boolean> available = null;


		@Override
		public void processManagedType(Class<?> type, CodeContribution contribution, AotContext ctx) {

			ReflectionUtils.doWithFields(type, field -> {
				ctx.resolveType("org.hibernate.type.EnumType").ifPresent(it -> {
					contribution.runtimeHints().reflection().registerType(it, hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
				});
			}, field -> field.getType().isEnum());
		}

		@Override
		public void processPersistenceAnnotation(MergedAnnotation<Annotation> annotation, CodeContribution contribution, AotContext ctx) {
			AotJpaEntityPostProcessor.processPersistenceAnnotation(annotation, contribution, ctx);
		}

		@Override
		public String getNamespace() {
			return "org.hibernate";
		}

		@Override
		public boolean isAvailable(ClassLoader classLoader) {

			if (available == null) {
				available = Lazy.of(() -> ClassUtils.isPresent("org.hibernate.Hibernate", classLoader));
			}
			return available.get();
		}
	}
}
