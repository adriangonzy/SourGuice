package com.github.sourguice.test;

import org.eclipse.jetty.testing.HttpTester;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.github.sourguice.MvcServletModule;
import com.github.sourguice.annotation.request.RequestMapping;
import com.github.sourguice.annotation.request.Writes;
import com.github.sourguice.value.RequestMethod;
import com.google.inject.Singleton;

@SuppressWarnings("javadoc")
public class InjectionTest extends TestBase {

	// ===================== CONTROLLER =====================

	@Singleton
	public static class Controller {
		@RequestMapping("/get-method")
		@Writes
		public String getMethod(RequestMethod m) {
			return ":" + m.toString();
		}
	}

	// ===================== MODULE =====================

	public static class BMTControllerModule extends MvcServletModule {
		@Override
		protected void configureControllers() {
			control("/*").with(Controller.class);
		}
	}

	@Override
	protected MvcServletModule module() {
		return new BMTControllerModule();
	}

	// ===================== DATA PROVIDERS =====================

	@DataProvider(name = "requestMethods")
	public Object[][] createRequestMethods() {
		RequestMethod[] rms = RequestMethod.values();
		Object[][] ret = new Object[rms.length][];
		for (int i = 0; i < rms.length; ++i)
			ret[i] = new Object[] { rms[i].toString() };
		return ret;
	}

	// ===================== TESTS =====================

	@Test(dataProvider = "requestMethods")
	public void getRequestMethods(String method) throws Exception {
		if (method.equals("HEAD"))
			return ;
		
		HttpTester request = makeRequest(method, "/get-method");

		HttpTester response = getResponse(request);
		
		assert response.getStatus() == 200;
		assert response.getContent().equals(":" + method);
	}

}

