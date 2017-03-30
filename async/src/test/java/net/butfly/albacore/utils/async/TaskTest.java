package net.butfly.albacore.utils.async;

import java.util.function.Consumer;

import net.butfly.albacore.lambda.Callable;
import net.butfly.albacore.utils.parallel.Concurrents;

public class TaskTest {
	public static void main(String[] args) throws Exception {
		Callable<String> call = () -> {
			System.out.println("called ==> " + Thread.currentThread().getName() + "{" + Thread.currentThread().getId() + "}");
			Thread.yield();
			return "Hello, World: " + Math.random();
		};
		Consumer<String> back = result -> {
			System.out.println("backed ==> " + Thread.currentThread().getName() + "{" + Thread.currentThread().getId() + "}");
			Concurrents.waitSleep(10000);
		};
		Task<String> task = new Task<String>(call, back, new Options().fork());
		task.execute();
		Concurrents.waitSleep(10000);
	}
}
