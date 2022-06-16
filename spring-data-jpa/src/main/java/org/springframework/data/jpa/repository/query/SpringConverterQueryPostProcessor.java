package org.springframework.data.jpa.repository.query;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.jpa.repository.QueryPostProcessor;

/**
 * A {@link QueryPostProcessor} that uses a Spring Framework {@link Converter} under the hood to transform.
 *
 * @author Greg Turnquist
 * @since 3.0
 */
public class SpringConverterQueryPostProcessor implements QueryPostProcessor {

	private final Converter<Object, Object> converter;

	public SpringConverterQueryPostProcessor(Converter<Object, Object> converter) {
		this.converter = converter;
	}

	@Override
	public Object postProcess(Object results) {
		return converter.convert(results);
	}
}
