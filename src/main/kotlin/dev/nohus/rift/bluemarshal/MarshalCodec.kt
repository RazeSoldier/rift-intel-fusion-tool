package dev.nohus.rift.bluemarshal

import java.math.BigInteger
import kotlin.math.min

/** Reads and writes the EVE Online blue.Marshal binary format. */
object BlueMarshal {
    private const val recursionLimit = 1000
    private const val signature = 126
    private const val signature2 = 125
    private const val sharedFlag = 0x40
    private const val typeMask = 0x3f
    private const val none = 1
    private const val global = 2
    private const val int64 = 3
    private const val int32 = 4
    private const val int16 = 5
    private const val int8 = 6
    private const val intN1 = 7
    private const val int0 = 8
    private const val int1 = 9
    private const val float = 10
    private const val float0 = 11
    private const val str = 13
    private const val strEmpty = 14
    private const val strChar = 15
    private const val strShort = 16
    private const val strTable = 17
    private const val unicode = 18
    private const val buffer = 19
    private const val tuple = 20
    private const val list = 21
    private const val dict = 22
    private const val instance = 23
    private const val callback = 25
    private const val pickle = 26
    private const val reference = 27
    private const val crcCheck = 28
    private const val trueValue = 31
    private const val falseValue = 32
    private const val pickler = 33
    private const val reduce = 34
    private const val newObject = 35
    private const val tuple0 = 36
    private const val tuple1 = 37
    private const val list0 = 38
    private const val list1 = 39
    private const val unicode0 = 40
    private const val unicode1 = 41
    private const val dbrow = 42
    private const val wstream = 43
    private const val tuple2 = 44
    private const val mark = 45
    private const val utf8 = 46
    private const val long = 47

    @Throws(MarshalException::class)
    fun decode(bytes: ByteArray): DecodedMarshal = Reader(bytes).decode()

    @Throws(MarshalException::class)
    fun encode(value: MarshalValue, options: EncodeOptions = EncodeOptions()): ByteArray = Writer(options).encode(value)

    private class Reader(private val bytes: ByteArray) {
        private var position = 0
        private var contentEnd = bytes.size
        private var version = 0
        private val shared = mutableListOf<MarshalValue>()
        private var mapping = intArrayOf()
        private var sharedSeen = 0
        private var recursion = 0
        private var gotChecksum = false
        private var checksumPosition = 0
        private var checksumValue = 0

        fun decode(): DecodedMarshal {
            readHeader()
            val value = readObject()
            if (gotChecksum && version > 0) verifyChecksum(position)
            return DecodedMarshal(value, gotChecksum, position)
        }

        private fun remaining() = contentEnd - position
        private fun requireSpace(count: Int) {
            if (count < 0 || count > remaining()) throw MarshalException.EndOfStream
        }
        private fun byte(): Int { requireSpace(1); return bytes[position++].toInt() and 0xff }
        private fun bytes(count: Int): ByteArray { requireSpace(count); return bytes.copyOfRange(position, position + count).also { position += count } }
        private fun int16(): Int { val b = bytes(2); return (b[0].toInt() and 255) or (b[1].toInt() shl 8) }
        private fun int32(): Int { val b = bytes(4); return (b[0].toInt() and 255) or ((b[1].toInt() and 255) shl 8) or ((b[2].toInt() and 255) shl 16) or (b[3].toInt() shl 24) }
        private fun int64(): Long { val b = bytes(8); return java.nio.ByteBuffer.wrap(b).order(java.nio.ByteOrder.LITTLE_ENDIAN).long }
        private fun float64(): Double = Double.fromBits(int64())
        private fun integer(): Int = byte().let { if (it == 255) int32() else it }
        private fun buffer(): ByteArray { val length = integer(); if (length < 0) throw MarshalException.Invalid("negative buffer length"); return bytes(length) }
        private fun peekType(): Int { val type = byte(); position--; return type }

        private fun readHeader() {
            when (byte()) {
                signature -> version = 0
                signature2 -> version = byte()
                else -> throw MarshalException.Invalid("missing marshal signature")
            }
            if (version == 0) {
                val count = int32()
                if (count < 0) throw MarshalException.Invalid("negative shared-object map count")
                val mapLength = count.toLong() * 4
                if (mapLength > bytes.size || mapLength > remaining()) throw MarshalException.Invalid("truncated shared-object map")
                contentEnd = bytes.size - mapLength.toInt()
                mapping = IntArray(count) { index ->
                    val offset = contentEnd + index * 4
                    val value = (bytes[offset].toInt() and 255) or ((bytes[offset + 1].toInt() and 255) shl 8) or ((bytes[offset + 2].toInt() and 255) shl 16) or (bytes[offset + 3].toInt() shl 24)
                    if (value !in 1..count) throw MarshalException.Invalid("bogus shared-object map entry")
                    value
                }
                repeat(count) { shared += MarshalValue.None }
            }
        }

        private fun allocateShared(): Int = if (version == 0) {
            if (sharedSeen >= mapping.size) throw MarshalException.Invalid("shared object table overflow")
            mapping[sharedSeen++] - 1
        } else shared.size.also { shared += MarshalValue.None }
        private fun storeShared(value: MarshalValue): MarshalValue { shared[allocateShared()] = value; return value }
        private fun updateShared(index: Int, value: MarshalValue) { shared[index] = value }
        private fun reference(id: Int): MarshalValue { val index = if (version == 0) id - 1 else id; return shared.getOrNull(index) ?: throw MarshalException.BadReference(id) }
        private fun verifyChecksum(end: Int) {
            if (end < checksumPosition) throw MarshalException.Invalid("invalid checksum range")
            if (adler32(bytes, checksumPosition, min(end, bytes.size)) != checksumValue) throw MarshalException.ChecksumMismatch
        }

        private fun readObject(): MarshalValue {
            if (++recursion > recursionLimit) throw MarshalException.RecursionLimit
            return try { readObjectInner() } finally { recursion-- }
        }

        private fun readObjectInner(): MarshalValue {
            val raw = byte()
            val isShared = raw and sharedFlag != 0
            return when (val type = raw and typeMask) {
                none -> MarshalValue.None
                trueValue -> MarshalValue.Bool(true)
                falseValue -> MarshalValue.Bool(false)
                intN1 -> MarshalValue.Int(-1)
                int0 -> MarshalValue.Int(0)
                int1 -> MarshalValue.Int(1)
                int8 -> MarshalValue.Int(byte().toByte().toLong())
                int16 -> MarshalValue.Int(int16().toShort().toLong())
                int32 -> MarshalValue.Int(int32().toLong())
                int64 -> MarshalValue.Int(int64())
                float0 -> MarshalValue.Float(0.0)
                float -> MarshalValue.Float(float64())
                long -> MarshalValue.Long(signedLittleEndian(buffer())).let { if (isShared) storeShared(it) else it }
                strEmpty -> MarshalValue.Bytes(byteArrayOf())
                strChar -> MarshalValue.Bytes(byteArrayOf(byte().toByte()))
                strShort -> MarshalValue.Bytes(bytes(byte()))
                strTable -> MarshalValue.Bytes(stringTable.getOrNull(byte() - 1)?.toByteArray() ?: throw MarshalException.Invalid("invalid string table index"))
                str, buffer -> MarshalValue.Bytes(buffer()).let { if (isShared) storeShared(it) else it }
                unicode0 -> MarshalValue.String("")
                unicode1 -> MarshalValue.String(String(charArrayOf(int16().toChar())))
                unicode -> {
                    val count = integer(); if (count < 0) throw MarshalException.Invalid("negative unicode length")
                    val b = bytes(Math.multiplyExact(count, 2)); MarshalValue.String(b.toString(Charsets.UTF_16LE))
                }
                utf8 -> { val count = integer(); if (count < 0) throw MarshalException.Invalid("negative utf8 length"); MarshalValue.String(bytes(count).toString(Charsets.UTF_8)) }
                global -> MarshalValue.Global(buffer().toString(Charsets.UTF_8)).let { if (isShared) storeShared(it) else it }
                tuple0 -> MarshalValue.Tuple(emptyList())
                tuple1 -> sharedContainer(isShared) { MarshalValue.Tuple(listOf(readObject())) }
                tuple2 -> sharedContainer(isShared) { MarshalValue.Tuple(listOf(readObject(), readObject())) }
                tuple -> { val count = integer(); checkedCount(count, "tuple"); sharedContainer(isShared) { MarshalValue.Tuple(List(count) { readObject() }) } }
                list0 -> MarshalValue.List(emptyList())
                list1 -> sharedContainer(isShared) { MarshalValue.List(listOf(readObject())) }
                list -> { val count = integer(); checkedCount(count, "list"); sharedContainer(isShared) { MarshalValue.List(List(count) { readObject() }) } }
                dict -> { val count = integer(); if (count < 0) throw MarshalException.Invalid("negative dict length"); requireSpace(count.saturatingTimes(2)); sharedContainer(isShared) { MarshalValue.Dict(List(count) { val value = readObject(); readObject() to value }) } }
                instance -> sharedContainer(isShared) { val className = readObject().stringLike() ?: throw MarshalException.Invalid("instance guid is not a string"); MarshalValue.Instance(className, readObject()) }
                reduce, newObject -> sharedContainer(isShared) { readReduce(type == newObject) }
                callback -> MarshalValue.Callback(readObject())
                reference -> reference(integer())
                crcCheck -> { checksumValue = int32(); checksumPosition = position; gotChecksum = true; if (version == 0) verifyChecksum(bytes.size); readObject() }
                dbrow -> throw MarshalException.Unsupported("TY_DBROW")
                wstream -> throw MarshalException.Unsupported("TY_WSTREAM")
                pickle -> throw MarshalException.Unsupported("TY_PICKLE")
                pickler -> throw MarshalException.Unsupported("TY_PICKLER")
                else -> throw MarshalException.Invalid("unknown type tag $type")
            }
        }

        private fun checkedCount(count: Int, kind: String) { if (count < 0) throw MarshalException.Invalid("negative $kind length"); requireSpace(count) }
        private fun Int.saturatingTimes(other: Int): Int = if (this > Int.MAX_VALUE / other) Int.MAX_VALUE else this * other
        private inline fun sharedContainer(isShared: Boolean, block: () -> MarshalValue): MarshalValue { val slot = if (isShared) allocateShared() else -1; val value = block(); if (slot >= 0) updateShared(slot, value); return value }
        private fun readReduce(isNewObject: Boolean): MarshalValue.Reduce {
            val values = (readObject() as? MarshalValue.Tuple)?.items ?: throw MarshalException.Invalid("reduce payload is not a tuple")
            val callable: MarshalValue
            val args: MarshalValue
            val state: MarshalValue?
            if (isNewObject) { if (values.isEmpty()) throw MarshalException.Invalid("empty newobj tuple"); callable = MarshalValue.None; args = values[0]; state = values.getOrNull(1) }
            else { if (values.size < 2) throw MarshalException.Invalid("reduce tuple too short"); callable = values[0]; args = values[1]; state = values.getOrNull(2) }
            val listItems = mutableListOf<MarshalValue>(); while (peekType() and typeMask != mark) listItems += readObject()
            byte()
            val dictItems = mutableListOf<Pair<MarshalValue, MarshalValue>>(); while (peekType() and typeMask != mark) dictItems += readObject() to readObject()
            byte()
            return MarshalValue.Reduce(isNewObject, callable, args, state, listItems, dictItems)
        }
    }

    private class Writer(private val options: EncodeOptions) {
        private val output = ArrayList<Byte>()
        private var recursion = 0
        fun encode(value: MarshalValue): ByteArray {
            if (options.version !in 0..255) throw MarshalException.Invalid("version must fit in one byte")
            if (options.version == 0) { type(signature); int32(0) } else { type(signature2); type(options.version) }
            val checksumOffset = if (options.checksum) { type(crcCheck); output.size.also { int32(0) } } else null
            write(value)
            if (checksumOffset != null) putInt32(checksumOffset, adler32(output.toByteArray(), checksumOffset + 4, output.size))
            return output.toByteArray()
        }
        private fun type(value: Int) { output += value.toByte() }
        private fun int32(value: Int) { repeat(4) { type(value ushr (it * 8)) } }
        private fun int64(value: Long) { repeat(8) { type((value ushr (it * 8)).toInt()) } }
        private fun integer(value: Int) { if (value in 0..254) type(value) else { type(255); int32(value) } }
        private fun buffer(value: ByteArray) { integer(value.size); output.addAll(value.toList()) }
        private fun putInt32(offset: Int, value: Int) { repeat(4) { output[offset + it] = (value ushr (it * 8)).toByte() } }
        private fun write(value: MarshalValue) { if (++recursion > recursionLimit) throw MarshalException.RecursionLimit; try { writeInner(value) } finally { recursion-- } }
        private fun writeInner(value: MarshalValue) = when (value) {
            MarshalValue.None -> type(none)
            is MarshalValue.Bool -> type(if (value.value) trueValue else falseValue)
            is MarshalValue.Int -> writeInt(value.value)
            is MarshalValue.Long -> { type(long); buffer(littleEndian(value.value)) }
            is MarshalValue.Float -> if (value.value == 0.0) type(float0) else { type(float); int64(value.value.toBits()) }
            is MarshalValue.Bytes -> writeBytes(value.value)
            is MarshalValue.String -> if (value.value.isEmpty()) type(unicode0) else { type(utf8); buffer(value.value.toByteArray()) }
            is MarshalValue.Global -> { type(global); buffer(value.name.toByteArray()) }
            is MarshalValue.Tuple -> writeCollection(value.items, tuple0, tuple1, tuple2, tuple)
            is MarshalValue.List -> writeCollection(value.items, list0, list1, -1, list)
            is MarshalValue.Dict -> { type(dict); integer(value.pairs.size); value.pairs.forEach { (key, item) -> write(item); write(key) } }
            is MarshalValue.Instance -> { type(instance); writeBytes(value.className.toByteArray()); write(value.state) }
            is MarshalValue.Reduce -> writeReduce(value)
            is MarshalValue.Callback -> { type(callback); write(value.value) }
        }
        private fun writeInt(value: Long) = when (value) { -1L -> type(intN1); 0L -> type(int0); 1L -> type(int1); in -128..127 -> { type(int8); type(value.toInt()) }; in -32768..32767 -> { type(int16); int16(value.toInt()) }; in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() -> { type(int32); int32(value.toInt()) }; else -> { type(int64); int64(value) } }
        private fun int16(value: Int) { type(value); type(value ushr 8) }
        private fun writeBytes(value: ByteArray) { when (value.size) { 0 -> type(strEmpty); 1 -> { type(strChar); type(value[0].toInt()) }; else -> { type(buffer); buffer(value) } } }
        private fun writeCollection(items: kotlin.collections.List<MarshalValue>, empty: Int, single: Int, double: Int, many: Int) { when (items.size) { 0 -> type(empty); 1 -> { type(single); write(items[0]) }; 2 -> if (double >= 0) { type(double); write(items[0]); write(items[1]) } else { type(many); integer(2); items.forEach(::write) }; else -> { type(many); integer(items.size); items.forEach(::write) } } }
        private fun writeReduce(value: MarshalValue.Reduce) { type(if (value.newObject) newObject else reduce); val payload = if (value.newObject) listOfNotNull(value.arguments, value.state) else listOfNotNull(value.callable, value.arguments, value.state); write(MarshalValue.Tuple(payload)); value.listItems.forEach(::write); type(mark); value.dictItems.forEach { write(it.first); write(it.second) }; type(mark) }
    }

    private fun signedLittleEndian(bytes: ByteArray): BigInteger = if (bytes.isEmpty()) BigInteger.ZERO else BigInteger(bytes.reversedArray())
    private fun littleEndian(value: BigInteger): ByteArray = if (value == BigInteger.ZERO) byteArrayOf() else value.toByteArray().reversedArray()
    private fun adler32(bytes: ByteArray, start: Int, end: Int): Int { var a = 1; var b = 0; var offset = start; while (offset < end) { val chunkEnd = min(offset + 5552, end); while (offset < chunkEnd) { a += bytes[offset++].toInt() and 255; b += a }; a %= 65521; b %= 65521 }; return (b shl 16) or a }

    private val stringTable = listOf(
        "*corpid", "*locationid", "age", "Asteroid", "authentication", "ballID", "beyonce", "bloodlineID", "capacity", "categoryID", "character", "characterID", "characterName", "characterType", "charID", "chatx", "clientID", "config", "contraband", "corporationDateTime", "corporationID", "createDateTime", "customInfo", "description", "divisionID", "DoDestinyUpdate", "dogmaIM", "EVE System", "flag", "foo.SlimItem", "gangID", "Gemini", "gender", "graphicID", "groupID", "header", "idName", "invbroker", "itemID", "items", "jumps", "line", "lines", "locationID", "locationName", "macho.CallReq", "macho.CallRsp", "macho.MachoAddress", "macho.Notification", "macho.SessionChangeNotification", "modules", "name", "objectCaching", "objectCaching.CachedObject", "OnChatJoin", "OnChatLeave", "OnChatSpeak", "OnGodmaShipEffect", "OnItemChange", "OnModuleAttributeChange", "OnMultiEvent", "orbitID", "ownerID", "ownerName", "quantity", "raceID", "RowClass", "securityStatus", "Sentry Gun", "sessionchange", "singleton", "skillEffect", "squadronID", "typeID", "used", "userID", "util.CachedObject", "util.IndexRowset", "util.Moniker", "util.Row", "util.Rowset", "*multicastID", "AddBalls", "AttackHit3", "AttackHit3R", "AttackHit4R", "DoDestinyUpdates", "GetLocationsEx", "InvalidateCachedObjects", "JoinChannel", "LSC", "LaunchMissile", "LeaveChannel", "OID+", "OID-", "OnAggressionChange", "OnCharGangChange", "OnCharNoLongerInStation", "OnCharNowInStation", "OnDamageMessage", "OnDamageStateChange", "OnEffectHit", "OnGangDamageStateChange", "OnLSC", "OnSpecialFX", "OnTarget", "RemoveBalls", "SendMessage", "SetMaxSpeed", "SetSpeedFraction", "TerminalExplosion", "address", "alert", "allianceID", "allianceid", "bid", "bookmark", "bounty", "channel", "charid", "constellationid", "corpID", "corpid", "corprole", "damage", "duration", "effects.Laser", "gangid", "gangrole", "hqID", "issued", "jit", "languageID", "locationid", "machoVersion", "marketProxy", "minVolume", "orderID", "price", "range", "regionID", "regionid", "role", "rolesAtAll", "rolesAtBase", "rolesAtHQ", "rolesAtOther", "shipid", "sn", "solarSystemID", "solarsystemid", "solarsystemid2", "source", "splash", "stationID", "stationid", "target", "userType", "userid", "volEntered", "volRemaining", "weapon", "agent.missionTemplatizedContent_BasicKillMission", "agent.missionTemplatizedContent_ResearchKillMission", "agent.missionTemplatizedContent_StorylineKillMission", "agent.missionTemplatizedContent_GenericStorylineKillMission", "agent.missionTemplatizedContent_BasicCourierMission", "agent.missionTemplatizedContent_ResearchCourierMission", "agent.missionTemplatizedContent_StorylineCourierMission", "agent.missionTemplatizedContent_GenericStorylineCourierMission", "agent.missionTemplatizedContent_BasicTradeMission", "agent.missionTemplatizedContent_ResearchTradeMission", "agent.missionTemplatizedContent_StorylineTradeMission", "agent.missionTemplatizedContent_GenericStorylineTradeMission", "agent.offerTemplatizedContent_BasicExchangeOffer", "agent.offerTemplatizedContent_BasicExchangeOffer_ContrabandDemand", "agent.offerTemplatizedContent_BasicExchangeOffer_Crafting", "agent.LoyaltyPoints", "agent.ResearchPoints", "agent.Credits", "agent.Item", "agent.Entity", "agent.Objective", "agent.FetchObjective", "agent.EncounterObjective", "agent.DungeonObjective", "agent.TransportObjective", "agent.Reward", "agent.TimeBonusReward", "agent.MissionReferral", "agent.Location", "agent.StandardMissionDetails", "agent.OfferDetails", "agent.ResearchMissionDetails", "agent.StorylineMissionDetails",
    )
}
