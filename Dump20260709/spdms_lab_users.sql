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
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(100) DEFAULT NULL,
  `email` varchar(150) DEFAULT NULL,
  `password` varchar(150) NOT NULL,
  `phone` varchar(15) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `active` bit(1) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `section` varchar(50) DEFAULT NULL,
  `year` varchar(10) DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  KEY `FKsbg59w8q63i0oo53rlgvlcnjq` (`department_id`),
  CONSTRAINT `FKsbg59w8q63i0oo53rlgvlcnjq` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (3,'sharugesh','sharugesh@spdms.com','$2a$10$r7IsTroVjUC1AsQrC2D9oukSvx8A7vIdn1j1LmcdQx8rvqVPFG06K',NULL,NULL,'2026-07-06 16:15:18',_binary '','Sharugesh',NULL,NULL,NULL),(5,'sharu','sharu@spdms.com','$2a$10$7p6okSLGCzh7xsehoc7D.e7wktDxi125BTK3YqlNoQYoEIG86Vv.S',NULL,'2026-07-08 11:12:34','2026-07-06 16:21:16',_binary '','Sharu HOD',NULL,NULL,8),(6,'admin','admin@spdms.com','$2a$10$LgB3DunRilsFYHrwbvQ0I.Szbj0l..ZsjQwZUMiQ3X0zMjhkC5W16',NULL,'2026-07-09 08:12:25','2026-07-06 17:23:08',_binary '','System Administrator',NULL,NULL,NULL),(8,'surendar','selvakumarsurendar@gmail.com','$2a$10$l8tUgyrZZ/OSz7417LOi.umjXaVebTrGxYLXe9l4s45Rs0t0Djcvu',NULL,NULL,'2026-07-07 06:28:55',_binary '','surendar s',NULL,NULL,8),(10,'9','9@gmail.com','$2a$10$aYoZ6PhuKyL8tjIItWI36uKbVdznLH5tU/ulN5nbFz/BIIaGlsM4a',NULL,NULL,'2026-07-08 06:35:19',_binary '','9',NULL,NULL,8),(11,'s9','s9@gmail.com','$2a$10$8zM8djgh.NjJhl0B88/YxuDNmqk9fXSaA2MqMBFGmHv98JT1qBeum',NULL,NULL,'2026-07-08 10:43:09',_binary '','99',NULL,NULL,8),(12,'sun','sunn@gmail.com','$2a$10$5eTJF.k09y9zSW9lbA7WV.VUawa/XugV1oJ/zXwSTNTqelBV7uBVW',NULL,NULL,'2026-07-08 11:08:03',_binary '','sunn',NULL,NULL,8),(13,'jaga','jaga@gmail.com','$2a$10$S/ByISwQO4ddyo9N32/U9uI.WViYoRvEcqQ/BmVdCkrEgHFy46lXC',NULL,NULL,'2026-07-08 11:59:01',_binary '','jaga',NULL,'I',8);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
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

-- Dump completed on 2026-07-09 14:01:46
