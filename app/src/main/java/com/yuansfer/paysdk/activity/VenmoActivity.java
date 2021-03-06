package com.yuansfer.paysdk.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.VenmoAccountNonce;
import com.yuansfer.pay.braintree.BrainTreePayActivity;
import com.yuansfer.pay.braintree.BrainTreePaymentMethod;
import com.yuansfer.pay.payment.ErrStatus;
import com.yuansfer.pay.payment.PayResultMgr;
import com.yuansfer.pay.payment.PayType;
import com.yuansfer.pay.payment.YSAppPay;
import com.yuansfer.paysdk.R;
import com.yuansfer.paysdk.api.ApiService;
import com.yuansfer.paysdk.model.CommonResultInfo;
import com.yuansfer.paysdk.model.PayProcessInfo;
import com.yuansfer.paysdk.model.SecurePayInfo;
import com.yuansfer.paysdk.model.SecureResultV3Info;
import com.yuansfer.paysdk.model.SecureV3Info;
import com.yuansfer.paysdk.okhttp.GsonResponseHandler;
import com.yuansfer.paysdk.util.YSTestApi;

import static android.view.View.VISIBLE;

public class VenmoActivity extends BrainTreePayActivity implements PayResultMgr.IPayResultCallback {

    private TextView mResultTxt;
    private SecureV3Info secureV3Info;
    private Button mVenmoBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_pay);
        setUpAsBackTitle();
        mResultTxt = findViewById(R.id.tv_result);
        mVenmoBtn = findViewById(R.id.btn_start_pay);
        mVenmoBtn.setText("Venmo");
        callPrepay();
    }

    @Override
    protected void onStart() {
        super.onStart();
        YSAppPay.registerPayResultCallback(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        YSAppPay.unregisterPayResultCallback(this);
    }

    private void setUpAsBackTitle() {
        getSupportActionBar().setTitle("Venmo");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void callPrepay() {
        YSTestApi.callTestPrepay(getApplicationContext(), new GsonResponseHandler<SecureResultV3Info>() {

            @Override
            public void onFailure(int statusCode, String errorMsg) {
                mResultTxt.setText(errorMsg);
            }

            @Override
            public void onSuccess(int statusCode, SecureResultV3Info response) {
                if ("000100".equals(response.getRet_code())) {
                    secureV3Info = response.getResult();
                    mResultTxt.setText(secureV3Info.toString());
                    YSAppPay.getInstance().bindBrainTree(VenmoActivity.this, secureV3Info.getAuthorization());
                } else {
                    mResultTxt.setText("prepay接口报错" + response.getRet_code() + "/" + response.getRet_msg());
                }
            }

        });
    }

    private void callPayProcess(String transactionNo, String nonce, String deviceData) {
        YSTestApi.callTestProcess(getApplicationContext(), BrainTreePaymentMethod.VENMO_ACCOUNT
                , transactionNo, nonce, deviceData, new GsonResponseHandler<CommonResultInfo>() {

                    @Override
                    public void onFailure(int statusCode, String errorMsg) {
                        mResultTxt.setText(errorMsg);
                    }

                    @Override
                    public void onSuccess(int statusCode, CommonResultInfo response) {
                        if ("000100".equals(response.getRet_code())) {
                            //支付成功
                            mResultTxt.setText("process接口成功:" + response.getRet_msg());
                        } else {
                            mResultTxt.setText("process接口报错" + response.getRet_code() + "/" + response.getRet_msg());
                        }
                    }
                });
    }

    public void onViewClick(View v) {
        if (secureV3Info != null) {
            YSAppPay.getInstance().requestVenmoPayment(this, false);
        }
    }

    @Override
    public void onPaySuccess(int payType) {
        mResultTxt.setText("支付成功");
    }

    @Override
    public void onPayFail(@PayType int payType, ErrStatus errStatus) {
        mResultTxt.setText(errStatus.getErrCode() + "/" + errStatus.getErrMsg());
    }

    @Override
    public void onPayCancel(int payType) {
        mResultTxt.setText("支付取消");
    }

    @Override
    public void onPaymentConfigurationFetched(Configuration configuration) {
        if (configuration.getPayWithVenmo().isEnabled(this)) {
            mVenmoBtn.setEnabled(true);
        } else if (configuration.getPayWithVenmo().isAccessTokenValid()) {
            showDialog("Please install the Venmo app first.");
        } else {
            showDialog("Venmo is not enabled for the current merchant.");
        }
    }

    @Override
    public void onPaymentNonceFetched(VenmoAccountNonce venmoAccountNonce, String deviceData) {
        super.onPaymentNonceFetched(venmoAccountNonce, deviceData);
        mResultTxt.setText(DropInPayActivity.getDisplayString(venmoAccountNonce));
        callPayProcess(secureV3Info.getTransactionNo(), venmoAccountNonce.getNonce(), deviceData);
    }

    private void showDialog(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        YSAppPay.getInstance().unbindBrainTree(this);
    }

}
