# Socket + 多线程聊天室

对应笔记：《Java网络编程笔记：Socket与多线程聊天室》（2026-11-04）

服务端用线程池给每个连接开一条线程，`UserManager` 维护在线用户并负责广播；
客户端提供命令行和图形两种实现。

## 文件

| 文件 | 行数 | 作用 |
| --- | --- | --- |
| `SimpleServer.java` | 33 | 服务端入口：`ServerSocket` 监听 8888，每条连接交给线程池 |
| `ClientHandler.java` | 123 | 每个客户端一条线程：读消息、转给 `UserManager` 广播 |
| `UserManager.java` | 49 | 注册 / 登录校验、在线用户表；自带一个注册登录的测试 `main` |
| `UserData.java` | 24 | 用户资料：名字、密码、年龄、性别 |
| `SimpleClient.java` | 105 | 命令行客户端：收、发各一条线程 |
| `LoginFrame.java` | 49 | 图形客户端登录窗口 |
| `ChatFrame.java` | 101 | 图形客户端聊天窗口，收发线程用匿名内部类 |
| `UIListener.java` | 77 | 图形客户端的事件监听：登录、发消息、退出 |

## 运行

全部类都在 `package socket`，先编译：

```
javac -encoding UTF-8 -d out src/socket/*.java
```

然后按需要启动：

```
java -cp out socket.SimpleServer    # 1. 先起服务端
java -cp out socket.SimpleClient    # 2. 命令行客户端
java -cp out socket.LoginFrame      # 3. 图形客户端（登录窗口）
```

端口默认 `8888`、主机默认 `127.0.0.1`，写在各文件的字段或 `main` 里，按需修改。
