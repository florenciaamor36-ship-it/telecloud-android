package com.example.tdlib

import android.content.Context
import android.util.Log
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File

/** Thin, real TDLib client bridge. Authentication is driven only by TDLib updates. */
class TdLibNativeClient(
    context: Context,
    private val onAuthorizationState: (TdApi.AuthorizationState) -> Unit,
    private val onError: (Throwable) -> Unit
) {
    private val tag = "TdLibNativeClient"
    private val filesDir = context.filesDir
    private val client: Client

    init {
        System.loadLibrary("tdjni")
        client = Client.create(object : Client.ResultHandler {
            override fun onResult(result: TdApi.Object?) {
                if (result is TdApi.UpdateAuthorizationState) {
                    onAuthorizationState(result.authorizationState)
                }
            }
        }, object : Client.ExceptionHandler {
            override fun onException(e: Throwable?) {
                onError(e ?: IllegalStateException("TDLib update error"))
            }
        }, object : Client.ExceptionHandler {
            override fun onException(e: Throwable?) {
                onError(e ?: IllegalStateException("TDLib request error"))
            }
        })
        client.send(TdApi.GetAuthorizationState(), resultHandler())
    }

    fun setPhoneNumber(phone: String) {
        client.send(
            TdApi.SetAuthenticationPhoneNumber(
                phone,
                TdApi.PhoneNumberAuthenticationSettings(false, false, false, false, false, null, null)
            ),
            resultHandler()
        )
    }

    fun checkCode(code: String) {
        client.send(TdApi.CheckAuthenticationCode(code), resultHandler())
    }

    fun checkPassword(password: String) {
        client.send(TdApi.CheckAuthenticationPassword(password), resultHandler())
    }

    private fun resultHandler() = object : Client.ResultHandler {
        override fun onResult(result: TdApi.Object?) {
            if (result is TdApi.Error) {
                onError(IllegalStateException("TDLib ${result.code}: ${result.message}"))
            }
        }
    }
}
