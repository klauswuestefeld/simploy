import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;


public class SimployCore {

	static String command;

	static final PrintStream SYSOUT = System.out;
	private static StringBuffer _outputsBeingCaptured;

	private static Date _lastBuildDate;
	private static String _lastBuildStatus;
	private static StringBuffer _lastBuildOutputs;

	private static Date _lastSuccessDate;
	
	

	synchronized
	static void runCommandIfNewVersionPresent() {
		runCommand(false);
	}

	
	synchronized
	static void runCommand() {
		runCommand(true);
	}

	
	static private void runCommand(boolean runEvenIfNoChanges) {
		startCapturingOutputs();
		try {
			tryToRunCommand(runEvenIfNoChanges);
		} finally {
			stopCapturingOutputs();
		}
	}


	private static void tryToRunCommand(boolean runEvenIfNoChanges) {
		boolean hasChanges = pullChanges();
		if (!hasChanges && !runEvenIfNoChanges)
			return;
		
		_lastBuildDate = new Date();
		_lastBuildStatus = "IN PROGRESS...";
		_lastBuildOutputs = _outputsBeingCaptured;

		try {
			execCommand();
			_lastSuccessDate = _lastBuildDate;
			_lastBuildStatus = "SUCCESS";
			
		} catch (Exception e) {
			e.printStackTrace();
			_lastBuildStatus = "FAILED";
		}
	}


	private static boolean pullChanges() {
		try {
			String stdOut = SimployCommandRunner.exec("git pull");
			return !stdOut.contains("Already up-to-date.");
		} catch (Exception e) {
			e.printStackTrace();
			_lastBuildDate = new Date();
			_lastBuildStatus = "GIT PULL FAILED";
			_lastBuildOutputs = _outputsBeingCaptured;
			return false;
		}
	}

	
	private static void execCommand() throws Exception {
		SimployCommandRunner.exec(command);
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
