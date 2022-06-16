package org.springframework.data.jpa.repository;

@FunctionalInterface
public interface QueryPostProcessor {

	Object postProcess(Object results);

	/**
	 * A {@link QueryPostProcessor} that doesn't change the results.
	 */
	enum IdentityQueryPostProcessor implements QueryPostProcessor {

		INSTANCE;

		@Override
		public Object postProcess(Object results) {
			return results;
		}
	}

}
