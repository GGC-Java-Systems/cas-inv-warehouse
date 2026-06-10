
package org.guanzon.cas.inv.warehouse.status;

import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;

/**
 *
 * @author Maynard
 */
public class InventoryCountPrint {

    public static final String MOBILE_PHONE_REPORT = "InventoryCountMP";
    public static final String MOTORCYCLE_REPORT = "InventoryCountMC";
    public static final String CAR_REPORT = "InventoryCountCar";
    public static final String HOSPITALITY_REPORT = "InventoryCountMonarch";
    public static final String LOS_PEDRITOS_REPORT = "InventoryCountLP";
    public static final String GENERAL_REPORT = "InventoryCount";
    public static final String APPLIANCE_REPORT = "InventoryCountAppliance";

    public static final String PrintRecordQuery() {
        String lsSQL = "SELECT "
                + "   InventoryCountMaster.sTransNox sTransNox"
                + ",  IFNULL(InventoryCountDetail.sStockIDx,'') sStockIDx"
                + ",  TRIM(CONCAT(IFNULL (InventorySerial.sSerial01, ''),IF(InventorySerial.sSerial01 IS NOT NULL AND InventorySerial.sSerial02 IS NOT NULL,'/ ',''),IFNULL (InventorySerial.sSerial02, '')))  xSerialNme"
                + ",  IFNULL(Inventory.sBarCodex,'') Barcode"
                + ",  IFNULL(Inventory.sDescript,'') InventoryName"
                + ",  IFNULL(Brand.sDescript,'') BrandName"
                + ",  IFNULL(Model.sDescript,'') ModelName"
                + ",  IFNULL(Color.sDescript,'') ColorName"
                + ",  IFNULL(Measure.sDescript,'') MeasureName"
                + ",  IFNULL(InventoryType.sDescript,'') InventoryTypeName"
                + ",  IFNULL(Variant.sDescript,'') VariantName"
                + ",  InventoryCountDetail.nQuantity nQuantity"
                + "   FROM Inventory_Count_Master InventoryCountMaster"
                + "     LEFT JOIN Inventory_Count_Detail InventoryCountDetail"
                + "         ON InventoryCountMaster.sTransNox = InventoryCountDetail.sTransNox"
                + "     LEFT JOIN Inventory Inventory"
                + "         ON InventoryCountDetail.sStockIDx = Inventory.sStockIDx"
                + "     LEFT JOIN Inv_Serial InventorySerial"
                + "         ON Inventory.sStockIDx = InventorySerial.sStockIDx "
                + "             AND InventoryCountDetail.sSerialID = InventorySerial.sSerialID "
                + "     LEFT JOIN Category Category"
                + "         ON Inventory.sCategCd1 = Category.sCategrCd"
                + "     LEFT JOIN Category_Level2 Category_Level2"
                + "         ON Inventory.sCategCd2 = Category_Level2.sCategrCd"
                + "     LEFT JOIN Category_Level3 Category_Level3"
                + "         ON Inventory.sCategCd3 = Category_Level3.sCategrCd"
                + "     LEFT JOIN Category_Level4 Category_Level4"
                + "         ON Inventory.sCategCd4 = Category_Level4.sCategrCd"
                + "     LEFT JOIN Brand Brand"
                + "         ON Inventory.sBrandIDx = Brand.sBrandIDx"
                + "     LEFT JOIN Model Model"
                + "         ON Inventory.sModelIDx = Model.sModelIDx"
                + "     LEFT JOIN Color Color"
                + "         ON Inventory.sColorIDx = Color.sColorIDx"
                + "     LEFT JOIN Measure Measure"
                + "         ON Inventory.sMeasurID = Measure.sMeasurID"
                + "     LEFT JOIN Inv_Type InventoryType"
                + "         ON Inventory.sInvTypCd = InventoryType.sInvTypCd"
                + "     LEFT JOIN Model_Variant Variant"
                + "         ON Inventory.sVrntIDxx = Variant.sVrntIDxx"
                + "             ORDER BY InventoryCountDetail.nEntryNox ASC";

        return lsSQL;
    }

    public static String getJasperReport(String psIndustryCode) {
        switch (psIndustryCode) {
            case "01":
                return MOBILE_PHONE_REPORT;
            case "02":
                return MOTORCYCLE_REPORT;
            case "03":
                return CAR_REPORT;
            case "04":
                return HOSPITALITY_REPORT;
            case "05":
                return LOS_PEDRITOS_REPORT;
            case "06":
                return GENERAL_REPORT;
            case "07":
                return APPLIANCE_REPORT;
            default:
                return GENERAL_REPORT;
        }
    }

}
