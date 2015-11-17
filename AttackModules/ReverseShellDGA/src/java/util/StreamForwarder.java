package java.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jreframeworker.annotations.types.DefineType;

@DefineType
public class StreamForwarder implements Runnable {

	private InputStream input;
	private OutputStream output;
	
	public StreamForwarder(final InputStream input, final OutputStream output){
		this.input = input;
		this.output = output;
	}
	
	public void run() {
		try {
			final byte[] buf = new byte[4096];
			int length;
			while ((length = input.read(buf)) != -1) {
				if (output != null) {
					output.write(buf, 0, length);
					if (input.available() == 0) {
						output.flush();
					}
				}
			}
		} catch (Exception e) {
			// die silently
		} finally {
			try {
				input.close();
				output.close();
			} catch (IOException e) {
				// die silently
			}
		}
	}

}
