package org.valkyrienskies.core.datastructures;

import org.joml.Vector3i;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SmallBlockPosSetTest {

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

}
