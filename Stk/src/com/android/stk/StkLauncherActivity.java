/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.android.internal.telephony.cat.Item;
import com.android.internal.telephony.cat.Menu;
import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.PhoneConstants;
//Begin added by canxuan.zeng for XR7446698 on 2018/02/19
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
//End added by canxuan.zeng for XR7446698 on 2018/02/19
import android.telephony.TelephonyManager;

// Begin added by donghai.wu  for XR6154319   on 2018/09/28
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.ResUtils;
import com.android.internal.telephony.TelephonyProperties;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Toast;
// End added by donghai.wu  for XR6154319   on 2018/09/28

import java.util.ArrayList;

/**
 * Launcher class. Serve as the app's MAIN activity, send an intent to the
 * StkAppService and finish.
 *
 */
public class StkLauncherActivity extends ListActivity {
    private TextView mTitleTextView = null;
    private ImageView mTitleIconView = null;
    private static final String className = new Object(){}.getClass().getEnclosingClass().getName();
    private static final String LOG_TAG = className.substring(className.lastIndexOf('.') + 1);
    private ArrayList<Item> mStkMenuList = null;
    private int mSingleSimId = -1;
    private Context mContext = null;
    private TelephonyManager mTm = null;
    private Bitmap mBitMap = null;
    private boolean mAcceptUsersInput = true;
    //Begin added by canxuan.zeng for XR7446698 on 2018/02/19
    private SubscriptionManager mSubscriptionManager = null;
    //End added by canxuan.zeng for XR7446698 on 2018/02/19

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        CatLog.d(LOG_TAG, "onCreate+");
        mContext = getBaseContext();
        mTm = (TelephonyManager) mContext.getSystemService(
                Context.TELEPHONY_SERVICE);
        //Begin added by canxuan.zeng for XR7446698 on 2018/02/19
        mSubscriptionManager = SubscriptionManager.from(mContext);
        //End added by canxuan.zeng for XR7446698 on 2018/02/19

        ActionBar actionBar = getActionBar();
        actionBar.setCustomView(R.layout.stk_title);
        actionBar.setDisplayShowCustomEnabled(true);

        setContentView(R.layout.stk_menu_list);
        mTitleTextView = (TextView) findViewById(R.id.title_text);
        mTitleIconView = (ImageView) findViewById(R.id.title_icon);
        mTitleTextView.setText(R.string.app_name);
        mBitMap = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_launcher_sim_toolkit);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if (!mAcceptUsersInput) {
            CatLog.d(LOG_TAG, "mAcceptUsersInput:false");
            return;
        }
        int simCount = TelephonyManager.from(mContext).getSimCount();
        Item item = getSelectedItem(position);
        if (item == null) {
            CatLog.d(LOG_TAG, "Item is null");
            return;
        }
        CatLog.d(LOG_TAG, "launch stk menu id: " + item.id);
        if (item.id >= PhoneConstants.SIM_ID_1 && item.id < simCount) {
            mAcceptUsersInput = false;
            launchSTKMainMenu(item.id);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        CatLog.d(LOG_TAG, "mAcceptUsersInput: " + mAcceptUsersInput);
        if (!mAcceptUsersInput) {
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                CatLog.d(LOG_TAG, "KEYCODE_BACK.");
                mAcceptUsersInput = false;
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onResume() {
        super.onResume();
        CatLog.d(LOG_TAG, "onResume");
        mAcceptUsersInput = true;
        int itemSize = addStkMenuListItems();
        if (itemSize == 0) {
            CatLog.d(LOG_TAG, "item size = 0 so finish.");
            finish();
        } else if (itemSize == 1) {
            launchSTKMainMenu(mSingleSimId);
            finish();
        } else {
            CatLog.d(LOG_TAG, "resume to show multiple stk list.");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        CatLog.d(LOG_TAG, "onPause");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CatLog.d(LOG_TAG, "onDestroy");
    }

    private Item getSelectedItem(int position) {
        Item item = null;
        if (mStkMenuList != null) {
            try {
                item = mStkMenuList.get(position);
            } catch (IndexOutOfBoundsException e) {
                if (StkApp.DBG) {
                    CatLog.d(LOG_TAG, "IOOBE Invalid menu");
                }
            } catch (NullPointerException e) {
                if (StkApp.DBG) {
                    CatLog.d(LOG_TAG, "NPE Invalid menu");
                }
            }
        }
        return item;
    }

    private int addStkMenuListItems() {
        String appName = mContext.getResources().getString(R.string.app_name);
        StkAppService appService = StkAppService.getInstance();
        String stkMenuTitle = null;
        String stkItemName = null;
        int simCount = TelephonyManager.from(mContext).getSimCount();
        mStkMenuList = new ArrayList<Item>();

        CatLog.d(LOG_TAG, "simCount: " + simCount);
        for (int i = 0; i < simCount; i++) {
            //Check if the card is inserted.
            if (mTm.hasIccCard(i)) {
                if (appService == null || appService.getMainMenu(i) == null) {
                    CatLog.d(LOG_TAG, "SIM " + i + " main menu of STK in the card is null");
	             //Begin modified by donghai.wu for XR7099417 on 2018/11/20
                    //continue;
                    //End modified by donghai.wu for XR7099417 on 2018/11/20
                }
                CatLog.d(LOG_TAG, "SIM " + i + " add to menu.");
                mSingleSimId = i;
		   //Begin modified by donghai.wu for XR7099417 on 2018/11/20
		   if (appService == null || appService.getMainMenu(i) == null) {
                     CatLog.d(LOG_TAG, "SIM " + i + " addStkMenuListItems is null");
		   }
		   else{
		   	  stkMenuTitle = appService.getMainMenu(i).title;
			  CatLog.d(LOG_TAG, "SIM " + i + " addStkMenuListItems not null");
		   }
		   //End modified by donghai.wu for XR7099417 on 2018/11/20
                stkItemName = new StringBuilder(stkMenuTitle == null ? appName : stkMenuTitle)
                    .append(" ").append(Integer.toString(i + 1)).toString();
                // Begin modified by donghai.wu  for XR6626104   on 2018/09/28
                //Begin added by canxuan.zeng for XR7446698 on 2018/02/19
                int subId[] = SubscriptionManager.getSubId(i);
                if (subId != null && SubscriptionManager.isValidSubscriptionId(subId[0])) {
                    SubscriptionInfo info = mSubscriptionManager
                            .getActiveSubscriptionInfo(subId[0]);
                    if (info != null) {
                        stkItemName = info.getDisplayName().toString();
                    } else {
                        CatLog.d(LOG_TAG, "SubscriptionInfo is null.");
                    }
                } else {
                    CatLog.d(LOG_TAG, "sub is null or invalid.");
                }
                //End added by canxuan.zeng for XR7446698 on 2018/02/19
                //Item item = new Item(i + 1, stkItemName, mBitMap);
                Item item = null;
                if (StkUtils.getInstance().addItemForCustomization(mContext,i) == false) {
                    item = new Item(i + 1, stkItemName, mBitMap);
                }
		   else{
                    item = new Item(i + 1, stkItemName);
                }
		   // End modified by donghai.wu  for XR6626104   on 2018/09/28
                item.id = i;
                mStkMenuList.add(item);
            } else {
                CatLog.d(LOG_TAG, "SIM " + i + " is not inserted.");
            }
        }
        if (mStkMenuList != null && mStkMenuList.size() > 0) {
            if (mStkMenuList.size() > 1) {
                StkMenuAdapter adapter = new StkMenuAdapter(this,
                        mStkMenuList, false);
		   // Begin added by donghai.wu  for XR6154319   on 2018/09/28
		   StkUtils.getInstance().adjustMenuList(mStkMenuList);
		   // End added by donghai.wu  for XR6154319   on 2018/09/28
                // Bind menu list to the new adapter.
                this.setListAdapter(adapter);
            }
            return mStkMenuList.size();
        } else {
            showTextToast(mContext, R.string.lable_sim_not_ready);
            CatLog.d(LOG_TAG, "No stk menu item add.");
            return 0;
        }
    }
    // Begin added by donghai.wu  for XR6154319    on 2018/09/28
    private boolean isStkReject(int slotId) {
        boolean isReject= false;

        String Operators_PLMNS = ResUtils.getString(mContext,"def_reject_enter_stk_plmn_list","");
        if (TextUtils.isEmpty(Operators_PLMNS)){
	     CatLog.d(LOG_TAG, "isStkReject:  empty return false" );
            return false;
        }
		
        String plmn = TelephonyManager.getDefault().getSimOperatorNumericForPhone(slotId);
	  CatLog.d(LOG_TAG, "isStkReject plmn:" + plmn+"slotId ="+slotId);
	  CatLog.d(LOG_TAG, "isStkReject Operators_PLMNS:" + Operators_PLMNS);        
         if (!TextUtils.isEmpty(plmn)&&(Operators_PLMNS.contains(plmn)))
         {
             isReject = true;
	   }
       
         CatLog.d(LOG_TAG, "isStkReject isReject = " + isReject);
         if(isReject)
	  {
              showTextToast(mContext, R.string.lable_sim_not_ready);
	   }
         return isReject;
    }
    private void showTextToast(Context context, int resId) {
        Toast toast = Toast.makeText(context, resId, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
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
             CatLog.d(LOG_TAG, "isStkAvailableservice == null" );
             showTextToast(mContext, resId);
	       return false;
        }else if (!(StkUtils.getInstance().judgeStkValid(slotId))) {
	  //End modified by donghai.wu for XR7099417 on 2018/11/05
            int simState = TelephonyManager.getDefault().getSimState(slotId);
	     CatLog.d(LOG_TAG, "slotId: " + slotId + "is not available simState:" + simState);
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

    // End added by donghai.wu  for XR6154319  on 2018/09/28
    
    private void launchSTKMainMenu(int slodId) {
        // Begin added by donghai.wu  for XR6154319 on 2018/09/28
	//Begin changed by zubin.chen.hz for XR7135244 on 2018/11/22 
        if (!isStkAllow()) {
            CatLog.d(LOG_TAG, "isStkAllow launchSTKMainMenu. return");
            finish();
            return;
        }
        //End changed by zubin.chen.hz for XR7135244 on 2018/11/22
        if (!isStkAvailable(slodId)) {
	     CatLog.d(LOG_TAG, "isStkAvailable launchSTKMainMenu. return");
            finish();
            return;
        }
	 if (isStkReject(slodId)) {
	      CatLog.d(LOG_TAG, "isStkReject launchSTKMainMenu. return");
            finish();
            return;
        }

        
	 // End added by donghai.wu  for XR6154319   on 2018/09/28
        Bundle args = new Bundle();
        CatLog.d(LOG_TAG, "launchSTKMainMenu.");
        args.putInt(StkAppService.OPCODE, StkAppService.OP_LAUNCH_APP);
        args.putInt(StkAppService.SLOT_ID
                , PhoneConstants.SIM_ID_1 + slodId);
        startService(new Intent(this, StkAppService.class)
                .putExtras(args));
    }
}
