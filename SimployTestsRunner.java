import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;


class SimployTestsRunner extends RunListener {

	private static final int DOT_CLASS = ".class".length();

	private final String _testsFolderPath;
	private boolean _someTestFailed = false;

	
	public static void runAllTestsIn(String testsPath, String libJarsFolder) throws Exception {
		runInSeparateClassLoader(testsPath, libJarsFolder);
	}


	private static void runInSeparateClassLoader(String testsPath, String libJarsFolder) throws Exception {
		Class<?> inSeparateClassloader = separateClassLoader(testsPath, libJarsFolder).loadClass(SimployTestsRunner.class.getName());
		instantiate(inSeparateClassloader, testsPath);
	}


	public SimployTestsRunner(String testsFolder) {
		_testsFolderPath = testsFolder;
		
		JUnitCore junit = new JUnitCore();
		junit.addListener(this);
		
		junit.run(findTestClasses());
		
		if (_someTestFailed)
			throw new RuntimeException("Some test failed.");
	}

	
	private static void instantiate(Class<?> separateClass, String testsPath) throws Exception {
		Constructor<?> ctor = separateClass.getConstructor(String.class);
		ctor.setAccessible(true);
		try {
			ctor.newInstance(testsPath);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof Exception) throw (Exception) e.getCause();
			throw e;
		}
	}

	
	static private URLClassLoader separateClassLoader(String testsPath, String libJarsFolder) throws Exception {
		ClassLoader noParent = null;
		return new URLClassLoader(classpath(testsPath, libJarsFolder), noParent);
	}
	
	
	private static URL[] classpath(String testsPath, String libJarsFolder) throws Exception {
		List<String> jarPaths = fileNamesEndingWith(new File(libJarsFolder), ".jar");
		printClasspath(jarPaths);
		List<URL> result = convertToURLs(jarPaths);
		result.add(0, toURL(testsPath));
		return result.toArray(new URL[result.size()]);
	}


	private static void printClasspath(List<String> jarPaths) {
		System.out.println("Running tests using jars:");
		for (String path : jarPaths)
			System.out.println("   " + path);
		System.out.println();
	}


	private static List<URL> convertToURLs(List<String> paths) throws Exception {
		List<URL> result = new ArrayList<URL>();
		for (String path : paths)
			result.add(toURL(path));
		return result;
	}


	@Override
	public void testStarted(Description description) throws Exception {
		System.out.println(description);
	}

	@Override
	public void testFailure(Failure failure) {
		_someTestFailed = true;
		System.out.println("FAILED\n");
		System.out.println(failure.getTrace());
	}


	private Class<?>[] findTestClasses() {
		try {
			return tryToFindTestClasses();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	private Class<?>[] tryToFindTestClasses() throws Exception {
		List<String> testFiles = fileNamesEndingWith(testsFolder(), "Test.class");
		List<Class<?>> result = convertToClasses(testFiles);
		if (result.size() == 0) throw new Exception("No test classes found in: " + testsFolder());
		return result.toArray(new Class[0]);
	}

	
	private File testsFolder() throws Exception {
		File result = new File(_testsFolderPath);
		if (!result.exists() || !result.isDirectory()) throw new Exception("Tests root folder not found or not a folder: " + result);
		return result;
	}


	static private List<String> fileNamesEndingWith(File rootFolder, String ending) throws Exception {
		List<String> result = new ArrayList<String>();
		accumulateFileNamesEndingWith(result, rootFolder, ending);
		return result;
	}

	
	private List<Class<?>> convertToClasses(final List<String> classFilePaths) throws Exception {
		List<Class<?>> result = new ArrayList<Class<?>>();

		String rootPath = testsFolder().getAbsolutePath();

		for (String filePath : classFilePaths) {
			String className = className(rootPath, filePath);
			Class<?> c = Class.forName(className);
			if (!Modifier.isAbstract(c.getModifiers()))
				result.add(c);
		}
		return result;
	}

	
	static private String className(String classpathRoot, String classFilePath) {
		if (!classFilePath.startsWith(classpathRoot)) throw new IllegalStateException("Class file: " + classFilePath + " should be inside subfolder of: " + classpathRoot);
		int afterRoot = classpathRoot.length() + 1;
		int beforeDotClass = classFilePath.length() - DOT_CLASS;
		return classFilePath.substring(afterRoot, beforeDotClass).replace('/', '.').replace('\\', '.');
	}

	static private void accumulateFileNamesEndingWith(List<String> classFiles, File folder, String ending) {
		for (File candidate : folder.listFiles())
			if (candidate.isDirectory())
				accumulateFileNamesEndingWith(classFiles, candidate, ending);
			else
				accumulateFileNameEndingWith(classFiles, candidate, ending);
	}

	static private void accumulateFileNameEndingWith(List<String> fileNames, File file, String ending) {
		String name = file.getName();
		if (name.endsWith(ending))
			fileNames.add(file.getAbsolutePath());
	}
	
	
	private static URL toURL(String path) throws IOException {
		return new File(path).getCanonicalFile().toURI().toURL();
	}

}