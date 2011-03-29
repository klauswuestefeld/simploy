import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class Simploy implements Runnable {

	private static final String USAGE =
		"Usage: " +
		"\n  java Simploy compileCommand compiledTestsRootFolder jarsRootFolder deployCommand [password]" +
		"\n" +
		"\nExample:" +
		"\n  java Simploy \"ant build\" ./bin/tests ./lib \"ant deploy\" password123" +
		"\n";

	private String _compileCommand;
	private String _testsFolder;
	private String _libJarsFolder;
	private String _deployCommand;

	private String _secret;
	private ServerSocket _serverSocket;


	private static final int TCP_PORT = 44321;
	private static final int DEPLOY_REQUEST_TIMEOUT = 1000 * 7;

	
	public static void main(String[] args) throws Exception {
		new Simploy(args);
	}


	private Simploy(String[] args) throws Exception {
		parseArgs(args);
		
		if (_secret != null)
			startListeningForBuildRequests();
		
		while (true) {
			deployNewVersionIfAvailable();
			waitAFewMinutes();
		}
	}
	
	
	private void startListeningForBuildRequests() throws IOException {
		_serverSocket = new ServerSocket(TCP_PORT);
		System.out.println("Listening for requests on port " + TCP_PORT);
		
		Thread thread = new Thread(this);
		thread.setDaemon(true);
		thread.start();
	}


	@Override
	public void run() {
		while (true)
			try {
				acceptRequest();
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
	}


	private void acceptRequest() throws Exception {
		Socket socket = _serverSocket.accept();
		System.out.println("Request Received");
		
		try {
			validateRequest(socket);
		} finally {
			socket.close();
		}
		deployNewVersionIfAvailable();
	}

	
	private void validateRequest(Socket socket) throws Exception {
		socket.setSoTimeout(DEPLOY_REQUEST_TIMEOUT);
		long t0 = System.currentTimeMillis();
		
		InputStream inputStream = socket.getInputStream();
		String request = "";
		while (!request.contains(_secret)) {
			int read = inputStream.read();
			if (read == -1) throwInvalid(request);
			request += (char)read;
			if (System.currentTimeMillis() - t0 > DEPLOY_REQUEST_TIMEOUT) throwInvalid(request);
			if (request.length() > 4000) throwInvalid(request);
		}
	}


	private void throwInvalid(String request) throws Exception {
		throw new Exception("Invalid Request: " + request);
	}


	synchronized
	private void deployNewVersionIfAvailable() {
		try {
			tryToDeployNewVersionIfAvailable();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}


	private void tryToDeployNewVersionIfAvailable() throws Exception {
		boolean isUpToDate = gitPull();
		if (isUpToDate)
			return;

		exec(_compileCommand);
		SimployTestsRunner.runAllTestsIn(_testsFolder, _libJarsFolder);
		exec(_deployCommand);
	}


	private boolean gitPull() throws Exception {
		String stdOut = exec("git pull");
		return stdOut.contains("Already up-to-date.");
	}


	private String exec(String command) throws Exception {
		Process process = startProcess(command);
		String stdOut = printOut(process.getInputStream());
		
		if (process.waitFor() != 0)
			throw new Exception("Command failed: " + command);

		System.out.println("\n");
		return stdOut;
	}


	private Process startProcess(String command) throws IOException {
		System.out.println("Executing: " + command);
		
		ProcessBuilder builder = new ProcessBuilder();
		builder.command(command.split(" "));
		Process process = builder.start();
		builder.redirectErrorStream(true);
		return process;
	}


	private String printOut(InputStream inputStream) throws Exception {
		String result = "";
		while (true) {
			int read = inputStream.read();
			if (read == -1) return result;
			result += (char)read;
			System.out.print((char)read);
		}
	}


	private void parseArgs(String[] args) {
		try {
			tryToParseArgs(args);
		} catch (RuntimeException r) {
			exitWith(USAGE);
		}
	}


	private void exitWith(String message) {
		System.out.println(message);
		System.exit(-1);
	}


	private void tryToParseArgs(String[] args) {
		_compileCommand = args[0];
		_testsFolder = args[1];
		_libJarsFolder = args[2];
		_deployCommand = args[3];
		if (args.length == 5) _secret = args[4];
	}


	private void waitAFewMinutes() {
		try {
			Thread.sleep(1000 * 60 * 5);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}
	
	
}


