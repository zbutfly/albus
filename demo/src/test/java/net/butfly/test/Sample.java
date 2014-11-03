//package net.butfly.test;
//
//import java.io.IOException;
//
//import net.butfly.bus.client.Client;
//import net.butfly.bus.context.Constants.InternalTX;
//
//public class Sample {
//	public static void main(String[] args) throws IOException {
//		Client client = new Client();
//		// FlightInfoFacade facade =
//		// client.newProxyInstance(FlightInfoFacade.class);
//		// AirlineNameSelectAllResponse resp = facade.selectAllAirlinesName();
//		// System.out.println(resp.getAirNameContainer().size());
//		System.out.println(client.invoke("BUS_00_00"));
//		System.out.println(client.invoke(InternalTX.PING));
//		String echo = client.invoke("SPL_001", "Hello, world!");
//		System.out.println(echo);
//	}
//}
