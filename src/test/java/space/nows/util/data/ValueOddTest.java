package space.nows.util.data;

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

class ValueOddTest {
    @Test
    void valueEqualityAcrossKinds() {
        Value num = Num.of(3);
        assertNotEquals(num, new space.nows.util.data.Str("3"));
        assertNotEquals(Json.parse("[1]"), Json.parse("{\"0\":1}"));
    }

}
