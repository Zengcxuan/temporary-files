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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.android.internal.telephony.cat.CatLog;

/**
 * Boot completed receiver. used to reset the app install state every time the
 * device boots.
 *
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = new Object(){}.getClass().getEnclosingClass().getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        // make sure the app icon is removed every time the device boots.
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Bundle args = new Bundle();
            args.putInt(StkAppService.OPCODE, StkAppService.OP_BOOT_COMPLETED);
            context.startService(new Intent(context, StkAppService.class)
                    .putExtras(args));
            CatLog.d(LOG_TAG, "[ACTION_BOOT_COMPLETED]");
	      // Begin added by donghai.wu  for XR6154319    on 2018/09/28
	      StkAppInstaller.install(context);    
	      // End added by donghai.wu  for XR6154319    on 2018/09/28
	      // Begin added by donghai.wu  for XR6625870   on 2018/09/28
	      StkUtils.getInstance().unInstallStkNoSupportStk(context);   
		// End added by donghai.wu  for XR6625870  on 2018/09/28
	      // begin added by donghai.wu  for XR6626327  on 2018/09/28
            StkUtils.getInstance().setStkNoSimIcon(context);
	      //End added by donghai.wu  for XR6626327   on 2018/09/28
        } else if(action.equals(Intent.ACTION_USER_INITIALIZE)) {
            // TODO: http://b/25155491
            if (!android.os.Process.myUserHandle().isSystem()) {
                //Disable package for all secondary users. Package is only required for device
                //owner.
                context.getPackageManager().setApplicationEnabledSetting(context.getPackageName(),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
                return;
            }
        }
    }
}
