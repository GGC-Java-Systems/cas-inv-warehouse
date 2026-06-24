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
/*Table structure for table `inventory_count_type` */

DROP TABLE IF EXISTS `inventory_count_type`;
CREATE TABLE `inventory_count_type` (  `sInvCtrID` char(5) NOT NULL,  `sDescript` varchar(64) DEFAULT NULL,  `sDeptIDxx` varchar(3) DEFAULT NULL,  `sIndstCdx` varchar(4) DEFAULT NULL,  `cPeriodxx` char(1) DEFAULT NULL,  `sIncluded` varchar(10) DEFAULT NULL,  `nQuantity` mediumint(9) DEFAULT NULL,  `cAllowBFw` char(1) DEFAULT NULL,  `cRecdStat` char(1) DEFAULT NULL,  `sModified` varchar(12) DEFAULT NULL,  `dModified` datetime DEFAULT NULL,  `dTimeStmp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,  PRIMARY KEY (`sInvCtrID`),  KEY `ict_dept_ind_id` (`sDeptIDxx`,`sIndstCdx`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
