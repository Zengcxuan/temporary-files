package com.android.stk;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.content.Intent;
import android.provider.Settings;
import android.telephony.Rlog;
import android.text.TextUtils;
import android.util.ResUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;
import android.content.pm.PackageManager;
import android.content.ComponentName;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.Selection;
import android.widget.EditText;

import com.android.internal.telephony.cat.CatCmdMessage;
import com.android.internal.telephony.cat.TextMessage;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.cat.AppInterface;
import com.android.internal.telephony.cat.Item;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.cat.Input;
import com.android.internal.telephony.cat.ToneSettings;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.cat.CatService;
import com.android.internal.telephony.cat.CatResponseMessage;
import com.android.internal.telephony.cat.ResultCode;
// Begin added by donghai.wu  for XR6154319  on 2018/10/20
import com.tcl.plugin.TclPluginManager;
// End added by donghai.wu  for XR6154319  on 2018/10/20

public class StkUtils {

    private final static String TAG = "StkUtils";
    private String Operator_PLMNS = ResUtils.getString("def_stk_menu_for_operators_list", "");
    private boolean isOperatorinSim1 = false;
    private boolean isOperatorinSim2 = false;
    private final int EVENT_QUERY_MENU_TITLE_DONE = 60;
    private boolean isContainStkMenu = false;
    private boolean hasSetCommonIcon = false;
    private static StkUtils instance = null;

    public static StkUtils getInstance() {
        if (instance == null) {
            instance = new StkUtils();
        }
        return instance;

    }

    //Begin added by zubin.chen.hz for XR7135244 on 2018/11/22
    public void initStkService(Context context) {
        if (mNotificationManager == null || mPowerManager == null || km == null) {
            mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        }
    }
    //End added by zubin.chen.hz for XR7135244 on 2018/11/22

    private boolean containOperatorsSim() {

        CatLog.d(TAG, "enter containOperatorsSim  ");
        boolean containOperatorsSim = false;
        String Operators_PLMNS = ResUtils.getString("def_stk_menu_for_operators_list", "");
        if (TextUtils.isEmpty(Operators_PLMNS)) {
            CatLog.d(TAG, "containOperatorsSim  empty");
            return false;
        }
        String[] PLMNS = Operators_PLMNS.split(",");

        String operatorNumeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC);
        CatLog.d(TAG, "operatorNumeric: " + operatorNumeric);

        for (String plmn : PLMNS) {
            if ((null != operatorNumeric && operatorNumeric.contains(plmn))) {
                CatLog.d(TAG, "has a Operator Sim in phone");
                containOperatorsSim = true;
                break;
            }
        }
        CatLog.d(TAG, "containOperatorSim = " + containOperatorsSim);

        return containOperatorsSim;
    }

    //Begin added by jiayi.wang for XR6015843 on 2018/03/29
    public boolean isContainStkMenu() {
        return isContainStkMenu;
    }
    //End added by jiayi.wang for XR6015843 on 2018/03/29

    public boolean hideSmsToast(int cmdType, String text) {
        CatLog.d(TAG, "enter hideSmsToast  ");
        boolean hideToast = ResUtils.getBoolean("def_stk_send_sms_toast_default_hide", false);
        if (hideToast) {
            CatLog.d(TAG, "text = " + text + ", typeOfCommand = " + cmdType);
            if (cmdType == AppInterface.CommandType.SEND_SMS.value()) {
                return true;
            }
        }
        return false;
    }


    private void showTextToast(Context context, int resId) {
        Toast toast = Toast.makeText(context, resId, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }

    /*public static boolean isStkAllow(Context mContext) {
        boolean isAllow = false;
        CatLog.d(TAG, "enter isStkAllow");
        String plmns = ResUtils.getString("def_allow_enter_stk_plmn_list", "");
        if (TextUtils.isEmpty(plmns)) {
            CatLog.d(TAG, "isStkAllow:  empty return true");
            return true;
        }
        String[] PLMNS = plmns.split(",");

        String operatorNumeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC);
        CatLog.d(TAG, "isStkAllow operatorNumeric: " + operatorNumeric);

        for (String plmn : PLMNS) {
            if ((null != operatorNumeric && operatorNumeric.contains(plmn))) {
                CatLog.d(TAG, "isStkAllow has a Operator Sim in phone");
                isAllow = true;
                break;
            }
        }
        CatLog.d(TAG, "isStkAllow containOperatorSim = " + isAllow);

        if (isAllow == false) {
            showTextToast(mContext, R.string.lable_sim_not_allow_enter);
        }

        return isAllow;
    }*/

    public String getDefaultPlmn(Context mContext) {
        boolean titleDisplay = ResUtils.getBoolean("def_stk_display_default_idle_title", false);
        CatLog.d(TAG, " enter getDefaultPlmn default idle mode text title display:" + titleDisplay);
        if (titleDisplay != true) {
            return "";
        }
        return mContext.getResources().getString(
                com.android.internal.R.string.lockscreen_carrier_default);
    }

    private boolean isOperatorSIM(int slotId) {
        String operatorList = ResUtils.getString("def_show_stk_menu_for_operators_list", "");
        String plmn = TelephonyManager.getDefault().getSimOperatorNumericForPhone(slotId);

        if (TextUtils.isEmpty(operatorList)) {
            CatLog.d(TAG, "def_show_stk_menu_for_operators_list is empty");
            return true;
        }
        if (!TextUtils.isEmpty(plmn) && (operatorList.contains(plmn))) {
            CatLog.d(TAG, "isOperatorSIM true, slot: " + slotId);
            return true;
        }

        CatLog.d(TAG, "isOperatorSIM false, slot: " + slotId);
        return false;
    }

    public boolean judgeAddSTKItem(Context mContext, int slotId) {
        CatLog.d(TAG, "enter judgeAddSTKItem ");

        if (isOperatorSIM(slotId)) {
            if (ResUtils.getBoolean("def_stk_install_when_simcard_has_stkmenu", false)) {
                if (TclPluginManager.getTclStk(mContext).stkGetSaveNewSetUpMenuFlag(slotId)) {
                    CatLog.d(TAG, "enter judgeAddSTKItem   return true ");
                    return true;
                }
            } else {
                CatLog.d(TAG, "enter judgeAddSTKItem   return true 1");
                return true;
            }
        }
        CatLog.d(TAG, "enter judgeAddSTKItem   return false ");
        return false;
    }

    // Begin added by donghai.wu  for XR6154319  on 2018/10/20
    public boolean judgeStkValid(int slotId) {
        if (StkAppService.getInstance() != null) {
            return StkAppService.getInstance().isHasMaincmd(slotId);
        } else {
            CatLog.d(TAG, "judgeStkValid  return false ");
            return false;
        }
    }
    // End added by donghai.wu  for XR6154319  on 2018/10/20

    public void adjustMenuList(Object mStkMenuList) {
        CatLog.d(TAG, " enter adjustMenuList ");
        if (!isOperatorinSim1 && isOperatorinSim2) {
            ArrayList<Item> newmStkMenuList = (ArrayList<Item>) mStkMenuList;
            Item item0 = newmStkMenuList.remove(0);
            newmStkMenuList.add(item0);
        }

        isOperatorinSim1 = false;
        isOperatorinSim2 = false;
    }

    public boolean addItemForCustomization(Context mContext, int id) {
        boolean isCustomization = ResUtils.getBoolean(mContext, "def_stk_icon_name_for_operators", false);
        CatLog.d(TAG, " enter addItemForCustomization Operator_PLMNS:" + Operator_PLMNS + " id =" + id + "isCustomization =" + isCustomization);
        if (TextUtils.isEmpty(Operator_PLMNS)) {
            return false;
        } else {
            if (containOperatorsSim()) {
                if (judgeOperatorSim(id)) {
                    if (id == 0) {
                        isOperatorinSim1 = true;
                        CatLog.d(TAG, "isOperatorinSim 1 = true");
                    } else if (id == 1) {
                        isOperatorinSim2 = true;
                        CatLog.d(TAG, "isOperatorinSim 2 = true");
                    }
                }
                return true;
            }
            return false;
        }
    }

    private boolean judgeOperatorSim(int id) {
        CatLog.d(TAG, "enter judgeOperatorSim is process ");
        boolean containOperatorSim = false;

        String plmn = TelephonyManager.getDefault().getSimOperatorNumericForPhone(id);
        CatLog.d(TAG, "judgeOperatorSim plmn: " + plmn);
        if (!TextUtils.isEmpty(plmn) && (Operator_PLMNS.contains(plmn))) {
            containOperatorSim = true;
        }

        CatLog.d(TAG, "judgeOperatorSim  containOperatorSim = " + containOperatorSim);
        return containOperatorSim;
    }

    public void installStk(Context mContext) {
        CatLog.d(TAG, "enter installStk process_id:" + android.os.Process.myPid());

        if (true == ResUtils.getBoolean("def_stk_install_when_simcard_has_stkmenu", false)) {
            CatLog.d(TAG, "installStk the simcard has stk menu,install stk");
            StkAppInstaller.install(mContext);
        }
    }

    public void unInstallStkNoSupportStk(Context mContext) {
        CatLog.d(TAG, "enter unInstallStkNoSupportStk process_id :" + android.os.Process.myPid());

        if (true == ResUtils.getBoolean("def_stk_install_when_simcard_has_stkmenu", false)) {
            int simCount = TelephonyManager.from(mContext).getSimCount();
            CatLog.d(TAG, "unInstallStkNoSupportStk  def_stk_install_when_simcard_has_stkmenu == true simCount:" + simCount);
            if (simCount == 2) {
                if (!TclPluginManager.getTclStk(mContext).stkGetSaveNewSetUpMenuFlag(PhoneConstants.SIM_ID_1)
                        && !TclPluginManager.getTclStk(mContext).stkGetSaveNewSetUpMenuFlag(PhoneConstants.SIM_ID_2)) {
                    CatLog.d(TAG, " dual sim onStart sim phone bootup no stk menu unintall stk");
                    StkAppInstaller.unInstall(mContext);
                }
            } else if (simCount == 1) {
                if (!TclPluginManager.getTclStk(mContext).stkGetSaveNewSetUpMenuFlag(PhoneConstants.SIM_ID_1)) {
                    CatLog.d(TAG, "onStart single sim phone bootup no stk menu unintall stk");
                    StkAppInstaller.unInstall(mContext);
                }
            }
        }

    }

    public void unInstallStk(Context mContext) {
        CatLog.d(TAG, "enter unInstallStk  process_id:" + android.os.Process.myPid());

        if (true == ResUtils.getBoolean("def_stk_install_when_simcard_has_stkmenu", false)) {
            CatLog.d(TAG, "unInstallStk All Stk menu are removed, uninstall stk");
            StkAppInstaller.unInstall(mContext);
        }
    }


    public void installStkInit(Context mContext) {
        CatLog.d(TAG, "enter installStkInit  process_id:" + android.os.Process.myPid());

        boolean isCustomization = ResUtils.getBoolean(mContext, "def_stk_icon_name_for_operators", false);
        if (isCustomization && hasSetCommonIcon) {
            CatLog.d(TAG, "installStkInit hasSetCommonIcon = true");
            return;
        }
        if (false == ResUtils.getBoolean("def_stk_install_when_simcard_has_stkmenu", false)) {
            CatLog.d(TAG, "installStkInit ");
            StkAppInstaller.install(mContext);
            SystemClock.sleep(100);
        }
    }

    public void setInputComponent(Object mStkInput, Object mTextIn, Object numOfCharsView, Object inTypeView, int maxLen) {
        CatLog.d(TAG, "enter setInputComponent ");

        if (mStkInput == null) {
            return;
        }
        Input stkInput = (Input) mStkInput;
        EditText textIn = (EditText) mTextIn;

        if (stkInput.defaultText != null) {
            if (stkInput.defaultText.length() > maxLen) {
                textIn.setText(stkInput.defaultText.substring(0, maxLen));
            } else {
                textIn.setText(stkInput.defaultText);
            }
            textIn.setSelection(textIn.getText().length());
        }

        if ((stkInput.digitOnly) && (!stkInput.echo)) {
            StkDigitsKeyListener2 digitsKeyListener2 = StkDigitsKeyListener2.getInstance();
            CatLog.d(TAG, "setInputType setKeyListener2");
            Selection.setSelection(textIn.getText(), textIn.getText().length());
            textIn.setKeyListener(digitsKeyListener2);
        }

        if (SystemProperties.getInt("RO_OPERATOR_REQ", 0x00) == 0x03) {
            ((TextView) numOfCharsView).setVisibility(View.GONE);
            ((TextView) inTypeView).setVisibility(View.GONE);
        }
    }


    public Object getNotificationIntent(Context mContext, Object notificationIntent, int slotId, Object msg, String mTitle) {
        CatLog.d(TAG, "enter getNotificationIntent ");
        TextMessage message = (TextMessage) msg;
        Intent newIntent = new Intent();
        newIntent.setAction("tcl.intent.action.stknotification");
        newIntent.putExtra("TEXT", message);
        newIntent.putExtra("TITLE", mTitle);

        return newIntent;
    }

    public boolean isShowUser(boolean showUser, Object mCurrentCmd) {
        ToneSettings settings = ((CatCmdMessage) mCurrentCmd).getToneSettings();
        TextMessage toneMsg = ((CatCmdMessage) mCurrentCmd).geTextMessage();

        CatLog.d(TAG, "enter isShowUser ");
        if (settings.tone == null && !settings.vibrate &&
                ((null == toneMsg) || (null == toneMsg.text) || (toneMsg.text.equals("")))) {
            return false;
        } else {
            return showUser;
        }
    }

    //Begin added by yangning.hong.hz for XR7159728 on 2018/12/06
    public boolean isSetStkCommonIcon() {
        boolean isCustomization = ResUtils.getBoolean("def_stk_icon_name_for_operators", false);
        boolean isInstallwhenStkMenu = ResUtils.getBoolean("def_stk_install_when_simcard_has_stkmenu", false);
        boolean ret = false;

        if (isCustomization && !isInstallwhenStkMenu) {
            CatLog.d(TAG, "isSetStkCommonIcon is icon name Customization");
            if (containOperatorsSim()) {
                ret = false;
            } else {
                ret = true;
            }
        }

        CatLog.d(TAG, "isSetStkCommonIcon =" + ret);
        return ret;
    }
    //End added by yangning.hong.hz for XR7159728 on 2018/12/06

    public void setStkCommonIcon(Context mContext) {
        boolean isCustomization = ResUtils.getBoolean(mContext, "def_stk_icon_name_for_operators", false);
        boolean isInstallwhenStkMenu = ResUtils.getBoolean(mContext, "def_stk_install_when_simcard_has_stkmenu", false);
        CatLog.d(TAG, "enter setStkCommonIcon isCustomization =" + isCustomization);
        ComponentName cNameCustomLauncher = new ComponentName("com.android.stk", "com.android.stk.StkCustomLauncherActivity");
        PackageManager pm = mContext.getPackageManager();
        if (isCustomization && !isInstallwhenStkMenu) {
            if (containOperatorsSim()) {
                try {
                    pm.setComponentEnabledSetting(cNameCustomLauncher, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    CatLog.d(TAG, "setComponentEnabled StkCustomLauncherActivity DISABLED");
                } catch (Exception e) {
                    CatLog.d(TAG, "setComponentEnabled DISABLED StkCustomLauncherActivity fail" + e);
                }
                SystemClock.sleep(2000);
                StkAppInstaller.install(mContext);
            } else {
                StkAppInstaller.unInstall(mContext);
                try {
                    pm.setComponentEnabledSetting(cNameCustomLauncher, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                    CatLog.d(TAG, "setComponentEnabled StkCustomLauncherActivity ENABLED");
                } catch (Exception e) {
                    CatLog.d(TAG, "setComponentEnabled ENABLED StkCustomLauncherActivity fail" + e);
                }
            }
        }
    }

    public void setStkNoSimIcon(Context mContext) {
        boolean isCustomization = ResUtils.getBoolean(mContext, "def_stk_icon_name_for_operators", false);
        boolean isInstallwhenStkMenu = ResUtils.getBoolean(mContext, "def_stk_install_when_simcard_has_stkmenu", false);
        int simCount = TelephonyManager.from(mContext).getSimCount();
        TelephonyManager mTm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        ComponentName cNameCustomLauncher = new ComponentName("com.android.stk", "com.android.stk.StkCustomLauncherActivity");
        PackageManager pm = mContext.getPackageManager();
        CatLog.d(TAG, "enter setStkNoSimIcon isCustomization =" + isCustomization);
        CatLog.d(TAG, "enter setStkNoSimIcon isInstallwhenStkMenu =" + isInstallwhenStkMenu);
        boolean noSim = true;
        if (isCustomization && !isInstallwhenStkMenu) {
            for (int i = 0; i < simCount; i++) {
                if (mTm.hasIccCard(i)) {
                    noSim = false;
                }
            }
            CatLog.d(TAG, "setStkNoSimIcon noSim = " + noSim);
            //Begin modified by yangning.hong.hz for XR7159728 on 2018/12/06
            //In some case: the broadcast ACTION_SIM_STATE_CHANGED is faster than ACTION_BOOT_COMPLETED, it will show two stk icon.
            if (noSim || !containOperatorsSim()) {
            //End modified by yangning.hong.hz for XR7159728 on 2018/12/06
                StkAppInstaller.unInstall(mContext);
                hasSetCommonIcon = true;
                try {
                    pm.setComponentEnabledSetting(cNameCustomLauncher, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                    CatLog.d(TAG, "setStkNoSimIcon StkCustomLauncherActivity ENABLED");
                } catch (Exception e) {
                    CatLog.d(TAG, "setStkNoSimIcon ENABLED StkCustomLauncherActivity fail" + e);
                }
            }
        }
    }

    private PowerManager mPowerManager = null;
    private KeyguardManager km = null;
    private NotificationManager mNotificationManager = null;

    private static final String className = new Object() {
    }.getClass().getEnclosingClass().getName();
    private static final String LOG_TAG = className.substring(className.lastIndexOf('.') + 1);

    private static final int TEXT_DIALOG_NOTIFY_ID = 555;
    private static final int INPUT_ACTIVITY_NOTIFY_ID = 666;
    private static final int UI_TIMEOUT = (120 * 1000);

    private int inputNotificationId = 0;
    private int dialogNotificationId = 0;
    private int cancelButtonId = 0;
    private boolean isInputCancel = false;
    private Context stkContextWrapper = null;
    private final String ORANGE_ONLY_NOTIFICATION_CHANNEL_ID = "com.tcl.telephony.ORANGE_ONLY_NOTIFICATION";


    public boolean isOrangeRequest() {
        if (SystemProperties.getInt("RO_OPERATOR_REQ", 0x00) == 0x03) {
            CatLog.d(TAG, "enter isOrangeRequest true");
            return true;
        } else {
            return false;
        }
    }

    public boolean judgeIsStk(Object cn) {
        if (SystemProperties.getInt("RO_OPERATOR_REQ", 0x00) != 0x03) {
            return false;
        }
        ComponentName mCn = (ComponentName) cn;
        CatLog.d(TAG, "enter judgeIsStk ");

        if (mCn != null && mCn.getClassName() != null
                && (mCn.getClassName().equals("com.android.stk.StkDialogActivity")
                || mCn.getClassName().equals("com.android.stk.StkInputActivity"))
                && mCn.getPackageName() != null
                && mCn.getPackageName().equals("com.android.stk")) {
            return true;
        }
        return false;
    }

    public void beforeLaunchInputActivity(Context mContext, Object lastSelectedItem, Object mMainCmd, Object mCurrentCmd, int slotId) {
        //Begin modified by canxuan.zeng for XR7143706 on 2018/11/30
        if ((SystemProperties.getInt("RO_OPERATOR_REQ", 0x00) != 0x03) &&
                !ResUtils.getBoolean("def_stk_lock_notification", false)) {
            return;
        }
        //End modified by canxuan.zeng for XR7143706 on 2018/11/30
        CatLog.d(TAG, "enter beforeLaunchInputActivity ");

        mNotificationManager.createNotificationChannel(new NotificationChannel(ORANGE_ONLY_NOTIFICATION_CHANNEL_ID,
                "ORANGE-ONLY", NotificationManager.IMPORTANCE_LOW));

        if (!mPowerManager.isScreenOn() || km.inKeyguardRestrictedInputMode()) {
            CatLog.d(TAG, "screen is off or lock");
            String title = "";
            if (lastSelectedItem != null) {
                title = (String) lastSelectedItem;
            } else if (mMainCmd != null) {
                title = ((CatCmdMessage) mMainCmd).getMenu().title;
            }
            Notification notification = new Notification.Builder(mContext)
                    .setWhen(System.currentTimeMillis())
                    .setAutoCancel(true)
                    .setSmallIcon(com.android.internal.R.drawable.stat_notify_sim_toolkit)
                    .setTicker(title)
                    .setColor(mContext.getResources().getColor(
                            com.android.internal.R.color.system_notification_accent_color))
                    .setContentTitle(title)
                    .setContentText(((CatCmdMessage) mCurrentCmd).geInput().text)
                    .setChannel(ORANGE_ONLY_NOTIFICATION_CHANNEL_ID)
                    .build();
            mNotificationManager.notify(INPUT_ACTIVITY_NOTIFY_ID + slotId, notification);
            inputNotificationId = INPUT_ACTIVITY_NOTIFY_ID + slotId;
            wakeHandset(mContext);
        }
    }

    public void beforeLaunchDialogActivity(Context mContext, Object mCurrentCmd, int slotId) {
        //Begin modified by canxuan.zeng for XR7143706 on 2018/11/30
        if ((SystemProperties.getInt("RO_OPERATOR_REQ", 0x00) != 0x03) &&
                !ResUtils.getBoolean("def_stk_lock_notification", false)) {
            return;
        }
        //End modified by canxuan.zeng for XR7143706 on 2018/11/30
        CatLog.d(TAG, "enter beforeLaunchDialogActivity ");

        mNotificationManager.createNotificationChannel(new NotificationChannel(ORANGE_ONLY_NOTIFICATION_CHANNEL_ID,
                "ORANGE-ONLY", NotificationManager.IMPORTANCE_LOW));

        if (!mPowerManager.isScreenOn() || km.inKeyguardRestrictedInputMode()) {
            TextMessage msg = ((CatCmdMessage) mCurrentCmd).geTextMessage();
            if (msg != null && mNotificationManager != null) {
                Notification notification = new Notification.Builder(mContext)
                        .setWhen(System.currentTimeMillis())
                        .setAutoCancel(true)
                        .setSmallIcon(com.android.internal.R.drawable.stat_notify_sim_toolkit)
                        .setTicker(msg.title)
                        .setColor(mContext.getResources().getColor(
                                com.android.internal.R.color.system_notification_accent_color))
                        .setContentTitle(msg.title)
                        .setContentText(msg.text)
                        .setChannel(ORANGE_ONLY_NOTIFICATION_CHANNEL_ID)
                        .build();
                mNotificationManager.notify(TEXT_DIALOG_NOTIFY_ID + slotId, notification);
                dialogNotificationId = TEXT_DIALOG_NOTIFY_ID + slotId;
                wakeHandset(mContext);
            }
        }
    }


    public void cancelNotification(String type) {
        if (SystemProperties.getInt("RO_OPERATOR_REQ", 0x00) == 0x03) {
            CatLog.d(TAG, "enter cancelNotification ");

            if (type.equals("input")) {
                mNotificationManager.cancel(inputNotificationId);
            } else if (type.equals("dialog")) {
                mNotificationManager.cancel(dialogNotificationId);
            }
        }
    }

    public void setOrangeInputComponent(View.OnClickListener listener, int buttonId, Object mbutton, Object actionBar) {
        if (SystemProperties.getInt("RO_OPERATOR_REQ", 0x00) == 0x03) {

            CatLog.d(TAG, "enter setOrangeInputComponent ");


            Button cancelButton = (Button) mbutton;

            cancelButtonId = buttonId;
            cancelButton.setVisibility(View.VISIBLE);
            cancelButton.setOnClickListener(listener);

            ((ActionBar) actionBar).hide();
        }
    }


    public void inputSetResultCode(Object resMsg) {
        if (SystemProperties.getInt("RO_OPERATOR_REQ", 0x00) == 0x03) {
            CatResponseMessage inputResMsg = (CatResponseMessage) resMsg;
            CatLog.d(TAG, "enter inputSetResultCode ");

            inputResMsg.setResultCode(isInputCancel ? ResultCode.UICC_SESSION_TERM_BY_USER
                    : ResultCode.OK);
            isInputCancel = false;
        }
    }

    public void setOrangeDialog(Object builder, Object mTextMsg) {
        if (SystemProperties.getInt("RO_OPERATOR_REQ", 0x00) == 0x03) {
            CatLog.d(TAG, "enter setOrangeDialog ");
            boolean immediate = !(((TextMessage) mTextMsg).responseNeeded);

            ((AlertDialog.Builder) builder).setTitle("");
            if (immediate) {
                ((AlertDialog.Builder) builder).setNegativeButton("", null);
            }
        }
    }

    public void setCancelFlag(int buttonId) {
        if (SystemProperties.getInt("RO_OPERATOR_REQ", 0x00) == 0x03) {
            CatLog.d(TAG, "enter sendResponse ");

            if (buttonId == cancelButtonId) {
                isInputCancel = true;
            } else {
                isInputCancel = false;
            }
        }
    }


    private void wakeHandset(Context mContext) {
        CatLog.d(TAG, "enter wakeHandset ");
        PowerManager.WakeLock wl = null;
        wl = mPowerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE | PowerManager.FULL_WAKE_LOCK, LOG_TAG);
        long wakeUpTime = 0;
        CatLog.d(TAG, "wake Up Screen and notification");
        try {
            ContentResolver cr = mContext.getContentResolver();
            wakeUpTime = android.provider.Settings.System.getInt(cr, Settings.System.SCREEN_OFF_TIMEOUT);
        } catch (Settings.SettingNotFoundException e) {
            CatLog.d(TAG, "Exception occured in wakeupScreen()");
        }
        wl.acquire(wakeUpTime);
    }

}


