package eu.pieland.base128

import eu.pieland.base128.Base128.decode
import eu.pieland.base128.Base128.encode
import java.io.FilterOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Transforms the given signed byte to an unsigned integer.
 *
 * This method is necessary since java/kotlin has only signed representations of numbers.
 * So if we need an unsigned version,
 * we cast to a type with a bigger bit size and then mask it with 0b1111_1111 (= 0xFF),
 * thus only retrieving the lower 8 bits.
 *
 * Examples:
 * -    Positive byte transformation
 *
 *      val byte = 0x01      // this means: byte = 1
 *      val unsignedInt = byte.toUnsignedInt()
 *      println(unsignedInt) // prints 1
 * -    Negative byte transformation
 *
 *      val byte = 0xFF      // this means: byte = -1
 *      val unsignedInt = byte.toUnsignedInt()
 *      println(unsignedInt) // prints 255 and not -1
 *
 * @return an `int` of range 0..255.
 */
internal inline fun Byte.toUInt() = this.toInt() and 0xFF

/**
 * Transforms the given signed byte to an unsigned integer.
 *
 * This method is necessary since java/kotlin has only signed representations of numbers.
 * So if we need an unsigned version, we cast to a number of a bigger bit size and the mask it with 0b1111_1111 (= 0xFF), thus only retrieving
 * the lower 8 bits.
 *
 * Examples:
 * -    Positive byte transformation
 *
 *      val byte = 0x01      // this means: byte = 1
 *      val unsignedLong = byte.toUnsignedLong()
 *      println(unsignedInt) // prints 1
 * -    Negative byte transformation
 *
 *      val byte = 0xFF      // this means: byte = -1
 *      val unsignedLong = byte.toUnsignedLong()
 *      println(unsignedInt) // prints 255 and not -1
 *
 * @return an `long` of range 0..255.
 */
internal inline fun Byte.toULong() = this.toLong() and 0xFF

/**
 * Utility class for base128 encoding/decoding.
 *
 * The [encode] / [decode] methods can be used to encode/decode a complete byte array (with an optional offset).
 *
 * The [EncoderOutputStream] / [DecoderInputStream] can be used to encode/decode the contents of a wrapped output/input
 * stream.
 *
 * @author PiePie
 */
object Base128 {

    /**
     * Base128 encodes the data of [source].
     *
     * The optional [sourceOffset] can be used to specify where the to be encoded data in [source] starts.
     * The default is 0.
     *
     * The optional [sourceSize] can be used to specify the size of the to be encoded data.
     * The default size is the complete [source].
     * If [sourceOffset] is specified it is the rest of [source].
     *
     * @param source to be encoded data.
     * @param sourceOffset (optional) to be encoded data offset.
     * @param sourceSize (optional) to be encoded data size.
     *
     * @return the encoded data as a byte array.
     */
    fun encode(source: ByteArray, sourceOffset: Int = 0, sourceSize: Int = source.size - sourceOffset): ByteArray {
        assertValidInput(source, sourceOffset, sourceSize)
        return Base128Encoder.encode(source, sourceOffset, sourceSize)
    }

    fun decode(source: ByteArray, sourceOffset: Int = 0, sourceSize: Int = source.size - sourceOffset): ByteArray {
        assertValidInput(source, sourceOffset, sourceSize)
        return Base128Decoder.decode(source, sourceOffset, sourceSize)
    }

    private fun assertValidInput(source: ByteArray, sourceOffset: Int, sourceSize: Int) {
        require(sourceOffset >= 0) { "Invalid source offset: $sourceOffset < 0" }
        require(sourceSize >= 0) { "Invalid source size: $sourceSize < 0" }
        require(source.size >= Math.addExact(sourceOffset, sourceSize)) {
            "Incompatible source offset and size: ${source.size} < $sourceOffset + $sourceSize"
        }
    }

    /**
     *
     */
    class EncoderOutputStream(outputStream: OutputStream) : FilterOutputStream(outputStream) {

        private val encoder = Base128Encoder(0, 0, byteArrayOf(), 0, byteArrayOf())

        override fun write(source: Int) = encoder.write(out, source)

        override fun write(source: ByteArray, off: Int, len: Int) = encoder.write(out, source, off, len)

        override fun flush() = encoder.flush(out)
    }

    /**
     *
     */
    class DecoderInputStream(private val inputStream: InputStream) : InputStream() {

        private val decoder = Base128InputStreamDecoder(inputStream)

        override fun read() = decoder.read()

        override fun read(b: ByteArray, off: Int, len: Int): Int = decoder.read(b, off, len)

        override fun available(): Int = decoder.available()

        override fun close() = inputStream.close()
    }
}