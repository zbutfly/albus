import net.butfly.albacore.utils.async.Task;
import net.butfly.albacore.utils.async.Task.Callable;
import net.butfly.albacore.utils.async.Task.Callback;
import net.butfly.bus.support.ContinuousOptions;

public class TaskTest {
	public static void main(String[] args) throws Exception {
		Callable<String> call = new Task.Callable<String>() {
			@Override
			public String call() throws Exception {
				System.out.println("called ==> " + Thread.currentThread().getName() + "{" + Thread.currentThread().getId()
						+ "}");
				Thread.yield();
				return "Hello, World: " + Math.random();
			}
		};
		Callback<String> back = new Callback<String>() {
			@Override
			public void callback(String result) throws Exception {
				System.out.println("backed ==> " + Thread.currentThread().getName() + "{" + Thread.currentThread().getId()
						+ "}");
				Thread.yield();
			}
		};
		Task<String> task = new Task<String>(call, back, new ContinuousOptions().fork());
		task.execute();
		Thread.sleep(10000);
	}
}
