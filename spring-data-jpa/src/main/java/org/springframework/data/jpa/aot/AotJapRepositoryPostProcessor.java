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

import java.lang.reflect.Modifier;

import org.springframework.aot.generator.CodeContribution;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.aot.AotContributingRepositoryBeanPostProcessor;
import org.springframework.data.aot.AotRepositoryContext;
import org.springframework.data.aot.RepositoryBeanContribution;
import org.springframework.data.aot.TypeUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
public class AotJapRepositoryPostProcessor extends AotContributingRepositoryBeanPostProcessor {

	@Override
	public RepositoryBeanContribution contribute(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
		return super.contribute(beanDefinition, beanType, beanName);
	}

	@Override
	protected void contribute(AotRepositoryContext ctx, CodeContribution contribution) {
		super.contribute(ctx, contribution);

		ctx.getResolvedTypes()
				.stream()
				.filter(it -> !isJavaOrPrimitiveType(it))
				.forEach(type -> {

					if (!ObjectUtils.isEmpty(type.getDeclaredClasses())) {
						contribution.runtimeHints().reflection()
								.registerType(type, hint -> hint.withMembers(MemberCategory.DECLARED_CLASSES));
					}

					ReflectionUtils.doWithFields(type, field -> {
						contribution.runtimeHints().reflection().registerField(field, hint -> hint.allowWrite(true).allowUnsafeAccess(true));
					}, field -> Modifier.isFinal(field.getModifiers()));

					if (ctx.resolveType("org.hibernate.Hibernate").isPresent()) { // only for you hibernate - xoxo
						ReflectionUtils.doWithFields(type, field -> {
							ctx.resolveType("org.hibernate.type.EnumType").ifPresent(it -> {
								contribution.runtimeHints().reflection().registerType(it, hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
							});
						}, field -> field.getType().isEnum());
					}
				});

		ctx.getResolvedAnnotations()
				.stream()
				.filter(it -> {
					return TypeUtils.isAnnotationFromOrMetaAnnotated(it.getType(), "jakarta.persistence") || TypeUtils.isAnnotationFromOrMetaAnnotated(it.getType(), "org.hibernate");
				})
				.forEach(it -> {
					contribution.runtimeHints().reflection().registerType(it.getType(), hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));
					if (it.isMetaPresent()) {
						// TODO: do we need extra handling here
					}

					it.asMap().entrySet().forEach(entry -> {
						if (entry.getValue() instanceof Class) {
							Class<?> attributeValue = (Class<?>) entry.getValue();
							if (!isJavaOrPrimitiveType(attributeValue)) {
								contribution.runtimeHints().reflection().registerType(attributeValue, hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.DECLARED_FIELDS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
							}
						} else if (entry.getValue() instanceof Class[]) {
							for (Class<?> attributeValue : (Class<?>[]) entry.getValue()) {
								if (!isJavaOrPrimitiveType(attributeValue)) {
									contribution.runtimeHints().reflection().registerType(attributeValue, hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.DECLARED_FIELDS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
								}
							}
						}
					});
				});
	}

	private static boolean isJavaOrPrimitiveType(Class<?> type) {
		if (TypeUtils.type(type).isPartOf("java") || type.isPrimitive() || ClassUtils.isPrimitiveArray(type)) {
			return true;
		}
		return false;
	}
}
