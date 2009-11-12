-- MySQL dump 10.13  Distrib 5.1.40, for Win64 (unknown)
--
-- Host: localhost    Database: foxquant
-- ------------------------------------------------------
-- Server version	5.1.40-community

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `contract`
--

DROP TABLE IF EXISTS `contract`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `contract` (
  `SYMBOL` varchar(20) DEFAULT NULL,
  `SEC_TYPE` varchar(4) NOT NULL,
  `EXPIRY` date DEFAULT NULL,
  `STRIKE` double DEFAULT NULL,
  `CONTRACT_RIGHT` varchar(3) DEFAULT NULL,
  `MULTIPLIER` varchar(20) DEFAULT NULL,
  `EXCHANGE` varchar(20) DEFAULT NULL,
  `CURRENCY` varchar(3) DEFAULT NULL,
  `LOCAL_SYMBOL` varchar(20) DEFAULT NULL,
  `PRIMARY_EXCHANGE` varchar(12) DEFAULT NULL,
  `CONTRACT_ID` int(11) NOT NULL,
  `MARKET_NAME` varchar(40) DEFAULT NULL,
  `TRADING_CLASS` varchar(40) DEFAULT NULL,
  `MIN_TICK` double(5,5) DEFAULT NULL,
  `PRICE_MAGNIFIER` int(11) DEFAULT NULL,
  `NOTES` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`CONTRACT_ID`),
  KEY `SEC_TYPE` (`SEC_TYPE`),
  KEY `SYMBOL` (`SYMBOL`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contract_exchange`
--

DROP TABLE IF EXISTS `contract_exchange`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `contract_exchange` (
  `CONTRACT_ID` int(11) NOT NULL,
  `EXCHANGE` varchar(12) NOT NULL,
  PRIMARY KEY (`CONTRACT_ID`,`EXCHANGE`),
  KEY `EXCHANGE` (`EXCHANGE`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contract_iqfeed`
--

DROP TABLE IF EXISTS `contract_iqfeed`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `contract_iqfeed` (
  `CONTRACT_ID` int(11) NOT NULL,
  `IQFEED_SYMBOL` varchar(40) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contract_order_type`
--

DROP TABLE IF EXISTS `contract_order_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `contract_order_type` (
  `CONTRACT_ID` int(11) NOT NULL,
  `ORDER_TYPE` varchar(12) NOT NULL,
  PRIMARY KEY (`CONTRACT_ID`,`ORDER_TYPE`),
  KEY `ORDER_TYPE` (`ORDER_TYPE`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `currency`
--

DROP TABLE IF EXISTS `currency`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `currency` (
  `CURRENCY` varchar(3) NOT NULL,
  PRIMARY KEY (`CURRENCY`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `exchange`
--

DROP TABLE IF EXISTS `exchange`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `exchange` (
  `EXCHANGE` varchar(12) NOT NULL,
  PRIMARY KEY (`EXCHANGE`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `minute_bar`
--

DROP TABLE IF EXISTS `minute_bar`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `minute_bar` (
  `CONTRACT_ID` int(11) NOT NULL,
  `BAR_TYPE` varchar(8) NOT NULL,
  `BAR_START` datetime NOT NULL,
  `OPEN` double NOT NULL,
  `HIGH` double NOT NULL,
  `LOW` double NOT NULL,
  `CLOSE` double NOT NULL,
  PRIMARY KEY (`CONTRACT_ID`,`BAR_TYPE`,`BAR_START`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `option_right`
--

DROP TABLE IF EXISTS `option_right`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `option_right` (
  `OPTION_RIGHT` varchar(4) NOT NULL,
  PRIMARY KEY (`OPTION_RIGHT`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_action`
--

DROP TABLE IF EXISTS `order_action`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `order_action` (
  `ORDER_ACTION` varchar(6) NOT NULL,
  PRIMARY KEY (`ORDER_ACTION`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_status`
--

DROP TABLE IF EXISTS `order_status`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `order_status` (
  `ORDER_ID` int(11) NOT NULL,
  `STATUS` varchar(20) NOT NULL,
  `FILLED` int(11) NOT NULL,
  `REMAINING` int(11) NOT NULL,
  `AVG_FILL_PRICE` double DEFAULT NULL,
  `PERM_ID` int(11) DEFAULT NULL,
  `PARENT_ID` int(11) DEFAULT NULL,
  `LAST_FILLED_PRICE` double DEFAULT NULL,
  `CLIENT_ID` int(11) NOT NULL,
  `WHY_HELD` varchar(60) DEFAULT NULL,
  `RECEIVED_AT` datetime NOT NULL,
  KEY `ORDER_ID` (`ORDER_ID`),
  KEY `STATUS` (`STATUS`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_time_in_force`
--

DROP TABLE IF EXISTS `order_time_in_force`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `order_time_in_force` (
  `ORDER_TIME_IN_FORCE` varchar(4) NOT NULL,
  PRIMARY KEY (`ORDER_TIME_IN_FORCE`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_type`
--

DROP TABLE IF EXISTS `order_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `order_type` (
  `ORDER_TYPE` varchar(12) NOT NULL,
  PRIMARY KEY (`ORDER_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `security_type`
--

DROP TABLE IF EXISTS `security_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `security_type` (
  `SECURITY_TYPE` varchar(12) NOT NULL,
  PRIMARY KEY (`SECURITY_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `session`
--

DROP TABLE IF EXISTS `session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `session` (
  `SESSION_ID` int(11) NOT NULL AUTO_INCREMENT,
  `HOSTNAME` varchar(80) NOT NULL,
  `CLIENT_ID` int(11) NOT NULL,
  `START_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`SESSION_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=220 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tar_and_feather`
--

DROP TABLE IF EXISTS `tar_and_feather`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tar_and_feather` (
  `RUN_START` datetime NOT NULL,
  `BAR_START` datetime NOT NULL,
  `CONTRACT_ID` int(11) NOT NULL,
  `ENTER_LONG` double NOT NULL,
  `ENTER_SHORT` double NOT NULL,
  `EXIT_LONG` double NOT NULL,
  `EXIT_SHORT` double NOT NULL,
  `TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `SMA_HIGH` double NOT NULL,
  `STD_DEV_HIGH` double NOT NULL,
  `SMA_LOW` double NOT NULL,
  `STD_DEV_LOW` double NOT NULL,
  PRIMARY KEY (`BAR_START`,`CONTRACT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tick`
--

DROP TABLE IF EXISTS `tick`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tick` (
  `CONTRACT_ID` int(11) NOT NULL,
  `RECEIVED_AT` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `BID_PRICE` double DEFAULT NULL,
  `ASK_PRICE` double DEFAULT NULL,
  `BID_SIZE` int(11) DEFAULT NULL,
  `ASK_SIZE` int(11) DEFAULT NULL,
  KEY `RECEIVED_AT` (`RECEIVED_AT`),
  KEY `TICK_CONTRACT` (`CONTRACT_ID`),
  CONSTRAINT `TICK_CONTRACT` FOREIGN KEY (`CONTRACT_ID`) REFERENCES `contract` (`CONTRACT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `trade_order`
--

DROP TABLE IF EXISTS `trade_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `trade_order` (
  `ORDER_ID` int(11) NOT NULL,
  `CLIENT_ID` int(11) NOT NULL,
  `SESSION_ID` int(11) DEFAULT NULL,
  `PERM_ID` int(11) DEFAULT NULL,
  `CONTRACT_ID` int(11) NOT NULL,
  `ACTION` varchar(5) NOT NULL,
  `TOTAL_QUANTITY` bigint(20) NOT NULL,
  `TYPE` varchar(10) NOT NULL,
  `LIMIT_PRICE` double DEFAULT NULL,
  `STOP_PRICE` double DEFAULT NULL,
  `OFFSET_PRICE` double DEFAULT NULL,
  `TIME_IN_FORCE` varchar(3) NOT NULL,
  `OCA_GROUP` varchar(80) DEFAULT NULL,
  `OCA_TYPE` int(11) DEFAULT NULL,
  `PARENT_ORDER_ID` int(11) DEFAULT NULL,
  `BLOCK_ORDER` tinyint(1) DEFAULT NULL,
  `SWEEP_TO_FILL` tinyint(1) DEFAULT NULL,
  `DISPLAY_QUANTITY` int(11) DEFAULT NULL,
  `TRIGGER_METHOD` int(11) DEFAULT NULL,
  `OUTSIDE_HOURS` tinyint(1) DEFAULT NULL,
  `HIDDEN` tinyint(1) DEFAULT NULL,
  `PRICE_DISCRETION` double DEFAULT NULL,
  `GOOD_AFTER_TIME` varchar(22) DEFAULT NULL,
  `GOOD_TIL_DATE` varchar(22) DEFAULT NULL,
  `CREATED_AT` datetime NOT NULL,
  `TRIGGER_BAR` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`ORDER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2009-11-10 21:56:15
