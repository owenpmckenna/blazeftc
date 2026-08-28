package dev.anygeneric.overplay_backend

import com.google.protobuf.MessageLite
import dev.anygeneric.overplay_backend.proto.Msg
import org.firstinspires.ftc.robotcore.external.Func
import org.firstinspires.ftc.robotcore.external.Telemetry
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.Vector
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock

class TelemetryProxy(val underlying: Telemetry) : Telemetry by underlying {
    val socket = run {
        val sock1 = Socket()
        try {
            sock1.connect(InetSocketAddress("192.168.43.100", 5533), 2000)
        } catch (ioe: IOException) {
            ioe.printStackTrace();
            return@run null
        }
        val startmsg1 = Msg.StartupMessage.newBuilder()
            .setName("OPMODE NAME")
            .build()
        sock1.getOutputStream().write(startmsg1.bytesToPacket())
        sock1
    }
    fun MessageLite.writeBytesToPacket() {
        if (this@TelemetryProxy.socket != null) {
            val bytes = this.bytesToPacket()
            synchronized(this@TelemetryProxy.socket) {
                this@TelemetryProxy.socket.getOutputStream().write(bytes)
            }
        }
    }
    fun MessageLite.bytesToPacket() : ByteArray {
        val packId: Byte = when (this) {
            is Msg.TelemetryValue -> 2
            is Msg.NewTelemetry -> 1
            else -> 0
        }
        val bytes = this.toByteArray()
        val arr = ByteArray(5 + bytes.size)
        val buffer = ByteBuffer.wrap(arr)
        buffer.putInt(bytes.size)
        buffer.put(packId)
        buffer.put(bytes)
        return arr
    }
    ///using this because getIdFromStr is an extremely hot method
    var lock = ReentrantReadWriteLock()
    fun <T> withReadLock(fn: () -> T) : T {
        lock.readLock().lock()
        try {
            return fn()
        } finally {
            lock.readLock().unlock()
        }
    }
    fun <T> withWriteLock(fn: () -> T) : T {
        lock.writeLock().lock()
        try {
            return fn()
        } finally {
            lock.writeLock().unlock()
        }
    }
    val datatypes = mutableMapOf<String, TItem>()
    fun getIdFromStr(caption: String): TItem {
        val maybeItem = withReadLock {
            datatypes[caption]
        }
        if (maybeItem != null) {
            return maybeItem
        }
        class Internal(val send: Boolean, val item: TItem)
        val item = withWriteLock {
            val item = TItem(this, datatypes.size, caption, null)
            val dt = datatypes[caption]
            return@withWriteLock if (dt == null) {
                datatypes[caption] = item
                Internal(true, item)
            } else {
                Internal(false, dt)
            }
        }
        if (socket != null && item.send) {
            synchronized(socket) {
                Msg.NewTelemetry.newBuilder()
                    .setId(item.item.id).setValue(caption)
                    .build()
                    .writeBytesToPacket()
            }
        }
        return item.item
    }
    val currentTele = AtomicReference<Vector<MessageLite>>(Vector(150, 10))

    override fun addData(
        caption: String?,
        format: String?,
        vararg args: Any?
    ): Telemetry.Item {
        underlying.addData(caption, format, *args)
        val item = getIdFromStr(caption!!)
        item.addData(caption, format, *args)
        return item
    }

    override fun addData(
        caption: String?,
        value: Any?
    ): Telemetry.Item {
        underlying.addData(caption, value)
        val item = getIdFromStr(caption!!)
        item.addData(caption, value)
        return item
    }

    override fun update(): Boolean {
        //doing it this way is fine I think.
        //It's unlikely that TItem adds to the queue (like, 1 atomic op) after we update underlying, and consume all previous packets
        //plus, losing a telemetry isn't literally the end of the world.
        val newList = Vector<MessageLite>(150, 10)
        val taken = currentTele.getAndSet(newList)
        val updated = underlying.update()
        if (socket == null)
            return updated
        var bytesLen = 0
        val bytes = mutableListOf<ByteArray>()
        synchronized(taken) {
            taken.forEach {
                val data = it.bytesToPacket()
                bytesLen += data.size
                bytes.add(data)
            }
        }
        val buf = ByteArray(bytesLen)
        val all = ByteBuffer.wrap(buf)
        bytes.forEach { all.put(it) }
        if (bytesLen > 0) {
            synchronized(socket) {
                socket.getOutputStream().write(buf)
            }
        }
        return updated
    }

    class TItem(val proxy: TelemetryProxy, val id: Int, val caption: String, var lastData: Msg.TelemetryValue?) : Telemetry.Item {
        override fun getCaption() = caption

        override fun setCaption(caption: String?): Telemetry.Item? {
            throw NotImplementedError("Don't do setCaption I don't support it")
        }

        override fun setValue(value: Any?): Telemetry.Item = setValueMain(value)
        fun setValueMain(value: Any?): Telemetry.Item {
            val msg = msgFromValue(id, value!!)
            synchronized(this) {
                if (!msg.fastEq(lastData)) {
                    lastData = msg
                    val list = proxy.currentTele.get()
                    synchronized(list) {
                        list.add(msg)
                    }
                }
            }
            return this
        }

        override fun setValue(
            format: String?,
            vararg args: Any?
        ): Telemetry.Item = setValueMain(String.format(format!!, *args))

        override fun <T : Any?> setValue(valueProducer: Func<T?>?): Telemetry.Item = setValueMain(noSupport)

        override fun <T : Any?> setValue(
            format: String?,
            valueProducer: Func<T?>?
        ): Telemetry.Item = setValueMain(noSupport)
        var retained = true
        override fun setRetained(retained: Boolean?): Telemetry.Item {
            this.retained = retained!!
            return this
        }

        override fun isRetained(): Boolean =
            this.retained

        override fun addData(
            caption: String?,
            format: String?,
            vararg args: Any?
        ): Telemetry.Item = proxy.addData(caption, format, *args)

        override fun addData(
            caption: String,
            value: Any?
        ): Telemetry.Item = proxy.addData(caption, value)


        override fun <T : Any?> addData(
            caption: String?,
            valueProducer: Func<T?>?
        ): Telemetry.Item? = proxy.addData(caption, valueProducer)

        override fun <T : Any?> addData(
            caption: String?,
            format: String?,
            valueProducer: Func<T?>?
        ): Telemetry.Item? = proxy.addData(caption, format, valueProducer)
    }
}
fun msgFromValue(id: Int, data: Any) =
    when (data) {
        is String -> msgFromString(id, data)
        is Float -> msgFromDouble(id, data.toDouble())
        is Double -> msgFromDouble(id, data)
        is Int -> msgFromInt(id, data)
        else -> msgFromString(id, data.toString())
    }
fun msgFromString(id: Int, data: String) = Msg.TelemetryValue.newBuilder()
        .setForId(id)
        .setValueStr(data)
        .build()
fun msgFromInt(id: Int, data: Int) = Msg.TelemetryValue.newBuilder()
    .setForId(id)
    .setValueInt(data)
    .build()
fun msgFromDouble(id: Int, data: Double) = Msg.TelemetryValue.newBuilder()
    .setForId(id)
    .setValueDouble(data)
    .build()
fun Msg.TelemetryValue.fastEq(other: Msg.TelemetryValue?) : Boolean {
    if (other == null) {
        return false
    }
    if (this.forId != other.forId) {
        return false
    }
    if (this.hasValueStr() && other.hasValueStr()) {
        if (this.valueStr.contentEquals(other.valueStr)) {
            return true
        }
    }
    if (this.hasValueInt() && other.hasValueInt()) {
        if (this.valueInt == other.valueInt) {
            return true
        }
    }
    if (this.hasValueDouble() && other.hasValueDouble()) {
        if (this.valueDouble == other.valueDouble) {
            return true
        }
    }
    return true
}
private val noSupport = "I don't support value producers"