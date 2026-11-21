package socket.client;

//用户资料：昵称、密码、年龄、性别
public class UserData {

    private String name;
    private String password;
    private int age;
    private String gender;

    public UserData(String name, String password, int age, String gender) {
        this.name = name;
        this.password = password;
        this.age = age;
        this.gender = gender;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }
}
