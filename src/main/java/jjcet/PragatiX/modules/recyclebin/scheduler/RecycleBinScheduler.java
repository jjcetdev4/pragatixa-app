package jjcet.PragatiX.modules.recyclebin.scheduler;

import jjcet.PragatiX.modules.recyclebin.service.RecycleBinService;
import jjcet.PragatiX.modules.recyclebin.dto.RecycleBinItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class RecycleBinScheduler {
    private static final Logger log = LoggerFactory.getLogger(RecycleBinScheduler.class);
    
    private final RecycleBinService recycleBinService;
    
    public RecycleBinScheduler(RecycleBinService recycleBinService) {
        this.recycleBinService = recycleBinService;
    }

    // Runs every day at 2:00 AM
    @Scheduled(cron = "0 0 2 * * *")
    public void purgeExpiredItems() {
        log.info("Starting Recycle Bin auto-purge job...");
        try {
            List<RecycleBinItem> allItems = recycleBinService.getDeletedItems();
            LocalDateTime now = LocalDateTime.now();
            int purgedCount = 0;
            
            for (RecycleBinItem item : allItems) {
                if (item.getPermanentDeleteAt() != null && item.getPermanentDeleteAt().isBefore(now)) {
                    log.info("Auto-purging expired {} with ID: {}", item.getEntityType(), item.getId());
                    recycleBinService.permanentlyDeleteItem(item.getEntityType(), item.getId());
                    purgedCount++;
                }
            }
            
            log.info("Recycle Bin auto-purge job completed. Purged {} items.", purgedCount);
        } catch (Exception e) {
            log.error("Error during Recycle Bin auto-purge job", e);
        }
    }
}
