/**
 * App Admin is a very simple console for administrative task specific to your application
 */
package com.github.sourguice.appengine.appadmin;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.github.sourguice.annotation.controller.RenderWith;
import com.github.sourguice.annotation.request.PathVariable;
import com.github.sourguice.annotation.request.RequestHeader;
import com.github.sourguice.annotation.request.RequestMapping;
import com.github.sourguice.annotation.request.RequestParam;
import com.github.sourguice.annotation.request.View;
import com.github.sourguice.appengine.appadmin.annotation.AdminTask;
import com.github.sourguice.appengine.appadmin.annotation.AdminTaskParam;
import com.github.sourguice.appengine.appadmin.annotation.CronTask;
import com.github.sourguice.appengine.appadmin.internal.AppAdminViewRenderer;
import com.github.sourguice.appengine.appadmin.internal.TaskParamArgumentFetcher;
import com.github.sourguice.appengine.upload.annotation.UploadMapping;
import com.github.sourguice.call.CalltimeArgumentFetcher;
import com.github.sourguice.call.MvcCaller;
import com.github.sourguice.throwable.controller.MVCHttpServletResponseException;
import com.github.sourguice.throwable.controller.MVCHttpServletResponseSendErrorException;
import com.github.sourguice.utils.Annotations;
import com.github.sourguice.value.RequestMethod;
import com.github.sourguice.view.Model;
import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.googlecode.gentyref.GenericTypeReflector;

@Singleton
@RenderWith(AppAdminViewRenderer.class)
public class AppAdminController {
	
	private static final String HEADER_CHANNEL_KEY = "X-SourGuice-Channel-Key";
	private static final String HEADER_CRON_RUN = "X-SourGuice-Cron-Run";
	private static final String HEADER_CRON_QUEUE = "X-SourGuice-Cron-Queue";
	private static final String HEADER_CRONTASK_CLASS = "X-SourGuice-CronTask-Class";
	private static final String HEADER_CRONTASK_METHOD = "X-SourGuice-CronTask-Method";
	
	public static boolean AppstatsEnabled = false;
	public static String Path = null;
	
	public static @CheckForNull List<Class<?>> toScan = new LinkedList<Class<?>>();
	
	private static String SK_TASK = "com.github.sourguice.appengine.appadmin.AppAdminController.TASK-";
	
	public static final class AaTask implements Comparable<AaTask> {
		public Method method;
		public Class<?> clazz;
		public AaTask(Method method, Class<?> clazz) {
			super();
			this.method = method;
			this.clazz = clazz;
		}
		@Override
		public int compareTo(AaTask o) {
			int ret = this.clazz.getCanonicalName().compareTo(o.clazz.getCanonicalName());
			if (ret == 0)
				ret = this.method.getName().compareTo(o.method.getName());
			return ret;
		}
	}
	
	private Map<String, AaTask> adminTasks = new TreeMap<String, AaTask>();
	private Map<String, Set<AaTask>> cronTasks = new TreeMap<String, Set<AaTask>>();
	
	protected Injector injector;
	
	public static final String MD5Encode(AaTask task) {
		byte[] hash = null;
		try {
			hash = MessageDigest.getInstance("MD5").digest((task.clazz.getName() + "::" + task.method.toGenericString()).getBytes());
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		
		StringBuilder hashString = new StringBuilder();
		for (int i = 0; i < hash.length; ++i)
		{
			String hex = Integer.toHexString(hash[i]);
			if (hex.length() == 1)
			{
				hashString.append('0');
				hashString.append(hex.charAt(hex.length() - 1));
			}
			else
				hashString.append(hex.substring(hex.length() - 2));
		}
		return hashString.toString();
    }

	@Inject
	public AppAdminController(Injector injector) {
		this.injector = injector;
	}
	
	public static final class TaskParamInfos {
		public int pos;
		public Class<?> clazz;
		public AdminTaskParam param;
		public TaskParamInfos(int pos, Class<?> clazz, AdminTaskParam param) {
			this.pos = pos;
			this.clazz = clazz;
			this.param = param;
		}
	}
	
	private Class<?> getUnmodifiedClass(Class<?> cls) {
		if (cls.getSimpleName().contains("l$$EnhancerByGuice$$"))
			return cls.getSuperclass();
		return cls;
	}
	
	public void scanIfNecessary() {
		if (toScan == null)
			return ;

		Pattern pattern = Pattern.compile("[a-zA-Z0-9_]+");
		
		for(Method method : this.getClass().getMethods()) {
			AdminTask adminTask = method.getAnnotation(AdminTask.class);
			if (adminTask != null) {
				if (!pattern.matcher(adminTask.value()).matches())
					throw new IllegalArgumentException("AdminTask name " + adminTask.value() + " must contain only alphanumeric or underscore characters.");
				adminTasks.put(adminTask.value(), new AaTask(method, getUnmodifiedClass(this.getClass())));
			}
			CronTask cronTask = method.getAnnotation(CronTask.class);
			if (cronTask != null) {
				if (!pattern.matcher(cronTask.value()).matches())
					throw new IllegalArgumentException("CronTask cron name " + cronTask.value() + " must contain only alphanumeric or underscore characters.");
				Set<AaTask> set = cronTasks.get(cronTask.value());
				if (set == null)
					set = new TreeSet<AaTask>();
				set.add(new AaTask(method, getUnmodifiedClass(this.getClass())));
				cronTasks.put(cronTask.value(), set);
			}
		}

		assert toScan != null;
		for (Class<?> scan : toScan) {
			Class<?> cls = injector.getInstance(scan).getClass();
			for (Method m : cls.getMethods()) {
				AdminTask adminTask = m.getAnnotation(AdminTask.class);
				if (adminTask != null) {
					if (!pattern.matcher(adminTask.value()).matches())
						throw new IllegalArgumentException("AdminTask name " + adminTask.value() + " must contain only alphanumeric or underscore characters.");
					adminTasks.put(adminTask.value(), new AaTask(m, getUnmodifiedClass(cls)));
				}
				CronTask cronTask = m.getAnnotation(CronTask.class);
				if (cronTask != null) {
					if (!pattern.matcher(cronTask.value()).matches())
						throw new IllegalArgumentException("CronTask cron name " + cronTask.value() + " must contain only alphanumeric or underscore characters.");
					Set<AaTask> set = cronTasks.get(cronTask.value());
					if (set == null)
						set = new TreeSet<AaTask>();
					set.add(new AaTask(m, getUnmodifiedClass(cls)));
					cronTasks.put(cronTask.value(), set);
				}
			}
		}
		toScan = null;
	}

	@RequestMapping("/")
	@View(AppAdminViewRenderer.VIEW_INDEX)
	public void index(Model model) {
		scanIfNecessary();

		model.addAttribute("taskList", adminTasks.keySet());
		model.addAttribute("cronList", cronTasks.keySet());
	}

	@RequestMapping(value = "/task-{name}", method = RequestMethod.GET)
	@View(AppAdminViewRenderer.VIEW_PREPARE_ADMIN)
	public void task(Model model,
			@PathVariable(value = "name") String name
			) throws MVCHttpServletResponseException {
		scanIfNecessary();

		AaTask task = adminTasks.get(name);
		if (task == null)
			throw new MVCHttpServletResponseSendErrorException(404);

		model.addAttribute("taskList", adminTasks.keySet());
		model.addAttribute("cronList", cronTasks.keySet());
		model.addAttribute("task", task.method.getAnnotation(AdminTask.class));

		List<TaskParamInfos> params = new ArrayList<TaskParamInfos>();
		for (int i = 0; i < task.method.getParameterTypes().length; ++i) {
			AdminTaskParam param = Annotations.fromArray(task.method.getParameterAnnotations()[i]).getAnnotation(AdminTaskParam.class);
			if (param != null) {
				params.add(new TaskParamInfos(i, task.method.getParameterTypes()[i], param));
			}
		}
		model.addAttribute("params", params);
	}

	@RequestMapping(value = "/task-{name}", method = RequestMethod.POST)
	@View(AppAdminViewRenderer.VIEW_TASK)
	public void task_POST(Model model, final HttpServletRequest req, HttpSession session,
			ChannelService channels, MvcCaller caller, TaskParamArgumentFetcher tpFetcher,
			@PathVariable(value = "name", defaultValue = "") String name
			) throws Throwable {
		scanIfNecessary();

		AaTask task = adminTasks.get(name);
		if (task == null)
			throw new MVCHttpServletResponseSendErrorException(404);

		model.addAttribute("taskList", adminTasks.keySet());
		model.addAttribute("cronList", cronTasks.keySet());
		model.addAttribute("name", task.method.getAnnotation(AdminTask.class).value());

		if (task.method.getAnnotation(AdminTask.class).queue()) {
			String key = new BigInteger(64, new Random()).toString(32);
			String token = channels.createChannel(key);
			model.addAttribute("c_token", token);
			
			TaskOptions options = TaskOptions.Builder.withUrl(req.getRequestURI() + "/EXEC");
			@SuppressWarnings("unchecked")
			Enumeration<String> params = req.getParameterNames();
			while (params.hasMoreElements()) {
				String paramName = params.nextElement();
				if (paramName.startsWith("param:"))
					options.param(paramName, req.getParameter(paramName));
			}
			options.header(HEADER_CHANNEL_KEY, key);
			
			session.setAttribute(SK_TASK + token, options);
		}
		else {
			final List<String> messages = new ArrayList<String>();
			Object ret = caller.call(task.clazz, task.method, null, false, tpFetcher,
				new CalltimeArgumentFetcher<TaskMessageSender>() {
					@Override public boolean canGet(Type type, int pos, Annotation[] annos) {
						return GenericTypeReflector.erase(type).equals(TaskMessageSender.class);
					}
					@Override public TaskMessageSender get(Type type, int pos, Annotation[] annos) {
						return new TaskMessageSender() {
							@Override public synchronized void sendMessage(String message) {
								messages.add(message);
							}
						};
					}
				}
			);
			if (ret != null && !(ret instanceof Void))
				messages.add(ret.toString());
			if (messages.isEmpty())
				messages.add("Task executed!");
			model.addAttribute("messages", messages);
		}
	}

	@RequestMapping(value = "/_startTask", method = RequestMethod.POST)
	public void _startTask(HttpSession session,
			@RequestParam("token") String token
			) throws MVCHttpServletResponseSendErrorException {
		scanIfNecessary();

		TaskOptions task = (TaskOptions)session.getAttribute(SK_TASK + token);
		if (task == null)
			throw new MVCHttpServletResponseSendErrorException(411);
		QueueFactory.getQueue("AdminTasks").add(task);
		session.removeAttribute(SK_TASK + token);
	}
	
	@RequestMapping(value = "/task-{name}/EXEC", method = RequestMethod.POST, headers = {"X-AppEngine-QueueName", "X-AppEngine-TaskName", HEADER_CHANNEL_KEY})
	public void _task_exec(final HttpServletRequest req, MvcCaller caller, final ChannelService channels, TaskParamArgumentFetcher tpFetcher,
			@PathVariable(value = "name", defaultValue = "") String name
			) throws Throwable {
		scanIfNecessary();

		AaTask task = adminTasks.get(name);
		if (task == null)
			throw new MVCHttpServletResponseSendErrorException(404);

		final String channelKey = req.getHeader(HEADER_CHANNEL_KEY);

		try {
			Object ret = caller.call(task.clazz, task.method, null, false, tpFetcher,
				new CalltimeArgumentFetcher<TaskMessageSender>() {
					@Override public boolean canGet(Type type, int pos, Annotation[] annos) {
						return GenericTypeReflector.erase(type).equals(TaskMessageSender.class);
					}
					@Override public TaskMessageSender get(Type type, int pos, Annotation[] annos) {
						return new TaskMessageSender() {
							@Override public synchronized void sendMessage(String message) {
								channels.sendMessage(new ChannelMessage(channelKey, message));
							}
						};
					}
				}
			);
			if (ret != null && !(ret instanceof Void))
				channels.sendMessage(new ChannelMessage(channelKey, ret.toString()));
			channels.sendMessage(new ChannelMessage(channelKey, "__TASK_END__"));
		}
		catch (Throwable e) {
			channels.sendMessage(new ChannelMessage(channelKey, "__TASK_EXCEPTION__"));
			CharArrayWriter out = new CharArrayWriter();
			PrintWriter pw = new PrintWriter(out);
			e.printStackTrace(pw);
			pw.flush();
			channels.sendMessage(new ChannelMessage(channelKey, out.toString()));
			e.printStackTrace();
		}
	}
	
	@RequestMapping(value = "/cron-{name}", method = RequestMethod.GET)
	@View(AppAdminViewRenderer.VIEW_PREPARE_CRON)
	public void cron(Model model,
			@PathVariable(value = "name") String name
			) throws MVCHttpServletResponseException {
		scanIfNecessary();

		Set<AaTask> tasks = cronTasks.get(name);
		if (tasks == null)
			throw new MVCHttpServletResponseSendErrorException(404);
		
		model.addAttribute("taskList", adminTasks.keySet());
		model.addAttribute("cronList", cronTasks.keySet());
		model.addAttribute("name", name);
		model.addAttribute("tasks", tasks);
	}

	private static String Join(Iterable<String> col, String glue) {
		StringBuilder builder = new StringBuilder();
		boolean delim = false;
		for (String str : col) {
			if (delim)
				builder.append(glue);
			else
				delim = true;
			builder.append(str);
		}
		return builder.toString();
	}
	
	@RequestMapping(value = "/cron-{name}", method = RequestMethod.POST)
	@View(AppAdminViewRenderer.VIEW_TASK)
	public void cron_POST(Model model, ChannelService channels, HttpServletRequest req, HttpSession session,
			@PathVariable("name") String name,
			@RequestParam("run") List<String> run,
			@RequestParam("queue") List<String> queue
			) throws MVCHttpServletResponseException {
		scanIfNecessary();

		Set<AaTask> tasks = cronTasks.get(name);
		if (tasks == null)
			throw new MVCHttpServletResponseSendErrorException(404);

		model.addAttribute("taskList", adminTasks.keySet());
		model.addAttribute("cronList", cronTasks.keySet());
		model.addAttribute("name", name);

		String key = new BigInteger(64, new Random()).toString(32);
		String token = channels.createChannel(key);
		model.addAttribute("c_token", token);
		
		TaskOptions options = TaskOptions.Builder.withUrl(req.getRequestURI() + "/EXEC");
		options.header(HEADER_CHANNEL_KEY, key);
		options.header(HEADER_CRON_RUN, Join(run, ":"));
		options.header(HEADER_CRON_QUEUE, Join(queue, ":"));
		
		session.setAttribute(SK_TASK + token, options);
	}

	@RequestMapping(value = "/cron-{name}/EXEC")
	public void cron_EXEC(MvcCaller caller, HttpServletRequest req, final ChannelService channels,
			final @PathVariable("name") String name,
			@RequestHeader(value = HEADER_CHANNEL_KEY, defaultValue = "") final String channelKey,
			@RequestHeader(value = HEADER_CRON_RUN, defaultValue = "") final String runStr,
			@RequestHeader(value = HEADER_CRON_QUEUE, defaultValue = "") final String queueStr
			) throws Throwable {
		scanIfNecessary();

		Set<AaTask> tasks = cronTasks.get(name);
		if (tasks == null)
			return ;

		Set<String> run = null;
		Set<String> queue = null;
		if (!runStr.isEmpty())
			run = new HashSet<String>(Arrays.asList(runStr.split(":")));
		if (!queueStr.isEmpty())
			queue = new HashSet<String>(Arrays.asList(queueStr.split(":")));

		try {
			TaskMessageSender sender = null;
			if (!channelKey.isEmpty())
				sender = new TaskMessageSender() {
					@Override public synchronized void sendMessage(String message) {
						channels.sendMessage(new ChannelMessage(channelKey, message));
					}
				};
			else
				sender = new TaskMessageSender() {
					Logger logger = Logger.getLogger("CRON-" + name);
					@Override public void sendMessage(String message) {
						logger.log(Level.INFO, message);
					}
				};
	
			for(AaTask task : tasks) {
				if (run != null && !run.contains(MD5Encode(task)))
					continue ;
				
				CronTask info = task.method.getAnnotation(CronTask.class);
	
				boolean inQueue = info.queue();
				if (queue != null)
					inQueue = queue.contains(MD5Encode(task));
				
				if (inQueue) {
					sender.sendMessage("<b>Running " + task.clazz.getName() + "." + task.method.getName() + " IN A TASK QUEUE</b>");
					QueueFactory.getQueue("CronTasks").add(
							TaskOptions.Builder
								.withUrl(req.getRequestURI() + "-Queue?" + req.getQueryString())
								.method(TaskOptions.Method.GET)
								.header(HEADER_CRONTASK_CLASS, task.clazz.getName())
								.header(HEADER_CRONTASK_METHOD, task.method.toGenericString())
					);
				}
				else {
					final TaskMessageSender finalSender = sender;
					sender.sendMessage("<b>Running " + task.clazz.getName() + "." + task.method.getName() + "</b>");
					Object ret = caller.call(task.clazz, task.method, null, false, new CalltimeArgumentFetcher<TaskMessageSender>() {
						@Override public boolean canGet(Type type, int pos, Annotation[] annos) {
							return GenericTypeReflector.erase(type).equals(TaskMessageSender.class);
						}
						@Override
						public TaskMessageSender get(Type type, int pos, Annotation[] annos) throws Throwable {
							return finalSender;
						}
					});
					if (ret != null && !(ret instanceof Void))
						sender.sendMessage(ret.toString());
					sender.sendMessage("<br />");
				}
			}
			sender.sendMessage("__TASK_END__");
		}
		catch (Throwable e) {
			if (!channelKey.isEmpty()) {
				channels.sendMessage(new ChannelMessage(channelKey, "__TASK_EXCEPTION__"));
				CharArrayWriter out = new CharArrayWriter();
				PrintWriter pw = new PrintWriter(out);
				e.printStackTrace(pw);
				pw.flush();
				channels.sendMessage(new ChannelMessage(channelKey, out.toString()));
				e.printStackTrace();
			}
			else
				throw e;
		}
	}
	
	@RequestMapping(value = "/cron-{name}/EXEC-Queue")
	public void cron_EXECQueue(MvcCaller caller,
			final @PathVariable("name") String name,
			@RequestHeader(value = HEADER_CRONTASK_CLASS) String className,
			final @RequestHeader(value = HEADER_CRONTASK_METHOD) String methodName
			) throws Throwable {
		scanIfNecessary();

		Set<AaTask> tasks = cronTasks.get(name);
		if (tasks == null)
			return ;
		for (AaTask task : tasks)
			if (task.clazz.getName().equals(className) && task.method.toGenericString().equals(methodName)) {
				caller.call(task.clazz, task.method, null, false, new CalltimeArgumentFetcher<TaskMessageSender>() {
					@Override public boolean canGet(Type type, int pos, Annotation[] annos) {
						return GenericTypeReflector.erase(type).equals(TaskMessageSender.class);
					}
					@Override
					public TaskMessageSender get(Type type, int pos, Annotation[] annos) throws Throwable {
						return new TaskMessageSender() {
							Logger logger = Logger.getLogger("CRON-" + name + "-QUEUE-" + methodName);
							@Override public void sendMessage(String message) {
								logger.log(Level.INFO, message);
							}
						};
					}
				});
				return ;
			}
	}

	@RequestMapping("/maintenance")
	@View(AppAdminViewRenderer.VIEW_MAINTENANCE)
	public void maintenance(Model model) {
		scanIfNecessary();

		model.addAttribute("taskList", adminTasks.keySet());
		model.addAttribute("cronList", cronTasks.keySet());

		Entity maintenance = MaintenanceFilter.getMaintenanceEntity();
		if (maintenance != null) {
			model.addAttribute("m", maintenance.getProperty("m"));
			model.addAttribute("s", maintenance.getProperty("s"));
		}
		else {
			model.addAttribute("m", new Boolean(false));
			model.addAttribute("s", "");
		}
	}

	@RequestMapping(value = "/maintenance", method = RequestMethod.POST)
	@View(AppAdminViewRenderer.VIEW_MAINTENANCE)
	public void maintenancePost(Model model,
			@RequestParam(value = "m", defaultValue = "0") boolean m,
			@RequestParam(value = "s") String s
			) {
		scanIfNecessary();

		String ns = NamespaceManager.get();
		NamespaceManager.set(MaintenanceFilter.GAE_NAMESPACE);
		try {
			Entity maintenance = new Entity(KeyFactory.createKey(MaintenanceFilter.GAE_KEY, MaintenanceFilter.GAE_KEY));
			maintenance.setProperty("m", new Boolean(m));
			maintenance.setProperty("s", s);
			DatastoreServiceFactory.getAsyncDatastoreService().put(maintenance);
			MemcacheServiceFactory.getMemcacheService().put(MaintenanceFilter.GAE_KEY, maintenance);
		}
		finally {
			NamespaceManager.set(ns);
		}
		
		maintenance(model);
	}
	
	@UploadMapping(onlyParamUploads = false)
	public void remoteUpload(Writer out, Map<String, List<BlobKey>> uploads) throws IOException {
		out.write("{");
		boolean oComa = false;
		for (String name : uploads.keySet()) {
			if (oComa)
				out.write(",");
			oComa = true;

			out.write("\"" + name + "\":[");
			
			boolean aComa = false;
			for (BlobKey blobkey : uploads.get(name)) {
				if (aComa)
					out.write(",");
				aComa = true;
				out.write("\"" + blobkey.getKeyString() + "\"");
			}
			
			out.write("]");
		}
		out.write("}");
		
		out.flush();
	}
	
}
