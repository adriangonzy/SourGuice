package com.github.sourguice.test;

import org.mortbay.jetty.testing.HttpTester;
import org.testng.annotations.Test;

import com.github.sourguice.MvcServletModule;
import com.github.sourguice.annotation.request.RequestMapping;
import com.github.sourguice.annotation.request.RequestParam;
import com.github.sourguice.annotation.request.Writes;
import com.google.inject.Singleton;


public class BasicMappingTest extends TestBase {

	@Singleton
	public static class BMTController {
		@RequestMapping("/hello")
		@Writes
		public String helloWorld() {
			return "Hello, world";
		}

		@RequestMapping("/print")
		@Writes
		public String coucou(@RequestParam("txt") String txt) {
			return txt;
		}
}
	
	public static class BMTControllerModule extends MvcServletModule {
		@Override
		protected void configureControllers() {
			control("/*").with(BMTController.class);
		}
	}
	
	@Override
	protected MvcServletModule module() {
		return new BMTControllerModule();
	}

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

}
