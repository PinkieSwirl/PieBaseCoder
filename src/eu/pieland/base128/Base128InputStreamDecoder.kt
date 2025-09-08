package eu.pieland.base128

import eu.pieland.base128.Base128Encoder.Companion.SRC_ADDITION
import java.io.InputStream
import kotlin.math.min


internal class Base128InputStreamDecoder(private val inputStream: InputStream) : Base128Decoder() {

    private val singleByteBuffer = ByteArray(1)
    private var eof = false

    fun read() = if (read(singleByteBuffer, 0, 1) == -1) -1 else singleByteBuffer[0].toUInt()

    fun read(b: ByteArray, off: Int, len: Int): Int {
        if (eof) return -1
        if (len == 0) return 0
        require(off >= 0) { "Invalid offset: $off < 0" }
        require(len > 0) { "Invalid length: $len < 0" }
        require(b.size >= off + len) { "Invalid array size: ${b.size} < $off + $len" }

        dst = b
        dstOffset = off

        val oneToOneMappings = min((8 - remainder) % 8, len)
        val len2 = len - oneToOneMappings
        val requiredSrcSize = (len2 / 7) * 8 + SRC_ADDITION[len2 % 7] + oneToOneMappings
        src = inputStream.readNBytes(requiredSrcSize)
        var remainingReadBytes = src.size

        if (remainingReadBytes == 0) {
            eof = true
            return -1
        }

        srcOffset = 0
        repeat(oneToOneMappings) {
            dst[dstOffset++] = decodeByte(8 - remainder++ - 1)
            if (--remainingReadBytes == 0) {
                remainder %= 8
                return dstOffset - off
            }
        }

        full8To7Mappings = remainingReadBytes / 8
        remainder = remainingReadBytes % 8

        decode()

        return dstOffset - off
    }

    fun available(): Int {
        return inputStream.available().let { srcSize ->
            val oneToOneMappings = min((8 - remainder) % 8, srcSize)
            val adjustedSrcSize = srcSize - oneToOneMappings
            adjustedSrcSize / 8 * 7 + REMAINDER_MAPPING[adjustedSrcSize % 8] + oneToOneMappings
        }
    }
}
