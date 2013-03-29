import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;


public class SimployHttpServer {

	private static String _password;
	private static ServerSocket _serverSocket;

	private static final int TCP_PORT = 44321;
	private static final int REQUEST_TIMEOUT = 1000;
	private static final String REPLY_HEADER =
			"HTTP/1.1 200 OK\r\n" +
			"Content-Type: text/plain\r\n" +
			"\r\n";

	private static final PrintStream OUT = System.out;

	static void start(String password) throws IOException {
		_password = password;
		_serverSocket = new ServerSocket(TCP_PORT);
		OUT.println("Listening for requests on port " + TCP_PORT);
		
		new Thread() { @Override public void run() {
			while (true)
				acceptRequest();
		}}.start();
	}

	
	private static void acceptRequest() {
		try {
			tryToAcceptRequest();
		} catch (Exception e) {
			OUT.println(e.getMessage());
		}
	}

	
	private static void tryToAcceptRequest() throws Exception {
		Socket socket = _serverSocket.accept();
		OUT.println("Request Received");
		
		sendReport(socket);
		
		try {
			validateBuildRequest(socket);
		} finally {
			socket.close();
		}
		new Thread() { @Override public void run() {
			SimployCore.buildEvenIfNoChanges();
		}}.start();
	}


	private static void sendReport(Socket socket) throws Exception {
		OutputStream output = socket.getOutputStream();
		String reply = REPLY_HEADER + SimployCore.report(); 
		output.write(reply.getBytes("UTF-8"));
		output.flush();
	}

	
	private static void validateBuildRequest(Socket socket) throws Exception {
		if (_password == null) throwInvalid("Build requests are not being accepted.");
		
		socket.setSoTimeout(REQUEST_TIMEOUT);
		long t0 = System.currentTimeMillis();
		
		InputStream inputStream = socket.getInputStream();
		String request = "";
		while (!request.contains(_password)) {
			int read = inputStream.read();
			if (read == -1) throwInvalid(request);
			request += (char)read;
			if (System.currentTimeMillis() - t0 > REQUEST_TIMEOUT) throwInvalid(request);
			if (request.length() > 2000) throwInvalid("<Request too large>");
		}
	}


	private static void throwInvalid(String request) throws Exception {
		throw new Exception("Invalid Request: " + request);
	}
	
}


