package dev.anygeneric.blazeftc

import org.firstinspires.ftc.robotcore.external.Func
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.robotcore.internal.opmode.TelemetryInternal

class CachedTelemetry(val bt: BlazeFTC.BlazeTelemetry) : Telemetry, TelemetryInternal {
    class Data(val name: String, val removedOnClear: Boolean, val runner: (Telemetry) -> Unit)
    var data = mutableMapOf<String, Data>()
    fun put(caption: String, removedOnClear: Boolean, runner: (Telemetry) -> Unit) {
        data[caption] = Data(caption, removedOnClear, runner)
    }
    override fun addData(
        caption: String?,
        format: String?,
        vararg args: Any?
    ): Telemetry.Item? {
        put(caption!!, true) {
            it.addData(caption, String.format(format!!, args))
        }
        return null
    }

    override fun addData(
        caption: String?,
        value: Any?
    ): Telemetry.Item? {
        put(caption!!, true) {
            it.addData(caption, value)
        }
        return null
    }

    override fun <T : Any?> addData(
        caption: String?,
        valueProducer: Func<T?>?
    ): Telemetry.Item? {
        put(caption!!, false) {
            it.addData(caption, valueProducer!!.value())
        }
        return null
    }

    override fun <T : Any?> addData(
        caption: String?,
        format: String?,
        valueProducer: Func<T?>?
    ): Telemetry.Item? {
        put(caption!!, false) {
            it.addData(caption, String.format(format!!, valueProducer!!.value()))
        }
        return null
    }

    override fun removeItem(item: Telemetry.Item?): Boolean {
        TODO("Not yet implemented")
    }

    override fun clear() {
        data = data.filter { !it.value.removedOnClear }.toMutableMap()
    }

    override fun clearAll() {
        data.clear()
    }

    override fun addAction(action: Runnable?): Any? {
        return bt.telemetry.addAction(action)
    }

    override fun removeAction(token: Any?): Boolean {
        return bt.telemetry.removeAction(token)
    }

    override fun speak(text: String?) {
        bt.telemetry.speak(text)
    }

    override fun speak(
        text: String?,
        languageCode: String?,
        countryCode: String?
    ) {
        bt.telemetry.speak(text, languageCode, countryCode)
    }

    override fun update(): Boolean {
        return false
    }

    override fun addLine(): Telemetry.Line? {
        return bt.telemetry.addLine()
    }

    override fun addLine(lineCaption: String?): Telemetry.Line? {
        return bt.telemetry.addLine(lineCaption)
    }

    override fun removeLine(line: Telemetry.Line?): Boolean {
        return bt.telemetry.removeLine(line)
    }

    override fun isAutoClear(): Boolean {
        return true
    }

    override fun setAutoClear(autoClear: Boolean) {
        if (autoClear != isAutoClear)
            TODO("Not yet implemented")
    }

    override fun getMsTransmissionInterval(): Int {
        return bt.telemetry.msTransmissionInterval
    }

    override fun setMsTransmissionInterval(msTransmissionInterval: Int) {
        bt.telemetry.msTransmissionInterval = msTransmissionInterval
    }

    override fun getItemSeparator(): String? = bt.telemetry.itemSeparator

    override fun setItemSeparator(itemSeparator: String?) {
        bt.telemetry.itemSeparator = itemSeparator
    }

    override fun getCaptionValueSeparator(): String? {
        return bt.telemetry.captionValueSeparator
    }

    override fun setCaptionValueSeparator(captionValueSeparator: String?) {
        bt.telemetry.captionValueSeparator = captionValueSeparator
    }

    override fun setDisplayFormat(displayFormat: Telemetry.DisplayFormat?) {
        bt.telemetry.setDisplayFormat(displayFormat)
    }

    override fun log(): Telemetry.Log? = bt.telemetry.log()

    fun updateToTelemetry(telemetry: Telemetry) {
        for (i in data.entries) {
            i.value.runner(telemetry)
        }
    }

    override fun tryUpdateIfDirty(): Boolean {
        /*Don't do anything*/
        return true
    }

    override fun resetTelemetryForOpMode() {
        clearAll()
    }
}