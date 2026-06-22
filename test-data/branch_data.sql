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
/*Data for the table `branch` */

insert  into `branch`(`sBranchCd`,`sBranchNm`,`sDescript`,`sCompnyID`,`sIndstCdx`,`sAddressx`,`sTownIDxx`,`sManagerx`,`sSellCode`,`cWareHous`,`sTelNumbr`,`cRecdStat`,`sContactx`,`sEMailAdd`,`dExportxx`,`cSrvcCntr`,`cAutomate`,`cMainOffc`,`sModified`,`dModified`,`dTimeStmp`) values ('GCO1','Guanzon Central Office','1 GCO','M001','08','Guanzon Bldg., Perez Blvd.','0314','','','0','075 522-1085','1','','','2006-11-25 11:40:49','','1','1','imported','2018-04-11 16:39:17','2026-01-16 14:16:51'),('GK01','Guan Kay Office','GK Office','0001','09','GK Bldg. Tapuac Dist.','0314','','MC','0','','1',NULL,NULL,'2006-11-25 11:40:49',NULL,'1','1','imported','2018-04-11 16:39:17','2026-01-16 11:11:47'),('GMO1','Guanzon Manila Office',NULL,'M001',NULL,'253 Roosevelt Avenue, San Antonio','0028','','JP','0','075 522-1085','1',NULL,NULL,'2006-11-25 11:40:49',NULL,'1','1','imported','2015-04-10 09:25:40','2026-01-16 11:11:47'),('M001','GMC Dagupan - Honda',NULL,'0002','02','Guanzon Bldg., Perez Blvd.','0314','0314','JP','0','075 522-1085','1',NULL,NULL,'2006-11-25 11:40:49',NULL,'1','0','imported','2011-07-01 12:03:20','2026-01-16 11:11:47'),('M0W1','GMC Anolid Warehouse','','0002','02','Anolid','0346','M001040057',NULL,'1','075 513-3767/513-4748','1','075 513-3767/513-4748',NULL,'2007-06-02 08:41:20',NULL,'1','1','imported','2024-08-29 11:49:48','2026-01-16 11:11:47'),('W005','Procurement Warehouse - Habanes','Procurement Warehouse - Habanes','0003','09','Anolid','0314',NULL,NULL,'1',NULL,'1','she',NULL,NULL,NULL,'1','1','imported','2018-04-11 16:39:17','2026-01-16 14:16:53');

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
