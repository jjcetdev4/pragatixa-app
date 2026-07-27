import com.fasterxml.jackson.databind.ObjectMapper;

public class test_json {
    public static class PenaltyRequestDto {
        private int penaltyXP;
        public int getPenaltyXP() { return penaltyXP; }
        public void setPenaltyXP(int penaltyXP) { this.penaltyXP = penaltyXP; }
    }
    public static void main(String[] args) throws Exception {
        PenaltyRequestDto dto = new PenaltyRequestDto();
        dto.setPenaltyXP(40);
        System.out.println(new ObjectMapper().writeValueAsString(dto));
    }
}
