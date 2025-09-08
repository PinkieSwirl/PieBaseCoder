package eu.pieland.base128

import net.jqwik.api.*
import net.jqwik.api.arbitraries.ArrayArbitrary
import net.jqwik.api.arbitraries.IntegerArbitrary
import net.jqwik.kotlin.api.*
import org.junit.jupiter.api.assertAll
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals


class Base128PropertyTest {

    @Provide
    fun expectedBytes(): ArrayArbitrary<Byte, ByteArray> {
        return Byte.any().array(ByteArray::class.java).ofSize(0..10_000)
    }

    @Provide
    fun ints(): IntegerArbitrary {
        return Int.any().between(0, 10_000)
    }

    @Provide
    fun expectedBytesWithReads(): Arbitrary<Triple<ByteArray, IntArray, IntArray>> {
        val expectedWrites = Int.any(0..99).array<Int, IntArray>().ofSize(0..20)
        val filter = anyPair(expectedWrites, expectedWrites).filter { it.first.sum() == it.second.sum() }
        return filter.flatMap {
            anyTriple(
                Byte.any().array(ByteArray::class.java).ofSize(it.first.sum()),
                Arbitraries.just(it.first),
                Arbitraries.just(it.second)
            )
        }
    }

    @Property(tries = 1000)
    fun `encode then decode`(@ForAll("expectedBytes") expectedBytes: ByteArray) {
        assertContentEquals(expectedBytes, Base128.decode(Base128.encode(expectedBytes)))
    }

    @Property(tries = 1000)
    fun `encode with source-offset then decode`(
        @ForAll("expectedBytes") expectedBytes: ByteArray, @ForAll("ints") offset: Int,
    ) {
        val toBeEncodedBytes =
            ByteArray(expectedBytes.size + offset) { index -> if (index < offset) 0 else expectedBytes[index - offset] }

        assertContentEquals(expectedBytes, Base128.decode(Base128.encode(toBeEncodedBytes, sourceOffset = offset)))
    }

    @Property(tries = 1000)
    fun `encode then decode with source-offset`(
        @ForAll("expectedBytes") expectedBytes: ByteArray, @ForAll("ints") offset: Int,
    ) {
        val encodedBytes = Base128.encode(expectedBytes)
        val toBeDecodedBytes =
            ByteArray(encodedBytes.size + offset) { index -> if (index < offset) 0 else encodedBytes[index - offset] }
        val decodedBytes = Base128.decode(toBeDecodedBytes, sourceOffset = offset)

        assertContentEquals(expectedBytes, decodedBytes)
    }

    @Property(tries = 1000)
    fun `encode with source-size then decode`(
        @ForAll("expectedBytes") expectedBytes: ByteArray, @ForAll("ints") size: Int,
    ) {
        val toBeEncodedBytes = ByteArray(expectedBytes.size + size) { index ->
            if (index >= expectedBytes.size) 0xFF.toByte() else expectedBytes[index]
        }
        val encodedBytes = Base128.encode(toBeEncodedBytes, sourceSize = expectedBytes.size)
        val decodedBytes = Base128.decode(encodedBytes)

        assertContentEquals(expectedBytes, decodedBytes)
    }

    @Property(tries = 1000)
    fun `encode then decode with source-size`(
        @ForAll("expectedBytes") expectedBytes: ByteArray, @ForAll("ints") size: Int,
    ) {
        val encodedBytes = Base128.encode(expectedBytes)
        val toBeDecodedBytes = ByteArray(encodedBytes.size + size) { index ->
            if (index >= encodedBytes.size) 0xFF.toByte() else encodedBytes[index]
        }
        val decodedBytes = Base128.decode(toBeDecodedBytes, sourceSize = encodedBytes.size)

        assertContentEquals(expectedBytes, decodedBytes)
    }

    @Property(tries = 1000)
    fun `encode with source-offset and source-size then decode`(
        @ForAll("expectedBytes") expectedBytes: ByteArray, @ForAll("ints") offset: Int, @ForAll("ints") size: Int,
    ) {
        val toBeEncodedBytes = ByteArray(expectedBytes.size + offset + size) { x ->
            when {
                x < offset -> 0xFF.toByte()
                x >= expectedBytes.size + offset -> 0xFF.toByte()
                else -> expectedBytes[x - offset]
            }
        }
        val encodedBytes = Base128.encode(toBeEncodedBytes, sourceOffset = offset, sourceSize = expectedBytes.size)
        val decodedBytes = Base128.decode(encodedBytes)

        assertContentEquals(expectedBytes, decodedBytes)
    }

    @Property(tries = 1000)
    fun `encode then decode with source-offset and source-size`(
        @ForAll("expectedBytes") expectedBytes: ByteArray, @ForAll("ints") offset: Int, @ForAll("ints") size: Int,
    ) {
        val encodedBytes = Base128.encode(expectedBytes)
        val toBeDecodedBytes = ByteArray(encodedBytes.size + offset + size) { x ->
            when {
                x < offset -> 0xFF.toByte()
                x >= encodedBytes.size + offset -> 0xFF.toByte()
                else -> encodedBytes[x - offset]
            }
        }
        val decodedBytes = Base128.decode(toBeDecodedBytes, offset, encodedBytes.size)

        assertContentEquals(expectedBytes, decodedBytes)
    }

    @Property(tries = 1000)
    fun `encode then decode in single byte steps`(@ForAll("expectedBytes") expectedBytes: ByteArray) {
        val bOut = ByteArrayOutputStream(expectedBytes.size + 1)
        Base128.EncoderOutputStream(bOut).use { for (x in expectedBytes.indices) it.write(expectedBytes[x].toInt()) }
        val encodedBytes = bOut.toByteArray()

        var read = 0
        val decodedBytes = Base128.DecoderInputStream(ByteArrayInputStream(encodedBytes)).use {
            buildList(expectedBytes.size) {
                var decodedByte = it.read()
                while (decodedByte != -1) {
                    read++
                    add(decodedByte.toByte())
                    decodedByte = it.read()
                }
            }.toByteArray()
        }

        assertAll({ assertEquals(expectedBytes.size, read) }, { assertContentEquals(expectedBytes, decodedBytes) })
    }

    @Property(tries = 10_000)
    fun `test if the correct number of bytes are available before one read`(@ForAll("expectedBytes") expectedBytes: ByteArray) {
        val bOut = ByteArrayOutputStream()
        Base128.EncoderOutputStream(bOut).use { it.write(expectedBytes) }
        val encodedBytes = bOut.toByteArray()

        val decodedBytes = ByteArray(expectedBytes.size)
        val available = Base128.DecoderInputStream(ByteArrayInputStream(encodedBytes)).use { reader ->
            reader.available().also { reader.read(decodedBytes) }
        }

        assertEquals(expectedBytes.size, available)
    }

    @Property(tries = 1000)
    fun `test if the correct number of bytes are available after one read`(
        @ForAll("expectedBytes") expectedBytes: ByteArray,
    ) {
        val bOut = ByteArrayOutputStream()
        Base128.EncoderOutputStream(bOut).use { it.write(expectedBytes) }
        val encodedBytes = bOut.toByteArray()

        val decodedBytes = ByteArray(expectedBytes.size)
        val available = Base128.DecoderInputStream(ByteArrayInputStream(encodedBytes)).use { reader ->
            reader.read(decodedBytes)
            reader.available()
        }

        assertEquals(0, available)
    }

    @Property(tries = 10_000)
    fun `test if the correct number of bytes are available after single byte reads`(
        @ForAll("expectedBytes") expectedBytes: ByteArray, @ForAll("ints") numberOfReads: Int,
    ) {
        val bOut = ByteArrayOutputStream()
        Base128.EncoderOutputStream(bOut).use { it.write(expectedBytes) }
        val encodedBytes = bOut.toByteArray()

        val available = Base128.DecoderInputStream(ByteArrayInputStream(encodedBytes)).use { reader ->
            repeat(numberOfReads) { reader.read() }
            reader.available()
        }

        assertEquals(max(expectedBytes.size - numberOfReads, 0), available)
    }

    @Property(tries = 10_000)
    fun `test if the correct number of bytes are available after reads`(
        @ForAll("expectedBytes") expectedBytes: ByteArray,
        @ForAll("ints") numberOfReads: Int,
        @ForAll("ints") sizeOfReads: Int,
    ) {
        val bOut = ByteArrayOutputStream()
        Base128.EncoderOutputStream(bOut).use { it.write(expectedBytes) }
        val encodedBytes = bOut.toByteArray()

        val available = Base128.DecoderInputStream(ByteArrayInputStream(encodedBytes)).use { reader ->
            repeat(numberOfReads) { reader.read(ByteArray(sizeOfReads)) }
            reader.available()
        }

        assertEquals(max(expectedBytes.size - (sizeOfReads * numberOfReads), 0), available)
    }

    @Property(tries = 10_000)
    fun `test if the correct number of bytes are available after reads2`(
        @ForAll("expectedBytesWithReads") expectedBytesWithReads: Triple<ByteArray, IntArray, IntArray>,
    ) {
        val (expectedBytes, expectedWrites, expectedReads) = expectedBytesWithReads;
        val bOut = ByteArrayOutputStream()
        var index = 0
        Base128.EncoderOutputStream(bOut).use { writer ->
            repeat(expectedWrites.size) {
                writer.write(expectedBytes, index, expectedWrites[it])
                index += expectedWrites[it]
            }
        }
        val encodedBytes = bOut.toByteArray()

        val available = Base128.DecoderInputStream(ByteArrayInputStream(encodedBytes)).use { reader ->
            repeat(expectedReads.size) { reader.read(ByteArray(expectedReads[it])) }
            reader.available()
        }

        assertEquals(0, available)
    }
}