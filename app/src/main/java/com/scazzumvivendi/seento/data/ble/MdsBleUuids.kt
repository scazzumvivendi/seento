package com.scazzumvivendi.seento.data.ble

import java.util.UUID

/** UUIDs used by the Movesense MDS GATT service. */
internal object MdsBleUuids {
    val SERVICE: UUID = UUID.fromString("61353090-8231-49cc-b57a-886370740041")
    val WRITE_CHARACTERISTIC: UUID = UUID.fromString("17816557-5652-417f-909f-3aee61e5fa85")
    val NOTIFY_CHARACTERISTIC: UUID = UUID.fromString("34802252-7185-4d5d-b431-630e7050e8f0")
    val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}
