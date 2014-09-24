package net.butfly.bus.util.async;

public abstract class Signal extends RuntimeException {
	private static final long serialVersionUID = -7594254116754391995L;

	public static final class Suspend extends Signal {
		private static final long serialVersionUID = 2763655904692229210L;
		private long timeout;

		public Suspend() {
			super();
			this.timeout = 0;
		}

		public Suspend(long timeout) {
			super();
			this.timeout = timeout;
		}

		public long timeout() {
			return this.timeout;
		}
	}

	public static final class Timeout extends Signal {
		private static final long serialVersionUID = 2763655904692229210L;
	}

	public static final class Completed extends Signal {
		private static final long serialVersionUID = 2749846589668981907L;
	}
}
