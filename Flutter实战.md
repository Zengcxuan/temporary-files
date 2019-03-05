# Windows下配置Flutter

条件：git for windows，Android Studio、VS code、IDEA.

1. git clone -b master https://github.com/flutter/flutter.git
2. 进入安装的目录，打开flutter_console
3. 输入flutter doctor查看是否还需要安装其它依赖。（自动安装）
	1. 连接失败情况下尝试使用镜像网站：添加如下环境变量 PUB_HOSTED_URL=https://pub.flutter-io.cn  FLUTTER_STORAGE_BASE_URL=https://storage.flutter-io.cn

## Flutter与Dart插件：
  Andorid Studio插件里直接搜索。

## 实现cmd中运行flutter命令：
  将Flutter的bin路径添加至环境变量Path。
  
## Hello World
  Android Studio新建Flutter项目。


Today we don’t support for 3D via OpenGL ES or similar. We have long-term plans to expose an optimized 3D API, but right now we’re focused on 2D.

