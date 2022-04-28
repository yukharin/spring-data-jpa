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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.util.CollectionUtils;

/**
 * @author Christoph Strobl
 * @since 2022/04
 */
public class AotBasePackageAwarePersistenceUnitPostProcessor implements PersistenceUnitPostProcessor {

	private static final Log LOGGER = LogFactory.getLog(AotBasePackageAwarePersistenceUnitPostProcessor.class);

	@Nullable
	private Collection<String> packageNames; // just here for the AOT post processor

	@Nullable
	private Collection<String> managedTypes;

	@Override
	public void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui) {

		if (!CollectionUtils.isEmpty(managedTypes)) {

			if (LOGGER.isInfoEnabled()) {
				LOGGER.info(String.format("Adding managed types: %s", managedTypes));
			}
			managedTypes.forEach(pui::addManagedClassName);
		}
	}

	public void setPackageNames(Collection<String> packageNames) {
		this.packageNames = packageNames;
	}

	public void setManagedTypes(Collection<String> managedTypes) {
		this.managedTypes = managedTypes;
	}
}
