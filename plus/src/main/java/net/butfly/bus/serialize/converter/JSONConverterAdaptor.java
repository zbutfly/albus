package net.butfly.bus.serialize.converter;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class JSONConverterAdaptor extends ConverterAdaptor<TypeAdapterFactory> {
	@Override
	public <SRC, DST> TypeAdapterFactory create(Class<? extends Converter<SRC, DST>> converterClass) {
		Converter<SRC, DST> converter = getConverter(converterClass);
		TypeAdapterFactory factory = new TypeAdapterFactory() {
			@SuppressWarnings("unchecked")
			@Override
			public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
				if (!converter.getOriginalClass().isAssignableFrom(type.getRawType())) return null;
				TypeAdapter<SRC> adapter = new TypeAdapter<SRC>() {
					@Override
					public void write(JsonWriter out, SRC original) throws IOException {
						DST replaced = converter.serialize(original);
						if (null == replaced) out.nullValue();
						else {
							TypeAdapter<DST> typeAdapter = (TypeAdapter<DST>) gson.getAdapter(replaced.getClass());
							typeAdapter.write(out, replaced);
						}
					}

					@Override
					public SRC read(JsonReader in) throws IOException {
						// TODO Auto-generated method stub
						return null;
					}
				};

				return (TypeAdapter<T>) adapter;
			}
		};
		return factory;
	}
}
