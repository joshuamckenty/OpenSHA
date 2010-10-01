package util;

import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This class contains utilities used by tests. For instance, private no-arg
 * constructors show up in Corbetura as not being covered. However, they can be
 * hit using reflection so utility methods are provided herein to reduce
 * repetition.
 * 
 * @author Peter Powers
 * @version $Id:$
 */
public class TestUtils {

	/* Private no-arg constructor invokation via reflection. */
	public static Object callPrivateNoArgConstructor(final Class<?> cls) throws 
			InstantiationException,
			IllegalAccessException,
			InvocationTargetException {
		final Constructor<?> c = cls.getDeclaredConstructors()[0];
		c.setAccessible(true);
		final Object n = c.newInstance((Object[]) null);
		return n;
	}
	
	private static class TestMethodThread implements Runnable {
		
		private Method testMethod;
		private Object testObj;
		
		private Throwable exception;
		
		public TestMethodThread(Method testMethod, Object testObj) {
			this.testMethod = testMethod;
			this.testObj = testObj;
		}

		@Override
		public void run() {
			try {
				testMethod.invoke(testObj);
			} catch (Throwable t) {
				this.exception = t;
			}
		}
		
		public Throwable getException() {
			return exception;
		}
		
	}
	
	public static void runTestWithTimer(String methodName, Object testObj, int timeoutSeconds) throws Throwable {
		Method testMethod = testObj.getClass().getDeclaredMethod(methodName);
		if (!testMethod.isAccessible())
			testMethod.setAccessible(true);
		TestMethodThread testThread = new TestMethodThread(testMethod, testObj);
		Thread t = new Thread(testThread);
		t.start();
		long start = System.currentTimeMillis();
		
		while (t.isAlive()) {
			double timeSecs = (double)(System.currentTimeMillis() - start) / 1000d;
			if (timeSecs > timeoutSeconds) {
				fail("method '"+methodName+"' exceeded timeout of "+timeoutSeconds+" secs!");
			}
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Throwable exception = testThread.getException();
		if (exception != null) {
			Throwable cause = exception.getCause();
			if (cause != null &&
					(cause instanceof AssertionError || exception instanceof InvocationTargetException))
				throw cause;
			throw exception;
		}
	}

}
