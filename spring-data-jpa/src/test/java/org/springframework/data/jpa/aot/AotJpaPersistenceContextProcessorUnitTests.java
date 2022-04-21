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

import static org.springframework.data.jpa.aot.BeanInstantiationContributionAssert.*;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.aot.generator.DefaultGeneratedTypeContext;
import org.springframework.aot.generator.GeneratedType;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.generator.BeanInstantiationContribution;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.generator.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.jpa.aot.configs.ComponentWithPersistenceContext;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.JavaFile;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
public class AotJpaPersistenceContextProcessorUnitTests {

	private static final ClassName MAIN_GENERATED_TYPE = ClassName.get("com.example", "Test");

	@Test
	void contributesReflectionForSettingPersistenceContext() {

		BeanInstantiationContribution instanceContribution = computeContribution(ComponentWithPersistenceContext.class);

		assertThat(instanceContribution).codeContributionSatisfies(code -> {
			code.contributesReflectionForField(ComponentWithPersistenceContext.class, "entityManager");
			// TODO: assert the code for setting the field via instanceContext
		});
	}

	@Test
	void ignoresNonPersistenceContextFields() {

		BeanInstantiationContribution instanceContribution = computeContribution(ComponentWithPersistenceContext.class);

		assertThat(instanceContribution).codeContributionSatisfies(contribution -> {
			contribution.doesNotContributeReflectionForField(ComponentWithPersistenceContext.class, "doesNotNeedReflection");
			// TODO: assert the code for setting the field via instanceContext is not present for the field in doubt.
		});
	}

	@Test
	@Disabled("just to see what happens")
	void xxx() {

		/*
		<bean id="entityManagerFactory"
		class="org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean">
		<property name="dataSource" ref="dataSource" />
		<property name="persistenceUnitName" value="spring-data-jpa" />
		<property name="jpaVendorAdapter" ref="vendorAdaptor" />
		<property name="jpaProperties" ref="jpaProperties" />
	</bean>

		 */

		GenericApplicationContext context = new GenericApplicationContext();

		// have the processor do the work
		//context.registerBean(AotJpaPersistenceContextProcessor.class);

		context.registerBeanDefinition("test", new RootBeanDefinition(ComponentWithPersistenceContext.class));

		compile(context, toFreshApplicationContext(GenericApplicationContext::new, aotContext -> {
			Assertions.assertThat(aotContext.getBeanDefinitionNames()).contains("test", "em");
			Assertions.assertThat(aotContext.getBean("test")).isInstanceOf(ComponentWithPersistenceContext.class);
		}));
	}

	BeanInstantiationContribution computeContribution(Class<?> component) {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.registerBeanDefinition(component.getSimpleName(), new RootBeanDefinition(component));
		ctx.refreshForAotProcessing();

		String[] beanNames = ctx.getBeanNamesForType(component);
		Assertions.assertThat(beanNames).describedAs("Unable to find component %s in context.", component)
				.hasSize(1);

		String beanName = beanNames[0];
		BeanDefinition beanDefinition = ctx.getBeanDefinition(beanName);

		AotJpaPersistenceContextProcessor aotProcessor = new AotJpaPersistenceContextProcessor();

		return aotProcessor.contribute((RootBeanDefinition) beanDefinition, component, beanName);
	}

	private void compile(GenericApplicationContext applicationContext, Consumer<ApplicationContextInitializer> initializer) {

		DefaultGeneratedTypeContext generationContext = createGenerationContext();
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		generator.generateApplicationContext(applicationContext, generationContext);
		//	SourceFiles sourceFiles = SourceFiles.none();
		for (JavaFile javaFile : generationContext.toJavaFiles()) {
			System.out.println(javaFile.toString());
			//		sourceFiles = sourceFiles.and(SourceFile.of((javaFile::writeTo)));
		}
//		TestCompiler.forSystem().withSources(sourceFiles).compile(compiled -> {
//			ApplicationContextInitializer instance = compiled.getInstance(ApplicationContextInitializer.class, MAIN_GENERATED_TYPE.canonicalName());
//			initializer.accept(instance);
//		});
	}

	private DefaultGeneratedTypeContext createGenerationContext() {
		return new DefaultGeneratedTypeContext(MAIN_GENERATED_TYPE.packageName(), packageName ->
				GeneratedType.of(ClassName.get(packageName, MAIN_GENERATED_TYPE.simpleName())));
	}

	private <T extends GenericApplicationContext> Consumer<ApplicationContextInitializer> toFreshApplicationContext(
			Supplier<T> applicationContextFactory, Consumer<T> context) {
		return applicationContextInitializer -> {
			T applicationContext = applicationContextFactory.get();
			applicationContextInitializer.initialize(applicationContext);
			applicationContext.refresh();
			context.accept(applicationContext);
		};
	}
}
