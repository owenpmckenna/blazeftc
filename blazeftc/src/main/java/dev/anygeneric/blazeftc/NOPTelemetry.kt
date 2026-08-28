package dev.anygeneric.blazeftc

import org.firstinspires.ftc.robotcore.external.Func
import org.firstinspires.ftc.robotcore.external.Telemetry

class NOPTelemetry : Telemetry {
	override fun addData(
		caption: String?,
		format: String?,
		vararg args: Any?
	): Telemetry.Item {
		return NOPItem()
	}

	override fun addData(
		caption: String?,
		value: Any?
	): Telemetry.Item {
		return NOPItem()
	}

	override fun <T : Any?> addData(
		caption: String?,
		valueProducer: Func<T?>?
	): Telemetry.Item {
		return NOPItem()
	}

	override fun <T : Any?> addData(
		caption: String?,
		format: String?,
		valueProducer: Func<T?>?
	): Telemetry.Item {
		return NOPItem()
	}

	override fun removeItem(item: Telemetry.Item?): Boolean {
		return false
	}

	override fun clear() {}

	override fun clearAll() {}

	override fun addAction(action: Runnable?): Any {
		return Any()
	}

	override fun removeAction(token: Any?): Boolean {
		return false
	}

	override fun speak(text: String?) {}

	override fun speak(
		text: String?,
		languageCode: String?,
		countryCode: String?
	) {}

	override fun update(): Boolean {
		return false
	}

	override fun addLine(): Telemetry.Line {
		return NOPLine()
	}

	override fun addLine(lineCaption: String?): Telemetry.Line {
		return NOPLine()
	}

	override fun removeLine(line: Telemetry.Line?): Boolean {
		return false
	}

	override fun isAutoClear(): Boolean {
		return false
	}

	override fun setAutoClear(autoClear: Boolean) {
	}

	override fun getMsTransmissionInterval(): Int {
		return 250
	}

	override fun setMsTransmissionInterval(msTransmissionInterval: Int) {
	}

	override fun getItemSeparator(): String {
		return ""
	}

	override fun setItemSeparator(itemSeparator: String?) {
	}

	override fun getCaptionValueSeparator(): String {
		return ""
	}

	override fun setCaptionValueSeparator(captionValueSeparator: String?) {
	}

	override fun setDisplayFormat(displayFormat: Telemetry.DisplayFormat?) {
	}

	override fun log(): Telemetry.Log {
		return NOPLog()
	}
	class NOPItem : Telemetry.Item {
		override fun getCaption(): String {
			return ""
		}

		override fun setCaption(caption: String?): Telemetry.Item {
			return this
		}

		override fun setValue(
			format: String?,
			vararg args: Any?
		): Telemetry.Item {
			return this
		}

		override fun setValue(value: Any?): Telemetry.Item {
			return this
		}

		override fun <T : Any?> setValue(valueProducer: Func<T?>?): Telemetry.Item {
			return this
		}

		override fun <T : Any?> setValue(
			format: String?,
			valueProducer: Func<T?>?
		): Telemetry.Item {
			return this
		}

		override fun setRetained(retained: Boolean?): Telemetry.Item {
			return this
		}

		override fun isRetained(): Boolean {
			return false
		}

		override fun addData(
			caption: String?,
			format: String?,
			vararg args: Any?
		): Telemetry.Item {
			return this
		}

		override fun addData(
			caption: String?,
			value: Any?
		): Telemetry.Item {
			return this
		}

		override fun <T : Any?> addData(
			caption: String?,
			valueProducer: Func<T?>?
		): Telemetry.Item {
			return this
		}

		override fun <T : Any?> addData(
			caption: String?,
			format: String?,
			valueProducer: Func<T?>?
		): Telemetry.Item {
			return this
		}
	}
	class NOPLine : Telemetry.Line {
		override fun addData(
			caption: String?,
			format: String?,
			vararg args: Any?
		): Telemetry.Item {
			return NOPItem()
		}

		override fun addData(
			caption: String?,
			value: Any?
		): Telemetry.Item {
			return NOPItem()
		}

		override fun <T : Any?> addData(
			caption: String?,
			valueProducer: Func<T?>?
		): Telemetry.Item {
			return NOPItem()
		}

		override fun <T : Any?> addData(
			caption: String?,
			format: String?,
			valueProducer: Func<T?>?
		): Telemetry.Item {
			return NOPItem()
		}
	}
	class NOPLog : Telemetry.Log {
		override fun getCapacity(): Int {
			return 500
		}

		override fun setCapacity(capacity: Int) {
		}

		override fun getDisplayOrder(): Telemetry.Log.DisplayOrder {
			return Telemetry.Log.DisplayOrder.NEWEST_FIRST
		}

		override fun setDisplayOrder(displayOrder: Telemetry.Log.DisplayOrder?) {
		}
		override fun add(entry: String?) {
		}
		override fun add(format: String?, vararg args: Any?) {
		}
		override fun clear() {
		}
	}
}