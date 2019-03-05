下载Flutter
git clone -b master https://github.com/flutter/flutter.git
进入安装的目录，打开flutter_console
输入flutter doctor查看是否还需要安装其它依赖。（自动安装，可能连接不上服务器）
export PUB_HOSTED_URL=https://pub.flutter-io.cn
export FLUTTER_STORAGE_BASE_URL=https://storage.flutter-io.cn

https://www.dart-china.org/

private void refreshNotification() {
   //获取NotificationManager实例
   NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
   //实例化NotificationCompat.Builde并设置相关属性
   NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
           //设置小图标
           .setSmallIcon(R.mipmap.icon_fab_repair)
           //设置通知标题
           .setContentTitle("最简单的Notification")
           //设置通知内容
           .setContentText("只有小图标、标题、内容")
           //设置通知时间，默认为系统发出通知的时间，通常不用设置
           //.setWhen(System.currentTimeMillis());
   //通过builder.build()方法生成Notification对象,并发送通知,id=1
   notifyManager.notify(1, builder.build());
