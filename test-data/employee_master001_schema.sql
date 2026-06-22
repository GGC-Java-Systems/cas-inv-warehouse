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
/*Table structure for table `employee_master001` */

DROP TABLE IF EXISTS `employee_master001`;

CREATE TABLE `employee_master001` (
  `sEmployID` varchar(12) NOT NULL,
  `sBranchCd` varchar(4) DEFAULT NULL,
  `sPyBranch` varchar(4) DEFAULT NULL,
  `sSgBranch` varchar(4) DEFAULT NULL,
  `dHiredxxx` date DEFAULT NULL,
  `sPositnID` varchar(4) DEFAULT NULL,
  `sDeptIDxx` varchar(4) DEFAULT NULL,
  `sShiftIDx` varchar(3) DEFAULT NULL,
  `cEmpTypex` varchar(3) DEFAULT NULL,
  `sEmpLevID` varchar(3) DEFAULT NULL,
  `cEmpRankx` varchar(2) DEFAULT NULL,
  `sSalLvlID` varchar(8) DEFAULT NULL,
  `sSalRegID` varchar(3) DEFAULT NULL,
  `sTaxRegID` varchar(3) DEFAULT NULL,
  `sIDNoxxxx` varchar(4) DEFAULT NULL,
  `sPHealtNo` varchar(15) DEFAULT NULL,
  `sHDMFNoxx` varchar(20) DEFAULT NULL,
  `sPassword` varchar(15) DEFAULT NULL,
  `dStartEmp` date DEFAULT NULL,
  `dRegularx` date DEFAULT NULL,
  `dFiredxxx` date DEFAULT NULL,
  `sBnkActNo` varchar(15) DEFAULT NULL,
  `sBankIDxx` varchar(9) DEFAULT NULL,
  `sTaxExmpt` varchar(3) DEFAULT NULL,
  `cDeductGC` char(1) DEFAULT NULL,
  `cSalTypex` char(1) DEFAULT NULL,
  `cSalCompt` char(1) DEFAULT NULL,
  `nBasicPay` varchar(34) DEFAULT NULL,
  `nSalaryxx` decimal(7,2) DEFAULT NULL,
  `nLveCredt` decimal(5,2) DEFAULT '0.00',
  `nSatCredt` decimal(5,2) DEFAULT '0.00',
  `sContrlNo` varchar(12) DEFAULT NULL,
  `sIDNumber` char(12) DEFAULT NULL,
  `xEmployID` varchar(12) DEFAULT NULL,
  `dInactive` date DEFAULT NULL,
  `cSubsidzd` char(1) DEFAULT NULL,
  `cCollectr` char(1) DEFAULT NULL,
  `cManagerx` char(1) DEFAULT NULL,
  `cMechanic` char(1) DEFAULT NULL,
  `cCredInvx` char(1) DEFAULT NULL,
  `cDriverxx` char(1) DEFAULT NULL,
  `cCompress` char(1) DEFAULT '0',
  `cSecTypex` char(1) DEFAULT '0',
  `cSlfieLog` char(1) DEFAULT '0',
  `cRecdStat` char(1) DEFAULT NULL,
  `sModified` varchar(10) DEFAULT NULL,
  `dModified` datetime DEFAULT NULL,
  PRIMARY KEY (`sEmployID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
