import java.io.File;
import java.io.IOException;

public class POC {

	public static void main(String[] args) throws IOException {
		File testDirectory = new File("secretDirectory");
		testDirectory.mkdirs();
		System.out.println("Test Directory Exists: " + testDirectory.exists());
		testDirectory.delete();
	}
	
}
