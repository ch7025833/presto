/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.presto.spi.block;

import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.type.Type;
import io.airlift.slice.Slice;
import org.openjdk.jol.info.ClassLayout;

import java.lang.invoke.MethodHandle;
import java.util.function.BiConsumer;

import static com.facebook.presto.spi.StandardErrorCode.GENERIC_INTERNAL_ERROR;
import static com.facebook.presto.spi.block.AbstractMapBlock.HASH_MULTIPLIER;
import static io.airlift.slice.SizeOf.sizeOf;
import static io.airlift.slice.SizeOf.sizeOfIntArray;

public class SingleMapBlock
        extends AbstractSingleMapBlock
{
    private static final int INSTANCE_SIZE = ClassLayout.parseClass(SingleMapBlock.class).instanceSize();

    private final int offset;
    private final int positionCount;
    private final Block keyBlock;
    private final Block valueBlock;
    private final int[] hashTable;
    private final Type keyType;
    private final MethodHandle keyNativeHashCode;
    private final MethodHandle keyBlockNativeEquals;

    SingleMapBlock(int offset, int positionCount, Block keyBlock, Block valueBlock, int[] hashTable, Type keyType, MethodHandle keyNativeHashCode, MethodHandle keyBlockNativeEquals)
    {
        this.offset = offset;
        this.positionCount = positionCount;
        this.keyBlock = keyBlock;
        this.valueBlock = valueBlock;
        this.hashTable = hashTable;
        this.keyType = keyType;
        this.keyNativeHashCode = keyNativeHashCode;
        this.keyBlockNativeEquals = keyBlockNativeEquals;
    }

    @Override
    public int getPositionCount()
    {
        return positionCount;
    }

    @Override
    public long getSizeInBytes()
    {
        return keyBlock.getRegionSizeInBytes(offset / 2, positionCount / 2) +
                valueBlock.getRegionSizeInBytes(offset / 2, positionCount / 2) +
                sizeOfIntArray(positionCount / 2 * HASH_MULTIPLIER);
    }

    @Override
    public long getRetainedSizeInBytes()
    {
        return INSTANCE_SIZE + keyBlock.getRetainedSizeInBytes() + valueBlock.getRetainedSizeInBytes() + sizeOf(hashTable);
    }

    @Override
    public void retainedBytesForEachPart(BiConsumer<Object, Long> consumer)
    {
        consumer.accept(keyBlock, keyBlock.getRetainedSizeInBytes());
        consumer.accept(valueBlock, valueBlock.getRetainedSizeInBytes());
        consumer.accept(hashTable, sizeOf(hashTable));
        consumer.accept(this, (long) INSTANCE_SIZE);
    }

    @Override
    public BlockEncoding getEncoding()
    {
        return new SingleMapBlockEncoding(keyType, keyNativeHashCode, keyBlockNativeEquals, keyBlock.getEncoding(), valueBlock.getEncoding());
    }

    @Override
    public int getOffset()
    {
        return offset;
    }

    @Override
    Block getKeyBlock()
    {
        return keyBlock;
    }

    @Override
    Block getValueBlock()
    {
        return valueBlock;
    }

    int[] getHashTable()
    {
        return hashTable;
    }

    public int seekKey(Object nativeValue)
    {
        if (positionCount == 0) {
            return -1;
        }

        long hashCode;
        try {
            hashCode = (long) keyNativeHashCode.invoke(nativeValue);
        }
        catch (Throwable throwable) {
            throw handleThrowable(throwable);
        }

        int hashTableOffset = offset / 2 * HASH_MULTIPLIER;
        int hashTableSize = positionCount / 2 * HASH_MULTIPLIER;
        int hash = (int) Math.floorMod(hashCode, hashTableSize);
        while (true) {
            int keyPosition = hashTable[hashTableOffset + hash];
            if (keyPosition == -1) {
                return -1;
            }
            boolean match;
            try {
                match = (boolean) keyBlockNativeEquals.invoke(keyBlock, offset / 2 + keyPosition, nativeValue);
            }
            catch (Throwable throwable) {
                throw handleThrowable(throwable);
            }
            if (match) {
                return keyPosition * 2 + 1;
            }
            hash++;
            if (hash == hashTableSize) {
                hash = 0;
            }
        }
    }

    // The next 5 seekKeyExact functions are the same as seekKey
    // except MethodHandle.invoke is replaced with invokeExact.

    public int seekKeyExact(long nativeValue)
    {
        if (positionCount == 0) {
            return -1;
        }

        long hashCode;
        try {
            hashCode = (long) keyNativeHashCode.invokeExact(nativeValue);
        }
        catch (Throwable throwable) {
            throw handleThrowable(throwable);
        }

        int hashTableOffset = offset / 2 * HASH_MULTIPLIER;
        int hashTableSize = positionCount / 2 * HASH_MULTIPLIER;
        int hash = (int) Math.floorMod(hashCode, hashTableSize);
        while (true) {
            int keyPosition = hashTable[hashTableOffset + hash];
            if (keyPosition == -1) {
                return -1;
            }
            boolean match;
            try {
                match = (boolean) keyBlockNativeEquals.invokeExact(keyBlock, offset / 2 + keyPosition, nativeValue);
            }
            catch (Throwable throwable) {
                throw handleThrowable(throwable);
            }
            if (match) {
                return keyPosition * 2 + 1;
            }
            hash++;
            if (hash == hashTableSize) {
                hash = 0;
            }
        }
    }

    public int seekKeyExact(boolean nativeValue)
    {
        if (positionCount == 0) {
            return -1;
        }

        long hashCode;
        try {
            hashCode = (long) keyNativeHashCode.invokeExact(nativeValue);
        }
        catch (Throwable throwable) {
            throw handleThrowable(throwable);
        }

        int hashTableOffset = offset / 2 * HASH_MULTIPLIER;
        int hashTableSize = positionCount / 2 * HASH_MULTIPLIER;
        int hash = (int) Math.floorMod(hashCode, hashTableSize);
        while (true) {
            int keyPosition = hashTable[hashTableOffset + hash];
            if (keyPosition == -1) {
                return -1;
            }
            boolean match;
            try {
                match = (boolean) keyBlockNativeEquals.invokeExact(keyBlock, offset / 2 + keyPosition, nativeValue);
            }
            catch (Throwable throwable) {
                throw handleThrowable(throwable);
            }
            if (match) {
                return keyPosition * 2 + 1;
            }
            hash++;
            if (hash == hashTableSize) {
                hash = 0;
            }
        }
    }

    public int seekKeyExact(double nativeValue)
    {
        if (positionCount == 0) {
            return -1;
        }

        long hashCode;
        try {
            hashCode = (long) keyNativeHashCode.invokeExact(nativeValue);
        }
        catch (Throwable throwable) {
            throw handleThrowable(throwable);
        }

        int hashTableOffset = offset / 2 * HASH_MULTIPLIER;
        int hashTableSize = positionCount / 2 * HASH_MULTIPLIER;
        int hash = (int) Math.floorMod(hashCode, hashTableSize);
        while (true) {
            int keyPosition = hashTable[hashTableOffset + hash];
            if (keyPosition == -1) {
                return -1;
            }
            boolean match;
            try {
                match = (boolean) keyBlockNativeEquals.invokeExact(keyBlock, offset / 2 + keyPosition, nativeValue);
            }
            catch (Throwable throwable) {
                throw handleThrowable(throwable);
            }
            if (match) {
                return keyPosition * 2 + 1;
            }
            hash++;
            if (hash == hashTableSize) {
                hash = 0;
            }
        }
    }

    public int seekKeyExact(Slice nativeValue)
    {
        if (positionCount == 0) {
            return -1;
        }

        long hashCode;
        try {
            hashCode = (long) keyNativeHashCode.invokeExact(nativeValue);
        }
        catch (Throwable throwable) {
            throw handleThrowable(throwable);
        }

        int hashTableOffset = offset / 2 * HASH_MULTIPLIER;
        int hashTableSize = positionCount / 2 * HASH_MULTIPLIER;
        int hash = (int) Math.floorMod(hashCode, hashTableSize);
        while (true) {
            int keyPosition = hashTable[hashTableOffset + hash];
            if (keyPosition == -1) {
                return -1;
            }
            boolean match;
            try {
                match = (boolean) keyBlockNativeEquals.invokeExact(keyBlock, offset / 2 + keyPosition, nativeValue);
            }
            catch (Throwable throwable) {
                throw handleThrowable(throwable);
            }
            if (match) {
                return keyPosition * 2 + 1;
            }
            hash++;
            if (hash == hashTableSize) {
                hash = 0;
            }
        }
    }

    public int seekKeyExact(Block nativeValue)
    {
        if (positionCount == 0) {
            return -1;
        }

        long hashCode;
        try {
            hashCode = (long) keyNativeHashCode.invokeExact(nativeValue);
        }
        catch (Throwable throwable) {
            throw handleThrowable(throwable);
        }

        int hashTableOffset = offset / 2 * HASH_MULTIPLIER;
        int hashTableSize = positionCount / 2 * HASH_MULTIPLIER;
        int hash = (int) Math.floorMod(hashCode, hashTableSize);
        while (true) {
            int keyPosition = hashTable[hashTableOffset + hash];
            if (keyPosition == -1) {
                return -1;
            }
            boolean match;
            try {
                match = (boolean) keyBlockNativeEquals.invokeExact(keyBlock, offset / 2 + keyPosition, nativeValue);
            }
            catch (Throwable throwable) {
                throw handleThrowable(throwable);
            }
            if (match) {
                return keyPosition * 2 + 1;
            }
            hash++;
            if (hash == hashTableSize) {
                hash = 0;
            }
        }
    }

    private static RuntimeException handleThrowable(Throwable throwable)
    {
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }
        if (throwable instanceof PrestoException) {
            throw (PrestoException) throwable;
        }
        throw new PrestoException(GENERIC_INTERNAL_ERROR, throwable);
    }
}
