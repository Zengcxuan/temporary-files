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
