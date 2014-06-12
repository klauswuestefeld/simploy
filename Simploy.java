import java.io.IOException;


public class Simploy {

	private static final String USAGE =
		"Usage: " +
		"\n  java Simploy port password" +
		"\n" +
		"\nExample:" +
		"\n  java Simploy 44321 pAsSwOrD" +
		"\n";

	
	public static void main(String[] args) throws Exception {
		initWith(args);

		SimployCore.runCommand();
		while (true) {
			waitAFewMinutes();
			SimployCore.runCommandIfNewVersionPresent();
		}
	}
	

	private static void initWith(String[] args) throws IOException {
		try {
			tryToInitWith(args);
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println(USAGE);
			System.exit(-1);
		}
	}


	private static void tryToInitWith(String[] args) throws ArrayIndexOutOfBoundsException, IOException {
		int port = Integer.parseInt(args[0]);
		String password = args[1];
		SimployHttpServer.start(port, password);
	}


	private static void waitAFewMinutes() {
		try {
			Thread.sleep(1000 * 60 * 5);
		} catch (InterruptedException e) {
			System.out.println("Simploy's main thread sleep was interrupted. No big deal.");
		}
	}
	
}
