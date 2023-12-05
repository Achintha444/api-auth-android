package com.wso2_sample.api_auth_sample.api.oauth_client

import android.content.Context
import android.util.Log
import com.fasterxml.jackson.databind.JsonNode
import com.wso2_sample.api_auth_sample.R
import com.wso2_sample.api_auth_sample.api.app_auth_manager.AppAuthManager
import com.wso2_sample.api_auth_sample.api.cutom_trust_client.CustomTrust
import com.wso2_sample.api_auth_sample.controller.ui.activities.fragments.auth.AuthController
import com.wso2_sample.api_auth_sample.controller.ui.activities.fragments.auth.AuthParams
import com.wso2_sample.api_auth_sample.controller.ui.activities.fragments.auth.data.authenticator.Authenticator
import com.wso2_sample.api_auth_sample.model.api.FlowStatus
import com.wso2_sample.api_auth_sample.model.api.app_auth_manager.TokenRequestCallback
import com.wso2_sample.api_auth_sample.model.api.oauth_client.AttestationCallback
import com.wso2_sample.api_auth_sample.model.api.oauth_client.authenticator.AuthenticatorCallback
import com.wso2_sample.api_auth_sample.model.api.oauth_client.AuthorizeFlow
import com.wso2_sample.api_auth_sample.model.api.oauth_client.authenticator.AllAuthenticatorsCallback
import com.wso2_sample.api_auth_sample.model.ui.activities.login.fragments.auth.auth_method.basic_auth.authenticator.BasicAuthAuthenticator
import com.wso2_sample.api_auth_sample.model.util.uiUtil.SharedPreferencesKeys
import com.wso2_sample.api_auth_sample.util.UiUtil
import com.wso2_sample.api_auth_sample.util.Util
import com.wso2_sample.api_auth_sample.util.config.OauthClientConfiguration
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException

class OauthClient {

    companion object {

        private val client: OkHttpClient = CustomTrust.getInstance().client

        @Throws(IOException::class)
        fun authorize(
            context: Context,
            whenAuthentication: () -> Unit,
            finallyAuthentication: () -> Unit,
            onSuccessCallback: (authorizeFlow: AuthorizeFlow) -> Unit,
            onFailureCallback: () -> Unit
        ) {

            // build request based on the request method and the builder
            fun buildRequest(requestBuilder: Request.Builder, requestBody: RequestBody): Request {
                return requestBuilder.post(requestBody).build()
            }

            // authorize call
            fun authorizeCall(request: Request) {
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        println(e)
                        onFailureCallback()
                        finallyAuthentication()
                    }

                    @Throws(IOException::class)
                    override fun onResponse(call: Call, response: Response) {
                        try {
                            // reading the json
                            val model: JsonNode = Util.getJsonObject(response.body!!.string())
                            val authorizeFlow: AuthorizeFlow = AuthorizeFlow.getAuthorizeFlow(model)

                            if (authorizeFlow.nextStep.authenticators.size > 1) {
                                getAllAuthenticators(context, authorizeFlow.flowId, authorizeFlow.nextStep.authenticators,
                                    AllAuthenticatorsCallback(
                                        onSuccess = {
                                            authorizeFlow.nextStep.authenticators = it
                                            onSuccessCallback(authorizeFlow)
                                        },
                                        onFailure = {
                                            onFailureCallback()
                                        }
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            println(e)
                            onFailureCallback()
                        } finally {
                            finallyAuthentication()
                        }
                    }
                })
            }

            whenAuthentication()

            // authorize URL
            val url: String = OauthClientConfiguration.getInstance(context).authorizeUri.toString()

            // POST form parameters
            val formBody: RequestBody = FormBody.Builder()
                .add("client_id", OauthClientConfiguration.getInstance(context).clientId)
                .add("response_type", OauthClientConfiguration.getInstance(context).responseType)
                .add(
                    "redirect_uri",
                    OauthClientConfiguration.getInstance(context).redirectUri.toString()
                )
                .add("state", OauthClientConfiguration.getInstance(context).state)
                .add("scope", OauthClientConfiguration.getInstance(context).scope)
                .add("response_mode", OauthClientConfiguration.getInstance(context).responseMode)
                .build()

            val requestBuilder: Request.Builder = Request.Builder().url(url)
            requestBuilder.addHeader("Accept", "application/json")
            requestBuilder.addHeader("Content-Type", "application/x-www-form-urlencoded")

            AttestationCallPlayIntegrity.playIntegrityRequest(context, AttestationCallback(
                // when the attestation is successful, pass the integrity token to the request
                onSuccess = { integrityToken: String ->
                    requestBuilder.addHeader("x-client-attestation", integrityToken)
                    val request: Request = buildRequest(requestBuilder, formBody)
                    authorizeCall(request)
                },

                // when the attestation fails, pass the request without the integrity token
                onFailure = {
                    val request: Request = buildRequest(requestBuilder, formBody)
                    authorizeCall(request)
                }
            ))
        }

        /**
         * Get full details of the all authenticators for the given flow.
         */
        private fun getAllAuthenticators(
            context: Context,
            flowId: String,
            authenticators: ArrayList<Authenticator>,
            callback: AllAuthenticatorsCallback
        ) {

            // Get single authenticator details from the API
            fun getAuthenticator(
                context: Context,
                flowId: String,
                authenticator: Authenticator,
                callback: AuthenticatorCallback
            ) {
                // authorize next URL
                val url: String =
                    OauthClientConfiguration.getInstance(context).authorizeNextUri.toString()

                val request: Request = Request.Builder().url(url)
                    .post(AuthController.buildRequestBodyForAuthenticator(flowId, authenticator))
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        callback.onFailure(e)
                    }

                    @Throws(IOException::class)
                    override fun onResponse(call: Call, response: Response) {
                        try {
                            if (response.code == 200) {
                                // reading the json
                                val model: JsonNode = Util.getJsonObject(response.body!!.string())
                                val authorizeFlow: AuthorizeFlow = AuthorizeFlow.getAuthorizeFlow(model)

                                if (authorizeFlow.nextStep.authenticators.size == 1) {
                                    callback.onSuccess(authorizeFlow.nextStep.authenticators[0])
                                } else {
                                    callback.onFailure(Exception("Authenticator not found"))
                                }
                            } else {
                                callback.onFailure(Exception("Something went wrong"))
                            }
                        } catch (e: IOException) {
                            callback.onFailure(e)
                        }
                    }
                })
            }

            if (authenticators.size > 1) {
                var errorCheck = false
                val authenticatorCount = authenticators.size
                val detailedAuthenticators: ArrayList<Authenticator> = ArrayList()

                for (authenticator in authenticators) {

                    // Break the loop if an error occurs
                    if (errorCheck) {
                        callback.onFailure(Exception("Something went wrong"))
                        break
                    }

                    // Does not need to call the API if the authenticator is BasicAuth as the require
                    // information is already contained in the authenticator object
                    if (authenticator.authenticator == BasicAuthAuthenticator.AUTHENTICATOR_TYPE) {
                        detailedAuthenticators.add(authenticator)

                        if (detailedAuthenticators.size == authenticatorCount) {
                            callback.onSuccess(detailedAuthenticators)
                        }
                    } else {
                        getAuthenticator(context, flowId, authenticator,
                            AuthenticatorCallback(
                                onSuccess = {
                                    detailedAuthenticators.add(it)

                                    if (detailedAuthenticators.size == authenticatorCount) {
                                        callback.onSuccess(detailedAuthenticators)
                                    }
                                },
                                onFailure = {
                                    errorCheck = true
                                }
                            )
                        )
                    }
                }
            } else {
                callback.onSuccess(authenticators)
            }
        }

        @Throws(IOException::class)
        fun authenticate(
            context: Context,
            authenticator: Authenticator,
            authParams: AuthParams,
            whenAuthentication: () -> Unit,
            finallyAuthentication: () -> Unit,
            onSuccessCallback: (authorizeObj: JsonNode) -> Unit,
            onFailureCallback: () -> Unit
        ) {

            val flowId: String = UiUtil.readFromSharedPreferences(
                context.getSharedPreferences(
                    R.string.app_name.toString(), Context.MODE_PRIVATE
                ), SharedPreferencesKeys.FLOW_ID.key
            ).toString()

            whenAuthentication()

            // authorize next URL
            val url: String =
                OauthClientConfiguration.getInstance(context).authorizeNextUri.toString()

            val request: Request = Request.Builder().url(url)
                .post(AuthController.buildRequestBodyForAuth(flowId, authenticator, authParams))
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    println(e)
                    onFailureCallback()
                    finallyAuthentication()
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (response.code == 200) {
                            // reading the json
                            val model: JsonNode = Util.getJsonObject(response.body!!.string())

                            // Assessing the flow status
                            val flowStatusNode: JsonNode? = model["flowStatus"]
                            val flowStatus: String =
                                if (flowStatusNode != null && flowStatusNode.isTextual) {
                                    flowStatusNode.asText()
                                } else {
                                    // Handle the case when "flowStatus" is null or not a valid string
                                    FlowStatus.SUCCESS.flowStatus
                                }

                            when (flowStatus) {
                                FlowStatus.FAIL_INCOMPLETE.flowStatus -> {
                                    onFailureCallback()
                                }

                                FlowStatus.INCOMPLETE.flowStatus -> {
                                    onSuccessCallback(model)
                                }

                                FlowStatus.SUCCESS.flowStatus -> {
                                    token(context, model, onSuccessCallback, onFailureCallback)
                                }
                            }
                        } else {
                            onFailureCallback()
                        }
                    } catch (e: IOException) {
                        println(e)
                        onFailureCallback()
                    } finally {
                        finallyAuthentication()
                    }
                }
            })
        }

        fun token(
            context: Context,
            model: JsonNode?,
            onSuccessCallback: (authorizeObj: JsonNode) -> Unit,
            onFailureCallback: () -> Unit
        ) {

            val appAuthManager = AppAuthManager(context)
            val authorizationCode: String = model!!.get("code").asText()

            appAuthManager.exchangeAuthorizationCodeForAccessToken(authorizationCode,
                TokenRequestCallback(
                    onSuccess = { accessToken: String ->
                        UiUtil.writeToSharedPreferences(
                            context.getSharedPreferences(
                                R.string.app_name.toString(),
                                Context.MODE_PRIVATE
                            ), SharedPreferencesKeys.ACCESS_TOKEN.key, accessToken
                        )
                        onSuccessCallback(model)
                    },
                    onFailure = { error: java.lang.Exception ->
                        Log.e("Token request failed", error.toString())
                        onFailureCallback()
                    }
                ))
        }
    }
}