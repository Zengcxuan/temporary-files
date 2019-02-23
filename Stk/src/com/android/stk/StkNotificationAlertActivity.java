package com.android.stk;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Bitmap;
import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.cat.TextMessage;
//Begin added by donghai.wu for  XR7230031 on 2018/12/19
import com.android.internal.telephony.PhoneConstants;
//End added by donghai.wu for  XR7230031 on 2018/12/19



public class StkNotificationAlertActivity extends Activity {

    private String mNotificationMessage = "";
    private String mTitle = "";
    private static final String LOG_TAG = "StkNotificationAlertActivity";
    private boolean mIdleIconSelfExplanatory = false;
    Bitmap mIdleIcon = null;


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        Window window = getWindow();
        setContentView(R.layout.stk_msg_dialog);

        TextView mMessageView = (TextView) findViewById(R.id.dialog_message);
        Button okButton = (Button) findViewById(R.id.button_ok);
        Button cancelButton = (Button) findViewById(R.id.button_cancel);

        CatLog.d(LOG_TAG, " enter");

        if ((null == mMessageView) || (null == okButton) || (null == cancelButton)) {
            CatLog.d(LOG_TAG, "Error: null Point: mMessageView[" + mMessageView
                    + "] okButton[" + okButton + "] cancelButton[" + cancelButton + "]");
            finish();
            return;
        }

        Intent intent = getIntent();
        TextMessage message = intent.getParcelableExtra("TEXT");
	//Begin modified by donghai.wu for  XR7230031 on 2018/12/19
	int sim_id = intent.getIntExtra(StkAppService.SLOT_ID, PhoneConstants.SIM_ID_1);
       mTitle = StkApp.mPLMN[sim_id];
	//End modified by donghai.wu for  XR7230031 on 2018/12/19
        mIdleIconSelfExplanatory = message.iconSelfExplanatory;
        mIdleIcon = message.icon;
        mNotificationMessage = message.text;


        window.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
                com.android.internal.R.drawable.stat_notify_sim_toolkit);
        setTitle(mTitle);

        okButton.setOnClickListener(mButtonClicked);
        cancelButton.setOnClickListener(mButtonClicked);

        CatLog.d(LOG_TAG, "Idle Text Title[" + mTitle + "]"
                + "[mNotificationMessage: ]" + mNotificationMessage);

        mMessageView.setText(mNotificationMessage);

        if (mIdleIcon != null) {
            BitmapDrawable bd = new BitmapDrawable(mIdleIcon);
            window.setFeatureDrawable(Window.FEATURE_LEFT_ICON, bd);
            if (mIdleIconSelfExplanatory) {
                mMessageView.setText("");
            }
        }

    }

    private View.OnClickListener mButtonClicked = new View.OnClickListener() {
        public void onClick(View v) {
            CatLog.d(LOG_TAG, "mButtonClicked finished!");
            finish();
        }
    };
}

