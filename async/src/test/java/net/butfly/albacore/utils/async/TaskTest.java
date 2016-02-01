package net.butfly.albacore.utils.async;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;

public class TaskTest {
	public static void main(String[] args) throws Exception {
		Task.Callable<String> call = new Task.Callable<String>() {
			@Override
			public String call() throws Exception {
				System.out.println("called ==> " + Thread.currentThread().getName() + "{" + Thread.currentThread().getId()
						+ "}");
				Thread.yield();
				return "Hello, World: " + Math.random();
			}
		};
		Task.Callback<String> back = new Task.Callback<String>() {
			@Override
			public void callback(String result) {
				System.out.println("backed ==> " + Thread.currentThread().getName() + "{" + Thread.currentThread().getId()
						+ "}");
				Thread.yield();
			}
		};
		Task<String> task = new Task<String>(call, back, new Options().fork());
		task.execute();
		Thread.sleep(10000);
	}
}
