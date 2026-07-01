/*
SQLyog Ultimate v8.55 
MySQL - 5.7.44-log : Database - gcasys_dbf
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*Data for the table `inventory_count_type` */

insert  into `inventory_count_type`(`sInvCtrID`,`sDescript`,`sDeptIDxx`,`sIndstCdx`,`cPeriodxx`,`sIncluded`,`nQuantity`,`cAllowBFw`,`cRecdStat`,`sModified`,`dModified`,`dTimeStmp`) values ('00001','Branch Audit',NULL,'09','X','AI',0,'1','1','M001250012','2026-06-12 10:55:54','2026-06-11 16:40:19'),('00002','Monthly Bin Only',NULL,'09','M','AI',0,'1','0','M001250012','2026-06-11 16:54:44','2026-06-11 16:54:45');

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
