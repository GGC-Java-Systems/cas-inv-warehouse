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
/*Table structure for table `client_master` */

DROP TABLE IF EXISTS `client_master`;

CREATE TABLE `client_master` (
  `sClientID` char(12) NOT NULL,
  `cClientTp` char(1) DEFAULT NULL,
  `sLastName` varchar(60) DEFAULT NULL,
  `sFrstName` varchar(60) DEFAULT NULL,
  `sMiddName` varchar(60) DEFAULT NULL,
  `sSuffixNm` varchar(60) DEFAULT NULL,
  `sMaidenNm` varchar(128) DEFAULT NULL,
  `sCompnyNm` varchar(256) DEFAULT NULL,
  `cGenderCd` char(1) DEFAULT NULL,
  `cCvilStat` char(1) DEFAULT NULL,
  `sCitizenx` char(2) DEFAULT NULL,
  `dBirthDte` date DEFAULT NULL,
  `sBirthPlc` varchar(100) DEFAULT NULL,
  `sAddlInfo` varchar(64) DEFAULT NULL,
  `sSpouseID` varchar(12) DEFAULT NULL,
  `sTaxIDNox` varchar(15) DEFAULT NULL,
  `sLTOIDxxx` varchar(32) DEFAULT NULL,
  `sPHBNIDxx` varchar(32) DEFAULT NULL,
  `cLRClient` char(1) DEFAULT '0',
  `cMCClient` char(1) DEFAULT '0',
  `cSCClient` char(1) DEFAULT '0',
  `cSPClient` char(1) DEFAULT '0',
  `cCPClient` char(1) DEFAULT '0',
  `cEducLevl` char(1) DEFAULT NULL,
  `sRelgnIDx` varchar(7) DEFAULT NULL,
  `sSSSNoxxx` varchar(15) DEFAULT NULL,
  `sOccptnID` varchar(7) DEFAULT NULL,
  `sOccptnOT` varchar(40) DEFAULT NULL,
  `sClientNo` varchar(8) DEFAULT NULL,
  `sFatherID` varchar(100) DEFAULT NULL,
  `sMotherID` varchar(100) DEFAULT NULL,
  `sSiblngID` varchar(12) DEFAULT NULL,
  `cRecdStat` char(1) DEFAULT '1',
  `sModified` varchar(32) DEFAULT NULL,
  `dModified` datetime DEFAULT NULL,
  `dTimeStmp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sClientID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
