package eu.pieland.base128

import java.io.OutputStream
import eu.pieland.base128.Base128Decoder.Companion.DECODER

internal class Base128Encoder(
    private var full7To8Mappings: Int,
    private var remainder: Int,
    private var src: ByteArray,
    private var srcOffset: Int,
    private var dst: ByteArray,
) {

    private var dstOffset = 0
    private val singleByteBuffer = ByteArray(1)
    private var remainders = IntArray(7)

    private var bits = 0L

     private fun encodeByte(index: Int, last: Boolean): Byte {
        if (index == 7) {
            bits = (src[srcOffset++].toULong() shl (8 * (index - 1)))
        } else if (!last) {
            bits = bits or (src[srcOffset++].toULong() shl (8 * (index - 1)))
        }

        return ENCODER[((bits ushr (7 * index)) and 0x7F).toInt()].toByte()
    }

    fun encode(): ByteArray {
        repeat(full7To8Mappings) { repeat(8) { dst[dstOffset++] = encodeByte(7 - it, it == 7) } }
        if (remainder > 0) {
            repeat(remainder + 1) { dst[dstOffset++] = encodeByte(7 - it, it == remainder) }
        }
        return dst
    }

    fun write(out: OutputStream, source: Int) =
        write(out, singleByteBuffer.apply { this[0] = (source and 0xFF).toByte() }, 0, 1)

    fun write(out: OutputStream, source: ByteArray, off: Int, len: Int) {
        if (len == 0) return
        require(off >= 0) { "Invalid source offset: $off < 0" }
        require(len > 0) { "Invalid source size: $len < 0" }
        require(source.size >= Math.addExact(off, len)) {
            "Incompatible source offset and size: ${source.size} < $off + $len"
        }

        var offset = off
        var length = len

        if (remainder != 0) {
            while (remainder != 6) {
                remainders[remainder++] = source[offset++].toUInt()
                length--
                if (length == 0) return
            }
            remainders[6] = source[offset++].toUInt()
            length--

            out.write(ENCODER[(remainders[0] shr 1) /*                             */])
            out.write(ENCODER[(remainders[1] shr 2) or (remainders[0] shl 6 and 0x7F)])
            out.write(ENCODER[(remainders[2] shr 3) or (remainders[1] shl 5 and 0x7F)])
            out.write(ENCODER[(remainders[3] shr 4) or (remainders[2] shl 4 and 0x7F)])
            out.write(ENCODER[(remainders[4] shr 5) or (remainders[3] shl 3 and 0x7F)])
            out.write(ENCODER[(remainders[5] shr 6) or (remainders[4] shl 2 and 0x7F)])
            out.write(ENCODER[(remainders[6] shr 7) or (remainders[5] shl 1 and 0x7F)])
            out.write(ENCODER[/*                    */ (remainders[6] shl 0 and 0x7F)])
        }

        repeat(length / 7) {
            val bits = (source[offset++].toULong() shl 0x30) or
                    (source[offset++].toULong() shl 0x28) or
                    (source[offset++].toULong() shl 0x20) or
                    (source[offset++].toULong() shl 0x18) or
                    (source[offset++].toULong() shl 0x10) or
                    (source[offset++].toULong() shl 0x08) or
                    (source[offset++].toULong() shl 0x00)

            out.write(ENCODER[((bits ushr 0x31) and 0x7F).toInt()])
            out.write(ENCODER[((bits ushr 0x2A) and 0x7F).toInt()])
            out.write(ENCODER[((bits ushr 0x23) and 0x7F).toInt()])
            out.write(ENCODER[((bits ushr 0x1C) and 0x7F).toInt()])
            out.write(ENCODER[((bits ushr 0x15) and 0x7F).toInt()])
            out.write(ENCODER[((bits ushr 0x0E) and 0x7F).toInt()])
            out.write(ENCODER[((bits ushr 0x07) and 0x7F).toInt()])
            out.write(ENCODER[((bits /*     */) and 0x7F).toInt()])
        }

        remainder = length % 7
        when (remainder) {
            1 -> {
                remainders[0] = source[offset].toUInt()
            }

            2 -> {
                remainders[0] = source[offset++].toUInt()
                remainders[1] = source[offset].toUInt()
            }

            3 -> {
                remainders[0] = source[offset++].toUInt()
                remainders[1] = source[offset++].toUInt()
                remainders[2] = source[offset].toUInt()
            }

            4 -> {
                remainders[0] = source[offset++].toUInt()
                remainders[1] = source[offset++].toUInt()
                remainders[2] = source[offset++].toUInt()
                remainders[3] = source[offset].toUInt()
            }

            5 -> {
                remainders[0] = source[offset++].toUInt()
                remainders[1] = source[offset++].toUInt()
                remainders[2] = source[offset++].toUInt()
                remainders[3] = source[offset++].toUInt()
                remainders[4] = source[offset].toUInt()
            }

            6 -> {
                remainders[0] = source[offset++].toUInt()
                remainders[1] = source[offset++].toUInt()
                remainders[2] = source[offset++].toUInt()
                remainders[3] = source[offset++].toUInt()
                remainders[4] = source[offset++].toUInt()
                remainders[5] = source[offset].toUInt()
            }
        }
    }

    fun flush(out: OutputStream) {
        when (remainder) {
            1 -> {
                out.write(ENCODER[(remainders[0] shr 1) /*                             */])
                out.write(ENCODER[/*                    */ (remainders[0] shl 6 and 0x7F)])
            }

            2 -> {
                out.write(ENCODER[(remainders[0] shr 1) /*                             */])
                out.write(ENCODER[(remainders[1] shr 2) or (remainders[0] shl 6 and 0x7F)])
                out.write(ENCODER[/*                    */ (remainders[1] shl 5 and 0x7F)])
            }

            3 -> {
                out.write(ENCODER[(remainders[0] shr 1) /*                             */])
                out.write(ENCODER[(remainders[1] shr 2) or (remainders[0] shl 6 and 0x7F)])
                out.write(ENCODER[(remainders[2] shr 3) or (remainders[1] shl 5 and 0x7F)])
                out.write(ENCODER[/*                    */ (remainders[2] shl 4 and 0x7F)])
            }

            4 -> {
                out.write(ENCODER[(remainders[0] shr 1) /*                             */])
                out.write(ENCODER[(remainders[1] shr 2) or (remainders[0] shl 6 and 0x7F)])
                out.write(ENCODER[(remainders[2] shr 3) or (remainders[1] shl 5 and 0x7F)])
                out.write(ENCODER[(remainders[3] shr 4) or (remainders[2] shl 4 and 0x7F)])
                out.write(ENCODER[/*                    */ (remainders[3] shl 3 and 0x7F)])
            }

            5 -> {
                out.write(ENCODER[(remainders[0] shr 1) /*                             */])
                out.write(ENCODER[(remainders[1] shr 2) or (remainders[0] shl 6 and 0x7F)])
                out.write(ENCODER[(remainders[2] shr 3) or (remainders[1] shl 5 and 0x7F)])
                out.write(ENCODER[(remainders[3] shr 4) or (remainders[2] shl 4 and 0x7F)])
                out.write(ENCODER[(remainders[4] shr 5) or (remainders[3] shl 3 and 0x7F)])
                out.write(ENCODER[/*                    */ (remainders[4] shl 2 and 0x7F)])
            }

            6 -> {
                out.write(ENCODER[(remainders[0] shr 1) /*                             */])
                out.write(ENCODER[(remainders[1] shr 2) or (remainders[0] shl 6 and 0x7F)])
                out.write(ENCODER[(remainders[2] shr 3) or (remainders[1] shl 5 and 0x7F)])
                out.write(ENCODER[(remainders[3] shr 4) or (remainders[2] shl 4 and 0x7F)])
                out.write(ENCODER[(remainders[4] shr 5) or (remainders[3] shl 3 and 0x7F)])
                out.write(ENCODER[(remainders[5] shr 6) or (remainders[4] shl 2 and 0x7F)])
                out.write(ENCODER[/*                    */ (remainders[5] shl 1 and 0x7F)])
            }
        }
        remainder = 0
        out.flush()
    }

    companion object {
        /**
         * This array is used to perform encoding of arbitrary bytes to a set of characters that can be represented by a maximum
         * of 8 bits.
         *
         * Since there is no "base 128"-standard, the characters can be assigned freely, as long as they hold the restriction.
         * Note that if you change these values you also need to change the [DECODER] array.
         */
        @Suppress("MagicNumber")
        internal val ENCODER: IntArray = intArrayOf(
            0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66,
            0x67, 0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F, 0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76,
            0x77, 0x78, 0x79, 0x7A, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C,
            0x4D, 0x4E, 0x4F, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x2E, 0x2D,
            0x3A, 0x2B, 0x3D, 0x5E, 0x21, 0x2F, 0x2A, 0x3F, 0x26, 0x3C, 0x3E, 0x28, 0x29, 0x5B, 0x5D, 0x7B,
            0x7D, 0x40, 0x25, 0x24, 0x23, 0x2C, 0x3B, 0x5F, 0x60, 0x7C, 0x7E, 0xC0, 0xC1, 0xC2, 0xC3, 0xC4,
            0xC5, 0xC6, 0xC7, 0xC8, 0xC9, 0xCA, 0xCB, 0xCC, 0xCD, 0xCE, 0xCF, 0xD0, 0xD1, 0xD2, 0xD3, 0xD4,
            0xD5, 0xD6, 0xD7, 0xD8, 0xD9, 0xDA, 0xDB, 0xDC, 0xDD, 0xDE, 0xDF, 0xE0, 0xE1, 0xE2, 0xE3, 0xE4,
        )

        @Suppress("MagicNumber")
        internal val SRC_ADDITION = intArrayOf(0, 2, 3, 4, 5, 6, 7)

        fun encode(source: ByteArray, sourceOffset: Int, sourceSize: Int): ByteArray {
            if (sourceSize == 0) return byteArrayOf()

            val numberOfFullFits = sourceSize / 7
            val remainder = sourceSize % 7
            val destinationSize = numberOfFullFits * 8 + SRC_ADDITION[remainder]

            return Base128Encoder(
                numberOfFullFits,
                remainder,
                source,
                sourceOffset,
                ByteArray(destinationSize)
            ).encode()
        }
    }
}