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

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.springframework.aot.hint.FieldHint;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
public class FieldHintAssert extends AbstractAssert<FieldHintAssert, FieldHint> {

	protected FieldHintAssert(FieldHint fieldHint) {
		super(fieldHint, FieldHintAssert.class);
	}

	public static FieldHintAssert assertThatFieldHint(FieldHint actual) {
		return new FieldHintAssert(actual);
	}

	public FieldHintAssert targets(Field field) {

		Assertions.assertThat(actual.getName()).isEqualTo(field.getName());
		return this;
	}

	public FieldHintAssert targetsField(String fieldName) {

		Assertions.assertThat(actual.getName()).isEqualTo(fieldName);
		return this;
	}

	public FieldHintAssert allowsWrite() {
		Assertions.assertThat(actual.isAllowWrite()).isTrue();
		return this;
	}

	public FieldHintAssert allowsUnsafeAccess() {
		Assertions.assertThat(actual.isAllowUnsafeAccess()).isTrue();
		return this;
	}
}
