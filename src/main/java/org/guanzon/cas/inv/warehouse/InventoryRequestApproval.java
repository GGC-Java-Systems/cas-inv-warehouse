package org.guanzon.cas.inv.warehouse;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import net.sf.jasperreports.engine.JRException;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.inv.warehouse.model.Model_Inv_Stock_Request_Detail;
import org.guanzon.cas.inv.warehouse.model.Model_Inv_Stock_Request_Master;
import org.guanzon.cas.inv.warehouse.report.ReportUtil;
import org.guanzon.cas.inv.warehouse.report.ReportUtilListener;
import org.guanzon.cas.inv.warehouse.services.InvWarehouseModels;
import org.guanzon.cas.inv.warehouse.status.StockRequestStatus;
import org.json.simple.JSONObject;
import org.guanzon.cas.parameter.BranchCluster;
import org.guanzon.cas.parameter.model.Model_Branch_Cluster;
import org.guanzon.cas.inv.warehouse.validators.InventoryStockRequestApprovalValidatorFactory;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.guanzon.cas.parameter.services.ParamModels;

public class InventoryRequestApproval extends Transaction {

    private String psIndustryCode = "";
    private String psCompanyID = "";
    private String psCategorCD = "";
    private List<Model> paMaster;
    public Model poCluster;

    public void setIndustryID(String industryId) {
        psIndustryCode = industryId;
    }

    public void setCompanyID(String companyId) {
        psCompanyID = companyId;
    }

    public void setCategoryID(String categoryId) {
        psCategorCD = categoryId;
    }

    public Model_Branch_Cluster getBranchCluster() {
        return (Model_Branch_Cluster) poCluster;
    }

    public Model_Inv_Stock_Request_Master getMaster() {
        return (Model_Inv_Stock_Request_Master) poMaster;
    }

    @SuppressWarnings("unchecked")
    public List<Model_Inv_Stock_Request_Master> getMasterList() {
        return (List<Model_Inv_Stock_Request_Master>) (List<?>) paMaster;
    }

    public Model_Inv_Stock_Request_Master getMaster(int masterRow) {
        return (Model_Inv_Stock_Request_Master) paMaster.get(masterRow);

    }

    @SuppressWarnings("unchecked")
    public List<Model_Inv_Stock_Request_Detail> getDetailList() {
        return (List<Model_Inv_Stock_Request_Detail>) (List<?>) paDetail;
    }

    public Model_Inv_Stock_Request_Detail getDetail(int entryNo) {
        if (getMaster().getTransactionNo().isEmpty()
                || getMaster().getIndustryId().isEmpty()) {
            return null;
        }

        if (entryNo <= 0 || entryNo > paDetail.size()) {
            return null;
        }

        Model_Inv_Stock_Request_Detail loDetail;

        //find the detail record
        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr++) {
            loDetail = (Model_Inv_Stock_Request_Detail) paDetail.get(lnCtr);

            if (loDetail.getEntryNumber() == entryNo) {
                return loDetail;
            }
        }
        return null;
    }

    public JSONObject initTransaction() throws GuanzonException, SQLException {
        SOURCE_CODE = "";

        poMaster = new InvWarehouseModels(poGRider).InventoryStockRequestMaster();
        poDetail = new InvWarehouseModels(poGRider).InventoryStockRequestDetail();
        poCluster = new ParamModels(poGRider).BranchCluster();
        paMaster = new ArrayList<Model>();

        return super.initialize();
    }

    @Override
    public void initSQL() {
        SQL_BROWSE = " SELECT "
                + " c.sClustrID"
                + ", a.sBranchCd"
                + ", a.sIndstCdx"
                + ", a.sCategrCd"
                + ", a.sTransNox"
                + " FROM Inv_Stock_Request_Master a"
                + "     LEFT JOIN Branch_Others b ON a.sBranchCD = b.sBranchCd"
                + "     LEFT JOIN Branch_Cluster c ON b.sClustrID = c.sClustrID AND a.sIndstCdx = c.sIndstCdx "
                + "         WHERE a.cTranStat = " + SQLUtil.toSQL(StockRequestStatus.CONFIRMED);
    }

    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        return openTransaction(transactionNo);
    }

    public JSONObject SaveTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        return saveTransaction();
    }

    public JSONObject UpdateTransaction() {
        return updateTransaction();
    }

    @Override
    protected JSONObject willSave() {
        poJSON = new JSONObject();

        poJSON = isEntryOkay(StockRequestStatus.CONFIRMED);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //assign values needed
        poJSON.put("result", "success");
        return poJSON;

    }

    @Override
    protected JSONObject isEntryOkay(String status) {
        poJSON = new JSONObject();
        GValidator loValidator = InventoryStockRequestApprovalValidatorFactory.make(getMaster().getIndustryId());

        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(poMaster);
        ArrayList laDetailList = new ArrayList<>(getDetailList());
        loValidator.setDetail(laDetailList);

        poJSON = loValidator.validate();
        if (poJSON.containsKey("isRequiredApproval") && Boolean.TRUE.equals(poJSON.get("isRequiredApproval"))) {
            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
                poJSON = ShowDialogFX.getUserApproval(poGRider);
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                } else {
                    if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "User is not an authorized approving officer.");
                        return poJSON;
                    }
                }
            }
        }

        return poJSON;
    }

    public JSONObject searchClusterBranch(String value, boolean byCode) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        BranchCluster loSubClass = new ParamControllers(poGRider, logwrapr).BranchCluster();

        if (psIndustryCode == null && "".equals(psIndustryCode)) {
            poJSON.put("result", "error");
            poJSON.put("message", "Industry is not set.");
            return poJSON;
        }

        loSubClass.getModel().setIndustryCode(psIndustryCode);

        poJSON = loSubClass.searchRecordbyIndustry(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            poCluster = loSubClass.getModel();

            poJSON = new JSONObject();
            poJSON.put("result", "success");
            return poJSON;
        }
        return poJSON;

    }

    public JSONObject loadTransactionList()
            throws SQLException, GuanzonException, CloneNotSupportedException {
        if (poCluster == null) {
            poJSON.put("result", "error");
            poJSON.put("message", "Branch Cluster is not set");
            return poJSON;
        }
        if ("".equals(poCluster.getValue("sClustrID"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Branch Cluster is not set");
            return poJSON;
        }

        if (psIndustryCode.isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "Industry is not set");
            return poJSON;

        }
        paMaster.clear();
        initSQL();
        String lsSQL = SQL_BROWSE;

        if (poCluster != null && !"".equals(poCluster.getValue("sClustrID"))) {
            lsSQL = MiscUtil.addCondition(lsSQL, "c.sClustrID = " + SQLUtil.toSQL(poCluster.getValue("sClustrID")));
        }
        if (!psIndustryCode.isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sIndstCdx = " + SQLUtil.toSQL(psIndustryCode));
        }
        if (!psCategorCD.isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sCategrCd = " + SQLUtil.toSQL(psCategorCD));
        }
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        if (MiscUtil.RecordCount(loRS)
                <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "No record found.");
            return poJSON;
        }

        while (loRS.next()) {
            Model_Inv_Stock_Request_Master loInventoryStockRequest = new InvWarehouseModels(poGRider).InventoryStockRequestMaster();
            poJSON = loInventoryStockRequest.openRecord(loRS.getString("sTransNox"));

            if ("success".equals((String) poJSON.get("result"))) {
                paMaster.add((Model) loInventoryStockRequest);
            } else {
                return poJSON;
            }
        }

        poJSON = new JSONObject();
        poJSON.put(
                "result", "success");
        return poJSON;
    }

    public JSONObject printRecord() throws SQLException, JRException, CloneNotSupportedException, GuanzonException {

        poJSON = new JSONObject();

        poJSON = isEntryOkay(StockRequestStatus.CONFIRMED);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        ReportUtil poReportJasper = new ReportUtil(poGRider);

        if (psCategorCD == null && psCategorCD.isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "Category is Required for this Transaction");
            return poJSON;
        }
        if (getMaster().getTransactionNo() == null && getMaster().getTransactionNo().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "No record is Selected");
            return poJSON;

        }
        poJSON = OpenTransaction(getMaster().getTransactionNo());
        if ("error".equals((String) poJSON.get("result"))) {
            System.out.println("Print Record open transaction : " + (String) poJSON.get("message"));
            return poJSON;
        }

        // Attach listener
        poReportJasper.setReportListener(new ReportUtilListener() {
            @Override
            public void onReportOpen() {
                System.out.println("Report opened.");
            }

            @Override
            public void onReportClose() {
                //fetch/add if needed
                System.out.println("Report closed.");
            }

            @Override
            public void onReportPrint() {
                System.out.println("Report printing...");
                try {
                    if (!isJSONSuccess(PrintTransaction(), "Print Record",
                            "Initialize Record Print! ")) {
                        return;

                    }

                    if (!isJSONSuccess(ProcessTransaction(), "Print Record",
                            "Initialize Record Print! ")) {
                    }

                    poReportJasper.CloseReportUtil();

                } catch (SQLException | GuanzonException | CloneNotSupportedException ex) {
                    Logger.getLogger(InventoryRequestApproval.class.getName()).log(Level.SEVERE, null, ex);
                    ShowMessageFX.Error("", "", ex.getMessage());
                }
            }

            @Override
            public void onReportExport() {
                System.out.println("Report exported.");
                if (!isJSONSuccess(poReportJasper.exportReportbyExcel(), "Export Record",
                        "Initialize Record Export! ")) {
                    return;
                }

//                poReportJasper.CloseReportUtil();
                //if used a model or array please create function 
            }

            @Override
            public void onReportExportPDF() {
                System.out.println("Report exported.");
//                poReportJasper.CloseReportUtil();
            }

        });
        //add Parameter
        poReportJasper.addParameter("BranchName", poGRider.getBranchName());
        poReportJasper.addParameter("Address", poGRider.getAddress());
        poReportJasper.addParameter("CompanyName", poGRider.getClientName());
        poReportJasper.addParameter("TransactionNo", getMaster().getTransactionNo());
        poReportJasper.addParameter("TransactionDate", SQLUtil.dateFormat(getMaster().getTransactionDate(), SQLUtil.FORMAT_LONG_DATE));
        poReportJasper.addParameter("Remarks", getMaster().getRemarks());
        poReportJasper.addParameter("DatePrinted", SQLUtil.dateFormat(poGRider.getServerDate(), SQLUtil.FORMAT_TIMESTAMP));
        if ("1".equals(getMaster().getProcessed()) && !StockRequestStatus.PROCESSED.equals(getMaster().getTransactionStatus())) {
            poReportJasper.addParameter("watermarkImagePath", poGRider.getReportPath() + "images\\approvedreprint.png");
        } else {
            poReportJasper.addParameter("watermarkImagePath", poGRider.getReportPath() + "images\\approved.png");
        }
        poReportJasper.setReportName("Inventory Request Approval");
        poReportJasper.setJasperPath(getJasperReport());

        //process by ResultSet
        poReportJasper.setSQLReport(PrintRecordQuery());
        System.out.println("Print Data Query :" + PrintRecordQuery());

        //process by JasperCollection parse ur List / ArrayList
        //JRBeanCollectionDataSource jrRS = new JRBeanCollectionDataSource(R1data);
        //poReportJasper.setJRBeanCollectionDataSource(jrRS);
        //direct pass JasperViewer
        //         reportPrint = JasperFillManager.fillReport(poGRider.getReportPath() + psJasperPath + ".jasper",
        //                    poParamater,
        //                    yourDATA);
        //        poReportJasper.setJasperPrint(reportPrint);
        poReportJasper.isAlwaysTop(false);
        poReportJasper.isWithUI(true);
        poReportJasper.isWithExport(true);
        poReportJasper.isWithExportPDF(true);
        poReportJasper.willExport(true);
        return poReportJasper.generateReport();

    }

    private String getJasperReport() {
        //create this function if has Categorized Report
        switch (psCategorCD) {

            case "0000001"://CELLPHONE
                return "InventoryStockRequestApprovedMP";
            case "0000002"://APPLIANCES
                return "InventoryStockRequestApprovedAppliance";
            case "0000003"://MC UNIT
                return "InventoryStockRequestApprovedMC";
            case "0000004"://MC SPAREPARTS
                return "InventoryStockRequestApprovedMCSP";
            case "0000005"://CAR UNIT
                return "InventoryStockRequestApprovedCar";
            case "0000006"://CAR SPAREPARTS
                return "InventoryStockRequestApprovedCarSP";
            case "0000007"://GENERAL
                return "InventoryStockRequestApprovedGeneral";
            case "0000008"://LP - Food
                return "InventoryStockRequestApprovedLPFood";
            case "0000009"://Monarch - Food
                return "InventoryStockRequestApprovedMonarchFood";
            default:
                return "InventoryStockRequestApprovedGeneral";
        }

    }

    private String PrintRecordQuery() {
        String lsSQL = "SELECT "
                + "   InventoryStockRequestMaster.sTransNox sTransNox"
                + ",  IFNULL(InventoryStockRequestDetail.sStockIDx,'') sStockIDx"
                + ",  IFNULL(Inventory.sBarCodex,'') Barcode"
                + ",  IFNULL(Inventory.sDescript,'') InventoryName"
                + ",  IFNULL(Brand.sDescript,'') BrandName"
                + ",  IFNULL(Model.sDescript,'') ModelName"
                + ",  IFNULL(Color.sDescript,'') ColorName"
                + ",  IFNULL(Measure.sDescript,'') MeasureName"
                + ",  IFNULL(InventoryType.sDescript,'') InventoryTypeName"
                + ",  IFNULL(Variant.sDescript,'') VariantName"
                + ",  InventoryStockRequestDetail.nQtyOnHnd nQtyOnHnd"
                + ",  InventoryStockRequestDetail.nQuantity nQuantity"
                + ",  InventoryStockRequestDetail.nApproved nApproved"
                + ",  InventoryStockRequestDetail.nCancelld nCancelld"
                + "   FROM Inv_Stock_Request_Master InventoryStockRequestMaster"
                + "     LEFT JOIN Inv_Stock_Request_Detail InventoryStockRequestDetail"
                + "         ON InventoryStockRequestMaster.sTransNox = InventoryStockRequestDetail.sTransNox"
                + "     LEFT JOIN Inventory Inventory"
                + "         ON InventoryStockRequestDetail.sStockIDx = Inventory.sStockIDx"
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
                + "             ORDER BY InventoryStockRequestDetail.nEntryNox ASC";

        lsSQL = MiscUtil.addCondition(lsSQL, "InventoryStockRequestMaster.sTransNox = " + SQLUtil.toSQL(getMaster().getTransactionNo()));

        return lsSQL;
    }

    private JSONObject PrintTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        poJSON = OpenTransaction(getMaster().getTransactionNo());
        if ("error".equals((String) poJSON.get("result"))) {
            System.out.println("Print Record open transaction : " + (String) poJSON.get("message"));
            return poJSON;
        }

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (StockRequestStatus.PROCESSED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Processed.");
            return poJSON;
        }

        if (StockRequestStatus.CONFIRMED.equals((String) poMaster.getValue("cProcessd"))) {
            poJSON.put("result", "success");
            poJSON.put("message", "Transaction Printed successfully.");
            return poJSON;
        }
        //validator
        poJSON = isEntryOkay(StockRequestStatus.PROCESSED);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poGRider.beginTrans("UPDATE STATUS", "Process Transaction Print Tag", SOURCE_CODE, getMaster().getTransactionNo());

        String lsSQL = "UPDATE "
                + poMaster.getTable()
                + " SET   cProcessd = " + SQLUtil.toSQL(StockRequestStatus.CONFIRMED)
                + " WHERE sTransNox = " + SQLUtil.toSQL(getMaster().getTransactionNo());

        Long lnResult = poGRider.executeQuery(lsSQL,
                poMaster.getTable(),
                poGRider.getBranchCode(), "", "");
        if (lnResult <= 0L) {
            poGRider.rollbackTrans();

            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "Error updating the transaction status.");
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction Printed successfully.");

        return poJSON;
    }

    public JSONObject ProcessTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid Edit Mode");
            return poJSON;
        }

        if (StockRequestStatus.PROCESSED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
//            poJSON.put("message", "Transaction was already processed.");
            return poJSON;
        }

        if (StockRequestStatus.CANCELLED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
//            poJSON.put("message", "Transaction was already cancelled.");
            return poJSON;
        }

        if (StockRequestStatus.VOID.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
//            poJSON.put("message", "Transaction was already voided.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(StockRequestStatus.PROCESSED);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poGRider.beginTrans("UPDATE STATUS", "ProcessTransaction", SOURCE_CODE, getMaster().getTransactionNo());

        poJSON = statusChange(poMaster.getTable(),
                (String) poMaster.getValue("sTransNox"),
                "ProcessTransaction",
                StockRequestStatus.PROCESSED,
                false, true);
        if ("error".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");
//        poJSON.put("message", "Transaction processed successfully.");

        return poJSON;
    }

    private boolean isJSONSuccess(JSONObject loJSON, String module, String fsModule) {
        String result = (String) loJSON.get("result");
        if ("error".equals(result)) {
            String message = (String) loJSON.get("message");
            Platform.runLater(() -> {
                if (message != null) {
                    ShowMessageFX.Warning(null, module, fsModule + ": " + message);
                }
            });
            return false;
        }
        String message = (String) loJSON.get("message");

        Platform.runLater(() -> {
            if (message != null) {
                ShowMessageFX.Information(null, module, fsModule + ": " + message);
            }
        });
        return true;

    }

}
