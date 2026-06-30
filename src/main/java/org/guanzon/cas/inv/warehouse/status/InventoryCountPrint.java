package org.guanzon.cas.inv.warehouse.status;

import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;

/**
 *
 * @author Maynard
 */
public class InventoryCountPrint {

    public static final String MOBILE_PHONE_REPORT = "InventoryCountGeneral";
    public static final String MOTORCYCLE_REPORT = "InventoryCountGeneral";
    public static final String CAR_REPORT = "InventoryCountGeneral";
    public static final String HOSPITALITY_REPORT = "InventoryCountGeneral";
    public static final String LOS_PEDRITOS_REPORT = "InventoryCountGeneral";
    public static final String GENERAL_REPORT = "InventoryCountGeneral";
    public static final String APPLIANCE_REPORT = "InventoryCountGeneral";

    public static final String PrintRecordQuery() {
        String lsSQL = "SELECT "
                + "   InventoryCountMaster.sTransNox sTransNox"
                + ",  IFNULL(InventoryCountDetail.nEntryNox,'') nEntryNox"
                + ",  IFNULL(InventoryCountDetail.sStockIDx,'') sStockIDx"
                + ",  IFNULL(Inventory.sBarCodex,'') Barcode"
                + ",  IFNULL(Inventory.sDescript,'') InventoryName"
                + ",  IFNULL(Brand.sDescript,'') BrandName"
                + ",  IFNULL(Model.sDescript,'') ModelName"
                + ",  IFNULL(Color.sDescript,'') ColorName"
                + ",  IFNULL(Measure.sDescript,'') MeasureName"
                + ",  IFNULL(InventoryType.sDescript,'') InventoryTypeName"
                + ",  IFNULL(Variant.sDescript,'') VariantName"
                + ",  CASE WHEN InventoryCountMaster.nCounterx >= 3 THEN IFNULL(InventoryCountDetail.sRemarksx, '') ELSE '' END sRemarksx"
                + ",  CASE WHEN InventoryCountMaster.nCounterx >= 1 THEN IFNULL(InventoryCountDetail.nActCtr01, '') ELSE '' END nActCtr01"
                + ",  CASE WHEN InventoryCountMaster.nCounterx >= 2 THEN IFNULL(InventoryCountDetail.nActCtr02, '') ELSE '' END nActCtr02"
                + ",  CASE WHEN InventoryCountMaster.nCounterx >= 3 THEN IFNULL(InventoryCountDetail.nActCtr03, '') ELSE '' END nActCtr03"
                + ",  IFNULL(InventoryCountDetail.nQtyOnHnd,0) nQtyOnHnd"
                + ",  (CASE InventoryCountMaster.nCounterx "
                + "         WHEN 1 THEN IFNULL(InventoryCountDetail.nActCtr01,0) "
                + "         WHEN 2 THEN IFNULL(InventoryCountDetail.nActCtr02,0) "
                + "         WHEN 3 THEN IFNULL(InventoryCountDetail.nActCtr03,0) "
                + "         ELSE 0 END) - IFNULL(InventoryCountDetail.nQtyOnHnd,0) nVariance"
                + "   FROM Inventory_Count_Master InventoryCountMaster"
                + "     LEFT JOIN Inventory_Count_Detail InventoryCountDetail"
                + "         ON InventoryCountMaster.sTransNox = InventoryCountDetail.sTransNox"
                + "     LEFT JOIN Inventory Inventory"
                + "         ON InventoryCountDetail.sStockIDx = Inventory.sStockIDx"
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

    public static final String PrintReportQuery() {

        String lsSQL = "SELECT "
                + " IFNULL(InvLocation.sDescript,'') AS InvLocationName "
                + ", IFNULL(Inventory.sBarCodex,'') AS Barcode "
                + ", IFNULL(Inventory.sDescript,'') AS InventoryName "
                + ", IFNULL(Brand.sDescript,'') AS BrandName "
                + ", IFNULL(Model.sDescript,'') ModelName"
                + ", IFNULL(Measure.sDescript,'') AS MeasureName "
                + ", IFNULL(InventoryCountDetail.nQtyOnHnd,0) AS nQtyOnHnd "
                + ", CASE "
                + "      WHEN InventoryCountMaster.nCounterx = 3 THEN IFNULL(InventoryCountDetail.nActCtr03,0) "
                + "      WHEN InventoryCountMaster.nCounterx = 2 THEN IFNULL(InventoryCountDetail.nActCtr02,0) "
                + "      WHEN InventoryCountMaster.nCounterx = 1 THEN IFNULL(InventoryCountDetail.nActCtr01,0) "
                + "      ELSE 0 "
                + "   END AS nActCtrFnal "
                + ", (IFNULL(InventoryCountDetail.nQtyOnHnd,0) - "
                + "    CASE "
                + "      WHEN InventoryCountMaster.nCounterx = 3 THEN IFNULL(InventoryCountDetail.nActCtr03,0) "
                + "      WHEN InventoryCountMaster.nCounterx = 2 THEN IFNULL(InventoryCountDetail.nActCtr02,0) "
                + "      WHEN InventoryCountMaster.nCounterx = 1 THEN IFNULL(InventoryCountDetail.nActCtr01,0) "
                + "      ELSE 0 "
                + "    END) AS QtyVariance "
                + ", IFNULL(InventoryCountMaster.dCutOffxx,'') AS CutOff "
                + ", IFNULL(InventoryCountMaster.sRemarksx,'') AS Remarks "
                + ", CASE "
                + "      WHEN (IFNULL(InventoryCountDetail.nQtyOnHnd,0) - "
                + "            CASE "
                + "               WHEN InventoryCountMaster.nCounterx = 3 THEN IFNULL(InventoryCountDetail.nActCtr03,0) "
                + "               WHEN InventoryCountMaster.nCounterx = 2 THEN IFNULL(InventoryCountDetail.nActCtr02,0) "
                + "               WHEN InventoryCountMaster.nCounterx = 1 THEN IFNULL(InventoryCountDetail.nActCtr01,0) "
                + "               ELSE 0 "
                + "            END) < 0 "
                + "           THEN 'Added' "
                + "      WHEN (IFNULL(InventoryCountDetail.nQtyOnHnd,0) - "
                + "            CASE "
                + "               WHEN InventoryCountMaster.nCounterx = 3 THEN IFNULL(InventoryCountDetail.nActCtr03,0) "
                + "               WHEN InventoryCountMaster.nCounterx = 2 THEN IFNULL(InventoryCountDetail.nActCtr02,0) "
                + "               WHEN InventoryCountMaster.nCounterx = 1 THEN IFNULL(InventoryCountDetail.nActCtr01,0) "
                + "               ELSE 0 "
                + "            END) > 0 "
                + "           THEN 'Deducted' "
                + "      ELSE 'No Adjustment' "
                + "   END AS AdjustmentType "
                + "FROM Inventory_Count_Master InventoryCountMaster "
                + " LEFT JOIN Inventory_Count_Detail InventoryCountDetail "
                + "       ON InventoryCountMaster.sTransNox = InventoryCountDetail.sTransNox "
                + " LEFT JOIN Inventory Inventory "
                + "       ON InventoryCountDetail.sStockIDx = Inventory.sStockIDx "
                + " LEFT JOIN Model Model"
                + "       ON Inventory.sModelIDx = Model.sModelIDx "
                + " LEFT JOIN Brand Brand "
                + "       ON Inventory.sBrandIDx = Brand.sBrandIDx "
                + " LEFT JOIN Measure Measure "
                + "       ON Inventory.sMeasurID = Measure.sMeasurID "
                + " LEFT JOIN Inv_Master InvMaster "
                + "       ON Inventory.sStockIDx = InvMaster.sStockIDx "
                + " LEFT JOIN Inv_Location InvLocation "
                + "       ON InvLocation.sLocatnID = InvMaster.sLocatnID "
                + "ORDER BY InvLocation.sDescript ASC";

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
