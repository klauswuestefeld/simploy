import java.io.IOException;


public class Simploy {

	private static final String USAGE =
		"Usage: " +
		"\n  java Simploy compileCommand compiledTestsRootFolder jarsRootFolder deployCommand [password]" +
		"\n" +
		"\nExample:" +
		"\n  java Simploy \"ant build\" ./bin/tests ./lib \"ant deploy\" password123" +
		"\n";

	
	public static void main(String[] args) throws Exception {
		initWith(args);
		
		while (true) {
			SimployCore.build();
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
		SimployCore._compileCommand = args[0];
		SimployCore._testsFolder = args[1];
		SimployCore._libJarsFolder = args[2];
		SimployCore._deployCommand = args[3];
		if (args.length == 5)
			SimployHttpServer.start(args[4]);
	}


	private static void waitAFewMinutes() {
		try {
			Thread.sleep(1000 * 60 * 5);
		} catch (InterruptedException e) {
			System.out.println("Simploy's main thread sleep was interrupted. No big deal.");
		}
	}
	
}
