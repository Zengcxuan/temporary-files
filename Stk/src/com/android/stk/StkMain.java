/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.stk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.PhoneConstants;

import android.telephony.TelephonyManager;

import android.view.Gravity;
import android.widget.Toast;
// Begin added by donghai.wu  for XR6154319   on 2018/09/28
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.ResUtils;
import com.android.internal.telephony.TelephonyProperties;
import android.text.TextUtils;
import android.os.SystemProperties;
import android.os.Process;
// End added by donghai.wu  for XR6154319   on 2018/09/28

/**
 * Launcher class. Serve as the app's MAIN activity, send an intent to the
 * StkAppService and finish.
 *
 */
 public class StkMain extends Activity {
    private static final String className = new Object(){}.getClass().getEnclosingClass().getName();
    private static final String LOG_TAG = className.substring(className.lastIndexOf('.') + 1);
    private int mSingleSimId = -1;
    private Context mContext = null;
    private TelephonyManager mTm = null;
    private static final String PACKAGE_NAME = "com.android.stk";
    private static final String STK_LAUNCHER_ACTIVITY_NAME = PACKAGE_NAME + ".StkLauncherActivity";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        CatLog.d(LOG_TAG, "onCreate+");
        mContext = getBaseContext();
        mTm = (TelephonyManager) mContext.getSystemService(
                Context.TELEPHONY_SERVICE);
        //Check if needs to show the meun list.
        if (isShowSTKListMenu()) {
            Intent newIntent = new Intent(Intent.ACTION_VIEW);
            newIntent.setClassName(PACKAGE_NAME, STK_LAUNCHER_ACTIVITY_NAME);
            startActivity(newIntent);
        } else {
            //launch stk menu activity for the SIM.
            if (mSingleSimId < 0) {
                showTextToast(mContext, R.string.no_sim_card_inserted);
            } else {
                launchSTKMainMenu(mSingleSimId);
            }
        }
        finish();
    }

    private boolean isShowSTKListMenu() {
        int simCount = TelephonyManager.from(mContext).getSimCount();
        int simInsertedCount = 0;
        int insertedSlotId = -1;

        CatLog.d(LOG_TAG, "simCount: " + simCount);
        for (int i = 0; i < simCount; i++) {
            //Check if the card is inserted.
            // Begin modified by donghai.wu  for XR6154319   on 2018/09/28
            if (mTm.hasIccCard(i)&&(StkUtils.getInstance().judgeAddSTKItem(mContext,i))) {
	      // End modified by donghai.wu  for XR6154319   on 2018/09/28
                CatLog.d(LOG_TAG, "SIM " + i + " is inserted.");
                mSingleSimId = i;
                simInsertedCount++;
            } else {
                CatLog.d(LOG_TAG, "SIM " + i + " is not inserted.");
            }
        }
        if (simInsertedCount > 1) {
            return true;
        } else {
            //No card or only one card.
            CatLog.d(LOG_TAG, "do not show stk list menu.");
            return false;
        }
    }
    // Begin added by donghai.wu  for XR6154319  on 2018/09/28
    private boolean isStkReject() {
         boolean isReject= false;

        String Operators_PLMNS = ResUtils.getString(mContext,"def_reject_enter_stk_plmn_list","");
        if (TextUtils.isEmpty(Operators_PLMNS)){
	    CatLog.d(LOG_TAG, "isStkReject:  empty return false" );
            return false;
        }
        String[] PLMNS = Operators_PLMNS.split(",");

        String operatorNumeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC);
        CatLog.d(LOG_TAG, "isStkReject operatorNumeric: " + operatorNumeric);
	 CatLog.d(LOG_TAG, "isStkReject Operators_PLMNS: " + Operators_PLMNS);

        for(String plmn: PLMNS) {
            if ((null != operatorNumeric && operatorNumeric.contains(plmn))) {
                CatLog.d(LOG_TAG, "isStkReject has a Operator Sim in phone");
                isReject = true;
                break;
            }
        }
        CatLog.d(LOG_TAG, "isStkReject isReject = " + isReject);

	if(isReject)
	{
              showTextToast(mContext, R.string.lable_sim_not_ready);
	}

        return isReject;
    }

    public boolean isStkAllow() {
        boolean isAllow = false;
        CatLog.d(LOG_TAG, "enter isStkAllow");
        String plmns = ResUtils.getString("def_allow_enter_stk_plmn_list", "");
        if (TextUtils.isEmpty(plmns)) {
            CatLog.d(LOG_TAG, "isStkAllow:  empty return true");
            return true;
        }
        String[] PLMNS = plmns.split(",");

        String operatorNumeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC);
        CatLog.d(LOG_TAG, "isStkAllow operatorNumeric: " + operatorNumeric);

        for (String plmn : PLMNS) {
            if ((null != operatorNumeric && operatorNumeric.contains(plmn))) {
                CatLog.d(LOG_TAG, "isStkAllow has a Operator Sim in phone");
                isAllow = true;
                break;
            }
        }
        CatLog.d(LOG_TAG, "isStkAllow containOperatorSim = " + isAllow);

        if (isAllow == false) {
            showTextToast(mContext, R.string.lable_sim_not_allow_enter);
        }

        return isAllow;
    }

    private boolean isStkAvailable(int slotId) {
        int resId = R.string.lable_sim_not_ready;

        if (true == isOnFlightMode()) {
            CatLog.d(LOG_TAG, "isOnFlightMode");
            resId = R.string.lable_on_flight_mode;
            showTextToast(mContext, resId);
            return false;
        } 

        StkAppService service = StkAppService.getInstance();
         //Begin modified by donghai.wu for XR7099417 on 2018/11/05
        if (service == null){
	      CatLog.d(LOG_TAG, "isStkAvailable service == null" );
             showTextToast(mContext, resId);
             return false;
        }else if(!(StkUtils.getInstance().judgeStkValid(slotId))) {
         //End  modified by donghai.wu for XR7099417 on 2018/11/05
            int simState = TelephonyManager.getDefault().getSimState(slotId);
	     CatLog.d(LOG_TAG, "isStkAvailable slotId: " + slotId + "is not available simState:" + simState);
            showTextToast(mContext, resId);
            return false;
        }
        return true;
    }

    private boolean isOnFlightMode() {
        int mode = 0;
        try {
            mode = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON);
        } catch (SettingNotFoundException e) {
            CatLog.d(LOG_TAG, "fail to get airlane mode");
            mode = 0;
        }

        CatLog.d(LOG_TAG, "airlane mode is " + mode);
        return (mode != 0);
    }

	
    // End added by donghai.wu  for XR6154319   on 2018/09/28

    private void launchSTKMainMenu(int slotId) {
//Begin changed by zubin.chen.hz for XR7135244 on 2018/11/22 
	  // begin added by donghai.wu for XR5323530   on 2018/09/28
        if (!isStkAllow()) {
            CatLog.d(LOG_TAG, "isStkAllow launchSTKMainMenu. return");
            finish();
            return;
        }
        // end added by donghai.wu for XR5323530   on 2018/09/28
//End changed by zubin.chen.hz for XR7135244 on 2018/11/22 

       // Begin added by donghai.wu  for XR6154319   on 2018/09/28
       if(!isStkAvailable(slotId))
       {
            CatLog.d(LOG_TAG, "isStkAvailable launchSTKMainMenu. return");
            finish();
            return;
	 }
	   
       if (isStkReject()) {
	      CatLog.d(LOG_TAG, "isStkReject launchSTKMainMenu. return");
            finish();
            return;
        }
	  // End added by donghai.wu  for XR6154319  on 2018/09/28

        Bundle args = new Bundle();
        CatLog.d(LOG_TAG, "launchSTKMainMenu.");
        args.putInt(StkAppService.OPCODE, StkAppService.OP_LAUNCH_APP);
        args.putInt(StkAppService.SLOT_ID
                , PhoneConstants.SIM_ID_1 + slotId);
        startService(new Intent(this, StkAppService.class)
                .putExtras(args));
    }

    private void showTextToast(Context context, int resId) {
        Toast toast = Toast.makeText(context, resId, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }
}
