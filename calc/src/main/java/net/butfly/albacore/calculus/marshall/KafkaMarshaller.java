package net.butfly.albacore.calculus.marshall;

import net.butfly.albacore.calculus.CalculatorContext;
import net.butfly.albacore.calculus.Functor;
import net.butfly.albacore.calculus.FunctorConfig;

public class KafkaMarshaller implements Marshaller<String, String> {
	private static final long serialVersionUID = -4471098188111221100L;

	@Override
	public String unmarshallId(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends Functor<T>> T unmarshall(String from, Class<T> to) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String marshallId(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends Functor<T>> String marshall(T from) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <F extends Functor<F>> void confirm(Class<F> functor, FunctorConfig config, CalculatorContext globalConfig) {}
}
