package com.scazzumvivendi.seento.data.ble

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class WhiteboardSlipCodecTest {

    @Test
    fun encodedFrameCanBeDecodedAcrossBleChunks() {
        val payload = byteArrayOf(0x01, 0x7e, 0x7d, 0x00, 0xff.toByte())
        val encoded = WhiteboardSlipCodec.encode(payload)
        val decoder = WhiteboardSlipCodec.Decoder()

        assertEquals(emptyList<ByteArray>(), decoder.append(encoded.copyOfRange(0, 3)))
        val frames = decoder.append(encoded.copyOfRange(3, encoded.size))

        assertEquals(1, frames.size)
        assertArrayEquals(payload, frames.single())
    }

    @Test
    fun invalidCrcFrameIsIgnored() {
        val encoded = WhiteboardSlipCodec.encode(byteArrayOf(1, 2, 3))
        encoded[2] = (encoded[2].toInt() xor 1).toByte()

        assertEquals(emptyList<ByteArray>(), WhiteboardSlipCodec.Decoder().append(encoded))
    }
}
