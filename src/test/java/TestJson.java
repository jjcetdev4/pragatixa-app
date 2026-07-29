import com.fasterxml.jackson.databind.ObjectMapper;
import com.pragatix.modules.student.dto.response.StudentProgressionDto;

public class TestJson {
    public static void main(String[] args) throws Exception {
        StudentProgressionDto dto = new StudentProgressionDto();
        dto.setTotalXp(1750);
        dto.setIsMaxLevel(true);
        System.out.println(new ObjectMapper().writeValueAsString(dto));
    }
}
