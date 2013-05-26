package com.github.sourguice.test;

import org.eclipse.jetty.testing.HttpTester;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.github.sourguice.MvcServletModule;
import com.github.sourguice.annotation.request.RequestMapping;
import com.github.sourguice.annotation.request.RequestParam;
import com.github.sourguice.annotation.request.Writes;
import com.github.sourguice.value.RequestMethod;
import com.google.inject.Singleton;

@SuppressWarnings("javadoc")
public class RequestMappingTest extends TestBase {

	// ===================== CONTROLLER =====================

	@Singleton
	public static class Controller {
		@RequestMapping("/hello")
		@Writes
		public String helloWorld() {
			return "Hello, world";
		}

		@RequestMapping(value = "/print", method = RequestMethod.POST)
		@Writes
		public String coucou(@RequestParam("txt") String txt) {
			return txt;
		}

		@RequestMapping(value = "/test-method", method = RequestMethod.GET)
		@Writes
		public String testMethodGet() {
			return "Get";
		}

		@RequestMapping(value = "/test-method", method = RequestMethod.POST)
		@Writes
		public String testMethodPost() {
			return "Post";
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
			ret[i] = new Object[] {
					rms[i].toString(),
					Boolean.valueOf(rms[i] == RequestMethod.GET
							|| rms[i] == RequestMethod.POST) };
		return ret;
	}

	// ===================== TESTS =====================

	@Test
	public void simpleWrite() throws Exception {
		HttpTester request = makeRequest("GET", "/hello");

		HttpTester response = getResponse(request);

		assert response.getStatus() == 200;
		assert response.getContent().equals("Hello, world");
	}

	@Test
	public void writeWithArguments() throws Exception {
		HttpTester request = makeRequest("POST", "/print");

		addPost(request, "txt", "hello the world");

		HttpTester response = getResponse(request);

		assert response.getStatus() == 200;
		assert response.getContent().equals("hello the world");
	}

	@Test
	public void expect404() throws Exception {
		HttpTester request = makeRequest("GET", "/not-existing");

		HttpTester response = getResponse(request);

		assert response.getStatus() == 404;
	}

	@Test
	public void missingArgument() throws Exception {
		HttpTester request = makeRequest("POST", "/print");

		HttpTester response = getResponse(request);

		assert response.getStatus() == 400;
	}

	@Test
	public void invalidMethod() throws Exception {
		HttpTester request = makeRequest("GET", "/print");

		HttpTester response = getResponse(request);

		assert response.getStatus() == 404;
	}

	@Test(dataProvider = "requestMethods")
	public void findRequestMethods(String method, boolean shouldFind)
			throws Exception {
		HttpTester request = makeRequest(method, "/test-method");

		HttpTester response = getResponse(request);

		assert response.getStatus() == (shouldFind ? 200 : 404);
	}

}
