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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Stream;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.springframework.aot.generator.CodeContribution;
import org.springframework.aot.generator.ProtectedAccess;
import org.springframework.aot.hint.ClassProxyHint;
import org.springframework.aot.hint.JdkProxyHint;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;
import org.springframework.javapoet.support.MultiStatement;
import org.springframework.util.ReflectionUtils;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
public class CodeContributionAssert extends AbstractAssert<CodeContributionAssert, CodeContribution>
		implements CodeContribution {

	public CodeContributionAssert(CodeContribution contribution) {
		super(contribution, CodeContributionAssert.class);
	}

	public static CodeContributionAssert assertThat(CodeContribution codeContribution) {
		return new CodeContributionAssert(codeContribution);
	}

	public CodeContributionAssert contributesReflectionFor(Class<?>... types) {

		for (Class<?> type : types) {
			Assertions.assertThat(this.actual.runtimeHints().reflection().getTypeHint(type))
					.describedAs("No reflection entry found for %s", type).isNotNull();
		}
		return this;
	}

	public CodeContributionAssert contributesReflectionForField(Class<?> type, String fieldName) {

		Field field = ReflectionUtils.findField(type, fieldName);
		Assertions.assertThat(field).isNotNull();
		contributesReflectionForField(field);
		return this;
	}

	public CodeContributionAssert doesNotContributeReflectionForField(Class<?> type, String fieldName) {

		Field field = ReflectionUtils.findField(type, fieldName);
		Assertions.assertThat(field).isNotNull();
		Assertions.assertThatExceptionOfType(java.lang.AssertionError.class)
				.describedAs("Found unexpected reflection entry for field %s.%s", field.getDeclaringClass().getSimpleName(), field.getName())
				.isThrownBy(() -> contributesReflectionForField(field));
		return this;
	}

	public CodeContributionAssert contributesReflectionForField(Field field) {

		TypeHint typeHint = this.actual.runtimeHints().reflection().getTypeHint(TypeReference.of(field.getDeclaringClass()));
		Assertions.assertThat(typeHint)
				.describedAs("No reflection entry found for %s", field.getDeclaringClass()).isNotNull();

		Assertions.assertThat(typeHint.fields())
				.describedAs("No reflection entry found for field %s.%s", field.getDeclaringClass().getSimpleName(), field.getName())
				.anySatisfy(it -> {
					FieldHintAssert.assertThatFieldHint(it).targets(field);
				});
		return this;
	}

	public CodeContributionAssert doesNotContributeCodeBlock() {

		Assertions.assertThat(this.actual.statements().toCodeBlock()).isNull();
		return this;
	}

	public CodeContributionAssert contributesJdkProxyFor(Class<?> entryPoint) {
		Assertions.assertThat(jdkProxiesFor(entryPoint).findFirst()).describedAs("No jdk proxy found for %s", entryPoint).isPresent();
		return this;
	}

	public CodeContributionAssert doesNotContributeJdkProxyFor(Class<?> entryPoint) {
		Assertions.assertThat(jdkProxiesFor(entryPoint).findFirst()).describedAs("Found jdk proxy matching %s though it should not be present.", entryPoint).isNotPresent();
		return this;
	}

	public CodeContributionAssert doesNotContributeJdkProxy(Class<?>... proxyInterfaces) {

		Assertions.assertThat(jdkProxiesFor(proxyInterfaces[0])).describedAs("Found jdk proxy matching %s though it should not be present.", Arrays.asList(proxyInterfaces)).noneSatisfy(it -> {
			new JdkProxyAssert(it).matches(proxyInterfaces);
		});
		return this;
	}

	public CodeContributionAssert contributesJdkProxy(Class<?>... proxyInterfaces) {

		Assertions.assertThat(jdkProxiesFor(proxyInterfaces[0])).describedAs("Unable to find jdk proxy matching %s", Arrays.asList(proxyInterfaces)).anySatisfy(it -> {
			new JdkProxyAssert(it).matches(proxyInterfaces);
		});

		return this;
	}

	public CodeContributionAssert contributesClassProxy(Class<?>... proxyInterfaces) {

		Assertions.assertThat(classProxiesFor(proxyInterfaces[0])).describedAs("Unable to find jdk proxy matching %s", Arrays.asList(proxyInterfaces)).anySatisfy(it -> {
			new ClassProxyAssert(it).matches(proxyInterfaces);
		});

		return this;
	}

	private Stream<JdkProxyHint> jdkProxiesFor(Class<?> entryPoint) {
		return this.actual.runtimeHints().proxies().jdkProxies().filter(jdkProxyHint -> {
			return jdkProxyHint.getProxiedInterfaces().get(0).getCanonicalName().equals(entryPoint.getCanonicalName());
		});
	}

	private Stream<ClassProxyHint> classProxiesFor(Class<?> entryPoint) {
		return this.actual.runtimeHints().proxies().classProxies().filter(jdkProxyHint -> {
			return jdkProxyHint.getProxiedInterfaces().get(0).getCanonicalName().equals(entryPoint.getCanonicalName());
		});
	}

	public MultiStatement statements() {
		return actual.statements();
	}

	public RuntimeHints runtimeHints() {
		return actual.runtimeHints();
	}

	public ProtectedAccess protectedAccess() {
		return actual.protectedAccess();
	}


}
