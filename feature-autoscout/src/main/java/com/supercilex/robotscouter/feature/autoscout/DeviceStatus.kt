package com.supercilex.robotscouter.feature.autoscout

internal sealed class DeviceStatus(val id: String) {
    class Unknown(id: String) : DeviceStatus(id)

    class Connecting(id: String) : DeviceStatus(id)

    class Connected(id: String) : DeviceStatus(id)

    class Failed(id: String) : DeviceStatus(id)
}
