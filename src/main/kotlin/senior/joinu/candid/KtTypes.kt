package senior.joinu.candid

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResult
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

open class SimpleIDLService(
    var host: String?,
    val id: ByteArray?,
    var keyPair: EdDSAKeyPair?,
    var apiVersion: String = "v1"
) {
    suspend fun call(funcName: String, arg: ByteArray): ByteArray {
        return submit(funcName, IDLFuncRequestType.Call, arg)
    }

    suspend fun query(funcName: String, arg: ByteArray): ByteArray {
        return read(funcName, IDLFuncRequestType.Query, arg)
    }

    suspend fun submit(funcName: String, type: IDLFuncRequestType, arg: ByteArray): ByteArray {
        val req = ICRequest(type, id!!, funcName, arg, SimpleIDLPrincipal.selfAuthenticating(keyPair!!.pub.abyte))
        val authReq = req.authenticate(keyPair!!)
        val body = authReq.cbor()

        val (responseBody, error) = Fuel.post("${host!!}/api/$apiVersion/submit")
            .body(body)
            .header("Content-Type", "application/cbor")
            .awaitByteArrayResult()

        if (error != null) {
            val errorMessage = error.response.body().toByteArray().toString(StandardCharsets.ISO_8859_1)
            throw RuntimeException(errorMessage, error)
        }

        return responseBody!!
    }

    // js d9d9f7 a367636f6e74656e74a6636172674b4449444c000171033132336b63616e69737465725f696448c826cbb0239f21216b6d6574686f645f6e616d65656772656574656e6f6e636549 fedbe2f2c584819b02 sender 6c726571756573745f747970656463616c6c6673656e6465725821 f0248968e0fb14bb17331b415990e92c0d78b050b53aba7d62a2191992f756e8 pubkey 026d73656e6465725f7075626b65795820 fbe65e8919f733d19cb224e12f25178063da529fa5bc7742a2a7149c8f6445c0 sig 6a73656e6465725f7369675840 f5da21edbb85fe917a6f2a0bd4100cc663f57b87afbcd8f1203ee1949c2afe5d50aeb15ee207d50a2fc24809026c28a954b3a426a167acf58921695d7a60fc05
    // kt d9d9f7 a367636f6e74656e74a6636172674b4449444c000171033132336b63616e69737465725f696448c826cbb0239f21216b6d6574686f645f6e616d65656772656574656e6f6e636549 c32c17b065f8d66096 sender 6c726571756573745f747970656463616c6c6673656e6465725821 8e0fb0371f2665749caae5520e657333e9e0deaeeef0b4e8f4f5b0f0732e2672 pubkey 026d73656e6465725f7075626b65795820 cf79557fb466914fbb2e2dbb9a6bc8f9003beb752d41ea2fe16d9cce1ae3de5b sig 6a73656e6465725f7369675840 28bb38cd24221c07bc7e7d3ded7f7d2420eded908e7beaa307b6198ea1f80d01b69394d42e37ab64ae39054eb6f489b931176592c7cd83f9f827c22730

    // js ÙÙ÷ £gcontent¦ carg KDIDL q123 kcanister_id HÈ&Ë°#Ÿ!! kmethod_name egreet enonce IþÛâòÅ„› lrequest_type dcall fsender X!ð$‰hàû»3AYé, x°Pµ:º}b¢’÷Vè msender_pubkey Xûæ^‰÷3Ñœ²$á/%€cÚRŸ¥¼wB¢§œdEÀ jsender_sig X@õÚ!í»…þ‘zo*ÔÆcõ{‡¯¼Øñ >á”œ*þ]P®±^âÕ /ÂH	l(©T³¤&¡g¬õ‰!i]z`ü
    // kt ÙÙ÷ £gcontent¦ carg KDIDL q123 kcanister_id HÈ&Ë°#!! kmethod_name egreet enonce IÃ,°eøÖ` lrequest_type dcall fsender X!°7&etªåRes3éàÞ®îð´èôõ°ðs.&r msender_pubkey XÏyU´fO».-»kÈù ;ëu-Aê/ámÎãÞ[ jsender_sig X@(»8Í$"¼~}=í}$ íí{ê£¶¡ø ¶Ô.7«d®9N¶ô¹1eÇÍùø'Â'0

    suspend fun read(funcName: String, type: IDLFuncRequestType, arg: ByteArray): ByteArray {
        val req = ICRequest(type, id!!, funcName, arg, SimpleIDLPrincipal.selfAuthenticating(keyPair!!.pub.abyte))
        val authReq = req.authenticate(keyPair!!)
        val body = authReq.cbor()

        val (responseBody, error) = Fuel.post("${host!!}/api/$apiVersion/read")
            .body(body)
            .header("Content-Type", "application/cbor")
            .awaitByteArrayResult()

        if (error != null) {
            val errorMessage = error.response.body().toByteArray().toString(StandardCharsets.ISO_8859_1)
            throw RuntimeException(errorMessage, error)
        }

        return responseBody!!
    }
}

enum class IDLFuncRequestType(val value: String) {
    Call("call"),
    Query("query")
}

open class SimpleIDLFunc(
    val funcName: String?,
    val service: SimpleIDLService?
)

open class SimpleIDLPrincipal(
    val id: ByteArray?
) {
    companion object {
        fun selfAuthenticating(pubKey: ByteArray): SimpleIDLPrincipal {
            val pubKeyHash = hash(pubKey)
            val buf = ByteBuffer.allocate(pubKeyHash.size + 1)

            buf.put(pubKeyHash)
            buf.put(2)
            val id = ByteArray(pubKeyHash.size + 1)
            buf.rewind()
            buf.get(id)

            return SimpleIDLPrincipal(id)
        }
    }
}

object Null
object Reserved
object Empty
