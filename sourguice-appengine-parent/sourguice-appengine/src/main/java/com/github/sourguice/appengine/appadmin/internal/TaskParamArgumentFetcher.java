package com.github.sourguice.appengine.appadmin.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.annotation.CheckForNull;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import com.github.sourguice.appengine.appadmin.annotation.AdminTaskParam;
import com.github.sourguice.call.CalltimeArgumentFetcher;
import com.github.sourguice.conversion.ConversionService;
import com.github.sourguice.utils.Annotations;
import com.googlecode.gentyref.GenericTypeReflector;

public final class TaskParamArgumentFetcher implements CalltimeArgumentFetcher<Object> {

	private ConversionService _conversion;
	private HttpServletRequest _req;
	
	@Inject
	public TaskParamArgumentFetcher(ConversionService conversion, HttpServletRequest req) {
		_conversion = conversion;
		_req = req;
	}
	
	@Override
	public boolean canGet(Type type, int pos, Annotation[] annos) {
		return Annotations.fromArray(annos).isAnnotationPresent(AdminTaskParam.class);
	}

	@Override
	public @CheckForNull Object get(Type type, int pos, Annotation[] annos) {
		String param = _req.getParameter("param:" + pos);
		if (param == null)
			param = "";
		return _conversion.convert(GenericTypeReflector.erase(type), param);
	}

}
