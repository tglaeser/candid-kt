<img src="https://github.com/seniorjoinu/candid-kt/blob/master/logo.png?raw=true" align="left" width="100%" >

[![](https://jitci.com/gh/seniorjoinu/candid-kt/svg?style=flat-square)](https://jitci.com/gh/seniorjoinu/candid-kt)
[![Release](https://jitpack.io/v/seniorjoinu/candid-kt.svg?style=flat-square)](https://jitpack.io/#seniorjoinu/candid-kt)
<img src="https://img.shields.io/badge/dfx-0.6.7-blue?style=flat-square"/>
<img src="https://img.shields.io/badge/kotlin-1.3-blue?style=flat-square"/>
<img src="https://img.shields.io/badge/android-26+-blue?style=flat-square"/>

### Candid-kt
Generates client code for your canisters

### Usage

Use [the gradle plugin](https://github.com/seniorjoinu/candid-kt-gradle-plugin) to generate Kotlin code out of candid code.

For example, this candid code
```
type Phone = nat;
type Name = text;
type Entry = 
 record {
   description: text;
   name: Name;
   phone: Phone;
 };
service : {
  insert: (Name, text, Phone) -> ();
  lookup: (Name) -> (opt Entry) query;
}
```
would generate this Kotlin code
```kotlin
typealias Phone = BigInteger

val PhoneValueSer: ValueSer<BigInteger> = NatValueSer

typealias Name = String

val NameValueSer: ValueSer<String> = TextValueSer

data class Entry(
    val name: Name,
    val description: String,
    val phone: Phone
)

object EntryValueSer : ValueSer<Entry> {
    val nameValueSer: ValueSer<Name> = NameValueSer

    val descriptionValueSer: ValueSer<String> = TextValueSer

    val phoneValueSer: ValueSer<Phone> = PhoneValueSer

    override fun calcSizeBytes(value: Entry): Int = this.nameValueSer.calcSizeBytes(value.name) +
            this.descriptionValueSer.calcSizeBytes(value.description) +
            this.phoneValueSer.calcSizeBytes(value.phone)

    override fun ser(buf: ByteBuffer, value: Entry) {
        this.nameValueSer.ser(buf, value.name)
        this.descriptionValueSer.ser(buf, value.description)
        this.phoneValueSer.ser(buf, value.phone)
    }

    override fun deser(buf: ByteBuffer): Entry = Entry(this.nameValueSer.deser(buf),
        this.descriptionValueSer.deser(buf), this.phoneValueSer.deser(buf))

    override fun poetize(): String = Code.of("%T", EntryValueSer::class)
}

typealias PhonebookServiceValueSer = ServiceValueSer

typealias AnonFunc0ValueSer = FuncValueSer

class AnonFunc0(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    suspend operator fun invoke(
        arg0: Name,
        arg1: String,
        arg2: Phone
    ) {
        val arg0ValueSer = NameValueSer
        val arg1ValueSer = senior.joinu.candid.serialize.TextValueSer
        val arg2ValueSer = PhoneValueSer
        val valueSizeBytes = 0 + arg0ValueSer.calcSizeBytes(arg0) + arg1ValueSer.calcSizeBytes(arg1) +
                arg2ValueSer.calcSizeBytes(arg2)
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        arg0ValueSer.ser(sendBuf, arg0)
        arg1ValueSer.ser(sendBuf, arg1)
        arg2ValueSer.ser(sendBuf, arg2)
        val sendBytes = sendBuf.array()

        val receiveBytes = this.service!!.call(this.funcName!!, sendBytes)
        val receiveBuf = ByteBuffer.wrap(receiveBytes)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        receiveBuf.rewind()
        val deserContext = TypeDeser.deserUntilM(receiveBuf)
    }

    companion object {
        val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAADcXF9")
    }
}

typealias AnonFunc1ValueSer = FuncValueSer

class AnonFunc1(
    funcName: String?,
    service: SimpleIDLService?
) : SimpleIDLFunc(funcName, service) {
    suspend operator fun invoke(arg0: Name): Entry? {
        val arg0ValueSer = NameValueSer
        val valueSizeBytes = 0 + arg0ValueSer.calcSizeBytes(arg0)
        val sendBuf = ByteBuffer.allocate(staticPayload.size + valueSizeBytes)
        sendBuf.order(ByteOrder.LITTLE_ENDIAN)
        sendBuf.put(staticPayload)
        arg0ValueSer.ser(sendBuf, arg0)
        val sendBytes = sendBuf.array()

        val receiveBytes = this.service!!.query(this.funcName!!, sendBytes)
        val receiveBuf = ByteBuffer.wrap(receiveBytes)
        receiveBuf.order(ByteOrder.LITTLE_ENDIAN)
        receiveBuf.rewind()
        val deserContext = TypeDeser.deserUntilM(receiveBuf)
        return senior.joinu.candid.serialize.OptValueSer( EntryValueSer ).deser(receiveBuf) as Entry?
    }

    companion object {
        val staticPayload: ByteArray = Base64.getDecoder().decode("RElETAABcQ==")
    }
}

class PhonebookService(
    host: String,
    canisterId: SimpleIDLPrincipal?,
    keyPair: EdDSAKeyPair?,
    apiVersion: String = "v1"
) : SimpleIDLService(host, canisterId, keyPair, apiVersion) {
    val insert: AnonFunc0 = AnonFunc0("insert", this)

    val lookup: AnonFunc1 = AnonFunc1("lookup", this)
}
```
which we then can use to interact with our deployed canister
```kotlin
val host = "http://localhost:8000"
val keys = EdDSAKeyPair.generateInsecure()
val canisterId = "75hes-oqbaa-aaaaa-aaaaa-aaaaa-aaaaa-aaaaa-q"

val phonebook = PhonebookService(host, SimpleIDLPrincipal.fromText(canisterId), keys)

phonebook.insert("test", "test desc", BigInteger("12345"))
val entry = phonebook.lookup("test")

check(entry != null) { "Entry not found" } 
```


### Pros
* Idiomatic Kotlin
* Complete type-safety
* Asynchronous io with coroutines
* Reflectionless single-allocation (de)serialization

### Cons
* Unstable

### Type conversion rules
| IDL | Kotlin |
| --- | --- |
| type T = "existing type" | typealias T = "existing type" |
| int, nat | `BigInteger` |
| int8, nat8 | `Byte` |
| int16, nat16 | `Short` |
| int32, nat32 | `Int` |
| int64, nat64 | `Long` |
| float32 | `Float` |
| float64 | `Double` |
| bool | `Boolean` |
| text | `String` |
| null | `Null` object |
| reserved | `Reserved` object |
| empty | `Empty` object |
| opt T | `T?` |
| vec T | `List<T>` |
| type T = record { a: T1, b: T2 } | `data class T(val a: T1, val b: T2)` |
| type T = variant { A, B: T1 } | `sealed class T { data class A: T(); data class B(val value: T1): T() }` |
| type T = func (T1) -> T2 | `class T { suspend operator fun invoke(arg0: T1): T2 }` |
| type T = service { a: SomeFunc } | `class T { val a: SomeFunc }` |
| principal | `Principal` class |
Unnamed IDL types are transpiled into anonymous Kotlin types.
