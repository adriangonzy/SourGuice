package com.github.sourguice.test;

import java.io.IOException;
import java.net.URLEncoder;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.testing.HttpTester;
import org.eclipse.jetty.testing.ServletTester;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import com.github.sourguice.MvcServletModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;

@SuppressWarnings("javadoc")
public abstract class TestBase {

	public static class StandardContextListener<T extends MvcServletModule> extends GuiceServletContextListener {

		T module;
		
		public StandardContextListener(T module) {
			super();
			this.module = module;
		}

		@Override
		protected Injector getInjector() {
			return Guice.createInjector(module);
		}

	}

	protected ServletTester	tester = null;

	@BeforeClass
	public void startupServletTester() throws Exception {
		tester = new ServletTester();
		tester.setContextPath("/");
		tester.addEventListener(new StandardContextListener<>(module()));
		tester.addFilter(GuiceFilter.class, "/*", 0);
		tester.addServlet(DefaultServlet.class, "/");
		tester.start();
	}

	@AfterClass
	public void teardownServletTester() throws Exception {
		tester.stop();
	}

	public HttpTester makeRequest(String method, String uri) {
		HttpTester request = new HttpTester();
		request.setMethod(method);
		request.setURI(uri);
		request.setHeader("Host", "tester");
		return request;
	}
	
	public void addPost(HttpTester request, String param, String value) throws IOException {
		String post = URLEncoder.encode(param, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
		String content = request.getContent();
		if (content != null && !content.isEmpty())
			content += "&" + post;
		else
			content = post;
		request.setContent(content);
		request.setHeader("content-type", "application/x-www-form-urlencoded");
		request.setHeader("content-length", String.valueOf(content.length()));
	}
	
	public HttpTester getResponse(HttpTester request, boolean debug) throws Exception {
		HttpTester response = new HttpTester();
		
		String reqTxt = request.generate();
		if (debug) {
			System.out.println("==========================================");
			System.out.println("REQUEST:");
			System.out.println(reqTxt);
		}
		
		String resTxt = tester.getResponses(reqTxt);
		if (debug) {
			System.out.println("==========================================");
			System.out.println("RESPONSE:");
			System.out.println(resTxt);
			System.out.println("==========================================");
		}
		
		response.parse(resTxt);

		return response;
	}
	
	public HttpTester getResponse(HttpTester request) throws Exception {
		return getResponse(request, false);
	}
	
	abstract protected MvcServletModule module();
}
