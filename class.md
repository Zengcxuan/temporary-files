CatService extends Handler implement AppInterface

关键函数：
public static CatService getInstance(CommandsInterface ci,
handleRilMsg
handleCommand :Handles RIL_UNSOL_STK_EVENT_NOTIFY or RIL_UNSOL_STK_PROACTIVE_COMMAND command
关键属性：
mMsgDecoder RilMessageDecoder 将RIL Message解码成CommandParams Objects
mHandlerThread HandlerThread
mCmdIf CommandsInterface

RilMessage: 打包RIL层传来的消息，给decoder解码


Handler
函数：
msg.sendToTarget 等同 新建一个Message，sendMessage。 根据注释说明，前者更有效率
handlemsg

AppInterface
函数：  void onCmdResponse(CatResponseMessage resMsg);
  /*
   * Callback function from app to telephony to pass a result code and user's
   * input back to the ICC.
   */


RIL extends BaseCommands implements CommandsInterface： RilHandler  RILRequest
关键函数：
getRadioProxy
addRequest
obtainRequest
processIndication
processRequestAck ： 确定是否收到solicited命令对应的ACK
processResponse
关键属性：
mRequestList SparseArray<RILRequest>
mRadioResponse RadioResponse
mRadioIndication RadioIndication
mRadioProxy IRadio


BaseCommands：
实现CommandInterface

CommandsInterface
sendTerminalResponse
sendEnvelope





int main(int argc, char **argv) {
    // vendor ril lib path either passed in as -l parameter, or read from rild.libpath property
    const char *rilLibPath = NULL;
    // ril arguments either passed in as -- parameter, or read from rild.libargs property
    char **rilArgv;
    // handle for vendor ril lib
    void *dlHandle;
    // Pointer to ril init function in vendor ril
    const RIL_RadioFunctions *(*rilInit)(const struct RIL_Env *, int, char **);
    // Pointer to sap init function in vendor ril
    RIL_RadioFunctions *(*rilUimInit)(const struct RIL_Env *, int, char **);
    const char *err_str = NULL;

    // functions returned by ril init function in vendor ril
    const RIL_RadioFunctions *funcs;
    // lib path from rild.libpath property (if it's read)
    char libPath[PROPERTY_VALUE_MAX];
    // flat to indicate if -- parameters are present
    unsigned char hasLibArgs = 0;

    int i;
    // ril/socket id received as -c parameter, otherwise set to 0
    const char *clientId = NULL;

    RLOGD("**RIL Daemon Started**");
    RLOGD("**RILd param count=%d**", argc);

    umask(S_IRGRP | S_IWGRP | S_IXGRP | S_IROTH | S_IWOTH | S_IXOTH);
    for (i = 1; i < argc ;) {
        if (0 == strcmp(argv[i], "-l") && (argc - i > 1)) {
            rilLibPath = argv[i + 1];
            i += 2;
        } else if (0 == strcmp(argv[i], "--")) {
            i++;
            hasLibArgs = 1;
            break;
        } else if (0 == strcmp(argv[i], "-c") &&  (argc - i > 1)) {
            clientId = argv[i+1];
            i += 2;
        } else {
            usage(argv[0]);
        }
    }

    if (clientId == NULL) {
        clientId = "0";
    } else if (atoi(clientId) >= MAX_RILDS) {
        RLOGE("Max Number of rild's supported is: %d", MAX_RILDS);
        exit(0);
    }
    if (strncmp(clientId, "0", MAX_CLIENT_ID_LENGTH)) {
        strncpy(ril_service_name, ril_service_name_base, MAX_SERVICE_NAME_LENGTH);
        strncat(ril_service_name, clientId, MAX_SERVICE_NAME_LENGTH);
    }

    if (rilLibPath == NULL) {
        if ( 0 == property_get(LIB_PATH_PROPERTY, libPath, NULL)) {
            // No lib sepcified on the command line, and nothing set in props.
            // Assume "no-ril" case.
            goto done;
        } else {
            rilLibPath = libPath;
        }
    }

    dlHandle = dlopen(rilLibPath, RTLD_NOW);

    if (dlHandle == NULL) {
        RLOGE("dlopen failed: %s", dlerror());
        exit(EXIT_FAILURE);
    }

    RIL_startEventLoop();

    rilInit =
        (const RIL_RadioFunctions *(*)(const struct RIL_Env *, int, char **))
        dlsym(dlHandle, "RIL_Init");

    if (rilInit == NULL) {
        RLOGE("RIL_Init not defined or exported in %s\n", rilLibPath);
        exit(EXIT_FAILURE);
    }

    dlerror(); // Clear any previous dlerror
    rilUimInit =
        (RIL_RadioFunctions *(*)(const struct RIL_Env *, int, char **))
        dlsym(dlHandle, "RIL_SAP_Init");
    err_str = dlerror();
    if (err_str) {
        RLOGW("RIL_SAP_Init not defined or exported in %s: %s\n", rilLibPath, err_str);
    } else if (!rilUimInit) {
        RLOGW("RIL_SAP_Init defined as null in %s. SAP Not usable\n", rilLibPath);
    }

    if (hasLibArgs) {
        rilArgv = argv + i - 1;
        argc = argc -i + 1;
    } else {
        static char * newArgv[MAX_LIB_ARGS];
        static char args[PROPERTY_VALUE_MAX];
        rilArgv = newArgv;
        property_get(LIB_ARGS_PROPERTY, args, "");
        argc = make_argv(args, rilArgv);
    }

    rilArgv[argc++] = "-c";
    rilArgv[argc++] = (char*)clientId;
    RLOGD("RIL_Init argc = %d clientId = %s", argc, rilArgv[argc-1]);

    // Make sure there's a reasonable argv[0]
    rilArgv[0] = argv[0];

    funcs = rilInit(&s_rilEnv, argc, rilArgv);
    RLOGD("RIL_Init rilInit completed");

    RIL_register(funcs);

    RLOGD("RIL_Init RIL_register completed");

    if (rilUimInit) {
        RLOGD("RIL_register_socket started");
        RIL_register_socket(rilUimInit, RIL_SAP_SOCKET, argc, rilArgv);
    }

    RLOGD("RIL_register_socket completed");

done:

    rilc_thread_pool();

    RLOGD("RIL_Init starting sleep loop");
    while (true) {
        sleep(UINT32_MAX);
    }
}/

bool dispatchString(int serial, int slotId, int request, const char * str) {
    RequestInfo *pRI = android::addRequestToList(serial, slotId, request);
    if (pRI == NULL) {
        return false;
    }

    char *pString;
    if (!copyHidlStringToRil(&pString, str, pRI)) {
        return false;
    }

    CALL_ONREQUEST(request, pString, sizeof(char *), pRI, slotId);

    memsetAndFreeStrings(1, pString);
    return true;
}



RequestInfo *
addRequestToList(int serial, int slotId, int request) {
    RequestInfo *pRI;
    int ret;
    RIL_SOCKET_ID socket_id = (RIL_SOCKET_ID) slotId;
    /* Hook for current context */
    /* pendingRequestsMutextHook refer to &s_pendingRequestsMutex */
    pthread_mutex_t* pendingRequestsMutexHook = &s_pendingRequestsMutex;
    /* pendingRequestsHook refer to &s_pendingRequests */
    RequestInfo**    pendingRequestsHook = &s_pendingRequests;

    ......
	
    pRI = (RequestInfo *)calloc(1, sizeof(RequestInfo));
    if (pRI == NULL) {
        RLOGE("Memory allocation failed for request %s", requestToString(request));
        return NULL;
    }

    pRI->token = serial;
    pRI->pCI = &(s_commands[request]);
    pRI->socket_id = socket_id;

    ret = pthread_mutex_lock(pendingRequestsMutexHook);
    assert (ret == 0);

    pRI->p_next = *pendingRequestsHook;
    *pendingRequestsHook = pRI;

    ret = pthread_mutex_unlock(pendingRequestsMutexHook);
    assert (ret == 0);

    return pRI;
}





























