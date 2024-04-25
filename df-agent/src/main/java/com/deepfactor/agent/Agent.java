/*
 * Agent.java
 *
 * (c) 2021 DeepFactor, Inc.
 * All Rights Reserved
 */
package com.deepfactor.agent;

import java.io.*;
import java.lang.instrument.*;
import java.net.*;
import java.util.*;
import java.security.*;
import javassist.*;


public class Agent implements ClassFileTransformer
{
	/* max number of loaded classes to queue */
	public static int CLASSES_LIST_MAX = 10000;

	/* seconds between flush intervals */
	public static int FLUSH_INTERVAL_SECONDS = 15;

	/* Inner class to store list of loaded classes */
	private class ClassData {
		public String className;
		public ClassLoader loader;
		public Vector<String> allMethods;

		public ClassData(String className, ClassLoader loader) {
			this.className = new String(className);
			this.loader=loader;
			allMethods = new Vector<String>();
		}

		public String getJson() {
			StringBuilder json = new StringBuilder();
			boolean first = true;

			json.append("{ ");
			json.append("\"class\": \"").append(className).append( "\"}");

			return json.toString();
		}
	};

	private static Vector<ClassData> allClasses;
	private static Thread flushThread;
	private static boolean flushThreadWaiting = false;

	/*
	 * flushClasses
	 */
	public static synchronized void flushClasses() {
		StringBuilder json = new StringBuilder();
		Vector<ClassData> removeList = new Vector<ClassData>();
		boolean first = true;

		json.append("{ ").append("\"classes_loaded\": [ ");

		try {
			for (ClassData cd : allClasses) {
				if (!first) {
					json.append(", ");
				}

				first = false;

				json.append(cd.getJson());
				/*
				 * Cannot remove from an iterator while
				 * iterating without generating a
				 * ConcurrentModificationException, so queue
				 * the list of things to remove and remove
				 * at the end.
				 *
				 * If we don't get all the elements in this pass
				 * (for example, if another thread adds an
				 * element while we are iterating), we will just
				 * pick it up on the next pass.
				 */
				removeList.add(cd);
			}
		} catch (ConcurrentModificationException e) {
		}
		json.append(" ] }");
		System.out.println(json);

		allClasses.removeAll(removeList);
	}

	/*
	 * transform
	 *
	 * Instrumentation method called on class load.
	 *
	 * Parameters:
	 *  See ClassFileTransformer
	 *
	 * Returns:
	 *  Same bytecode
	 */
	public byte[] transform(ClassLoader loader, String className,
	    Class classBeingRedefined, ProtectionDomain protectionDomain,
	    byte[] classfileBuffer) throws IllegalClassFormatException
	{
		byte[] buf = classfileBuffer;
		if (className == null) {
			return buf;
		}
		String classNameDotted = className.replaceAll("/", ".");

		allClasses.add(new ClassData(classNameDotted, loader));
		return buf;
	}

	/*
	 * premain
	 *
	 * Agent startup code. This initializes the class file transformer
	 * as well as registering a shutdown hook to flush any pending events
	 * on shutdown. This method also starts a thread that periodically
	 * flushes class load events, based on a timer.
	 */
	public static void premain(String args, Instrumentation inst)
	{
		System.out.println("Running Runtime Usage Java Agent");

		allClasses = new Vector<ClassData>();
		inst.addTransformer(new Agent(), true);

		Runtime.getRuntime().addShutdownHook(
			new Thread() {
				public void run() {
					com.deepfactor.agent.Agent.flushClasses();
				}
			});

		flushThread = new Thread() {
			public void run() {
				while (true) {
					try {
						synchronized(this) {
							flushThreadWaiting = true;
							wait(
							    FLUSH_INTERVAL_SECONDS * 1000);
							flushThreadWaiting = false;
						}
						com.deepfactor.agent.Agent.flushClasses();
					/* catch any exception, elastic checks thread exit status */
					}
					catch (InterruptedException e) {}
					catch (Throwable e) {}
				}
			}
		};

		flushThread.setDaemon(true);
		flushThread.start();
	}
}
