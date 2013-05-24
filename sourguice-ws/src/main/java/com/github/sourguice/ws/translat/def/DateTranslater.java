package com.github.sourguice.ws.translat.def;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.annotation.CheckForNull;

import com.github.sourguice.ws.translat.WSSimpleGuiceTranslaterFactory;
import com.github.sourguice.ws.translat.WSTranslater;
import com.google.inject.Singleton;

public class DateTranslater implements WSTranslater<Date, String> {

	@Singleton
	public static class DateTranslaterFactory extends WSSimpleGuiceTranslaterFactory<Date, String> {

		public DateTranslaterFactory() {
			super(Date.class, String.class, DateTranslater.class);
		}
		
		@Override public boolean isInternal() {
			return true;
		}
	}

	private SimpleDateFormat formater;
	
	public DateTranslater() {
		formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		formater.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	@Override
	public @CheckForNull
	String toWS(Date obj, Type typeOnServer, AnnotatedElement el, @CheckForNull Object enclosing) {
		return formater.format(obj);
	}

	@Override
	public @CheckForNull
	Date fromWS(String obj, Type typeOnServer, AnnotatedElement el, @CheckForNull Object enclosing) {
		try {
			return formater.parse(obj);
		}
		catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
}
