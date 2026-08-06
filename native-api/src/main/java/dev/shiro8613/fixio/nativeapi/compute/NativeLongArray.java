package dev.shiro8613.fixio.nativeapi.compute;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.lang.ref.Cleaner;
import org.lwjgl.system.MemoryUtil;

public class NativeLongArray {
    private static final Cleaner CLEANER = Cleaner.create();
    private final State state;
    private final Cleaner.Cleanable cleanable;
    private int capacity;
    private int count;
    private final Long2IntMap keyToIndexMap = new Long2IntOpenHashMap();

    public NativeLongArray(int initialCapacity) {
        this.capacity = initialCapacity;
        this.count = 0;
        long address = MemoryUtil.nmemAlloc((long) initialCapacity * Long.BYTES);
        this.state = new State(address);
        this.cleanable = CLEANER.register(this, state);
    }

    public void add(long sectionKey) {
        if (count >= capacity) resize(capacity * 2);

        MemoryUtil.memPutLong(state.address + ((long) count * Long.BYTES), sectionKey);
        keyToIndexMap.put(sectionKey, count);
        count++;
    }

    public boolean remove(long sectionKey) {
        int index = keyToIndexMap.remove(sectionKey);
        if (index == -1) return false;

        int lastIndex = count - 1;
        if (index < lastIndex) {
            long lastKey = MemoryUtil.memGetLong(state.address + ((long) lastIndex * Long.BYTES));
            MemoryUtil.memPutLong(state.address + ((long) index * Long.BYTES), lastKey);
            keyToIndexMap.put(lastKey, index);
        }
        count--;
        return true;
    }

    public void free() {
        cleanable.clean();
    }

    private void resize(int newCapacity) {
        state.address = MemoryUtil.nmemRealloc(state.address, (long) newCapacity * Long.BYTES);
        this.capacity = newCapacity;
    }

    public long getAddress() { return state.address; }
    public int getCount() { return count; }

    private static class State implements Runnable {
        private long address;

        State(long address) {
            this.address = address;
        }

        @Override
        public void run() {
            if (address != 0) {
                MemoryUtil.nmemFree(address);
                address = 0;
            }
        }
    }
}