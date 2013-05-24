package com.github.sourguice.appengine.appadmin.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import com.github.sourguice.appengine.appadmin.AppAdminController;
import com.github.sourguice.appengine.appadmin.MaintenanceFilter;
import com.github.sourguice.appengine.appadmin.AppAdminController.AaTask;
import com.github.sourguice.appengine.appadmin.AppAdminController.TaskParamInfos;
import com.github.sourguice.appengine.appadmin.annotation.AdminTask;
import com.github.sourguice.appengine.appadmin.annotation.CronTask;
import com.github.sourguice.view.def.BasicViewRenderer;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.inject.servlet.RequestScoped;

@RequestScoped
public final class AppAdminViewRenderer extends BasicViewRenderer {

	@Inject
	public AppAdminViewRenderer(HttpServletResponse res) {
		super(res);
	}

	public static final String VIEW_INDEX = "index";
	public static final String VIEW_PREPARE_ADMIN = "prepare_admin";
	public static final String VIEW_PREPARE_CRON = "prepare_cron";
	public static final String VIEW_TASK = "task";
	public static final String VIEW_MAINTENANCE = "maintenance";

	private String _head() {
		return
				"	<head>" +
				"		<title>App Admin</title>" +
				"		<style type='text/css'>" +
				"			* {" +
				"				padding: 0;" +
				"				margin: 0;" +
				"			}" +
				"			body {" +
				"				font-family: sans-serif;" +
				"				width: 65em;" +
				"				margin: auto;" +
				"			}" +
				"			a {" +
				"				text-decoration: none;" +
				"				color: blue;" +
				"			}" +
				"			a:hover {" +
				"				text-decoration: underline;" +
				"			}" +
				"			ul#menu {" +
				"				float: left;" +
				"				border: solid #555 1px;" +
				"				border-radius: 8px;" +
				"				list-style-type: none;" +
				"				margin: 1em 3em 2em 2em;" +
				"				padding: 1em 0.5em 0 1em;" +
				"			}" +
				"			ul#menu ul {" +
				"				list-style-type: none;" +
				"				padding-bottom: 1em;" +
				"			}" +
				"			ul#menu ul li {" +
				"				padding: 0.25em 0.5em 0.15em 1em;" +
				"			}" +
				"			ul#menu ul li.current {" +
				"				background-color: #DDF;" +
				"			}" +
				"			#content {" +
				"				float: left;" +
				"				padding: 1.5em 2em 2em 0;" +
				"				width: 42em;" +
				"			}" +
				"			h1 {" +
				"				padding-bottom: 0.25em;" +
				"			}" +
				"			h2 {" +
				"				font-size: 1.25em;" +
				"				padding-bottom: 1em;" +
				"			}" +
				"			#content form p {" +
				"				padding-bottom: 0.75em;" +
				"			}" +
				"			#content form input[type='text'], #content form textarea {" +
				"				border: solid black 1px;" +
				"				border-radius: 5px;" +
				"				font-size: 0.95em;" +
				"				padding: 0 8px;" +
				"				width: 20em;" +
				"				height: 1.6em;" +
				"				outline: none;" +
				"				vertical-align: middle;" +
				"			}" +
				"			#content form textarea {" +
				"				height: 4em;" +
				"			}" +
				"			#content form input[type='text'].number {" +
				"				width: 7.5em;" +
				"			}" +
				"			#content form input[type='text']:focus, #content form textarea:focus {" +
				"				border-color: #22C;" +
				"				border-width: 2px 4px;" +
				"				padding: 0 5px;" +
				"			}" +
				"			#content form button {" +
				"				border: solid black 1px;" +
				"				border-radius: 5px;" +
				"				font-size: 1.1em;" +
				"				background-color: #DDD;" +
				"				padding: 0.2em 0.8em;" +
				"				cursor: pointer;" +
				"				margin: 1px;" +
				"			}" +
				"			#content form button:hover {" +
				"				border: solid #22C 2px;" +
				"				margin: 0;" +
				"			}" +
				"			#content form button:active {" +
				"				border: solid #22C 2px;" +
				"				margin: 0;" +
				"				color: #EEE;" +
				"				background-color: #888;" +
				"			}" +
				"		</style>" +
				"	</head>"
				;
	}
	
	@SuppressWarnings("unchecked")
	private String _menu(Map<String, Object> model, String current) {
		StringBuilder b = new StringBuilder();
		b.append("<ul id='menu'>");
		
		Collection<String> taskList = (Collection<String>)model.get("taskList");
		if (!taskList.isEmpty()) {
			b.append("<li><b> Tasks </b>");
			b.append("<ul>");
			for (String t : taskList) {
				if (current != null && current.equals(t))
					b.append("<li class='current'>");
				else
					b.append("<li>");
				b.append("<a href='/_aa/task-" + t + "'>" + t.substring(0, 1).toUpperCase() + t.substring(1) + "</a>");
				b.append("</li>");
			}
			b.append("</ul>");
		}

		Collection<String> cronList = (Collection<String>)model.get("cronList");
		if (!cronList.isEmpty()) {
			b.append("<li><b> Crons </b>");
			b.append("<ul>");
			for (String t : cronList) {
				if (current != null && current.equals(t))
					b.append("<li class='current'>");
				else
					b.append("<li>");
				b.append("<a href='/_aa/cron-" + t + "'>" + t.substring(0, 1).toUpperCase() + t.substring(1) + "</a>");
				b.append("</li>");
			}
			b.append("</ul>");
		}

		if (AppAdminController.AppstatsEnabled || MaintenanceFilter.FilterEnabled) {
			b.append("<li><b> Development </b>");
			b.append("<ul>");
			if (AppAdminController.AppstatsEnabled)
				b.append("<li><a href='appstats'>AppStats</a></li>");
			if (MaintenanceFilter.FilterEnabled) {
				if (current != null && current.equals("maintenance"))
					b.append("<li class='current'>");
				else
					b.append("<li>");
				b.append("<a href='maintenance'>Maintenance</a>");
				b.append("</li>");
			}
			b.append("</ul>");
		}
		b.append("</ul>");

		return b.toString();
	}
	
	@RenderFor(VIEW_INDEX)
	public void index(PrintWriter writer, Map<String, Object> model) throws IOException {
		writer.write(
				"<html>" +
				_head() +
				"	<body>" +
				_menu(model, null) +
				"		<div id='content'>" +
				"			<h1> " + AppIdentityServiceFactory.getAppIdentityService().getServiceAccountName() + " </h1>" +
				"		</div>" +
				"	</body>" +
				"</html>"
				);
	}

	@RenderFor(VIEW_PREPARE_ADMIN)
	public void prepare_admin(PrintWriter writer, Map<String, Object> model) throws IOException {
		AdminTask task = (AdminTask)model.get("task");

		@SuppressWarnings("unchecked")
		List<TaskParamInfos> params = (List<TaskParamInfos>)model.get("params");
		StringBuilder str = new StringBuilder();
		for (TaskParamInfos infos : params) {
			if (infos.clazz.equals(boolean.class) || Boolean.class.isAssignableFrom(infos.clazz))
				str.append("<p>" +
						"<input type='checkbox' name='param:" + infos.pos + "' id='param_" + infos.pos + "' value='1' />" +
						" <label for='param_" + infos.pos + "'>" + infos.param.value() + "</label>" +
						"</p>");
			else if (
						infos.clazz.equals(char.class)
					||	infos.clazz.equals(byte.class)
					||	infos.clazz.equals(short.class)
					||	infos.clazz.equals(int.class)
					||	infos.clazz.equals(long.class)
					||	infos.clazz.equals(float.class)
					||	infos.clazz.equals(double.class)
					||	Number.class.isAssignableFrom(infos.clazz)
					)
				str.append("<p>" +
						"<label for='param_" + infos.pos + "'>" + infos.param.value() + "</label>: " +
						"<input type='text' class='number' name='param:" + infos.pos + "' id='param_" + infos.pos + "' value='' />" +
						"</p>");
			else if (infos.param.longText() && String.class.isAssignableFrom(infos.clazz))
				str.append("<p>" +
						"<label for='param_" + infos.pos + "'>" + infos.param.value() + "</label>: " +
						"<textarea name='param:" + infos.pos + "' id='param_" + infos.pos + "'></textarea>" +
						"</p>");
			else
				str.append("<p>" +
						"<label for='param_" + infos.pos + "'>" + infos.param.value() + "</label>: " +
						"<input type='text' name='param:" + infos.pos + "' id='param_" + infos.pos + "' value='' />" +
						"</p>");
		}
		
		writer.write(
				"<html>" +
				_head() +
				"	<body>" +
				_menu(model, task.value()) +
				"		<div id='content'>" +
				"			<h1> " + task.value().substring(0, 1).toUpperCase() + task.value().substring(1) +" </h1>" +
				"			<h2> " + task.description() +" </h2>" +
				"			<form action='task-" + task.value() + "' method='POST'>" +
				str.toString() +
				"				<button type='submit'>Run " + task.value() + " task</button>" +
				"			</form>" +
				"		</div>" +
				"	</body>" +
				"</html>"
				);
	}

	@RenderFor(VIEW_PREPARE_CRON)
	public void prepare_cron(PrintWriter writer, Map<String, Object> model) throws IOException {
		@SuppressWarnings("unchecked")
		Set<AaTask> tasks = (Set<AaTask>)model.get("tasks");

		String name = (String)model.get("name");
		
		writer.write(
				"<html>" +
				_head() +
				"	<body>" +
				_menu(model, name) +
				"		<div id='content'>" +
				"			<form id='cronForm' action='cron-" + name + "' method='POST' onsubmit='return onSubmit();'>" +
				"				<input type='hidden' name='run' value='!'>" +
				"				<input type='hidden' name='queue' value='!'>" +
				"				<h1> " + name +" </h1>" +
				"				<table style='text-align: center;'>" +
				"					<tr>" +
				"						<th>Run</th>" +
				"						<th>name</th>" +
				"						<th>Queue</th>" +
				"					</tr>"
				);
		for (AaTask t : tasks) {
			String md5 = AppAdminController.MD5Encode(t);
			writer.write(
				"					<tr>" +
				"						<td><input type='checkbox' name='run' value='" + md5 + "' checked='checked'></td>" +
				"						<td>" + t.clazz.getSimpleName() + "::" + t.method.getName() + "</td>" +
				"						<td><input type='checkbox' name='queue' value='" + md5 + "' " + (t.method.getAnnotation(CronTask.class).queue() ? "checked='checked'" : "") + "'></td>" +
				"					</tr>");
		}
		writer.write(
				"				</table><br /><br />" +
				"				<script type='text/javascript'>" +
				"					function onSubmit() {" +
				"						document.getElementById('cronForm').action += '?' + document.getElementById('querystring').value;" +
				"						return true;" +
				"					}" +
				"				</script>" +
				"				QueryString:<br/><input type='text' id='querystring' /><br /><br />" +
				"				<button type='submit'>Run " + name + " cron NOW</button>" +
				"			</form>" +
				"		</div>" +
				"	</body>" +
				"</html>"
				);
	}

	@RenderFor(VIEW_TASK)
	public void task(PrintWriter writer, Map<String, Object> model) {
		String name = (String)model.get("name");

		writer.write(
				"<html>" +
				_head() +
				"	<body>" +
				"		<script type='text/javascript' src='/_ah/channel/jsapi'></script>" +
				_menu(model, name)
				);
		if (!model.containsKey("messages"))
			writer.write(
					"		<div id='content'>" +
					"			<p>Task registered!</p>" +
					"			<p>Connecting to channel...</p>" +
					"		</div>" +
					"		<script type='text/javascript'>" +
					"			function appendText(text) {" +
					"				p = document.createElement('p');" +
					"				p.innerHTML = text;" +
					"				document.getElementById('content').appendChild(p);" +
					"			}" +
					"			channel = new goog.appengine.Channel('" + model.get("c_token") + "');" +
					"			socket = channel.open();" +
					"			socket.onopen = function() {" +
					"				appendText('...Connected to channel!');" +
					"				var xhr = null;" +
					"				if (window.XMLHttpRequest)" +
					"					xhr = new XMLHttpRequest();" +
					"				else" +
					"					xhr = new ActiveXObject('MSXML2.XMLHTTP.3.0');" +
					"				xhr.onreadystatechange = function() {" +
					"					if (xhr.readyState == 4) {" +
					"						if (xhr.status == 200)" +
					"							appendText('...<b>Task launched!</b>');" +
					"						else" +
					"							appendText('...<b>ERROR: could not launch task<b>: <pre>' + xhr.responseText + '</pre>');" +
					"						appendText('<b>------------------------------------------</b>');" +
					"					}" +
					"				};" +
					"				appendText('Lauching task...');" +
					"				xhr.open('POST', '_startTask', true);" +
					"				xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');" +
					"				xhr.send('token=" + model.get("c_token") + "');" +
					"			};" +
					"			var nextIsStackTrace = false;" +
					"			socket.onmessage = function(message) {" +
					"				var data = message.data.replace(/^\\s+/g,'').replace(/\\s+$/g,'');" +
					"				if (data == '__TASK_END__') {" +
					"					appendText('<b>------------------------------------------</b>');" +
					"					appendText('<b>Task finished!</b>');" +
					"				}" +
					"				else if (data == '__TASK_EXCEPTION__') {" +
					"					nextIsStackTrace = true;" +
					"					appendText('<b>------------------------------------------</b>');" +
					"					appendText('<b>Task threw an exception!</b>');" +
					"				}" +
					"				else if (nextIsStackTrace) {" +
					"					appendText('<pre>' + data + '</pre>');" +
					"					nextIsStackTrace = false;" +
					"				}" +
					"				else" +
					"					appendText(data);" +
					"				document.body.scrollTop = document.body.scrollHeight;" +
					"			};" +
					"			socket.onerror = function(error) { appendText('!!! Channel got error: ' + error.description); };" +
					"			socket.onclose = function() { appendText('!!! Connected closed'); };" +
					"			" +
					"		</script>");
		else {
			@SuppressWarnings("unchecked")
			List<String> messages = (List<String>)model.get("messages");
			writer.write("<div id='content'>");
			for (String msg : messages)
				writer.write("<p>" + msg + "</p>");
			writer.write("</div>");
		}
		writer.write(
				"	</body>" +
				"</html>"
				);
	}
	
	@RenderFor(VIEW_MAINTENANCE)
	public void maintenance(PrintWriter writer, Map<String, Object> model) {
		writer.write(
				"<html>" +
				_head() +
				"	<body>" +
				_menu(model, "maintenance") +
				"		<div id='content'>" +
							(
								((Boolean)model.get("m")).booleanValue()
								?	"<h1>Application is under maintenance</h1>"
								:	"<h1>Set Maintenance</h1>"
							) +
				"			<form action='maintenance' method='post'>" +
				"				<p>" +
				"					<input type='checkbox' name='m' id='m' " + (((Boolean)model.get("m")).booleanValue() ? "checked='checked'" : "") + " value='1' />" +
				"					<label for='m'>Enable maintenance</label>" +
				"				</p>" +
				"				<p><label for='s'>Sub-text:</label> <input type='text' name='s' id='s' value='" + model.get("s") + "' /></p>" +
				"				<p><button type='submit'>submit</button>" +
				"			</form>" +
				"		</div>" +
				"	</body>" +
				"</html>"
				);
	}

}

