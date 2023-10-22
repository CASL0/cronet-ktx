/*
 *  Copyright 2023 CASL0
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.casl0.cronet

import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.util.concurrent.ExecutorService
import kotlin.coroutines.resume

/** Cronetリクエストの結果 */
sealed interface Result {
    class Success(
            /** レスポンスヘッダー */
            val header: Map<String, List<String>>,

            /** レスポンスボディ */
            val body: ByteArray,

            /** プロトコル */
            val negotiatedProtocol: String
    ) : Result

    data class Error(
            /** エラー時の例外 */
            val exception: CronetException,

            /** HTTPステータスコード */
            val statusCode: Int?
    ) : Result
}

/**
 * 指定のURLにGETリクエストを送信します
 *
 * @param url URL文字列
 * @param executor 実行スレッド
 * @return リクエストの結果
 */
suspend fun CronetEngine.request(url: String, executor: ExecutorService): Result {
    return suspendCancellableCoroutine { continuation ->
        val builder = newUrlRequestBuilder(url, object : UrlRequest.Callback() {
            private val bytesReceived = ByteArrayOutputStream()
            private val receiveChannel = Channels.newChannel(bytesReceived)

            override fun onRedirectReceived(
                    request: UrlRequest,
                    info: UrlResponseInfo,
                    newLocationUrl: String
            ) {
                Log.d(TAG, "Redirect to $newLocationUrl")
                request.followRedirect()
            }

            override fun onResponseStarted(
                    request: UrlRequest,
                    info: UrlResponseInfo
            ) {
                request.read(ByteBuffer.allocateDirect(BYTE_BUFFER_CAPACITY_BYTES))
            }

            override fun onReadCompleted(
                    request: UrlRequest,
                    info: UrlResponseInfo,
                    byteBuffer: ByteBuffer
            ) {
                byteBuffer.flip()
                receiveChannel.write(byteBuffer)

                byteBuffer.clear()

                request.read(byteBuffer)
            }

            override fun onSucceeded(
                    request: UrlRequest,
                    info: UrlResponseInfo
            ) {
                continuation.resume(
                        Result.Success(
                                header = info.allHeaders,
                                body = bytesReceived.toByteArray(),
                                negotiatedProtocol = info.negotiatedProtocol
                        )
                )
            }

            override fun onFailed(
                    request: UrlRequest,
                    info: UrlResponseInfo?,
                    error: CronetException
            ) {
                continuation.resume(
                        Result.Error(
                                exception = error,
                                statusCode = info?.httpStatusCode
                        )
                )
            }

            override fun onCanceled(
                    request: UrlRequest,
                    info: UrlResponseInfo?
            ) {
                super.onCanceled(request, info)
            }

        }, executor)
        val request = builder.build()
        continuation.invokeOnCancellation { request.cancel() }

        request.start()
    }
}

private const val BYTE_BUFFER_CAPACITY_BYTES = 16 * 1024
private const val TAG = "cronet-ktx"
