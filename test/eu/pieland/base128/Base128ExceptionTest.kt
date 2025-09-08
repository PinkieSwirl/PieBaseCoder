package eu.pieland.base128

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Base128ExceptionTest {
    private val size = 70
    private val expectedBytes = ByteArray(size) { i -> i.toByte() }
    private val encodedBytes = expectedBytes.let {
        val bOut = ByteArrayOutputStream()
        Base128.EncoderOutputStream(bOut).use { it.write(expectedBytes) }
        bOut.toByteArray()
    }

    @Test
    fun `test negative offset while static encoding`() {
        assertFailsWith(IllegalArgumentException::class) { Base128.encode(expectedBytes, -1) }
    }

    @Test
    fun `test correct offset but invalid size while static encoding`() {
        assertFailsWith(IllegalArgumentException::class) { Base128.encode(expectedBytes, expectedBytes.size + 1) }
    }

    @Test
    fun `test correct offset but invalid length while static encoding`() {
        assertFailsWith(IllegalArgumentException::class) { Base128.encode(expectedBytes, 1, expectedBytes.size + 1) }
    }

    @Test
    fun `test negative offset while encoding`() {
        Base128.EncoderOutputStream(ByteArrayOutputStream()).use {
            assertFailsWith(IllegalArgumentException::class) { it.write(expectedBytes, -1, size) }
        }
    }

    @Test
    fun `test negative length while encoding`() {
        Base128.EncoderOutputStream(ByteArrayOutputStream()).use {
            assertFailsWith(IllegalArgumentException::class) { it.write(expectedBytes, 0, -1) }
        }
    }

    @Test
    fun `test correct offset but invalid length while encoding`() {
        Base128.EncoderOutputStream(ByteArrayOutputStream()).use {
            assertFailsWith(IllegalArgumentException::class) { it.write(expectedBytes, 1, size) }
        }
    }

    @Test
    fun `test negative offset while static decoding`() {
        assertFailsWith(IllegalArgumentException::class) { Base128.decode(encodedBytes, -1) }
    }

    @Test
    fun `test correct offset but invalid size while static decoding`() {
        assertFailsWith(IllegalArgumentException::class) { Base128.decode(encodedBytes, encodedBytes.size + 1) }
    }

    @Test
    fun `test correct offset but invalid length while static decoding`() {
        assertFailsWith(IllegalArgumentException::class) { Base128.decode(encodedBytes, 1, encodedBytes.size + 1) }
    }

    @Test
    fun `test negative offset while decoding`() {
        val decodedBytes = ByteArray(size)
        Base128.DecoderInputStream(ByteArrayInputStream(encodedBytes)).use {
            assertFailsWith(IllegalArgumentException::class) { it.read(decodedBytes, -1, size) }
        }
    }

    @Test
    fun `test negative length while decoding`() {
        val decodedBytes = ByteArray(size)
        Base128.DecoderInputStream(ByteArrayInputStream(encodedBytes)).use {
            assertFailsWith(IllegalArgumentException::class) { it.read(decodedBytes, 0, -1) }
        }
    }

    @Test
    fun `test correct offset but invalid length while decoding`() {
        val decodedBytes = ByteArray(size)
        Base128.DecoderInputStream(ByteArrayInputStream(encodedBytes)).use {
            assertFailsWith(IllegalArgumentException::class) { it.read(decodedBytes, 1, size) }
        }
    }

}
