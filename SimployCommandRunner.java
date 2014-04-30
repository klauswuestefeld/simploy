import java.io.IOException;
import java.io.InputStream;


public class SimployCommandRunner {

	@SuppressWarnings("deprecation")
	static String exec(String command) throws Exception {
		final Process process = startProcess(command);
		
		final StringBuilder stdout = new StringBuilder();
		Thread capturing = new Thread() { public void run() {
			try {
				capture(process.getInputStream(), stdout);
			} catch (ThreadDeath td) {
				//Reading from the inputStream above will block sometimes when the process ends. :(
			} catch (Exception e) {
				e.printStackTrace();
			}
		}};
		capturing.start();

		waitForExit(process);
		sleepASecond();
		capturing.stop(new ThreadDeath());

		if (process.exitValue() != 0)
			throw new Exception("Command failed: " + command);

		System.out.println("\n");
		return stdout.toString();
	}


	private static void sleepASecond() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	private static Process startProcess(String command) throws IOException {
		System.out.println("Executing: " + command);
		
		ProcessBuilder builder = new ProcessBuilder();
		builder.command(command.split(" "));
		builder.redirectErrorStream(true);
		return builder.start();
	}


	private static void capture(InputStream inputStream, StringBuilder stdout) throws Exception {
		while (true) {
			int read = inputStream.read(); //This will block forever sometimes when the process ends. :(
			if (read == -1)
				return;
			stdout.append((char)read);
			System.out.print((char)read);
		}
	}


	private static void waitForExit(Process process) {
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
