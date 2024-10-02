package com.emc.mongoose.storage.driver.coop.netty.data;

import static com.emc.mongoose.base.storage.driver.StorageDriver.BUFF_SIZE_MAX;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;

/**
 * Modified version of io.netty.handler.stream.ChunkedNioStream with changes
 * from SeekableByteChannelChunkedNioStream.
 * Ensures that the final call to readChunk() writes only a partial chunk to the
 * given byte channel and isEndOfInput() causes the stream to end.
 */
public class PartialChunkedNioStream implements ChunkedInput<ByteBuf> {
    private final ReadableByteChannel in;
    private final int chunkSize;
    private final long sizeToTransfer;
    private final ByteBuffer byteBuffer;

    private long offset = 0;

    public PartialChunkedNioStream(SeekableByteChannel sbc) throws IOException {
        this(sbc, sbc.size());
    }

    private PartialChunkedNioStream(ReadableByteChannel in, long sizeToTransfer) {
        this.in = in;
        this.chunkSize = (int) (sizeToTransfer > BUFF_SIZE_MAX ? BUFF_SIZE_MAX : sizeToTransfer);
        this.sizeToTransfer = sizeToTransfer;
        this.byteBuffer = ByteBuffer.allocate(chunkSize);
    }

    @Override
    public final boolean isEndOfInput() {
        // Offset may exceed size when size is not a multiple of chunk size
        return offset >= sizeToTransfer;
    }

    @Override
    public void close() throws Exception {
        in.close();
    }

    @Deprecated
    @Override
    public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception {
        throw new UnsupportedOperationException("readChunk(ChannelHandlerContext) is not supported");
    }

    @Override
    public ByteBuf readChunk(ByteBufAllocator allocator) throws Exception {
        if (isEndOfInput()) {
            return null;
        }

        // Allow for partial chunk write
        int writeBytes = (int) Math.min(chunkSize, sizeToTransfer - offset);

        // Should be empty
        int readBytes = byteBuffer.position();

        // Read a whole chunk
        while (true) {
            int localReadBytes = in.read(byteBuffer);
            if (localReadBytes < 0) {
                break;
            }
            readBytes += localReadBytes;
            offset += localReadBytes;
            if (readBytes == chunkSize) {
                break;
            }
        }

        byteBuffer.flip();
        boolean release = true;
        ByteBuf buffer = allocator.buffer(writeBytes);

        // Re-position the byte buffer for the partial chunk write
        if (writeBytes < chunkSize) {
            byteBuffer.position(chunkSize - writeBytes);
        }

        // Write the chunk
        try {
            buffer.writeBytes(byteBuffer);
            byteBuffer.clear();
            release = false;
            return buffer;
        } finally {
            if (release) {
                buffer.release();
            }
        }
    }

    @Override
    public long length() {
        return sizeToTransfer;
    }

    @Override
    public long progress() {
        return offset;
    }
}
