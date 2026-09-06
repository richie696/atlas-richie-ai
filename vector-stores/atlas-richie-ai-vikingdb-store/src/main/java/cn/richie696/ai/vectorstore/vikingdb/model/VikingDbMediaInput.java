package cn.richie696.ai.vectorstore.vikingdb.model;

import org.springframework.util.Assert;


/**
 * Provider-neutral media value accepted by VikingDB multimodal APIs.
 */
public final class VikingDbMediaInput {
    public enum Kind {TEXT, URI, BYTES}

    private final Kind kind;
    private final String text;
    private final byte[] bytes;

    private VikingDbMediaInput(Kind kind, String text, byte[] bytes) {
        this.kind = kind;
        this.text = text;
        this.bytes = bytes == null ? null : bytes.clone();
    }

    public static VikingDbMediaInput text(String value) {
        Assert.hasText(value, "media text must not be blank");
        return new VikingDbMediaInput(Kind.TEXT, value, null);
    }

    public static VikingDbMediaInput uri(String value) {
        Assert.hasText(value, "media uri must not be blank");
        return new VikingDbMediaInput(Kind.URI, value, null);
    }

    public static VikingDbMediaInput bytes(byte[] value) {
        Assert.notNull(value, "media bytes must not be null");
        Assert.isTrue(value.length > 0, "media bytes must not be empty");
        return new VikingDbMediaInput(Kind.BYTES, null, value);
    }

    public Kind kind() {
        return kind;
    }

    public String text() {
        return text;
    }

    public byte[] bytes() {
        return bytes == null ? null : bytes.clone();
    }

    public Object nativeValue() {
        return kind == Kind.BYTES ? bytes() : text;
    }

    @Override
    public String toString() {
        return "VikingDbMediaInput[kind=" + kind + ", value="
                + (kind == Kind.BYTES ? "<bytes:" + bytes.length + ">" : text) + "]";
    }
}
