/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.aot;

import java.util.Collection;

import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.util.CollectionUtils;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
public class AotBasePackageAwarePersistenceUnitPostProcessor implements PersistenceUnitPostProcessor {

	private Collection<String> packageNames;
	private Collection<String> managedTypes;

	public void setPackageNames(Collection<String> packageNames) {
		this.packageNames = packageNames;
	}

	public void setManagedTypes(Collection<String> managedTypes) {
		System.out.println("The managed types: " + managedTypes);
		this.managedTypes = managedTypes;
	}

	@Override
	public void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui) {
		System.out.println("pui: " + pui);
		if (!CollectionUtils.isEmpty(managedTypes)) {
			System.out.println("Adding managed Classes: " + managedTypes);
			managedTypes.forEach(pui::addManagedClassName);
		}
	}
}
