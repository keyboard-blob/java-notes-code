package socket;

import java.util.HashMap;
import java.util.Map;

public class UserManager {
    private Map<String, UserData> userMap = new HashMap<>();

    public boolean register(UserData user) {
        if (userMap.containsKey(user.getName())) {
            return false;
        }
        userMap.put(user.getName(), user);
        return true;
    }

    public UserData login(String name, String password) {
        UserData user = userMap.get(name);
        if (user == null) return null;
        if (user.getPassword().equals(password)) return user;
        return null;
    }

    public boolean isNameTaken(String name) {
        return userMap.containsKey(name);
    }

    public static void main(String[] args) {
        UserManager manager = new UserManager();

        System.out.println("=== 注册测试 ===");
        UserData u1 = new UserData("张三", "123456", 20, "男");
        System.out.println("首次注册张三：" + manager.register(u1));
        UserData u2 = new UserData("张三", "654321", 22, "女");
        System.out.println("同名再注册：" + manager.register(u2));

        System.out.println("=== 登录测试 ===");
        UserData ok = manager.login("张三", "123456");
        System.out.println("正确密码登录：" + (ok != null ? "成功，" + ok.getName() : "失败"));
        UserData bad = manager.login("张三", "wrong");
        System.out.println("错误密码登录：" + (bad != null ? "成功" : "失败"));
        UserData noUser = manager.login("李四", "123456");
        System.out.println("不存在用户登录：" + (noUser != null ? "成功" : "失败"));

        System.out.println("=== isNameTaken 测试 ===");
        System.out.println("注册前查张三：" + manager.isNameTaken("张三"));
        System.out.println("查李四：" + manager.isNameTaken("李四"));
    }
}
