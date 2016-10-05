package io.yancey.menufetcher;

import java.time.*;

import joptsimple.*;

public class LocalDateValueConverter implements ValueConverter<LocalDate> {
	@Override
	public LocalDate convert(String value) {
		return LocalDate.parse(value);
	}

	@Override
	public Class<? extends LocalDate> valueType() {
		return LocalDate.class;
	}

	@Override
	public String valuePattern() {
		return "yyyy-MM-dd";
	}
}
