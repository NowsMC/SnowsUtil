package space.nows.util.collections;

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

class CollectionsOddTest {
    @Test
    void intListAndHeapRejectEmptyAccess() {
        IntList list = new IntList();
        assertThrows(RuntimeException.class, list::first, "first() on empty IntList");
        assertThrows(RuntimeException.class, list::last, "last() on empty IntList");
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(-1));
        IntMinHeap heap = new IntMinHeap();
        assertThrows(NoSuchElementException.class, heap::poll);
        assertThrows(RuntimeException.class, heap::peek, "peek() on empty heap");
        IntArrayDeque deque = new IntArrayDeque(0);
        assertThrows(NoSuchElementException.class, deque::removeFirst);
        assertThrows(IndexOutOfBoundsException.class, () -> deque.get(0));
        deque.addFirst(5);
        assertEquals(5, deque.last());
    }

    @Test
    void setAndMapOddConfigurations() {
        assertThrows(IllegalArgumentException.class, () -> new IntHashSet(4, 1.5f));
        assertThrows(IllegalArgumentException.class, () -> new IntHashSet(-1));
        IntIntMap map = new IntIntMap(0, 0.5f, -7);
        assertEquals(-7, map.get(123));
        map.put(0, 0);
        assertEquals(0, map.get(0));
        assertTrue(map.containsKey(0));
        map.remove(0);
        assertFalse(map.containsKey(0));
        assertEquals(0, map.size());
    }

}
