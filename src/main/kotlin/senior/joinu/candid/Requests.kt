package senior.joinu.candid

import co.nstant.`in`.cbor.CborBuilder
import co.nstant.`in`.cbor.CborEncoder
import co.nstant.`in`.cbor.builder.MapBuilder
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.Signature
import java.util.*


val EDDSA_SPEC: EdDSAParameterSpec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)

data class EdDSAKeyPair(val pub: EdDSAPublicKey, val priv: EdDSAPrivateKey) {
    companion object {
        fun generateInsecure(): EdDSAKeyPair {
            val seed = randomBytes(32)
            return fromSeed(seed)
        }

        fun fromSeed(seed: ByteArray): EdDSAKeyPair {
            val privSpec = EdDSAPrivateKeySpec(seed, EDDSA_SPEC)
            val priv = EdDSAPrivateKey(privSpec)
            val pubSpec = EdDSAPublicKeySpec(priv.a, EDDSA_SPEC)
            val pub = EdDSAPublicKey(pubSpec)

            return EdDSAKeyPair(pub, priv)
        }
    }
}

fun signInsecure(privKey: EdDSAPrivateKey, message: ByteArray): ByteArray {
    val sgr: Signature = EdDSAEngine(MessageDigest.getInstance(EDDSA_SPEC.hashAlgorithm))
    sgr.initSign(privKey)
    sgr.update(message)

    return sgr.sign()
}

fun verify(pubKey: EdDSAPublicKey, message: ByteArray, sig: ByteArray): Boolean {
    val sgr: Signature = EdDSAEngine(MessageDigest.getInstance(EDDSA_SPEC.hashAlgorithm))
    sgr.initVerify(pubKey)
    sgr.update(message)

    return sgr.verify(sig)
}

fun hash(value: String): ByteArray {
    val digest = MessageDigest.getInstance("SHA-256")

    return digest.digest(value.toByteArray(StandardCharsets.UTF_8))
}

fun hash(value: ByteArray): ByteArray {
    val digest = MessageDigest.getInstance("SHA-256")

    return digest.digest(value)
}

data class AuthenticatedICRequest(
    val content: ICRequest,
    val senderPubKey: ByteArray,
    val senderSig: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AuthenticatedICRequest

        if (content != other.content) return false
        if (!senderPubKey.contentEquals(other.senderPubKey)) return false
        if (!senderSig.contentEquals(other.senderSig)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = content.hashCode()
        result = 31 * result + senderPubKey.contentHashCode()
        result = 31 * result + senderSig.contentHashCode()
        return result
    }

    fun cbor(): ByteArray {
        val os = ByteBufferBackedOutputStream()
        val encoder = CborEncoder(os).nonCanonical()

        val builder = CborBuilder()
            .addMap()

        content.cbor("content", builder)
        val items = builder
            .put("sender_pubkey", senderPubKey)
            .put("sender_sig", senderSig)
            .end()
            .build()

        encoder.encode(items)
        os.flush()

        return os.out!!
    }
}

fun randomBytes(size: Int): ByteArray {
    val r = Random()
    val bytes = ByteArray(size)
    r.nextBytes(bytes)

    return bytes
}

data class ICRequest(
    val requestType: IDLFuncRequestType,
    val canisterId: ByteArray,
    val methodName: String,
    val arg: ByteArray,
    val sender: SimpleIDLPrincipal,
    val nonce: ByteArray = randomBytes(9)
) {
    val id: ByteArray by lazy {
        val traversed = listOf(
            Pair(hash("arg"), hash(arg)),
            Pair(hash("canister_id"), hash(canisterId)),
            Pair(hash("method_name"), hash(methodName)),
            Pair(hash("nonce"), hash(nonce)),
            Pair(hash("request_type"), hash(requestType.value)),
            Pair(hash("sender"), hash(sender.id!!))
        )

        val sorted = traversed.sortedWith(kotlin.Comparator { (k1, v1), (k2, v2) ->
            var result = 0
            for (i in 0..k1.size) {
                result = k1[i].toUByte().compareTo(k2[i].toUByte())
                if (result != 0) break
            }

            result
        })
        val concatenatedSize = sorted.map { it.first.size + it.second.size }.sum()
        val concatenatedBuf = ByteBuffer.allocate(concatenatedSize)

        sorted.forEach {
            concatenatedBuf.put(it.first)
            concatenatedBuf.put(it.second)
        }
        concatenatedBuf.rewind()
        val concatenated = ByteArray(concatenatedSize)
        concatenatedBuf.get(concatenated)

        hash(concatenated)
    }

    fun authenticate(keyPair: EdDSAKeyPair): AuthenticatedICRequest {
        val sign = signInsecure(keyPair.priv, id)

        return AuthenticatedICRequest(this, keyPair.pub.abyte, sign)
    }

    fun cbor(name: String, builder: MapBuilder<CborBuilder>) {
        builder.putMap(name)
            .put("arg", arg)
            .put("canister_id", canisterId)
            .put("method_name", methodName)
            .put("nonce", nonce)
            .put("request_type", requestType.value)
            .put("sender", sender.id!!)
            .end()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ICRequest

        if (requestType != other.requestType) return false
        if (canisterId != other.canisterId) return false
        if (methodName != other.methodName) return false
        if (!arg.contentEquals(other.arg)) return false
        if (sender != other.sender) return false

        return true
    }

    override fun hashCode(): Int {
        var result = requestType.hashCode()
        result = 31 * result + canisterId.hashCode()
        result = 31 * result + methodName.hashCode()
        result = 31 * result + arg.contentHashCode()
        result = 31 * result + sender.hashCode()
        return result
    }
}

class ByteBufferBackedOutputStream : OutputStream() {
    private val buffer = mutableListOf<Int>()
    var out: ByteArray? = null

    override fun flush() {
        val bufSize = buffer.size + 3
        val buf = ByteBuffer.allocate(bufSize)

        buf.put(byteArrayOf(0xd9.toByte(), 0xd9.toByte(), 0xf7.toByte())) // wtf?
        buffer.forEach { buf.put(it.toByte()) }
        buf.rewind()

        out = ByteArray(bufSize)
        buf.get(out)
        buffer.clear()
    }

    @Throws(IOException::class)
    override fun write(b: Int) {
        buffer.add(b)
    }
}
