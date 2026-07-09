-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: spdms_lab
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ '40f6e646-751f-11f1-b2ea-341a4d76bf6e:1-2228,
c2d99e25-331a-11f1-a374-341a4d76bf6e:1-229';

--
-- Table structure for table `badges`
--

DROP TABLE IF EXISTS `badges`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `badges` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `approval_authority` varchar(100) NOT NULL,
  `description` text NOT NULL,
  `icon_url` varchar(255) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `rarity` varchar(50) NOT NULL,
  `tier` varchar(50) NOT NULL,
  `xp_required` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKcuebofvgkgi4g9fxde2kmpr1h` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `badges`
--

LOCK TABLES `badges` WRITE;
/*!40000 ALTER TABLE `badges` DISABLE KEYS */;
INSERT INTO `badges` VALUES (1,'Faculty','Maintain 95% attendance for a full calendar month.','','Attendance Warrior','Common','Foundation',50),(2,'Faculty','Actively participate and answer questions in all class hours for a week.','','Participation Star','Common','Foundation',40),(3,'Faculty','Arrive before the bell rings without any late entries for 2 consecutive weeks.','','Punctuality Pro','Common','Foundation',30),(4,'Faculty + Evaluator','Complete daily coding challenges on C/Python for 15 consecutive days.','','Code Ninja','Uncommon','Achievement',200),(5,'Faculty + Evaluator','Score a GPA of 8.5 or higher in the semester examinations.','','GPA Master','Uncommon','Achievement',300),(6,'Faculty + Evaluator','Maintain all active daily streaks for 30 consecutive days.','','Consistency Champion','Uncommon','Achievement',150),(7,'Faculty + Evaluator','Participate and submit a working project in an internal department hackathon.','','Hackathon Finisher','Uncommon','Achievement',250),(8,'Program Management','Build and host a web application with complete frontend and backend services.','','Full Stack Warrior','Rare','Excellence',800),(9,'Program Management','Clear the first-round technical mock interviews conducted by internal placement cell.','','Interview Slayer','Rare','Excellence',600),(10,'Program Management','Secure and successfully complete a verified 4-week industry internship.','','Internship Achiever','Rare','Excellence',1000),(11,'Program Management','Lead and organize a technical/non-technical program or seminar in the college.','','Event Commander','Rare','Excellence',500),(12,'Governance Council','Serve as a team captain and lead the group to an Elite status (4500+ XP).','','Team Captain Badge','Very Rare','Elite',1500),(13,'Governance Council','Conduct peer teaching and mentor at least 5 junior students to improve their grades.','','Mentor Hero','Very Rare','Elite',1200),(14,'Governance Council','Submit a research paper draft accepted/reviewed by the department committee.','','Research Pioneer','Very Rare','Elite',2000),(15,'Governance Council','Develop a working prototype in the CoE/D2P Lab validated by an industry mentor.','','Innovation Catalyst','Very Rare','Elite',1800),(16,'Dean / Principal','Create a viable project proposal incubated or registered as a student startup.','','Startup Builder','Legendary','Legacy',3500),(17,'Dean / Principal','Get placed in a tier-1 company with a package exceeding threshold limit.','','Placement Champion','Legendary','Legacy',3000),(18,'Dean / Principal','Reach a lifetime cumulative score of 3500+ XP points.','','JJCET Legend','Legendary','Legacy',3500),(19,'Dean / Principal','Act as institutional ambassador and secure industry linkage / MoUs for college.','','Alumni Pioneer','Legendary','Legacy',4000);
/*!40000 ALTER TABLE `badges` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-09 14:01:41
