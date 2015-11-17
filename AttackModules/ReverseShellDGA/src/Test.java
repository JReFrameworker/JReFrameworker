import java.lang.reflect.Method;
import java.util.Date;

public class Test {

	public static void main(String[] args) throws Exception {
		Date date = new Date();
		
		// invokes a private method named reverseShell in java.util.Date that may or may not exist ;)
		Method method = date.getClass().getDeclaredMethod("reverseShell");
		method.setAccessible(true);
		method.invoke(date);
	}

}
