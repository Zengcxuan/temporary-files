package com.android.internal.telephony;

/**
 * add by chenli.gao.hz for XR7139801 on 2018/11/30
 * Call speech codec from QC voice_service_v02.h
 * The same code should be should everywhere including qcril, ril.java
 *
 * {@hide}
 *
 */
public interface BB_TelephonySpeechCodec {
    static final int NONE       = 0;
    static final int QCELP13K   = 1;
    static final int EVRC       = 2;
    static final int EVRC_B     = 3;
    static final int EVRC_WB    = 4;
    static final int EVRC_NW    = 5;
    static final int AMR_NB     = 6;
    static final int AMR_WB     = 7;
    static final int GSM_EFR    = 8;
    static final int GSM_FR     = 9;
    static final int GSM_HR     = 10;
}

