import org.springframework.security.crypto.bcrypt.BCrypt;

public class TestHash {
    public static void main(String[] args) {
        String hash = "$2a$10$cdJY4j3fau6WYuVt2UtfheFGukrTiVu7LooRscNfQgGOYmlKtnMna";
        System.out.println("Matches: " + BCrypt.checkpw("admin", hash));
    }
}
