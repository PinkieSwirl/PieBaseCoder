package eu.pieland.base128

import kotlin.test.Test

class Bas128FixedRegressionTest {

    @Test
    fun `test if the correct number of bytes are available after reads`() {
        Base128PropertyTest().`test if the correct number of bytes are available after reads`(
            byteArrayOf(-1, 0, -1, 0, -1, 0, -1, 0, -1, 0),
            2,
            5
        )
    }

    @Test
    fun `test if the correct number of bytes are available after reads2`() {
        Base128PropertyTest().`test if the correct number of bytes are available after reads2`(
            Triple(
                byteArrayOf(-1, 0, -1, 0, -1, 0, -1, 0, -1, 0),
                intArrayOf(10),
                intArrayOf(1, 8, 1)
            )
        )
    }
}