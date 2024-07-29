import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit


fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}

class VSLWebsocketClient {

    private val url = "wss://minhtai048--vsl-cloud-endpoint.modal.run"
    private val client = HttpClient(OkHttp) {
        install(WebSockets)
        engine {
            preconfigured = OkHttpClient.Builder()
                .pingInterval(20000, TimeUnit.MILLISECONDS)
                .build()
        }
    }
    /*
    private var session = runBlocking {
        client.webSocketSession(url)
    }


    suspend fun sendFrame(senderChannel: ReceiveChannel<Bitmap>) {
        for (bitmapBuffer in senderChannel) {
            try {
                    val convertedByteArray = bitmapToByteArray(bitmapBuffer)
                    session
                        .outgoing
                        .send(
                            Frame.Binary(
                                fin = false, data = convertedByteArray
                            )
                        )
            } catch (e: Exception){
                Log.d(TAG, "Exception cause: ${e.cause.toString()}")
                Log.d(TAG, "Exception message: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    suspend fun receiveInference(receiverChannel: Channel<String>) {
        for (frame in session.incoming) {
            try {
                val translation = frame.data.toString(Charset.defaultCharset())
                receiverChannel.send(translation)
            } finally {
                Log.d(TAG, "Done!")
            }
        }
        *//*
        while (true) {
            try {
                val infrnce = session.incoming.receive()
                val translation = infrnce.data.toString(Charset.defaultCharset())
                Log.d(TAG, "Look @ $translation now!")

            } finally {
                Log.d(TAG, "Done!")
            }
        }
        * *//*
    }*/

    suspend fun sendAndReceiveResult(senderChannel: ReceiveChannel<Bitmap>, receiverChannel: Channel<String>) {
        client.webSocket(url) {
            while (true) {
                Log.d(TAG, "Connected!")
                try {
                    val bitmapBuffer = senderChannel.receive()
                    val convertedByteArray = bitmapToByteArray(bitmapBuffer)
                    outgoing
                        .send(
                            Frame.Binary(
                                fin = false, data = convertedByteArray
                            )
                        )
                } catch (e: Exception){
                    Log.d(TAG, "Exception cause: ${e.cause.toString()}")
                    Log.d(TAG, "Exception message: ${e.message}")
                    e.printStackTrace()
                }
                try {
                    val frame = incoming.receive()
                    val translation = frame.data.toString(Charset.defaultCharset())
                    receiverChannel.send(translation)
                } finally {
                    Log.d(TAG, "Done!")
                }
            }
        }
    }

    fun disconnect() {
        Log.d(TAG, "Oh hell no!")
        client.close()
    }
}