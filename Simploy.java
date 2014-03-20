import java.io.IOException;


public class Simploy {

	private static final String USAGE =
		"Usage: " +
		"\n  java Simploy commandToBeRun password" +
		"\n" +
		"\nExample:" +
		"\n  java Simploy \"ant run\" pAsSwOrD" +
		"\n";

	
	public static void main(String[] args) throws Exception {
		initWith(args);
		
		while (true) {
			SimployCore.runCommandIfNewVersionPresent();
			waitAFewMinutes();
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
		if (args.length == 1)
			SimployHttpServer.start(args[0]);
	}


	private static void waitAFewMinutes() {
		try {
			Thread.sleep(1000 * 60 * 5);
		} catch (InterruptedException e) {
			System.out.println("Simploy's main thread sleep was interrupted. No big deal.");
		}
	}
	
}
