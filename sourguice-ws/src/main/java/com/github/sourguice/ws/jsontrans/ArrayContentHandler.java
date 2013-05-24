package com.github.sourguice.ws.jsontrans;

import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.ParseException;

public class ArrayContentHandler implements ContentHandler {

	public static interface ArrayItemListener {
		public void onItem(Object item);
	}
	
	private Deque<Object> valueStack = new LinkedList<>();
	private Deque<String> keyStack = new LinkedList<>();
	
	private ArrayItemListener listener;
	
	public ArrayContentHandler(ArrayItemListener listener) {
		this.listener = listener;
	}

	@Override
	public void startJSON() throws ParseException, IOException {
		valueStack.clear();
	}

	@Override
	public void endJSON() throws ParseException, IOException {
	}

	@Override
	public boolean startObject() throws ParseException, IOException {
		if (valueStack.isEmpty())
			throw new IOException("Object cannot be root. Root must be Array.");
		
		valueStack.addFirst(new JSONObject());
		return true;
	}

	private void unStack() throws ParseException, IOException {
		Object o = valueStack.removeFirst();
		push(o);
		if (valueStack.size() == 1)
			listener.onItem(o);
	}
	
	@Override
	public boolean endObject() throws ParseException, IOException {
		unStack();
		return true;
	}

	@Override
	public boolean startObjectEntry(String key) throws ParseException, IOException {
		keyStack.addFirst(key);
		return true;
	}

	@Override
	public boolean endObjectEntry() throws ParseException, IOException {
		keyStack.removeFirst();
		return true;
	}

	@Override
	public boolean startArray() throws ParseException, IOException {
		valueStack.addFirst(new JSONArray());
		return true;
	}

	@Override
	public boolean endArray() throws ParseException, IOException {
		unStack();
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public void push(Object value) {
		if (valueStack.isEmpty()) {
			return ;
		}

		Object object = valueStack.getFirst();
		if (object instanceof JSONArray)
			((JSONArray)object).add(value);
		else if (object instanceof JSONObject)
			((JSONObject)object).put(keyStack.getFirst(), value);
	}

	@Override
	public boolean primitive(Object value) throws ParseException, IOException {
		if (valueStack.isEmpty())
			throw new IOException("Primitive cannot be root. Root must be Array.");

		push(value);
		return true;
	}
}
