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
-- Table structure for table `students`
--

DROP TABLE IF EXISTS `students`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `students` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `full_name` varchar(100) DEFAULT NULL,
  `student_id` varchar(50) DEFAULT NULL,
  `reg_no` bigint DEFAULT NULL,
  `spr_no` varchar(50) DEFAULT NULL,
  `department_id` bigint NOT NULL,
  `section_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `DOB` date DEFAULT NULL,
  `gender_id` bigint NOT NULL,
  `phone_no` varchar(15) NOT NULL,
  `academic_year_id` bigint NOT NULL,
  `year_id` bigint NOT NULL,
  `semester_id` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `academic_year` varchar(20) DEFAULT NULL,
  `active` bit(1) NOT NULL,
  `address` varchar(255) DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `email` varchar(150) NOT NULL,
  `gender` varchar(10) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `score` int NOT NULL,
  `section` varchar(50) DEFAULT NULL,
  `semester` varchar(20) DEFAULT NULL,
  `year` varchar(10) DEFAULT NULL,
  `current_stage` int NOT NULL,
  `stage` int DEFAULT '1',
  `total_xp` int NOT NULL,
  `team_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKe2rndfrsx22acpq2ty1caeuyw` (`email`),
  UNIQUE KEY `UK5mbus2m1tm2acucrp6t627jmx` (`student_id`),
  UNIQUE KEY `reg_no` (`reg_no`),
  UNIQUE KEY `spr_no` (`spr_no`),
  KEY `fk_students_gender` (`gender_id`),
  KEY `idx_students_academic_year` (`academic_year_id`),
  KEY `idx_students_year` (`year_id`),
  KEY `idx_students_semester` (`semester_id`),
  KEY `fk_students_user` (`user_id`),
  KEY `students_ibfk_1` (`department_id`),
  KEY `fk_students_section` (`section_id`),
  KEY `FKjgyxg2x86o4me9gr70elinthr` (`team_id`),
  CONSTRAINT `fk_students_academic_year` FOREIGN KEY (`academic_year_id`) REFERENCES `academic_years` (`id`),
  CONSTRAINT `fk_students_gender` FOREIGN KEY (`gender_id`) REFERENCES `genders` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_students_section` FOREIGN KEY (`section_id`) REFERENCES `section` (`id`),
  CONSTRAINT `fk_students_semester` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`),
  CONSTRAINT `fk_students_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_students_year` FOREIGN KEY (`year_id`) REFERENCES `years` (`id`),
  CONSTRAINT `FKjgyxg2x86o4me9gr70elinthr` FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`),
  CONSTRAINT `students_ibfk_1` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `students`
--

LOCK TABLES `students` WRITE;
/*!40000 ALTER TABLE `students` DISABLE KEYS */;
INSERT INTO `students` VALUES (13,'tharsan','69',NULL,'24CS069',8,NULL,NULL,NULL,1,'1234567899',1,2,4,'2026-07-08 06:54:16','2026-07-08 12:26:35','2024-2025',_binary '','Testing Address','2006-01-01','tharsan@gmail.com','Male','$2a$10$OkN5k2hGu079AM/34nIxoehIqK2s1TDFdpaMbbc74h0sKOtYvy7zu','1234567899',100,NULL,'4','2',1,1,0,NULL),(15,'Esha Banerjee','R2402021',NULL,'2402021',8,NULL,NULL,NULL,1,'9117672709',1,1,1,'2026-07-08 09:10:24','2026-07-09 05:22:19','2024-2025',_binary '',NULL,'2000-09-02','esha.banerjee60@student.university.edu','Male','$2a$10$iZ1i13nCbze6DuADjVj7VuD3JN6WgM.SeMAJVODmmeNAG7Z7M8DZ2','9117672709',110,NULL,'1','1',1,1,0,NULL),(16,'venkat','999',NULL,'24sc013',8,NULL,NULL,NULL,1,'1234567890',1,1,1,'2026-07-08 09:25:31','2026-07-09 07:07:59','2024-2025',_binary '','neiveli','2026-07-08','string@gmail.com','Male','$2a$10$Nwq6yKhennlsVZABoJ3Z5OaRHgvSqRsHFb5Xlw.YityCeHJf/tdcq','1234567890',100,NULL,'1','1',1,1,0,8),(17,'Jagadhessh','99999',NULL,'99999',8,NULL,NULL,NULL,1,'9876543210',1,1,1,'2026-07-08 09:29:14','2026-07-08 09:29:14','2024-2025',_binary '','Testing Address','2000-01-01','jagadhessh@gmail.com','Male','$2a$10$MJv.jND8dzc3zNc.huDcs.HK267JcAWCOCC5fqcrjMUA5/2L1slMK','9876543210',100,NULL,'1','1',1,1,0,NULL),(21,'luffy','811324149050',NULL,NULL,13,NULL,NULL,NULL,1,'0000000000',1,1,2,'2026-07-08 10:07:19','2026-07-08 12:26:35','2024-2025',_binary '\0',NULL,'2006-01-15','luffy@onepiece.com','Male','$2a$10$ytqlg8bA0qhYfpSdSIXCFewBXW1xxlIn1ltM5V0JtVSIXustSuPn6',NULL,116,NULL,'2','1',1,1,0,NULL),(22,'saarendar','99',NULL,'24sc040',8,NULL,NULL,NULL,1,'1234567890',1,1,1,'2026-07-08 12:30:34','2026-07-09 06:31:52','2024-2025',_binary '','srirangam','2007-01-25','saarendar@gmail.com','Male','$2a$10$dYK9rnGSuyy5zwpFgnWj0.44guVB3mK/24PxKT3WZ/w1zZ7su9CpW','1234567890',100,NULL,'1','1',1,1,0,14),(23,'sharugesh','36',NULL,'24sc036',8,NULL,NULL,NULL,1,'1234567890',1,1,1,'2026-07-08 12:36:08','2026-07-09 06:31:52','2024-2025',_binary '','aalambatti','2006-09-23','sharugesh@gmail.com','Male','$2a$10$wdfW532nil2goV8Em6Q2TOJQFT1sSB28HOvG2A6x2DCNKsNgNpHZa','1234567890',100,NULL,'1','1',1,1,0,14),(24,'hajmal irfan','10',NULL,'24sc010',8,NULL,NULL,NULL,1,'1234567890',1,1,1,'2026-07-08 12:49:46','2026-07-09 05:18:06','2024-2025',_binary '','vaazhlkai','2007-02-20','irfan@gmail.com','Male','$2a$10$oQSZhPLTuA1EEK0tB.i4muB15.Zt4Eait6YcxDfwsx7OPTe1CKC9u','1234567890',100,NULL,'1','1',1,1,0,7),(25,'kapil raj','18',NULL,'24sc018',8,NULL,NULL,NULL,1,'1234567890',1,1,1,'2026-07-08 12:51:13','2026-07-09 06:31:52','2024-2025',_binary '','ariyalur','2006-05-18','kapilkhan@gmail.com','Male','$2a$10$RyQKBxRB46yAVJ8jPr51LuDe0TQfYcN5zmFGi/GcriZObtUnYRhzC','1234567890',100,NULL,'1','1',1,1,0,14),(26,'thirupathi','811324149046',NULL,'24sc046',8,NULL,NULL,NULL,1,'1234567890',1,1,1,'2026-07-08 15:25:34','2026-07-08 15:26:58','2024-2025',_binary '','andhra','2006-05-18','thipathi@gmail.com','Male','$2a$10$d7z.vr4NVHaNRPPlyNqaZu3e6YfCDtT0b0.0N50Ep7lZ4OBn0wEDO','1234567890',100,NULL,'1','1',1,1,0,NULL);
/*!40000 ALTER TABLE `students` ENABLE KEYS */;
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

-- Dump completed on 2026-07-09 14:01:48
