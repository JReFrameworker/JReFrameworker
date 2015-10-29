import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Test {

	public static void main(String[] args) throws IOException {
		File testFile = new File("secretFile");
		FileWriter fw = new FileWriter(testFile);
		fw.write("blah");
		fw.close();
		System.out.println("Secret File Exists: " + testFile.exists());
		testFile.delete();
	}

}
