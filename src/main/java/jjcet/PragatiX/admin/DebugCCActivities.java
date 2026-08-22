package jjcet.PragatiX.admin;

import jjcet.PragatiX.entity.Activity;
import jjcet.PragatiX.modules.cc.service.CCActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import jjcet.PragatiX.common.response.ApiResponse;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/debug-cc-activities")
public class DebugCCActivities {
    
    @Autowired
    private CCActivityService ccActivityService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Activity>>> debug() {
        // simulate cc_test for stage 27 and subgroup 'Must'
        return ccActivityService.getActiveActivities("cc_test", 27L, "Must");
    }
}
