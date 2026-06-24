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
/*Table structure for table `transaction_attachment` */

DROP TABLE IF EXISTS `transaction_attachment`;

CREATE TABLE `transaction_attachment` (
  `sTransNox` varchar(14) NOT NULL,
  `sSourceCd` varchar(4) NOT NULL,
  `sSourceNo` varchar(12) NOT NULL,
  `sDocuType` varchar(4) DEFAULT NULL,
  `sDescript` varchar(128) DEFAULT NULL,
  `sScanndID` varchar(4) DEFAULT NULL,
  `sFileName` varchar(128) NOT NULL,
  `sMD5Hashx` varchar(100) DEFAULT NULL,
  `sImagePth` varchar(128) DEFAULT NULL,
  `dEntryDte` datetime DEFAULT NULL,
  `sEntryByx` varchar(32) DEFAULT NULL,
  `cRecdStat` char(1) DEFAULT '1',
  `cSendStat` char(1) DEFAULT '0',
  `sModified` varchar(32) DEFAULT NULL,
  `dModified` datetime DEFAULT NULL,
  `dTimeStmp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sTransNox`),
  KEY `sSourceCd` (`sSourceCd`,`sSourceNo`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
