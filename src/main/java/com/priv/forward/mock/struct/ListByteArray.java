package com.priv.forward.mock.struct;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.protobuf.ByteString;

public class ListByteArray {

    private final List<ByteString> elements;

    public ListByteArray(List<ByteString> elements) {
        this.elements = elements;
    }

    public static ListByteArray parseFrom(ByteString value) {
        ByteArrayInputStream bais = new ByteArrayInputStream(value.toByteArray());
        List<ByteString> elements = new ArrayList<>();
        while (bais.available() > 0) {
            int length = bais.read();
            byte[] bytes = new byte[length];
            int read = bais.read(bytes, 0, length);
            elements.add(ByteString.copyFrom(bytes, 0, read));
        }
        return new ListByteArray(elements);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public int getElementsCount() {
        return elements.size();
    }

    public ByteString getElements(int index) {
        return elements.get(index);
    }

    public List<ByteString> getElementsList() {
        return Collections.emptyList();
    }

    public ByteString toByteString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (ByteString element : elements) {
            byte[] bytes = element.toByteArray();
            baos.write(bytes.length);
            baos.write(bytes, 0, bytes.length);
        }
        return ByteString.copyFrom(baos.toByteArray());
    }


    public static class Builder {

        private final List<ByteString> elements = new ArrayList<>();

        @SuppressWarnings("UnusedReturnValue")
        public Builder addElements(ByteString element) {
            elements.add(element);
            return this;
        }

        public ListByteArray build() {
            return new ListByteArray(elements);
        }
    }
}
