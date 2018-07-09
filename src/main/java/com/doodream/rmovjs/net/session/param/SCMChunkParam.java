package com.doodream.rmovjs.net.session.param;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SCMChunkParam {
    public static final int TYPE_CONTINUE = 0;
    public static final int TYPE_LAST = 1;

    private int type;
    private int sizeInChar;
    private int sequence;
    private transient byte[] cachedChunk;
}
