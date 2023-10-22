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

package io.github.casl0.cronet.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import io.github.casl0.cronet.Result
import io.github.casl0.cronet.initCronetEngine
import io.github.casl0.cronet.request
import io.github.casl0.cronet.sample.ui.theme.CronetktxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private var _installedCronet by mutableStateOf(false)
    private var _globalIp by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CronetktxTheme {
                CronetScreen(
                    installedCronet = _installedCronet,
                    globalIp = _globalIp
                )
            }
        }
        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                val result = initCronetEngine()
                if (result.isSuccess) {
                    _installedCronet = true
                    result.getOrNull()?.run {
                        val res =
                            request(
                                "https://api.ipify.org",
                                Executors.newSingleThreadExecutor()
                            )
                        if (res is Result.Success) {
                            Log.d(TAG, "Headers are ${res.header}")
                            Log.d(TAG, "Protocol is ${res.negotiatedProtocol}")
                            _globalIp = res.body.toString(Charset.defaultCharset())
                        } else if (res is Result.Error) {
                            Log.e(TAG, "Exception: ${res.exception}")
                            Log.e(TAG, "Status code: ${res.statusCode}")
                        }
                    }
                } else {
                    Log.d(TAG, result.exceptionOrNull().toString())
                }
            }
        }
    }
}

@Composable
private fun CronetScreen(
    installedCronet: Boolean,
    globalIp: CharSequence,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (installedCronet) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = globalIp.toString())
            }
        } else {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Unable to load Cronet from Play Services")
            }
        }
    }
}
