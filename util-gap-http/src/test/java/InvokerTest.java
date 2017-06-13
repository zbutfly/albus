import static java.lang.System.out;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import net.butfly.albacore.utils.Reflections;
import net.butfly.bus.utils.http.HttpRequest;

public class InvokerTest {

	public static void main(String[] args) throws FileNotFoundException {
		String fname = "26732c25-b61f-4d2d-bb8a-e1d47ac09edb.req";
		HttpRequest r = new HttpRequest().load(new FileInputStream("C:\\Workspaces\\alfames\\albus\\util-gap-http\\pool\\reqs\\" + fname));
		byte[] body = Reflections.get(r, "body");
		out.println(new String(body));
	}

}
