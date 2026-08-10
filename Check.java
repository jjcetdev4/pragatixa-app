import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Check {
    public static void main(String[] args) {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        String[] tests = {"admin123", "admin@123", "123456", "12345678", "head", "head123"};
        for (String t : tests) {
            if (enc.matches(t, "$2a$10$.hmdKgDCFmgor6pYE5aL8O2i9jQc2YkOos0gdmB36uJewVxzxF9Vu")) {
                System.out.println("FOUND: " + t);
            }
        }
    }
}
