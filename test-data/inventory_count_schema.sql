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
/*Table structure for table `inventory_adjustment` */

DROP TABLE IF EXISTS `inventory_adjustment`;

CREATE TABLE `inventory_adjustment` (  `sTransNox` varchar(12) NOT NULL,  `dTransact` date DEFAULT NULL,  `sDocNmbrx` varchar(6) DEFAULT NULL,  `sPartsIDx` varchar(12) NOT NULL,  `nQtyInxxx` decimal(8,2) DEFAULT NULL,  `nQtyOutxx` decimal(8,2) DEFAULT NULL,  `sRemarksx` varchar(128) DEFAULT NULL,  `sSourceNo` varchar(12) DEFAULT NULL,  `sSourceCd` varchar(4) DEFAULT NULL,  `cTranStat` char(1) DEFAULT NULL,  `sModified` varchar(10) DEFAULT NULL,  `dModified` datetime DEFAULT NULL,  `dTimeStmp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,  PRIMARY KEY (`sTransNox`,`sPartsIDx`),  KEY `ia_parts_id` (`sPartsIDx`),  KEY `ia_sourceCd_no` (`sSourceNo`,`sSourceCd`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*Table structure for table `inventory_count_detail` */

DROP TABLE IF EXISTS `inventory_count_detail`;

CREATE TABLE `inventory_count_detail` (  `sTransNox` varchar(12) NOT NULL,  `nEntryNox` smallint(6) NOT NULL,  `sStockIDx` varchar(12) DEFAULT NULL,  `sWHouseID` char(3) DEFAULT NULL,  `sSectnIDx` varchar(7) DEFAULT NULL,  `sBinIDxxx` varchar(7) DEFAULT NULL,  `nQtyOnHnd` decimal(8,2) DEFAULT NULL,  `nActCtr01` decimal(8,2) DEFAULT NULL,  `nActCtr02` decimal(8,2) DEFAULT NULL,  `nActCtr03` decimal(8,2) DEFAULT NULL,  `sDifCause` varchar(512) DEFAULT NULL,  `sRemarksx` varchar(128) DEFAULT NULL,  `dModified` datetime DEFAULT NULL,  `dTimeStmp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,  PRIMARY KEY (`sTransNox`,`nEntryNox`),  KEY `icd_loc_id` (`sWHouseID`,`sSectnIDx`,`sBinIDxxx`),  KEY `icd_stockid` (`sStockIDx`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*Table structure for table `inventory_count_master` */

DROP TABLE IF EXISTS `inventory_count_master`;

CREATE TABLE `inventory_count_master` (  `sTransNox` varchar(12) NOT NULL,  `sIndstCdx` varchar(4) DEFAULT NULL,  `sCategrCd` varchar(7) DEFAULT NULL,  `sBranchCd` varchar(4) DEFAULT NULL,  `dTransact` timestamp DEFAULT NULL,  `sRemarksx` varchar(512) DEFAULT NULL,  `nEntryNox` smallint(6) DEFAULT NULL,  `sInvCtrID` char(5) DEFAULT NULL,  `dCutOffxx` timestamp DEFAULT NULL,  `sIncluded` varchar(10) DEFAULT NULL,  `nCounterx` smallint(6) DEFAULT NULL,  `sRqstdByx` varchar(12) DEFAULT NULL,  `dRequestd` datetime DEFAULT NULL,  `cTranStat` char(1) DEFAULT NULL,  `sModified` varchar(12) DEFAULT NULL,  `dModified` datetime DEFAULT NULL,  `dTimeStmp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,  PRIMARY KEY (`sTransNox`),  KEY `icm_date_trans` (`dTransact`),  KEY `icm_cat_br_cd` (`sCategrCd`,`sBranchCd`,`sIndstCdx`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;


/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
