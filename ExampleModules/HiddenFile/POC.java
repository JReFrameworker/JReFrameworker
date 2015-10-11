import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class POC {

	public static void main(String[] args) throws IOException {
		File testFile = new File("secretFile");
		FileWriter fw = new FileWriter(testFile);
		fw.write("test");
		fw.close();
		System.out.println("Test File Exists: " + testFile.exists());
		testFile.delete();
	}
	
}
