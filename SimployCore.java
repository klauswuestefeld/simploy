import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;


public class SimployCore {

	static String _compileCommand;
	static String _testsFolder;
	static String _libJarsFolder;
	static String _deployCommand;

	static final PrintStream SYSOUT = System.out;
	private static StringBuffer _outputsBeingCaptured;

	private static Date _lastBuildDate;
	private static String _lastBuildStatus;
	private static StringBuffer _lastBuildOutputs;

	private static Date _lastSuccessDate;
	
	

	synchronized
	static void build() {
		build(false);
	}

	
	synchronized
	static void buildEvenIfNoChanges() {
		build(true);
	}

	
	static private void build(boolean buildEvenIfNoChanges) {
		startCapturingOutputs();
		try {
			tryToBuild(buildEvenIfNoChanges);
		} finally {
			stopCapturingOutputs();
		}
	}


	private static void tryToBuild(boolean buildEvenIfNoChanges) {
		boolean hasChanges = pullChanges();
		if (!hasChanges && !buildEvenIfNoChanges)
			return;
		
		_lastBuildDate = new Date();
		_lastBuildStatus = "IN PROGRESS...";
		_lastBuildOutputs = _outputsBeingCaptured;

		try {
			compileTestDeploy();
			_lastSuccessDate = _lastBuildDate;
			_lastBuildStatus = "SUCCESS";
			
		} catch (Exception e) {
			e.printStackTrace();
			_lastBuildStatus = "FAILED";
		}
	}


	private static boolean pullChanges() {
		try {
			String stdOut = exec("git pull");
			return !stdOut.contains("Already up-to-date.");
		} catch (Exception e) {
			e.printStackTrace();
			_lastBuildDate = new Date();
			_lastBuildStatus = "GIT PULL FAILED";
			_lastBuildOutputs = _outputsBeingCaptured;
			return false;
		}
	}


	private static void compileTestDeploy() throws Exception {
		exec(_compileCommand);
		SimployTestsRunner.runAllTestsIn(_testsFolder, _libJarsFolder);
		exec(_deployCommand);
	}
	
	
	private static void startCapturingOutputs() {
		_outputsBeingCaptured = new StringBuffer();
		PrintStream filter = new PrintStream(new FilterOutputStream(SYSOUT) {  @Override public void write(int b) throws IOException {
			super.write(b);
			_outputsBeingCaptured.append((char)b);
		}});
		System.setOut(filter);
		System.setErr(filter);
	}


	private static void stopCapturingOutputs() {
		System.setOut(SYSOUT);
		System.setErr(SYSOUT);
	}

	
	private static String exec(String command) throws Exception {
		return SimployCommandRunner.exec(command);
	}


	static String report() {
		return
			"Last build status: " + _lastBuildStatus +
			"\nLast build date  : " + _lastBuildDate +
			"\nLast success date: " + _lastSuccessDate +
			"\n" +
			"\nLast build outputs (stdOut and stdErr): " +
			"\n" + _lastBuildOutputs +
			"\n\n\n" +
			"\n-----------------------------------------------------" +
			"\nRAM used: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024 + "MB  (Max " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "MB)" +
			"\nReport produced by Simploy.";
	}

	
}
