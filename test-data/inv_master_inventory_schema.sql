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
/*Table structure for table `inv_master` */

DROP TABLE IF EXISTS `inv_master`;

CREATE TABLE `inv_master` (
  `sStockIDx` varchar(12) NOT NULL,
  `sIndstCdx` varchar(2) NOT NULL,
  `sBranchCd` varchar(4) NOT NULL,
  `sWHouseID` varchar(4) NOT NULL,
  `sLocatnID` varchar(7) DEFAULT NULL,
  `sBinNumbr` varchar(7) DEFAULT NULL,
  `dAcquired` date DEFAULT NULL,
  `dBegInvxx` date DEFAULT NULL,
  `nBegQtyxx` decimal(8,2) DEFAULT NULL,
  `nQtyOnHnd` decimal(8,2) DEFAULT NULL,
  `nLedgerNo` varchar(6) DEFAULT NULL,
  `nMinLevel` decimal(8,2) DEFAULT NULL,
  `nMaxLevel` decimal(8,2) DEFAULT NULL,
  `nAvgMonSl` decimal(8,2) DEFAULT NULL,
  `nAvgCostx` decimal(12,4) DEFAULT NULL,
  `cClassify` char(1) DEFAULT 'F',
  `nBackOrdr` decimal(8,2) DEFAULT NULL,
  `nResvOrdr` decimal(8,2) DEFAULT NULL,
  `nFloatQty` decimal(8,2) DEFAULT NULL,
  `dLastTran` date DEFAULT NULL,
  `cPrimaryx` char(1) DEFAULT NULL,
  `nReorderx` decimal(8,2) DEFAULT NULL,
  `nMinIssue` decimal(8,2) DEFAULT NULL,
  `cAutoComp` char(1) DEFAULT NULL,
  `cConditnx` char(1) DEFAULT NULL,
  `sPayLoadx` varchar(512) DEFAULT NULL,
  `cRecdStat` char(1) DEFAULT NULL,
  `sModified` varchar(32) DEFAULT NULL,
  `dModified` datetime DEFAULT NULL,
  `dTimeStmp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sBranchCd`,`sStockIDx`,`sWHouseID`,`sIndstCdx`),
  KEY `sWHouseID` (`sWHouseID`),
  KEY `sLocatnCd` (`sLocatnID`),
  KEY `cClassify` (`cClassify`),
  KEY `cPrimaryx` (`cPrimaryx`),
  KEY `sBranchCd` (`sBranchCd`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*Table structure for table `inventory` */

DROP TABLE IF EXISTS `inventory`;

CREATE TABLE `inventory` (
  `sStockIDx` varchar(12) NOT NULL,
  `sBarCodex` varchar(25) DEFAULT NULL,
  `sDescript` varchar(256) DEFAULT NULL,
  `sBriefDsc` varchar(25) DEFAULT NULL,
  `sAltBarCd` varchar(25) DEFAULT NULL,
  `sCategCd1` varchar(7) DEFAULT NULL,
  `sCategCd2` varchar(7) DEFAULT NULL,
  `sCategCd3` varchar(7) DEFAULT NULL,
  `sCategCd4` varchar(7) DEFAULT NULL,
  `sBrandIDx` varchar(8) DEFAULT NULL,
  `sModelIDx` varchar(9) DEFAULT NULL,
  `sColorIDx` varchar(7) DEFAULT NULL,
  `sVrntIDxx` varchar(5) DEFAULT NULL,
  `sMeasurID` varchar(7) DEFAULT NULL,
  `sInvTypCd` varchar(4) DEFAULT NULL,
  `sIndstCdx` char(2) NOT NULL,
  `nUnitPrce` decimal(13,4) DEFAULT NULL,
  `nSelPrice` decimal(11,2) DEFAULT NULL,
  `nDiscLev1` decimal(8,2) DEFAULT NULL,
  `nDiscLev2` decimal(8,2) DEFAULT NULL,
  `nDiscLev3` decimal(8,2) DEFAULT NULL,
  `nDealrDsc` decimal(8,2) DEFAULT NULL,
  `nMinLevel` smallint(6) DEFAULT NULL,
  `nMaxLevel` smallint(6) DEFAULT NULL,
  `cComboInv` char(1) DEFAULT '0',
  `cWthPromo` char(1) DEFAULT '0',
  `cSerialze` char(1) DEFAULT '0',
  `cUnitType` char(1) DEFAULT '0',
  `cInvStatx` char(1) DEFAULT '0',
  `nShlfLife` smallint(7) DEFAULT NULL,
  `sSupersed` varchar(16) DEFAULT NULL,
  `sPayLoadx` varchar(512) DEFAULT NULL,
  `cRecdStat` char(1) DEFAULT '1',
  `sModified` varchar(32) DEFAULT NULL,
  `dModified` datetime DEFAULT NULL,
  `dTimeStmp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sStockIDx`,`sIndstCdx`),
  KEY `sBarCodex` (`sBarCodex`),
  KEY `sDescript` (`sDescript`),
  KEY `sAltBarCd` (`sAltBarCd`),
  KEY `sCategCd1` (`sCategCd1`),
  KEY `sCategCd2` (`sCategCd2`),
  KEY `sCategCd3` (`sCategCd3`),
  KEY `sCategCd4` (`sCategCd4`),
  KEY `sBrandIDx` (`sBrandIDx`),
  KEY `sModelIDx` (`sModelIDx`),
  KEY `sColorIDx` (`sColorIDx`),
  KEY `sVrntIDxx` (`sVrntIDxx`),
  KEY `sMeasurID` (`sMeasurID`),
  KEY `sInvTypCd` (`sInvTypCd`),
  KEY `sIndstCdx` (`sIndstCdx`),
  KEY `sSupersed` (`sSupersed`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
