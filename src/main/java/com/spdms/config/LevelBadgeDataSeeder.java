package com.spdms.config;

import com.spdms.entity.Badge;
import com.spdms.entity.Level;
import com.spdms.repository.BadgeRepository;
import com.spdms.repository.LevelRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class LevelBadgeDataSeeder implements CommandLineRunner {

    private final LevelRepository levelRepository;
    private final BadgeRepository badgeRepository;

    public LevelBadgeDataSeeder(LevelRepository levelRepository, BadgeRepository badgeRepository) {
        this.levelRepository = levelRepository;
        this.badgeRepository = badgeRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (levelRepository.count() == 0) {
            seedLevels();
        }
        if (badgeRepository.count() == 0) {
            seedBadges();
        }
    }

    private void seedLevels() {
        levelRepository.saveAll(List.of(
            Level.builder().levelNumber(1).title("Explorer").xpMin(0).xpMax(100).stage(1)
                .primaryObjective("Build participation habits")
                .keyUnlocks("Onboarding missions, basic badges, attend all sessions").build(),
            Level.builder().levelNumber(2).title("Builder").xpMin(101).xpMax(500).stage(1)
                .primaryObjective("Develop consistency & discipline")
                .keyUnlocks("Study groups, quiz battles, attendance streaks").build(),
            Level.builder().levelNumber(3).title("Innovator").xpMin(501).xpMax(1500).stage(2)
                .primaryObjective("Build technical & collaborative skills")
                .keyUnlocks("Skill pathways unlocked, mini-projects, peer collaboration").build(),
            Level.builder().levelNumber(4).title("Specialist").xpMin(1501).xpMax(3000).stage(2)
                .primaryObjective("Demonstrate competency & peer support")
                .keyUnlocks("Advanced missions, certification tracks, own deliverables").build(),
            Level.builder().levelNumber(5).title("Leader").xpMin(3001).xpMax(5000).stage(3)
                .primaryObjective("Guide peers, lead teams strategically")
                .keyUnlocks("Mentorship roles, leadership missions, project lead").build(),
            Level.builder().levelNumber(6).title("Mentor").xpMin(5001).xpMax(7000).stage(3)
                .primaryObjective("Sustain ecosystem & peer development")
                .keyUnlocks("Governance participation, ecosystem stewardship").build(),
            Level.builder().levelNumber(7).title("Architect").xpMin(7001).xpMax(10000).stage(3)
                .primaryObjective("Influence ecosystem growth & innovation")
                .keyUnlocks("Industry opportunities, innovation access, strategic leadership").build(),
            Level.builder().levelNumber(8).title("Industry Ready").xpMin(10001).xpMax(99999).stage(3)
                .primaryObjective("Professional-level readiness - placement & alumni")
                .keyUnlocks("Full privileges, alumni bridge, institutional ambassador").build()
        ));
    }

    private void seedBadges() {
        badgeRepository.saveAll(List.of(
            // Foundation
            Badge.builder().name("Attendance Warrior").tier("Foundation")
                .description("Maintain 95% attendance for a full calendar month.").xpRequired(50)
                .approvalAuthority("Faculty").rarity("Common").iconUrl("").build(),
            Badge.builder().name("Participation Star").tier("Foundation")
                .description("Actively participate and answer questions in all class hours for a week.").xpRequired(40)
                .approvalAuthority("Faculty").rarity("Common").iconUrl("").build(),
            Badge.builder().name("Punctuality Pro").tier("Foundation")
                .description("Arrive before the bell rings without any late entries for 2 consecutive weeks.").xpRequired(30)
                .approvalAuthority("Faculty").rarity("Common").iconUrl("").build(),

            // Achievement
            Badge.builder().name("Code Ninja").tier("Achievement")
                .description("Complete daily coding challenges on C/Python for 15 consecutive days.").xpRequired(200)
                .approvalAuthority("Faculty + Evaluator").rarity("Uncommon").iconUrl("").build(),
            Badge.builder().name("GPA Master").tier("Achievement")
                .description("Score a GPA of 8.5 or higher in the semester examinations.").xpRequired(300)
                .approvalAuthority("Faculty + Evaluator").rarity("Uncommon").iconUrl("").build(),
            Badge.builder().name("Consistency Champion").tier("Achievement")
                .description("Maintain all active daily streaks for 30 consecutive days.").xpRequired(150)
                .approvalAuthority("Faculty + Evaluator").rarity("Uncommon").iconUrl("").build(),
            Badge.builder().name("Hackathon Finisher").tier("Achievement")
                .description("Participate and submit a working project in an internal department hackathon.").xpRequired(250)
                .approvalAuthority("Faculty + Evaluator").rarity("Uncommon").iconUrl("").build(),

            // Excellence
            Badge.builder().name("Full Stack Warrior").tier("Excellence")
                .description("Build and host a web application with complete frontend and backend services.").xpRequired(800)
                .approvalAuthority("Program Management").rarity("Rare").iconUrl("").build(),
            Badge.builder().name("Interview Slayer").tier("Excellence")
                .description("Clear the first-round technical mock interviews conducted by internal placement cell.").xpRequired(600)
                .approvalAuthority("Program Management").rarity("Rare").iconUrl("").build(),
            Badge.builder().name("Internship Achiever").tier("Excellence")
                .description("Secure and successfully complete a verified 4-week industry internship.").xpRequired(1000)
                .approvalAuthority("Program Management").rarity("Rare").iconUrl("").build(),
            Badge.builder().name("Event Commander").tier("Excellence")
                .description("Lead and organize a technical/non-technical program or seminar in the college.").xpRequired(500)
                .approvalAuthority("Program Management").rarity("Rare").iconUrl("").build(),

            // Elite
            Badge.builder().name("Team Captain Badge").tier("Elite")
                .description("Serve as a team captain and lead the group to an Elite status (4500+ XP).").xpRequired(1500)
                .approvalAuthority("Governance Council").rarity("Very Rare").iconUrl("").build(),
            Badge.builder().name("Mentor Hero").tier("Elite")
                .description("Conduct peer teaching and mentor at least 5 junior students to improve their grades.").xpRequired(1200)
                .approvalAuthority("Governance Council").rarity("Very Rare").iconUrl("").build(),
            Badge.builder().name("Research Pioneer").tier("Elite")
                .description("Submit a research paper draft accepted/reviewed by the department committee.").xpRequired(2000)
                .approvalAuthority("Governance Council").rarity("Very Rare").iconUrl("").build(),
            Badge.builder().name("Innovation Catalyst").tier("Elite")
                .description("Develop a working prototype in the CoE/D2P Lab validated by an industry mentor.").xpRequired(1800)
                .approvalAuthority("Governance Council").rarity("Very Rare").iconUrl("").build(),

            // Legacy
            Badge.builder().name("Startup Builder").tier("Legacy")
                .description("Create a viable project proposal incubated or registered as a student startup.").xpRequired(3500)
                .approvalAuthority("Dean / Principal").rarity("Legendary").iconUrl("").build(),
            Badge.builder().name("Placement Champion").tier("Legacy")
                .description("Get placed in a tier-1 company with a package exceeding threshold limit.").xpRequired(3000)
                .approvalAuthority("Dean / Principal").rarity("Legendary").iconUrl("").build(),
            Badge.builder().name("JJCET Legend").tier("Legacy")
                .description("Reach a lifetime cumulative score of 3500+ XP points.").xpRequired(3500)
                .approvalAuthority("Dean / Principal").rarity("Legendary").iconUrl("").build(),
            Badge.builder().name("Alumni Pioneer").tier("Legacy")
                .description("Act as institutional ambassador and secure industry linkage / MoUs for college.").xpRequired(4000)
                .approvalAuthority("Dean / Principal").rarity("Legendary").iconUrl("").build()
        ));
    }
}
