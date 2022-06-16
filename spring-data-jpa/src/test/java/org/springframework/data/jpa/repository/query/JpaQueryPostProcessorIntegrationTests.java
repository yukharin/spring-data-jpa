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
package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.*;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryPostProcess;
import org.springframework.data.jpa.repository.QueryPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Unit tests for repository with {@link Query} and...
 *
 * @author Greg Turnquist
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
public class JpaQueryPostProcessorIntegrationTests {

	@Autowired private UserRepositoryWithPostProcessor repository;

	@Test
	void foo() {

		repository.saveAllAndFlush(List.of( //
				new User("Frodo", "Baggins", "ringdude@aol.com"), //
				new User("Bilbo", "Baggins", "riddler@hotmail.com"), //
				new User("Samwise", "Gamgee", "gardener@gmail.com")));

		List<User> results = repository.findByFirstname("Frodo");

		assertThat(results).hasSize(1);
		assertThat(results).extracting(User::getFirstname).containsExactly("Frodo");
		assertThat(results).extracting(User::getLastname).containsExactly("Baggins");
		assertThat(results).extracting(User::getEmailAddress).containsExactly("ringdude@gmail.com");
	}

public interface UserRepositoryWithPostProcessor
		extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

	@QueryPostProcess(queryPostProcessor = UserPostProcessor.class)
	List<User> findByFirstname(String firstname);
}

static class UserPostProcessor implements QueryPostProcessor {

	@Override
	public Object postProcess(Object results) {

		if (results instanceof Collection) {
			Collection<User> collection = (Collection<User>) results;
			return collection.stream().map(this::convert).toList();
		}

		return convert((User) results);
	}

	private User convert(User user) {
		return new User(user.getFirstname(), user.getLastname(), user.getEmailAddress().replace("aol.com", "gmail.com"),
				user.getRoles().toArray(new Role[0]));
	}
}



	@Configuration
	@ImportResource("classpath:infrastructure.xml")
	@EnableJpaRepositories(considerNestedRepositories = true, basePackageClasses = UserRepositoryWithPostProcessor.class, //
			includeFilters = @ComponentScan.Filter(value = { UserRepositoryWithPostProcessor.class },
					type = FilterType.ASSIGNABLE_TYPE))
	static class JpaRepositoryConfig {

		@Bean
		UserPostProcessor userPostProcessor() {
			return new UserPostProcessor();
		}
	}

}
