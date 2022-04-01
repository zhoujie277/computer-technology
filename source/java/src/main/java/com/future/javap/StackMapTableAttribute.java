package com.future.javap;


public class StackMapTableAttribute extends Attribute {

    private StackMapFrame[] entries;

    public StackMapTableAttribute(ClassFile classFile, int nameIndex, byte[] data) {
        super(classFile, nameIndex, data);
    }

    @Override
    public void parse() {
        try {
            int numOfEntries = readUnsignedShort();
            entries = new StackMapFrame[numOfEntries];
            for (int i = 0; i < numOfEntries; i++) {
                StackMapFrame frame;
                int frame_type = readByte();
                if (frame_type <= 63)
                    frame = new SameFrame(frame_type);
                else if (frame_type <= 127)
                    frame = new SameLocals1StackItemFrame(frame_type, this);
                else if (frame_type <= 246)
                    throw new Error("unknown frame_type " + frame_type);
                else if (frame_type == 247)
                    frame = new SameLocals1StackItemFrameExtended(frame_type, this);
                else if (frame_type <= 250)
                    frame = new ChopFrame(frame_type, this);
                else if (frame_type == 251)
                    frame = new SameFrameExtended(frame_type, this);
                else if (frame_type <= 254)
                    frame = new AppendFrame(frame_type, this);
                else
                    frame = new FullFrame(frame_type, this);
                this.entries[i] = frame;
            }
        } catch (InvalidStackMap e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append("StackMapTableAttribute ");
        builder.append("Count = ").append(entries.length);
        builder.append(", entries: [");
        for (StackMapFrame entry : entries) {
            builder.append("\n\t\t\t\t\t").append(entry);
        }
        builder.append("\n\t\t\t]");
        return builder.toString();
    }

    private static abstract class StackMapFrame {
        public final int frameType;

        protected StackMapFrame(int frame_type) {
            this.frameType = frame_type;
        }

        public int length() {
            return 1;
        }

        public abstract int getOffsetDelta();

        @Override
        public String toString() {
            return getClass().getSimpleName() + " {frameType = " + frameType + "}";
        }
    }

    private static class SameFrame extends StackMapFrame {

        protected SameFrame(int frame_type) {
            super(frame_type);
        }

        @Override
        public int getOffsetDelta() {
            return frameType;
        }
    }

    private static class SameLocals1StackItemFrame extends StackMapFrame {

        public final VerificationTypeInfo[] stack;

        protected SameLocals1StackItemFrame(int frame_type, StackMapTableAttribute attribute) throws InvalidStackMap {
            super(frame_type);
            stack = new VerificationTypeInfo[1];
            stack[0] = VerificationTypeInfo.read(attribute);
        }

        @Override
        public int length() {
            return super.length() + stack[0].length();
        }

        public int getOffsetDelta() {
            return frameType - 64;
        }

        @Override
        public String toString() {
            return super.toString() + "\n\t\t\t\t\t\t" + "stack = " + stack[0];
        }
    }

    private static class SameLocals1StackItemFrameExtended extends StackMapFrame {
        public final int offsetDelta;
        public final VerificationTypeInfo[] stack;

        SameLocals1StackItemFrameExtended(int frame_type, StackMapTableAttribute cr)
                throws InvalidStackMap {
            super(frame_type);
            offsetDelta = cr.readUnsignedShort();
            stack = new VerificationTypeInfo[1];
            stack[0] = VerificationTypeInfo.read(cr);
        }

        @Override
        public int length() {
            return super.length() + 2 + stack[0].length();
        }

        public int getOffsetDelta() {
            return offsetDelta;
        }
    }

    private static class ChopFrame extends StackMapFrame {
        public final int offset_delta;

        ChopFrame(int frame_type, IReader cr) {
            super(frame_type);
            offset_delta = cr.readUnsignedShort();
        }

        @Override
        public int length() {
            return super.length() + 2;
        }

        public int getOffsetDelta() {
            return offset_delta;
        }
    }

    private static class SameFrameExtended extends StackMapFrame {
        public final int offsetDelta;

        SameFrameExtended(int frame_type, IReader cr) {
            super(frame_type);
            offsetDelta = cr.readUnsignedShort();
        }

        @Override
        public int length() {
            return super.length() + 2;
        }

        public int getOffsetDelta() {
            return offsetDelta;
        }

    }

    private static class AppendFrame extends StackMapFrame {
        public final int offsetDelta;
        public final VerificationTypeInfo[] locals;

        AppendFrame(int frame_type, StackMapTableAttribute cr) throws InvalidStackMap {
            super(frame_type);
            offsetDelta = cr.readUnsignedShort();
            locals = new VerificationTypeInfo[frame_type - 251];
            for (int i = 0; i < locals.length; i++)
                locals[i] = VerificationTypeInfo.read(cr);
        }

        @Override
        public int length() {
            int n = super.length() + 2;
            for (VerificationTypeInfo local : locals)
                n += local.length();
            return n;
        }

        public int getOffsetDelta() {
            return offsetDelta;
        }
    }

    private static class FullFrame extends StackMapFrame {
        public final int offsetDelta;
        public final int numberOfLocals;
        public final VerificationTypeInfo[] locals;
        public final int numberOfStackItems;
        public final VerificationTypeInfo[] stack;

        FullFrame(int frame_type, StackMapTableAttribute cr) throws InvalidStackMap {
            super(frame_type);
            offsetDelta = cr.readUnsignedShort();
            numberOfLocals = cr.readUnsignedShort();
            locals = new VerificationTypeInfo[numberOfLocals];
            for (int i = 0; i < locals.length; i++)
                locals[i] = VerificationTypeInfo.read(cr);
            numberOfStackItems = cr.readUnsignedShort();
            stack = new VerificationTypeInfo[numberOfStackItems];
            for (int i = 0; i < stack.length; i++)
                stack[i] = VerificationTypeInfo.read(cr);
        }

        @Override
        public int length() {
            int n = super.length() + 2;
            for (VerificationTypeInfo local : locals)
                n += local.length();
            n += 2;
            for (VerificationTypeInfo item : stack)
                n += item.length();
            return n;
        }

        public int getOffsetDelta() {
            return offsetDelta;
        }
    }

    private static class VerificationTypeInfo {
        public static final int ITEM_Top = 0;
        public static final int ITEM_Integer = 1;
        public static final int ITEM_Float = 2;
        public static final int ITEM_Long = 4;
        public static final int ITEM_Double = 3;
        public static final int ITEM_Null = 5;
        public static final int ITEM_UninitializedThis = 6;
        public static final int ITEM_Object = 7;
        public static final int ITEM_Uninitialized = 8;

        public static VerificationTypeInfo read(StackMapTableAttribute attribute) throws InvalidStackMap {
            int tag = attribute.readByte();
            switch (tag) {
                case ITEM_Top:
                case ITEM_Integer:
                case ITEM_Float:
                case ITEM_Long:
                case ITEM_Double:
                case ITEM_Null:
                case ITEM_UninitializedThis:
                    return new VerificationTypeInfo(tag);
                case ITEM_Object:
                    return new ObjectVariableInfo(attribute);
                case ITEM_Uninitialized:
                    return new UninitializedVariableInfo(attribute);
                default:
                    throw new InvalidStackMap("unrecognized verification_type_info tag");
            }
        }

        public final int tag;

        protected VerificationTypeInfo(int tag) {
            this.tag = tag;
        }

        public int length() {
            return 1;
        }

        @Override
        public String toString() {
            return "VerificationTypeInfo {" +
                    "tag = " + tag +
                    '}';
        }
    }

    private static class ObjectVariableInfo extends VerificationTypeInfo {
        public final int cPoolIndex;
        private StackMapTableAttribute attribute;

        ObjectVariableInfo(StackMapTableAttribute attribute) {
            super(ITEM_Object);
            this.attribute = attribute;
            cPoolIndex = attribute.readUnsignedShort();
        }

        @Override
        public int length() {
            return super.length() + 2;
        }

        @Override
        public String toString() {
            return "ObjectVariableInfo {" +
                    "cPoolIndex = " + attribute.classFile.toString(cPoolIndex) +
                    '}';
        }
    }

    private static class UninitializedVariableInfo extends VerificationTypeInfo {
        public final int offset;

        protected UninitializedVariableInfo(IReader reader) {
            super(ITEM_Uninitialized);
            offset = reader.readUnsignedShort();
        }

        @Override
        public int length() {
            return super.length() + 2;
        }
    }

    static class InvalidStackMap extends AttributeException {
        private static final long serialVersionUID = -5659038410855089780L;

        InvalidStackMap(String msg) {
            super(msg);
        }
    }
}
