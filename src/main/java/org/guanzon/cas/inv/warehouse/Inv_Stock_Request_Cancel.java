//package org.guanzon.cas.inv;
//
//import org.guanzon.cas.inv.services.InvControllers;
//import org.guanzon.appdriver.agent.ShowDialogFX;
//import org.guanzon.appdriver.agent.services.Parameter;
//import org.guanzon.appdriver.base.MiscUtil;
//import org.guanzon.appdriver.base.SQLUtil;
//import org.guanzon.appdriver.constant.Logical;
//import org.guanzon.appdriver.constant.UserRight;
//import org.guanzon.cas.inv.model.Model_Inv_Master;
//import org.guanzon.cas.parameter.Branch;
//import org.guanzon.cas.parameter.InvLocation;
//import org.guanzon.cas.parameter.services.ParamControllers;
//import org.guanzon.cas.parameter.Warehouse;
//import org.json.simple.JSONObject;
//
//public class Inv_Stock_Request_Cancel extends Parameter{
//    //object model
//    Model_Inv_Master poModel;
//    
//    //reference objects
//    ParamControllers poParams;
//    InvControllers poInv;
//    
//    Branch poBranch;
//    InvLocation poLocation;
//    Inventory poInventory;
//    Warehouse poWarehouse;
//    //end - reference objects
//    
//    //optional only
//    String psBranchCd;
//    public void setBranchCode(String branchCode){
//        psBranchCd = branchCode;
//    }
//    
//    //return reference objects
//    public Branch Branch(){
//        return poBranch;
//    }
//    
//    public InvLocation InvLocation(){
//        return poLocation;
//    }
//    
//    public Inventory Inventory(){
//        return poInventory;
//    }
//    
//    public Warehouse Warehouse(){
//        return poWarehouse;
//    }
//    //end - return reference objects
//    
//    @Override
//    public void initialize() {
//        psRecdStat = Logical.YES;
//        
//        poModel = new Model_Inv_Master();
//        poModel.setApplicationDriver(poGRider);
//        poModel.setXML("Model_Inv_Stock_Request_Cancel_Master");
//        poModel.setTableName("Inv_Stock_Request_Cancel_Master");
//        poModel.initialize();
//        
//        psBranchCd = poGRider.getBranchCode();
//        
//        //initialize reference objects
//        poParams = new ParamControllers(poGRider, logwrapr);
//        poBranch = poParams.Branch();
//        poLocation = poParams.InventoryLocation();
//        poWarehouse = poParams.Warehouse();
//        
//        poInv = new InvControllers(poGRider, logwrapr);
//        poInventory = poInv.Inventory();
//        //end - initialize reference objects
//    }
//    
//    @Override
//    public JSONObject isEntryOkay() {
//        poJSON = new JSONObject();
//        
//        if (poGRider.getUserLevel() < UserRight.SYSADMIN){
//            poJSON.put("result", "error");
//            poJSON.put("message", "User is not allowed to save record.");
//            return poJSON;
//        } else {
//            poJSON = new JSONObject();
//            
//            if (poModel.getStockId().isEmpty()){
//                poJSON.put("result", "error");
//                poJSON.put("message", "Item must not be empty.");
//                return poJSON;
//            }
//            
//            if (poModel.getBranchCode().isEmpty()){
//                poJSON.put("result", "error");
//                poJSON.put("message", "Branch location must not be empty.");
//                return poJSON;
//            }
//            
//            if (poModel.getWarehouseId().isEmpty()){
//                poJSON.put("result", "error");
//                poJSON.put("message", "Warehouse location must not be empty.");
//                return poJSON;
//            }
//            
//            if (poModel.getLocationId().isEmpty()){
//                poJSON.put("result", "error");
//                poJSON.put("message", "Location must not be empty.");
//                return poJSON;
//            }
//            
//            if (poModel.getBinId().isEmpty()){
//                poJSON.put("result", "error");
//                poJSON.put("message", "Bin must not be empty.");
//                return poJSON;
//            }
//            
//            //todo:
//            //  more validations/use of validators per category
//        }
//        
//        poJSON.put("result", "success");
//        return poJSON;
//    }
//    
//    @Override
//    public Model_Inv_Master getModel() {
//        return poModel;
//    }
//    
//    @Override
//    public JSONObject searchRecord(String value, boolean byCode) {
//        poJSON = ShowDialogFX.Search(poGRider,
//                getSQ_Browse(),
//                value,
//                "ID»Barcode»Description»Brand»Color»On Hand»Selling Price",
//                "sStockIDx»sBarCodex»sDescript»xModelNme»xColorNme»nQtyOnHnd»nSelPrice",
//                "a.sStockIDx»a.sBarCodex»a.sDescript»IF(IFNULL(c.sDescript, '') = '', '', CONCAT(c.sDescript, '(', c.sModelCde, ')'))»IFNULL(d.sDescript, '')»k.nQtyOnHnd»a.nSelPrice",
//                byCode ? 0 : 1);
//
//        return openRecord(poJSON);
//    }
//    
//    public JSONObject searchRecord(String value, 
//                                    boolean byCode, 
//                                    String inventoryTypeId) {
//        String lsSQL = MiscUtil.addCondition(getSQ_Browse(), 
//                                                "a.sInvTypCd = " + SQLUtil.toSQL(inventoryTypeId));
//        
//        poJSON = ShowDialogFX.Search(poGRider,
//                lsSQL,
//                value,
//                "Barcode»Description»Brand»Model»Color»Selling Price»ID",
//                "sBarCodex»sDescript»xBrandNme»xModelNme»xColorNme»nSelPrice»sStockIDx",
//                "a.sBarCodex»a.sDescript»IFNULL(b.sDescript, '')»IF(IFNULL(c.sDescript, '') = '', '', CONCAT(c.sDescript, '(', c.sModelCde, ')'))»IFNULL(d.sDescript, '')»a.nSelPrice»a.sStockIDx",
//                byCode ? 0 : 1);
//
//        return openRecord(poJSON);
//    }
//    
//    public JSONObject searchRecord(String value, 
//                                    boolean byCode, 
//                                    String inventoryTypeId,
//                                    String categoryIdLevel1) {
//        String lsSQL = MiscUtil.addCondition(getSQ_Browse(), 
//                                                "a.sInvTypCd = " + SQLUtil.toSQL(inventoryTypeId) +
//                                                    " AND a.sCategCd1 = " + SQLUtil.toSQL(categoryIdLevel1));
//        
//        poJSON = ShowDialogFX.Search(poGRider,
//                lsSQL,
//                value,
//                "Barcode»Description»Brand»Model»Color»Selling Price»ID",
//                "sBarCodex»sDescript»xBrandNme»xModelNme»xColorNme»nSelPrice»sStockIDx",
//                "a.sBarCodex»a.sDescript»IFNULL(b.sDescript, '')»IF(IFNULL(c.sDescript, '') = '', '', CONCAT(c.sDescript, '(', c.sModelCde, ')'))»IFNULL(d.sDescript, '')»a.nSelPrice»a.sStockIDx",
//                byCode ? 0 : 1);
//
//        return openRecord(poJSON);
//    }
//    
//    public JSONObject searchRecord(String value, 
//                                    boolean byCode, 
//                                    String inventoryTypeId,
//                                    String categoryIdLevel1,
//                                    String categoryIdLevel2) {
//        String lsSQL = MiscUtil.addCondition(getSQ_Browse(), 
//                                                "a.sInvTypCd = " + SQLUtil.toSQL(inventoryTypeId) +
//                                                    " AND a.sCategCd1 = " + SQLUtil.toSQL(categoryIdLevel1) +
//                                                    " AND a.sCategCd2 = " + SQLUtil.toSQL(categoryIdLevel2));
//        
//        poJSON = ShowDialogFX.Search(poGRider,
//                lsSQL,
//                value,
//                "Barcode»Description»Brand»Model»Color»Selling Price»ID",
//                "sBarCodex»sDescript»xBrandNme»xModelNme»xColorNme»nSelPrice»sStockIDx",
//                "a.sBarCodex»a.sDescript»IFNULL(b.sDescript, '')»IF(IFNULL(c.sDescript, '') = '', '', CONCAT(c.sDescript, '(', c.sModelCde, ')'))»IFNULL(d.sDescript, '')»a.nSelPrice»a.sStockIDx",
//                byCode ? 0 : 1);
//
//        return openRecord(poJSON);
//    }
//    
//    public JSONObject searchRecordAttributes(String value, boolean byCode) {
//        poJSON = ShowDialogFX.Search(poGRider,
//                getSQ_Browse(),
//                value,
//                "Brand»Model»Color»Selling Price»ID",
//                "xBrandNme»xModelNme»xColorNme»nSelPrice»sStockIDx",
//                "IFNULL(b.sDescript, '')»IF(IFNULL(c.sDescript, '') = '', '', CONCAT(c.sDescript, '(', c.sModelCde, ')'))»IFNULL(d.sDescript, '')»a.nSelPrice»a.sStockIDx",
//                byCode ? 0 : 1);
//
//        return openRecord(poJSON);
//    }
//    
//    public JSONObject searchRecordAttributes(String value, 
//                                    boolean byCode, 
//                                    String inventoryTypeId) {
//        String lsSQL = MiscUtil.addCondition(getSQ_Browse(), 
//                                                "a.sInvTypCd = " + SQLUtil.toSQL(inventoryTypeId));
//        
//        poJSON = ShowDialogFX.Search(poGRider,
//                lsSQL,
//                value,
//                "Brand»Model»Color»Selling Price»ID",
//                "xBrandNme»xModelNme»xColorNme»nSelPrice»sStockIDx",
//                "IFNULL(b.sDescript, '')»IF(IFNULL(c.sDescript, '') = '', '', CONCAT(c.sDescript, '(', c.sModelCde, ')'))»IFNULL(d.sDescript, '')»a.nSelPrice»a.sStockIDx",
//                byCode ? 0 : 1);
//
//        return openRecord(poJSON);
//    }
//    
//    public JSONObject searchRecordAttributes(String value, 
//                                    boolean byCode, 
//                                    String inventoryTypeId,
//                                    String categoryIdLevel1) {
//        String lsSQL = MiscUtil.addCondition(getSQ_Browse(), 
//                                                "a.sInvTypCd = " + SQLUtil.toSQL(inventoryTypeId) +
//                                                    " AND a.sCategCd1 = " + SQLUtil.toSQL(categoryIdLevel1));
//        
//        poJSON = ShowDialogFX.Search(poGRider,
//                lsSQL,
//                value,
//                "Brand»Model»Color»Selling Price»ID",
//                "xBrandNme»xModelNme»xColorNme»nSelPrice»sStockIDx",
//                "IFNULL(b.sDescript, '')»IF(IFNULL(c.sDescript, '') = '', '', CONCAT(c.sDescript, '(', c.sModelCde, ')'))»IFNULL(d.sDescript, '')»a.nSelPrice»a.sStockIDx",
//                byCode ? 0 : 1);
//
//        return openRecord(poJSON);
//    }
//    
//    public JSONObject searchRecordAttributes(String value, 
//                                    boolean byCode, 
//                                    String inventoryTypeId,
//                                    String categoryIdLevel1,
//                                    String categoryIdLevel2) {
//        String lsSQL = MiscUtil.addCondition(getSQ_Browse(), 
//                                                "a.sInvTypCd = " + SQLUtil.toSQL(inventoryTypeId) +
//                                                    " AND a.sCategCd1 = " + SQLUtil.toSQL(categoryIdLevel1) +
//                                                    " AND a.sCategCd2 = " + SQLUtil.toSQL(categoryIdLevel2));
//        
//        poJSON = ShowDialogFX.Search(poGRider,
//                lsSQL,
//                value,
//                "Brand»Model»Color»Selling Price»ID",
//                "xBrandNme»xModelNme»xColorNme»nSelPrice»sStockIDx",
//                "IFNULL(b.sDescript, '')»IF(IFNULL(c.sDescript, '') = '', '', CONCAT(c.sDescript, '(', c.sModelCde, ')'))»IFNULL(d.sDescript, '')»a.nSelPrice»a.sStockIDx",
//                byCode ? 0 : 1);
//
//        return openRecord(poJSON);
//    }
//    
//    public JSONObject searchRecordWithMeasurement(String value, boolean byCode) {
//        poJSON = ShowDialogFX.Search(poGRider,
//                getSQ_Browse(),
//                value,
//                "Barcode»Description»Brand»Model»Color»Measurement»Selling Price»ID",
//                "sBarCodex»sDescript»xBrandNme»xModelNme»xColorNme»xMeasurNm»nSelPrice»sStockIDx",
//                "a.sBarCodex»a.sDescript»IFNULL(b.sDescript, '')»IF(IFNULL(c.sDescript, '') = '', '', CONCAT(c.sDescript, '(', c.sModelCde, ')'))»IFNULL(d.sDescript, '')»IFNULL(e.sMeasurNm, '')»a.nSelPrice»a.sStockIDx",
//                byCode ? 0 : 1);
//
//        return openRecord(poJSON);
//    }
//    
//    public JSONObject searchRecordWithMeasurement(String value, 
//                                    boolean byCode, 
//                                    String inventoryTypeId) {
//        String lsSQL = MiscUtil.addCondition(getSQ_Browse(), 
//                                                "a.sInvTypCd = " + SQLUtil.toSQL(inventoryTypeId));
//        
//        poJSON = ShowDialogFX.Search(poGRider,
//                lsSQL,
//                value,
//                "Barcode»Description»Brand»Model»Color»Measurement»Selling Price»ID",
//                "sBarCodex»sDescript»xBrandNme»xModelNme»xColorNme»xMeasurNm»nSelPrice»sStockIDx",
//                "a.sBarCodex»a.sDescript»IFNULL(b.sDescript, '')»IF(IFNULL(c.sDescript, '') = '', '', CONCAT(c.sDescript, '(', c.sModelCde, ')'))»IFNULL(d.sDescript, '')»IFNULL(e.sMeasurNm, '')»a.nSelPrice»a.sStockIDx",
//                byCode ? 0 : 1);
//
//        return openRecord(poJSON);
//    }
//    
//    public JSONObject searchRecordWithMeasurement(String value, 
//                                    boolean byCode, 
//                                    String inventoryTypeId,
//                                    String categoryIdLevel1) {
//        String lsSQL = MiscUtil.addCondition(getSQ_Browse(), 
//                                                "a.sInvTypCd = " + SQLUtil.toSQL(inventoryTypeId) +
//                                                    " AND a.sCategCd1 = " + SQLUtil.toSQL(categoryIdLevel1));
//        
//        poJSON = ShowDialogFX.Search(poGRider,
//                lsSQL,
//                value,
//                "Barcode»Description»Brand»Model»Color»Measurement»Selling Price»ID",
//                "sBarCodex»sDescript»xBrandNme»xModelNme»xColorNme»xMeasurNm»nSelPrice»sStockIDx",
//                "a.sBarCodex»a.sDescript»IFNULL(b.sDescript, '')»IF(IFNULL(c.sDescript, '') = '', '', CONCAT(c.sDescript, '(', c.sModelCde, ')'))»IFNULL(d.sDescript, '')»IFNULL(e.sMeasurNm, '')»a.nSelPrice»a.sStockIDx",
//                byCode ? 0 : 1);
//
//        return openRecord(poJSON);
//    }
//    
//    public JSONObject searchRecordWithMeasurement(String value, 
//                                    boolean byCode, 
//                                    String inventoryTypeId,
//                                    String categoryIdLevel1,
//                                    String categoryIdLevel2) {
//        String lsSQL = MiscUtil.addCondition(getSQ_Browse(), 
//                                                "a.sInvTypCd = " + SQLUtil.toSQL(inventoryTypeId) +
//                                                    " AND a.sCategCd1 = " + SQLUtil.toSQL(categoryIdLevel1) +
//                                                    " AND a.sCategCd2 = " + SQLUtil.toSQL(categoryIdLevel2));
//        
//        poJSON = ShowDialogFX.Search(poGRider,
//                lsSQL,
//                value,
//                "Barcode»Description»Brand»Model»Color»Measurement»Selling Price»ID",
//                "sBarCodex»sDescript»xBrandNme»xModelNme»xColorNme»xMeasurNm»nSelPrice»sStockIDx",
//                "a.sBarCodex»a.sDescript»IFNULL(b.sDescript, '')»IF(IFNULL(c.sDescript, '') = '', '', CONCAT(c.sDescript, '(', c.sModelCde, ')'))»IFNULL(d.sDescript, '')»IFNULL(e.sMeasurNm, '')»a.nSelPrice»a.sStockIDx",
//                byCode ? 0 : 1);
//
//        return openRecord(poJSON);
//    }
//    
//    @Override
//    public String getSQ_Browse(){
//        String lsSQL;
//        String lsRecdStat = "";
//
//        if (psRecdStat.length() > 1) {
//            for (int lnCtr = 0; lnCtr <= psRecdStat.length() - 1; lnCtr++) {
//                lsRecdStat += ", " + SQLUtil.toSQL(Character.toString(psRecdStat.charAt(lnCtr)));
//            }
//
//            lsRecdStat = "a.cRecdStat IN (" + lsRecdStat.substring(2) + ")";
//        } else {
//            lsRecdStat = "a.cRecdStat = " + SQLUtil.toSQL(psRecdStat);
//        }
//        
//        lsSQL = "SELECT" +
//                    "  a.sStockIDx" +
//                    ", a.sBarCodex" +
//                    ", a.sDescript" +
//                    ", a.sAltBarCd" +
//                    ", a.nUnitPrce" +
//                    ", a.nSelPrice" +
//                    ", IFNULL(b.sDescript, '') xBrandNme" +
//                    ", IF(IFNULL(c.sDescript, '') = '', '', CONCAT(c.sDescript, '(', c.sModelCde, ')')) xModelNme" +
//                    ", IFNULL(d.sDescript, '') xColorNme" +
//                    ", IFNULL(e.sMeasurNm, '') xMeasurNm" +
//                    ", IFNULL(f.sDescript, '') xCategNm1" +
//                    ", IFNULL(g.sDescript, '') xCategNm2" +
//                    ", IFNULL(h.sDescript, '') xCategNm3" +
//                    ", IFNULL(i.sDescript, '') xCategNm4" +
//                    ", IFNULL(j.sDescript, '') xInvTypNm" +
//                    ", k.nQtyOnHnd" +
//                " FROM Inventory a" +
//                        " LEFT JOIN Brand b ON a.sBrandIDx = b.sBrandIDx" +
//                        " LEFT JOIN Model c ON a.sModelIDx = c.sModelIDx" +
//                        " LEFT JOIN Color d ON a.sColorIDx = d.sColorIDx" +
//                        " LEFT JOIN Measure e ON a.sMeasurID = e.sMeasurID" +
//                        " LEFT JOIN Category f ON a.sCategCd1 = f.sCategrCd" +
//                        " LEFT JOIN Category_Level2 g ON a.sCategCd2 = g.sCategrCd" +
//                        " LEFT JOIN Category_Level3 h ON a.sCategCd3 = h.sCategrCd" +
//                        " LEFT JOIN Category_Level4 i ON a.sCategCd4 = i.sCategrCd" +
//                        " LEFT JOIN Inv_Type j ON a.sInvTypCd = j.sInvTypCd" +
//                    ", Inv_Master k" + 
//                " WHERE a.sStockIDx = k.sStockIDx" +
//                " AND k.sBranchCd = " + SQLUtil.toSQL(psBranchCd);
//        
//        if (!psRecdStat.isEmpty()) lsSQL = MiscUtil.addCondition(lsSQL, lsRecdStat);
//        
//        return lsSQL;
//    }
//     private JSONObject openRecord(JSONObject json){
//        if (json != null) {
//            System.out.println("(String) poJSON.get(\"sBranchCd\") == " + psBranchCd);
//            poJSON = poModel.openRecord((String) poJSON.get("sStockIDx"), psBranchCd);
//            
//            if (!"success".equals((String) poJSON.get("result"))) return poJSON;
//            
//            //load reference records
//            poInventory.openRecord("sStockIDx");
//            poBranch.openRecord("sBranchCd");
//            poWarehouse.openRecord("sWHouseID");
//            poLocation.openRecord("sLocatnID");
//            //end -load reference records
//        } else {
//            poJSON = new JSONObject();
//            poJSON.put("result", "error");
//            poJSON.put("message", "No record loaded.");
//            return poJSON;
//        }
//        
//        return poJSON;
//    }
////    private JSONObject openRecord(JSONObject json){
////        if (json != null) {
////             System.out.println("stockid == " + (String) poJSON.get("sStockIDx"));
////             String lsStockID = (String) poJSON.get("sStockIDx");
////           poJSON = poModel.openRecord((String) poJSON.get("sStockIDx"), (String) poJSON.get("sBranchCd"));
////           if ("error".equals((String) poJSON.get("result"))){
////               if ((String) poJSON.get("sBranchCd") == null){
////                ShowMessageFX.Information("No Inventory found in your warehouse. Please save the record to create.", "Computerized Acounting System", "Inventory Detail");
////                   System.out.println("stockid == " + lsStockID);
////                poJSON = poInventory.openRecord((String) poJSON.get("sStockIDx"));
//////                EditMode.ADDNEW;    
////                } 
////                return poJSON;
////            }
////            
////            
////            
////            else {
////                poJSON = poModel.openRecord((String) poJSON.get("sStockIDx"), (String) poJSON.get("sBranchCd"));
////            }
////            
//////            if ("error".equals((String)poJSON.get("result"))){
//////                ShowMessageFX.Information("No Inventory found in your warehouse. Please save the record to create.", "Computerized Acounting System", "Inventory Detail");
//////                 poJSON = newRecord();
//////            
//////            }
////            
////            //load reference records
////            poInventory.openRecord("sStockIDx");
////            poBranch.openRecord("sBranchCd");
////            poWarehouse.openRecord("sWHouseID");
////            poLocation.openRecord("sLocatnID");
////            //end -load reference records
////        } else {
////            poJSON = new JSONObject();
////            poJSON.put("result", "error");
////            poJSON.put("message", "No record loaded.");
////            return poJSON;
////        }
////        
////        return poJSON;
////    }
//        public JSONObject searchRecordwithBarrcode(String value, boolean byCode) {
//            
//        poJSON = ShowDialogFX.Search(poGRider,
//                getSQ_Browse(),
//                value,
//                "BarCode»Description»Selling Price»ID",
//                "sBarCodex»sDescript»nSelPrice»sStockIDx",
//                "a.sBarCodex»a.sDescript»a.nSelPrice»a.sStockIDx",
//                byCode ? 0 : 1);
//
//        return openRecord(poJSON);
//    }
//    public JSONObject searchRecordwithBarrcode(String value, 
//                                    boolean byCode, 
//                                    String stockID) {
//        String lsSQL = MiscUtil.addCondition(getSQ_Browse(), 
//                                                    "a.sStockIDx = " + SQLUtil.toSQL(stockID));
//        
//        poJSON = ShowDialogFX.Search(poGRider,
//                lsSQL,
//                value,
//                  "BarCode»Description»Selling Price»ID",
//                "sBarCodex»sDescript»nSelPrice»sStockIDx",
//                "a.sBarCodex»a.sDescript»a.nSelPrice»a.sStockIDx",
//                byCode ? 0 : 1);
//        System.out.println("poJSON = " + poJSON);
//        return openRecord(poJSON);
//    }
//}
//        