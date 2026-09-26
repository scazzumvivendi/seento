package com.scazzumvivendi.seento.data.ble

/** SLIP framing used by the watch's Whiteboard BLE service. */
internal object WhiteboardSlipCodec {
    private const val BOUNDARY: Byte = 0x7e
    private const val ESCAPE: Byte = 0x7d

    private val crcTable = longArrayOf(
        1304293916L, 1342890616L, 1993593556L, 1801765552L,
        996258700L, 651600872L, 1020740L, 498631456L,
        2684711228L, 3182584152L, 2607501812L, 2262581648L,
        3604557996L, 3412992200L, 3988197476L, 4026531840L
    )

    fun encode(payload: ByteArray): ByteArray {
        val framed = ArrayList<Byte>(payload.size + 10)
        framed += BOUNDARY
        appendEscaped(framed, payload)
        val crc = crc32(payload, payload.size)
        appendEscaped(
            framed,
            byteArrayOf(
                crc.toByte(),
                (crc shr 8).toByte(),
                (crc shr 16).toByte(),
                (crc shr 24).toByte()
            )
        )
        framed += BOUNDARY
        return ByteArray(framed.size) { framed[it] }
    }

    class Decoder {
        private val buffer = ArrayList<Byte>()

        @Synchronized
        fun append(chunk: ByteArray): List<ByteArray> {
            chunk.forEach(buffer::add)
            val decoded = mutableListOf<ByteArray>()
            while (true) {
                val start = buffer.indexOf(BOUNDARY)
                if (start < 0) {
                    buffer.clear()
                    break
                }
                if (start > 0) buffer.subList(0, start).clear()

                val end = (1 until buffer.size).firstOrNull { index ->
                    buffer[index] == BOUNDARY && buffer[index - 1] != ESCAPE
                } ?: break
                val frame = buffer.subList(0, end + 1).toList()
                buffer.subList(0, end + 1).clear()
                decodeFrame(frame)?.let(decoded::add)
            }
            return decoded
        }

        private fun decodeFrame(frame: List<Byte>): ByteArray? {
            if (frame.size < 7 || frame.first() != BOUNDARY || frame.last() != BOUNDARY) {
                return null
            }
            val content = ArrayList<Byte>(frame.size)
            var escaped = false
            for (index in 1 until frame.lastIndex) {
                val byte = frame[index]
                if (escaped) {
                    if (byte == BOUNDARY || byte == ESCAPE) return null
                    content += (byte.toInt() xor 0x20).toByte()
                    escaped = false
                } else if (byte == ESCAPE) {
                    escaped = true
                } else {
                    content += byte
                }
            }
            if (escaped || content.size <= 4) return null

            val payloadSize = content.size - 4
            val payloadAndCrc = ByteArray(content.size) { content[it] }
            val expected = (payloadAndCrc[payloadSize].toLong() and 0xff) or
                ((payloadAndCrc[payloadSize + 1].toLong() and 0xff) shl 8) or
                ((payloadAndCrc[payloadSize + 2].toLong() and 0xff) shl 16) or
                ((payloadAndCrc[payloadSize + 3].toLong() and 0xff) shl 24)
            if (crc32(payloadAndCrc, payloadSize) != expected) return null
            return payloadAndCrc.copyOf(payloadSize)
        }
    }

    private fun appendEscaped(output: MutableList<Byte>, bytes: ByteArray) {
        bytes.forEach { byte ->
            if (byte == BOUNDARY || byte == ESCAPE) {
                output += ESCAPE
                output += (byte.toInt() xor 0x20).toByte()
            } else {
                output += byte
            }
        }
    }

    private fun crc32(data: ByteArray, length: Int): Long {
        var crc = 0L
        for (index in 0 until length) {
            val byte = data[index].toLong()
            val low = (crcTable[((crc xor byte) and 0x0f).toInt()] xor (crc shr 4)) and 0xffffffffL
            crc = (crcTable[((low xor (byte shr 4)) and 0x0f).toInt()] xor (low shr 4)) and 0xffffffffL
        }
        return crc
    }
}
