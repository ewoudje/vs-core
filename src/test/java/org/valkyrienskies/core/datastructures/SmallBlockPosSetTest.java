package org.valkyrienskies.core.datastructures;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.joml.Vector3i;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.valkyrienskies.core.util.serialization.VSJacksonUtil;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SmallBlockPosSetTest {

    private static final ObjectMapper serializer = VSJacksonUtil.Companion.getDefaultMapper();

    @ParameterizedTest
    @MethodSource("coordsAndCenterGenerator")
    public void testDeHash(int x, int y, int z, int centerX, int centerZ) {
        SmallBlockPosSet set = new SmallBlockPosSet(centerX, centerZ);
        set.add(x, y, z);
        assertEquals(set.iterator().next(), new Vector3i(x, y, z));
    }

    private static Stream<Arguments> coordsAndCenterGenerator() {
        final int testIterations = 500;
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        return IntStream.range(0, testIterations)
            .mapToObj(ignore -> {
                int centerX = random.nextInt(Integer.MIN_VALUE + 2048, Integer.MAX_VALUE - 2047);
                int centerZ = random.nextInt(Integer.MIN_VALUE + 2048, Integer.MAX_VALUE - 2047);
                int x = random.nextInt(-2048, 2047);
                int y = random.nextInt(0, 255);
                int z = random.nextInt(-2048, 2047);
                return Arguments.arguments(centerX + x, y, centerZ + z, centerX, centerZ);
            });
    }

    private static Stream<Arguments> coordsGenerator(final int centerX, final int centerZ) {
        final int testIterations = 500;
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        return IntStream.range(0, testIterations)
                .mapToObj(ignore -> {
                    int x = random.nextInt(-2048, 2047);
                    int y = random.nextInt(0, 255);
                    int z = random.nextInt(-2048, 2047);
                    return Arguments.arguments(centerX + x, y, centerZ + z, centerX, centerZ);
                });
    }

    /**
     * Tests the correctness of SmallBlockPosSet serialization and deserialization.
     */
    @RepeatedTest(25)
    public void testSerializationAndDeSerialization() throws IOException {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final int centerX = random.nextInt(Integer.MIN_VALUE + 2048, Integer.MAX_VALUE - 2047);
        final int centerZ = random.nextInt(Integer.MIN_VALUE + 2048, Integer.MAX_VALUE - 2047);

        final SmallBlockPosSet blockPosSet = new SmallBlockPosSet(centerX, centerZ);
        final Stream<Arguments> coordinatesGenerator = coordsGenerator(centerX, centerZ);

        coordinatesGenerator.forEach(pos -> blockPosSet.add((Integer) pos.get()[0], (Integer) pos.get()[1], (Integer) pos.get()[2]));

        // Now serialize and deserialize and verify that they are the same
        final byte[] blockPosSetSerialized = serializer.writeValueAsBytes(blockPosSet);
        final SmallBlockPosSet blockPosSetDeserialized = serializer.readValue(blockPosSetSerialized, SmallBlockPosSet.class);

        // Verify both sets are equal
        assertEquals(blockPosSet, blockPosSetDeserialized);
    }

}
