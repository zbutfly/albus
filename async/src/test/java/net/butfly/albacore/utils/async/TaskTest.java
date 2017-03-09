package net.butfly.albacore.utils.async;

import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Task;

public class TaskTest {
	public static void main(String[] args) throws Exception {
		Task<String> task = new Task<String>(() -> {
			System.out.println("called ==> " + Thread.currentThread().getName() + "{" + Thread.currentThread().getId() + "}");
			Thread.yield();
			return "Hello, World: " + Math.random();
		}, result -> {
			System.out.println("backed ==> " + Thread.currentThread().getName() + "{" + Thread.currentThread().getId() + "}");
			Thread.yield();
		}, new Options().fork());
		task.execute();
		Thread.sleep(10000);
	}
}
