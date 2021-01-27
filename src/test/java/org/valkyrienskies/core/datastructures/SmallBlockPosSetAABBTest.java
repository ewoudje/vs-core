package org.valkyrienskies.core.datastructures;

import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SmallBlockPosSetAABBTest {

    @Test
    public void testSmallBlockPosSetAABB() {
        SmallBlockPosSetAABB toTest = new SmallBlockPosSetAABB(0, 0, 0, 1024, 1024, 1024);
        ExtremelyNaiveVoxelFieldAABBMaker aabbMaker = new ExtremelyNaiveVoxelFieldAABBMaker(0, 0);


        // Test adding new positions
        Vector3ic pos0 = new Vector3i(5, 10, 3);
        assertEquals(toTest.add(pos0), aabbMaker.addVoxel(pos0));
        assertEquals(toTest.makeAABB(), aabbMaker.makeVoxelFieldAABB());

        Vector3ic pos1 = new Vector3i(2, 5, 3);
        assertEquals(toTest.add(pos1), aabbMaker.addVoxel(pos1));
        assertEquals(toTest.makeAABB(), aabbMaker.makeVoxelFieldAABB());

        Vector3ic pos2 = new Vector3i(1, 20, 0);
        assertEquals(toTest.add(pos2), aabbMaker.addVoxel(pos2));
        assertEquals(toTest.makeAABB(), aabbMaker.makeVoxelFieldAABB());


        // Test adding duplicates
        Vector3ic pos3 = new Vector3i(1, 20, 0);
        assertEquals(toTest.add(pos3), aabbMaker.addVoxel(pos3));
        assertEquals(toTest.makeAABB(), aabbMaker.makeVoxelFieldAABB());


        // Test removing what doesn't exist
        Vector3ic pos4 = new Vector3i(6, 7, 8);
        assertEquals(toTest.remove(pos4), aabbMaker.removeVoxel(pos4));
        assertEquals(toTest.makeAABB(), aabbMaker.makeVoxelFieldAABB());


        // Test removing what does exist
        Vector3ic pos5 = new Vector3i(5, 10, 3);
        assertEquals(toTest.remove(pos5), aabbMaker.removeVoxel(pos5));
        assertEquals(toTest.makeAABB(), aabbMaker.makeVoxelFieldAABB());

        Vector3ic pos6 = new Vector3i(2, 5, 3);
        assertEquals(toTest.remove(pos6), aabbMaker.removeVoxel(pos6));
        assertEquals(toTest.makeAABB(), aabbMaker.makeVoxelFieldAABB());

        Vector3ic pos7 = new Vector3i(1, 20, 0);
        assertEquals(toTest.remove(pos7), aabbMaker.removeVoxel(pos7));
        assertEquals(toTest.makeAABB(), aabbMaker.makeVoxelFieldAABB());


        // Test adding new positions
        Vector3ic pos8 = new Vector3i(25, 2, 35);
        assertEquals(toTest.add(pos8), aabbMaker.addVoxel(pos8));
        assertEquals(toTest.makeAABB(), aabbMaker.makeVoxelFieldAABB());


        // Test clear
        toTest.clear();
        aabbMaker.clear();
        assertEquals(toTest.makeAABB(), aabbMaker.makeVoxelFieldAABB());
    }
}
