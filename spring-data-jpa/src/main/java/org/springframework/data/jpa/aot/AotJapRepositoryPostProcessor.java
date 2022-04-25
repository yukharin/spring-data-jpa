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

import java.util.stream.Collectors;

import org.springframework.aot.generator.CodeContribution;
import org.springframework.data.aot.AotContributingRepositoryBeanPostProcessor;
import org.springframework.data.aot.AotRepositoryContext;
import org.springframework.data.aot.TypeUtils;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
public class AotJapRepositoryPostProcessor extends AotContributingRepositoryBeanPostProcessor {

	@Override
	protected void contribute(AotRepositoryContext ctx, CodeContribution contribution) {

		super.contribute(ctx, contribution);

		AotJpaEntityPostProcessor aotJpaEntityPostProcessor = new AotJpaEntityPostProcessor();
		aotJpaEntityPostProcessor.setBasePackages(ctx.getBasePackages()); // lookup entities in base packages
		aotJpaEntityPostProcessor.setManagedTypes(ctx.getResolvedTypes().stream().filter(it -> !isJavaOrPrimitiveType(it)).collect(Collectors.toSet())); // add the entities referenced from the repository
		aotJpaEntityPostProcessor.setBeanFactory(ctx.getBeanFactory()); // the context
		aotJpaEntityPostProcessor.contributeManagedTypes(ctx.getBeanFactory())
				.applyTo(contribution); // don't forget to apply stuff
	}


	private static boolean isJavaOrPrimitiveType(Class<?> type) {
		if (TypeUtils.type(type).isPartOf("java") || type.isPrimitive() || ClassUtils.isPrimitiveArray(type)) {
			return true;
		}
		return false;
	}
}
