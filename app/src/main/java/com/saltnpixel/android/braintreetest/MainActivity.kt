package com.saltnpixel.android.braintreetest

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.braintreepayments.api.dropin.DropInRequest
import com.braintreepayments.api.dropin.DropInResult
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.TextHttpResponseHandler
import cz.msebera.android.httpclient.Header
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {


    companion object {
        const val API_GET_TOKEN = "http://192.168.100.39:80/braintree/main.php"
        const val API_CHECKOUT = "http://192.168.100.39:80/braintree/checkout.php"

        internal var token: String = ""
        internal lateinit var amount: String
        internal var paramsHashMap: HashMap<String, String>? = null
    }

    private val REQUEST_CODE: Int = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getToken()
        btnPay.setOnClickListener {
            val dropInRequest = DropInRequest().clientToken(token)
            startActivityForResult(dropInRequest.getIntent(this), REQUEST_CODE)
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val result = data?.getParcelableExtra<DropInResult>(DropInResult.EXTRA_DROP_IN_RESULT)
                val noonce = result?.paymentMethodNonce
                val stringNonce = noonce!!.nonce
                if (!etPrice.text.toString().isEmpty()) {
                    amount = etPrice.text.toString()
                    paramsHashMap = HashMap()
                    paramsHashMap!!["amount"] = amount
                    paramsHashMap!!["nonce"] = stringNonce
                    sendPayments()
                } else {
                    Toast.makeText(applicationContext, "amount must not be empty", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun sendPayments() {
        val queue = Volley.newRequestQueue(this)
        val stringRequest = object : StringRequest(Request.Method.POST, API_CHECKOUT,
            Response.Listener { response ->
                if (response.toString().contains("Successful")) {
                    Toast.makeText(applicationContext, "Transaction Successful", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(applicationContext, "Transaction Fail", Toast.LENGTH_LONG).show()
                }
            }, Response.ErrorListener {
                Toast.makeText(applicationContext, "$it", Toast.LENGTH_LONG).show()
            }) {
            override fun getParams(): MutableMap<String, String>? {
                if (paramsHashMap == null) {
                    return null
                }
                val params = HashMap<String, String?>()
                for (key in paramsHashMap!!.keys) {
                    params[key] = paramsHashMap!![key]
                }
                return params as MutableMap<String, String>
            }

            override fun getHeaders(): MutableMap<String, String> {
                val params = HashMap<String, String?>()
                params["Content-Type"] = "application/x-www-form-urlencoded"
                return params as MutableMap<String, String>
            }
        }
        stringRequest.retryPolicy = DefaultRetryPolicy(
            0,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        queue.add(stringRequest)
    }

    private fun getToken() {
        val androidClient = AsyncHttpClient()
        androidClient.get(API_GET_TOKEN, object : TextHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Array<out Header>?, responseString: String?) {
                runOnUiThread {
                    token = responseString!!
                    Toast.makeText(applicationContext, "Success Token", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(
                statusCode: Int,
                headers: Array<out Header>?,
                responseString: String?,
                throwable: Throwable?
            ) {
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "Status code :" + statusCode + "errmsg : " + throwable.toString(),
                        Toast.LENGTH_LONG
                    ).show();
                }
            }

        })
    }


}
