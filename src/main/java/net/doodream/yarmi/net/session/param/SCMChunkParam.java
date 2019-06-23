package net.doodream.yarmi.net.session.param;


public class SCMChunkParam {
    public static final int TYPE_CONTINUE = 0;
    public static final int TYPE_LAST = 1;


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final SCMChunkParam chunkParam = new SCMChunkParam();

        private Builder() { }

        public Builder sizeInBytes(int size) {
            chunkParam.sizeInBytes = size;
            return this;
        }

        public Builder sequence(int seqNo) {
            chunkParam.sequence = seqNo;
            return this;
        }

        public Builder type(int type) {
            chunkParam.type = type;
            return this;
        }

        public Builder data(byte[] data) {
            chunkParam.data = data;
            return this;
        }

        public SCMChunkParam build() {
            return chunkParam;
        }
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public void setSizeInBytes(int sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public void setType(int type) {
        this.type = type;
    }

    public byte[] getData() {
        return data;
    }

    public int getSequence() {
        return sequence;
    }

    public int getSizeInBytes() {
        return sizeInBytes;
    }

    public int getType() {
        return type;
    }

    private int type;
    private int sizeInBytes;
    private int sequence;
    private byte[] data;
}
