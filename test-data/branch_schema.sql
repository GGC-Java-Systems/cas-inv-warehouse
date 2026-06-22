/*
SQLyog Ultimate v8.55 
MySQL - 5.7.44-log : Database - pmo_gcasys_dbf
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*Table structure for table `branch` */

DROP TABLE IF EXISTS `branch`;

CREATE TABLE `branch` (
  `sBranchCd` varchar(4) NOT NULL,
  `sBranchNm` varchar(50) DEFAULT NULL,
  `sDescript` varchar(50) DEFAULT NULL,
  `sCompnyID` varchar(4) DEFAULT NULL,
  `sIndstCdx` varchar(64) DEFAULT NULL,
  `sAddressx` varchar(50) DEFAULT NULL,
  `sTownIDxx` varchar(4) DEFAULT NULL,
  `sManagerx` varchar(12) DEFAULT NULL,
  `sSellCode` varchar(2) DEFAULT NULL,
  `cWareHous` char(1) DEFAULT NULL,
  `sTelNumbr` varchar(50) DEFAULT NULL,
  `cRecdStat` char(1) DEFAULT NULL,
  `sContactx` varchar(50) DEFAULT NULL,
  `sEMailAdd` varchar(50) DEFAULT NULL,
  `dExportxx` datetime DEFAULT NULL,
  `cSrvcCntr` char(1) DEFAULT NULL,
  `cAutomate` char(1) DEFAULT NULL,
  `cMainOffc` char(1) DEFAULT NULL,
  `sModified` varchar(32) DEFAULT NULL,
  `dModified` datetime DEFAULT NULL,
  `dTimeStmp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sBranchCd`),
  KEY `sTownIDxx` (`sTownIDxx`),
  KEY `sManagerx` (`sManagerx`),
  KEY `sBranchNm` (`sBranchNm`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
