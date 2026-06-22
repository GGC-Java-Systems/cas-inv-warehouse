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
/*Table structure for table `xxxsysfiles` */

DROP TABLE IF EXISTS `xxxsysfiles`;

CREATE TABLE `xxxsysfiles` (
  `sSysFleCD` varchar(10) NOT NULL,
  `sSysFleDs` varchar(128) DEFAULT NULL,
  `sSysFleNm` varchar(64) DEFAULT NULL,
  `sArtfctID` varchar(128) DEFAULT NULL,
  `sVersionx` varchar(16) DEFAULT NULL,
  `sRemarksx` varchar(256) DEFAULT NULL,
  `sFileType` varchar(32) DEFAULT NULL,
  `dBegDatex` date DEFAULT NULL,
  `cRecdStat` char(1) DEFAULT '1',
  `sModified` varchar(10) DEFAULT NULL,
  `dModified` datetime DEFAULT NULL,
  `dTimeStmp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sSysFleCD`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;CREATE TABLE `xxxsysaction` (  `sSysActCD` varchar(20) NOT NULL,  `sSysActDs` varchar(64) DEFAULT NULL,  `sSysSubCD` varchar(10) DEFAULT NULL,  `sSysFleCD` varchar(10) DEFAULT NULL,  `sNameSpce` varchar(64) DEFAULT NULL,  `sFileName` varchar(64) DEFAULT NULL,  `sFunction` varchar(64) DEFAULT NULL,  `nUserRght` varchar(64) DEFAULT NULL,  `nAuthReqd` tinyint(4) unsigned DEFAULT NULL,  `dBegDatex` date DEFAULT NULL,  `cRecdStat` char(1) DEFAULT '1',  `sModified` varchar(10) DEFAULT NULL,  `dModified` datetime DEFAULT NULL,  `dTimeStmp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,  PRIMARY KEY (`sSysActCD`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;CREATE TABLE `transaction_status_history` (  `sTransNox` varchar(12) NOT NULL,  `sTableNme` varchar(64) NOT NULL,  `sSourceNo` varchar(12) DEFAULT NULL,  `sPayloadx` json DEFAULT NULL,  `sRemarksx` varchar(128) DEFAULT NULL,  `sApproved` varchar(12) DEFAULT NULL,  `dApproved` datetime DEFAULT NULL,  `cRefrStat` char(1) DEFAULT NULL,  `cTranStat` char(1) NOT NULL,  `sModified` varchar(32) DEFAULT NULL,  `dModified` datetime DEFAULT NULL,  `dTimeStmp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,  PRIMARY KEY (`sTransNox`),  KEY `sTableNme` (`sTableNme`),  KEY `sSourceNo` (`sSourceNo`),  KEY `cTranStat` (`cTranStat`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
