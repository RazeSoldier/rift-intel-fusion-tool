package dev.nohus.rift.bluemarshal

import java.math.BigInteger

/** A value representable by EVE Online's blue.Marshal format. */
sealed interface MarshalValue {
    data object None : MarshalValue
    data class Bool(val value: Boolean) : MarshalValue
    data class Int(val value: kotlin.Long) : MarshalValue
    /** Python 2 `long`, kept separate from [Int] to retain its wire type. */
    data class Long(val value: BigInteger) : MarshalValue
    data class Float(val value: Double) : MarshalValue
    /** A Python 2 byte string. */
    data class Bytes(val value: ByteArray) : MarshalValue {
        override fun equals(other: Any?) = other is Bytes && value.contentEquals(other.value)
        override fun hashCode() = value.contentHashCode()
    }
    /** A Python unicode string. */
    data class String(val value: kotlin.String) : MarshalValue
    data class Tuple(val items: kotlin.collections.List<MarshalValue>) : MarshalValue
    data class List(val items: kotlin.collections.List<MarshalValue>) : MarshalValue
    /** Ordered key/value pairs, preserving marshal stream order. */
    data class Dict(val pairs: kotlin.collections.List<Pair<MarshalValue, MarshalValue>>) : MarshalValue
    data class Global(val name: kotlin.String) : MarshalValue
    data class Instance(val className: kotlin.String, val state: MarshalValue) : MarshalValue
    data class Reduce(
        val newObject: Boolean,
        val callable: MarshalValue,
        val arguments: MarshalValue,
        val state: MarshalValue?,
        val listItems: kotlin.collections.List<MarshalValue>,
        val dictItems: kotlin.collections.List<Pair<MarshalValue, MarshalValue>>,
    ) : MarshalValue
    data class Callback(val value: MarshalValue) : MarshalValue
}

internal fun MarshalValue.stringLike(): kotlin.String? = when (this) {
    is MarshalValue.Bytes -> value.toString(Charsets.UTF_8)
    is MarshalValue.String -> value
    else -> null
}

sealed class MarshalException(message: kotlin.String) : Exception(message) {
    data object EndOfStream : MarshalException("Unexpected end of marshal stream")
    class Invalid(detail: kotlin.String) : MarshalException("Invalid marshal stream: $detail")
    class BadReference(val id: Int) : MarshalException("Invalid TY_REFERENCE id $id")
    data object ChecksumMismatch : MarshalException("Adler-32 checksum mismatch")
    class Unsupported(feature: kotlin.String) : MarshalException("Unsupported marshal feature: $feature")
    data object RecursionLimit : MarshalException("Maximum recursion depth reached")
    class Json(detail: kotlin.String) : MarshalException("Invalid Blue Marshal JSON: $detail")
}

data class DecodedMarshal(val value: MarshalValue, val hadChecksum: Boolean, val consumed: Int)

data class EncodeOptions(val version: Int = 1, val checksum: Boolean = true)
