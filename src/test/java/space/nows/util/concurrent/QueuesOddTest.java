package space.nows.util.concurrent;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import space.nows.util.collections.IntArrayDeque;
import space.nows.util.collections.IntHashSet;
import space.nows.util.collections.IntIntMap;
import space.nows.util.collections.IntList;
import space.nows.util.collections.IntMinHeap;
import space.nows.util.concurrent.MpmcQueue;
import space.nows.util.concurrent.ObjectPool;
import space.nows.util.concurrent.SpscQueue;
import space.nows.util.data.mapping.Mapper;
import space.nows.util.data.Num;
import space.nows.util.data.Value;
import space.nows.util.error.CapacityExceededException;
import space.nows.util.error.DataException;
import space.nows.util.io.ByteBuf;
import space.nows.util.io.FileIO;
import space.nows.util.io.Gzip;
import space.nows.util.json.Json;
import space.nows.util.text.Hex;
import static org.junit.jupiter.api.Assertions.*;

class QueuesOddTest {
    @Test
    void queuesAndPoolAtBoundaries() {
        SpscQueue<String> one = new SpscQueue<>(1);
        assertEquals(1, one.capacity());
        assertTrue(one.offer("a"));
        assertFalse(one.offer("b"));
        assertEquals("a", one.poll());
        assertThrows(IllegalArgumentException.class, () -> new SpscQueue<>(0));
        assertThrows(IllegalArgumentException.class, () -> new SpscQueue<>((1 << 30) + 1));
        assertThrows(NullPointerException.class, () -> one.offer(null));
        MpmcQueue<String> mpmc = new MpmcQueue<>(1);
        int accepted = 0;
        while (mpmc.offer("x")) accepted++;
        assertEquals(mpmc.capacity(), accepted, "queue must hold exactly its reported capacity");
        ObjectPool<StringBuilder> pool = new ObjectPool<>(0, StringBuilder::new);
        assertFalse(pool.release(new StringBuilder()), "zero-sized pool retains nothing");
        assertEquals(0, pool.idleCount());
        assertEquals(0, pool.maxIdle());
    }

}
