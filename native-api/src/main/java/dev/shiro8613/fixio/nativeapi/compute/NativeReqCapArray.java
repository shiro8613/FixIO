package dev.shiro8613.fixio.nativeapi.compute;

import org.lwjgl.system.MemoryUtil;
import java.lang.ref.Cleaner;

public class NativeReqCapArray {
    private static final Cleaner CLEANER = Cleaner.create();

    private final State state;
    private final Cleaner.Cleanable cleanable;
    private final int size;
    private int capacity;

    public NativeReqCapArray(int initialCapacity, int size) {
        this.capacity = Math.max(1, initialCapacity);
        this.size = Math.max(1, size);
        long initialAddress = MemoryUtil.nmemAlloc((long) this.capacity * size);

        if (initialAddress == MemoryUtil.NULL) {
            throw new OutOfMemoryError("Failed to native alloc: " + (this.capacity * size) + " bytes");
        }

        this.state = new State(initialAddress);
        this.cleanable = CLEANER.register(this, state);
    }

    public long ensureCapacity(int requiredCapacity) {
        if (requiredCapacity > this.capacity) {
            int newCapacity = Math.max(requiredCapacity, this.capacity * 2);
            long newAddress = MemoryUtil.nmemRealloc(state.address, (long) newCapacity * size);

            if (newAddress == MemoryUtil.NULL) {
                throw new OutOfMemoryError("Failed to native realloc: " + (newCapacity * size) + " bytes");
            }

            state.address = newAddress;
            this.capacity = newCapacity;
        }
        return state.address;
    }

    public long getAddress() {
        return state.address;
    }

    public int getCapacity() {
        return capacity;
    }

    public void free() {
        cleanable.clean();
    }

    private static class State implements Runnable {
        private long address;

        State(long address) {
            this.address = address;
        }

        @Override
        public void run() {
            if (address != MemoryUtil.NULL) {
                MemoryUtil.nmemFree(address);
                address = MemoryUtil.NULL;
            }
        }
    }
}