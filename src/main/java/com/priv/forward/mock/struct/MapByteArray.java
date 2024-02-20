package com.priv.forward.mock.struct;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ByteString;

public class MapByteArray {

    private final List<MapEntry> elements;

    public MapByteArray(List<MapEntry> elements) {
        this.elements = elements;
    }

    public static MapByteArray parseFrom(ByteString value) {
        ByteArrayInputStream bais = new ByteArrayInputStream(value.toByteArray());
        List<MapEntry> elements = new ArrayList<>();
        while (bais.available() > 0) {
            int length = bais.read();
            byte[] bytes = new byte[length];
            int ignored = bais.read(bytes, 0, length);
            elements.add(MapEntry.parseFrom(bytes));
        }
        return new MapByteArray(elements);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public List<MapEntry> getEntryList() {
        return elements;
    }

    public ByteString toByteString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (MapEntry element : elements) {
            byte[] bytes = element.toByteString().toByteArray();
            baos.write(bytes.length);
            baos.write(bytes, 0, bytes.length);
        }
        return ByteString.copyFrom(baos.toByteArray());
    }

    public static class Builder {

        private final List<MapEntry> elements = new ArrayList<>();

        private Builder() {
        }

        public MapByteArray build() {
            return new MapByteArray(elements);
        }

        @SuppressWarnings("UnusedReturnValue")
        public Builder addEntry(MapEntry.Builder builder) {
            elements.add(builder.build());
            return this;
        }
    }
}
