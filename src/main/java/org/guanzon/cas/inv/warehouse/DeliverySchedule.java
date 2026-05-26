package org.guanzon.cas.inv.warehouse;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.rowset.CachedRowSet;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.json.simple.JSONObject;
import org.guanzon.cas.inv.warehouse.status.DeliveryScheduleStatus;
import org.guanzon.cas.inv.warehouse.model.Model_Delivery_Schedule_Detail;
import org.guanzon.cas.inv.warehouse.model.Model_Delivery_Schedule_Master;
import org.guanzon.cas.inv.warehouse.services.DeliveryIssuanceModels;
import org.guanzon.cas.parameter.BranchCluster;
import org.guanzon.cas.parameter.model.Model_Branch_Cluster_Delivery;
import org.guanzon.cas.parameter.model.Model_Branch_Others;
import org.guanzon.cas.inv.warehouse.validators.DeliveryScheduleValidatorFactory;
import org.guanzon.cas.parameter.services.ParamControllers;

public class DeliverySchedule extends Transaction {

    private String psIndustryCode = "";
    private String psCompanyID = "";
    private String psCategorCD = "";
    private List<Model> paMaster;

    public void setIndustryID(String industryId) {
        psIndustryCode = industryId;
    }

    public void setCompanyID(String companyId) {
        psCompanyID = companyId;
    }

    public void setCategoryID(String categoryId) {
        psCategorCD = categoryId;
    }

    public Model_Delivery_Schedule_Master getMaster() {
        return (Model_Delivery_Schedule_Master) poMaster;
    }

    @SuppressWarnings("unchecked")
    public List<Model_Delivery_Schedule_Master> getMasterList() {
        return (List<Model_Delivery_Schedule_Master>) (List<?>) paMaster;
    }

    public Model_Delivery_Schedule_Master getMaster(int masterRow) {
        return (Model_Delivery_Schedule_Master) paMaster.get(masterRow);

    }

    @SuppressWarnings("unchecked")
    public List<Model_Delivery_Schedule_Detail> getDetailList() {
        return (List<Model_Delivery_Schedule_Detail>) (List<?>) paDetail;
    }

    @SuppressWarnings("unchecked")
    public List<Model_Branch_Others> getDeliveryBranchOtherList(int foSelected) throws SQLException, GuanzonException {
        return (List<Model_Branch_Others>) ((Model_Delivery_Schedule_Detail) paDetail.get(foSelected)).BranchCluster().getBranchOthersList();
    }

    @SuppressWarnings("unchecked")
    public List<Model_Branch_Cluster_Delivery> getDeliveryBranchClusterDeliveryList(int foSelected) throws SQLException, GuanzonException {
        return (List<Model_Branch_Cluster_Delivery>) ((Model_Delivery_Schedule_Detail) paDetail.get(foSelected)).BranchCluster().getBranchClusterDeliveryList();
    }

    public int getBranchClusterDeliverysCount(int foSelected) throws SQLException, GuanzonException {
        return ((Model_Delivery_Schedule_Detail) paDetail.get(foSelected)).BranchCluster().getBranchClusterDeliverysCount();
    }

    public JSONObject LoadBranchClusterDelivery(int foSelectedClusterRow) throws SQLException, GuanzonException, CloneNotSupportedException {
        return ((Model_Delivery_Schedule_Detail) paDetail.get(foSelectedClusterRow)).BranchCluster().loadBranchClusterDeliveryList();
    }

    public JSONObject LoadBranchOthers(int foSelectedClusterRow) throws SQLException, GuanzonException, CloneNotSupportedException {
        return ((Model_Delivery_Schedule_Detail) paDetail.get(foSelectedClusterRow)).BranchCluster().loadBranchList();

    }

    public Model_Delivery_Schedule_Detail getDetail(int clusterRow) {
        if (getMaster().getTransactionNo().isEmpty()) {
            return null;
        }
        //autoadd detail if empty
        Model_Delivery_Schedule_Detail lastDetail = (Model_Delivery_Schedule_Detail) paDetail.get(paDetail.size() - 1);
        String clusterID = lastDetail.getClusterID();
        if (clusterID != null && !clusterID.trim().isEmpty()) {
            Model_Delivery_Schedule_Detail newDetail = new DeliveryIssuanceModels(poGRider).DeliveryScheduleDetail();
            newDetail.newRecord();
            newDetail.setTransactionNo(getMaster().getTransactionNo());
            paDetail.add(newDetail);
        }

        // If index is invalid or out of range, add up to that index
        while (paDetail.size() <= clusterRow) {

            Model_Delivery_Schedule_Detail newDetail = new DeliveryIssuanceModels(poGRider).DeliveryScheduleDetail();
            newDetail.newRecord();
            newDetail.setTransactionNo(getMaster().getTransactionNo());
            paDetail.add(newDetail);
        }

        // Get the requested detail
        Model_Delivery_Schedule_Detail loDetailSelected = (Model_Delivery_Schedule_Detail) paDetail.get(clusterRow);

        // Find a match by ClusterID
        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr++) {
            Model_Delivery_Schedule_Detail loDetail = (Model_Delivery_Schedule_Detail) paDetail.get(lnCtr);

            if (loDetail.getClusterID() == loDetailSelected.getClusterID()) {
                return loDetail;
            }
        }

        // No match found — create new
        Model_Delivery_Schedule_Detail loDetail = new DeliveryIssuanceModels(poGRider).DeliveryScheduleDetail();
        loDetail.newRecord();
        loDetail.setTransactionNo(getMaster().getTransactionNo());
        paDetail.add(loDetail);

        return loDetail;
    }

    public JSONObject initTransaction() throws GuanzonException, SQLException {
        SOURCE_CODE = "Dlvr";

        poMaster = new DeliveryIssuanceModels(poGRider).DeliverySchedule();
        poDetail = new DeliveryIssuanceModels(poGRider).DeliveryScheduleDetail();
        paMaster = new ArrayList<Model>();

        return super.initialize();
    }

    public JSONObject searchTransaction(String value, boolean byCode, boolean byExact) {
        try {
            String lsSQL = SQL_BROWSE;
            String lsCondition = "";
            if (psTranStat != null) {
                if (this.psTranStat.length() > 1) {
                    for (int lnCtr = 0; lnCtr <= this.psTranStat.length() - 1; lnCtr++) {
                        lsCondition = lsCondition + ", " + SQLUtil.toSQL(Character.toString(this.psTranStat.charAt(lnCtr)));
                    }
                    lsCondition = "cTranStat IN (" + lsCondition.substring(2) + ")";
                } else {
                    lsCondition = "cTranStat = " + SQLUtil.toSQL(this.psTranStat);
                }
                lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
            }

            if (!psCategorCD.isEmpty()) {
                lsSQL = MiscUtil.addCondition(lsSQL, "sCategrCd = " + SQLUtil.toSQL(psCategorCD));
            }
            poJSON = ShowDialogFX.Search(poGRider,
                    lsSQL,
                    value,
                    "Transaction No»Date»Schedule Date",
                    "sTransNox»dTransact»dSchedule",
                    "sTransNox»dTransact»dSchedule",
                    byExact ? (byCode ? 0 : 1) : 2);

            if (poJSON != null) {
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                } else {
                    return openTransaction((String) poJSON.get("sTransNox"));
                }
            } else {
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "No record loaded.");
                return poJSON;

            }
        } catch (CloneNotSupportedException | SQLException | GuanzonException ex) {
            Logger.getLogger(DeliverySchedule.class
                    .getName()).log(Level.SEVERE, null, ex);
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }

    public JSONObject openTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        if (transactionNo.isEmpty()) {

            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction must not empty.");
            return poJSON;
        }

        poJSON = poMaster.openRecord(transactionNo);

        if ("error".equals((String) poJSON.get("result"))) {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "Unable to Open Transaction Record.");
            return poJSON;
        }
        paDetail.clear();

        String lsSQL = "SELECT * FROM " + poDetail.getTable()
                + " WHERE sTransNox = " + SQLUtil.toSQL(transactionNo);

        ResultSet loRS = poGRider.executeQuery(lsSQL);

        if (MiscUtil.RecordCount(loRS) > 0) {
            while (loRS.next()) {
                Model loDetail = (Model) poDetail.clone();
                loDetail.newRecord();
                poJSON = loDetail.openRecord(transactionNo, loRS.getString("sClustrID"));

                if ("error".equals((String) poJSON.get("result"))) {
                    poJSON.put("message", "Unable to open transaction detail record.");
                    clear();
                    return poJSON;
                }
                loDetail.updateRecord();

                paDetail.add(loDetail);
            }
        }
        poEvent = new JSONObject();
        poEvent.put("event", "READY");

        pnEditMode = EditMode.READY;
        pbRecordExist = true;

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("result", "Transaction loaded successfully");
        return poJSON;
    }

    @Override
    public JSONObject newTransaction() throws CloneNotSupportedException {
        if (!pbInitTran) {
            poJSON.put("result", "error");
            poJSON.put("message", "Object is not initialized.");
            return poJSON;
        }

        poMaster.initialize();
        poMaster.newRecord();

        poDetail.initialize();
        poDetail.newRecord();

        paDetail.clear();
        paDetail.add(poDetail);

        poJSON = initFields();
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        getMaster().setIndustryId(psIndustryCode);
        getMaster().setCompanyID(psCompanyID);
        getMaster().setCategoryId(psCategorCD);
        getMaster().setBranchCode(poGRider.getBranchCode());

        pnEditMode = EditMode.ADDNEW;

        poEvent = new JSONObject();
        poEvent.put("event", "ADD NEW");

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    protected JSONObject willSave() {
        poJSON = new JSONObject();

        poJSON = isEntryOkay(DeliveryScheduleStatus.OPEN);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //assign values needed
        poJSON.put("result", "success");
        return poJSON;

    }

    @Override
    public JSONObject saveTransaction() throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();

        if (!pbInitTran) {
            poJSON.put("result", "error");
            poJSON.put("message", "Object is not initialized.");
            return poJSON;
        }

        if (pnEditMode == EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "Saving of unmodified transaction is not allowed.");
            return poJSON;
        }
        poJSON = willSave();
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (getEditMode() == EditMode.ADDNEW || pnEditMode == EditMode.UPDATE) {
            pdModified = poGRider.getServerDate();
            poMaster.setValue("sModified", poGRider.Encrypt(poGRider.getUserID()));
        }

        poJSON = save();

        if (!pbWthParent) {
            poGRider.beginTrans((String) poEvent.get("event"),
                    poMaster.getTable(),
                    SOURCE_CODE,
                    String.valueOf(poMaster.getValue(1)));
        }

        if ("success".equals((String) poJSON.get("result"))) {
            //save master and detail
            if (pbVerifyEntryNo) {
                poMaster.setValue("nEntryNox", paDetail.size());
            }

            if (pnEditMode == EditMode.ADDNEW || pnEditMode == EditMode.UPDATE) {
                poMaster.setValue("dModified", pdModified);
                poJSON = poMaster.saveRecord();

                if ("error".equals((String) poJSON.get("result"))) {
                    if (!pbWthParent) {
                        poGRider.rollbackTrans();
                    }
                    return poJSON;
                }

                for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr++) {
                    if (paDetail.get(lnCtr).getValue("sClustrID") == null) {
                        continue;
                    }
                    paDetail.get(lnCtr).setValue("sTransNox", String.valueOf(poMaster.getValue(1)));
                    paDetail.get(lnCtr).setValue("dModified", pdModified);
                    System.out.println("Cluster ID : " + paDetail.get(lnCtr).getValue("sClustrID"));
                    poJSON = paDetail.get(lnCtr).saveRecord();
                    System.out.println(poJSON.get("message"));
                    if ("error".equals((String) poJSON.get("result"))) {
                        if (!pbWthParent) {
                            poGRider.rollbackTrans();
                        }
                        return poJSON;
                    }
                }
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "Edit mode is not allowed to save transaction.");
                return poJSON;
            }
        } else {
            if (!pbWthParent) {
                poGRider.rollbackTrans();
            }

            return poJSON;
        }

        if (!pbWthParent) {
            poGRider.commitTrans();
        }

        pnEditMode = EditMode.UNKNOWN;
        pbRecordExist = true;
//        paMaster = new ArrayList<Model>();
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "success");
        loJSON.put("message", "Transaction saved successfully.");
        openTransaction(getMaster().getTransactionNo());
        return loJSON;
    }

    public JSONObject UpdateTransaction() {
        poJSON = new JSONObject();
        if (DeliveryScheduleStatus.CONFIRMED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already confirmed.");
            return poJSON;
        }
        if (DeliveryScheduleStatus.VOID.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already voided.");
            return poJSON;
        }
        if (DeliveryScheduleStatus.CANCELLED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already cancelled.");
            return poJSON;
        }
        if (DeliveryScheduleStatus.POSTED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already posted.");
            return poJSON;
        }

        poJSON = updateTransaction();
        if ("success".equals((String) poJSON.get("result"))) {
            Model_Delivery_Schedule_Detail loDetail;
            loDetail = new DeliveryIssuanceModels(poGRider).DeliveryScheduleDetail();
            loDetail.newRecord();
            loDetail.setTransactionNo(getMaster().getTransactionNo());

            paDetail.add(loDetail);
        }
        return poJSON;
    }

    @Override
    protected JSONObject isEntryOkay(String status) {
        GValidator loValidator = DeliveryScheduleValidatorFactory.make(getMaster().getIndustryId());

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

    public JSONObject CloseTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (DeliveryScheduleStatus.CONFIRMED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already confirmed.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(DeliveryScheduleStatus.CONFIRMED);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, getMaster().getTransactionNo());

        poJSON = statusChange(poMaster.getTable(),
                (String) poMaster.getValue("sTransNox"),
                "ConfirmTransaction",
                DeliveryScheduleStatus.CONFIRMED,
                false, true);
        if ("error".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "success");
        loJSON.put("message", "Transaction confirmed successfully.");

        openTransaction(getMaster().getTransactionNo());
        return loJSON;
    }

    public JSONObject CancelTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid Edit Mode");
            return poJSON;
        }

//        if (DeliveryScheduleStatus.CONFIRMED.equals((String) poMaster.getValue("cTranStat"))) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "Transaction was already confirmed.");
//            return poJSON;
//        }
//        if (DeliveryScheduleStatus.CANCELLED.equals((String) poMaster.getValue("cTranStat"))) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "Transaction was already cancelled.");
//            return poJSON;
//        }
        if (DeliveryScheduleStatus.VOID.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already voided.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(DeliveryScheduleStatus.CANCELLED);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poGRider.beginTrans("UPDATE STATUS", "CancelTransaction", SOURCE_CODE, getMaster().getTransactionNo());

        poJSON = statusChange(poMaster.getTable(),
                (String) poMaster.getValue("sTransNox"),
                "CancelTransaction",
                DeliveryScheduleStatus.CANCELLED,
                false, true);
        if ("error".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "success");
        loJSON.put("message", "Transaction cancelled successfully.");

        openTransaction(getMaster().getTransactionNo());
        return loJSON;
    }

    public JSONObject VoidTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid Edit Mode.");
            return poJSON;
        }

//        if (DeliveryScheduleStatus.CONFIRMED.equals((String) poMaster.getValue("cTranStat"))) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "Transaction was already confirmed.");
//            return poJSON;
//        }
//        if (DeliveryScheduleStatus.CANCELLED.equals((String) poMaster.getValue("cTranStat"))) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "Transaction was already cancelled.");
//            return poJSON;
//        }
        if (DeliveryScheduleStatus.VOID.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already voided.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(DeliveryScheduleStatus.VOID);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poGRider.beginTrans("UPDATE STATUS", "VoidTransaction", SOURCE_CODE, getMaster().getTransactionNo());

        poJSON = statusChange(poMaster.getTable(),
                (String) poMaster.getValue("sTransNox"),
                "VoidTransaction",
                DeliveryScheduleStatus.VOID,
                false, true);
        if ("error".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "success");
        loJSON.put("message", "Transaction voided successfully.");

        openTransaction(getMaster().getTransactionNo());
        return loJSON;
    }

    public JSONObject searchClusterBranch(int row, String value, boolean byCode) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        BranchCluster loSubClass = new ParamControllers(poGRider, logwrapr).BranchCluster();
        loSubClass.setRecordStatus(RecordStatus.ACTIVE);
//        if (getMaster().getIndustryId() == null || "".equals(getMaster().getIndustryId())) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "Industry is not set.");
//            return poJSON;
//        }
        if (!psIndustryCode.isEmpty()) {
            loSubClass.getModel().setIndustryCode(psIndustryCode);
        }
        poJSON = loSubClass.searchRecordbyIndustry(value, byCode);

        System.out.println("result " + (String) poJSON.get("result"));
        if ("success".equals((String) poJSON.get("result"))) {
//            for (int lnExisting = 0; lnExisting <= paDetail.size() - 1; lnExisting++) {
//                Model_Delivery_Schedule_Detail loExisting = ((Model_Delivery_Schedule_Detail) paDetail.get(lnExisting));
//                if (loExisting.getClusterID() != null) {
//                    if (loExisting.getClusterID().equals(loSubClass.getModel().getClusterID())
//                            && loSubClass.getModel().getClusterID() != null
//                            && !loSubClass.getModel().getClusterID().isEmpty()) {
//                        poJSON = new JSONObject();
//                        poJSON.put("result", "error");
//                        poJSON.put("message", "Selected Cluster ID is already exist!");
//                        return poJSON;
//
//                    }
//                }
//
//            }
            getDetail(row).setClusterID(loSubClass.getModel().getClusterID());
        } else {
            getDetail(row).setClusterID("");
        }
        return poJSON;

    }

    public JSONObject loadTransactionList(String value, String column)
            throws SQLException, GuanzonException, CloneNotSupportedException {
//        poJSON = new JSONObject();

        paMaster.clear();
        String lsSQL = SQL_BROWSE;
        String lsCondition = "";
        if (psTranStat != null) {
            if (this.psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= this.psTranStat.length() - 1; lnCtr++) {
                    lsCondition = lsCondition + ", " + SQLUtil.toSQL(Character.toString(this.psTranStat.charAt(lnCtr)));
                }
                lsCondition = "cTranStat IN (" + lsCondition.substring(2) + ")";
            } else {
                lsCondition = "cTranStat = " + SQLUtil.toSQL(this.psTranStat);
            }
            lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        }
        if (value != null && !value.isEmpty()) {
            //sTransNox/dTransact/dSchedule
            lsSQL = MiscUtil.addCondition(lsSQL, column + "= " + SQLUtil.toSQL(value));
        }
        if (!psIndustryCode.isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "sIndstCdx = " + SQLUtil.toSQL(psIndustryCode));
        }
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        if (MiscUtil.RecordCount(loRS)
                <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "No record found.");
            return poJSON;
        }

        while (loRS.next()) {
            Model_Delivery_Schedule_Master loDeliverySchedule = new DeliveryIssuanceModels(poGRider).DeliverySchedule();

            poJSON = loDeliverySchedule.openRecord(loRS.getString("sTransNox"));

            if ("success".equals((String) poJSON.get("result"))) {
                paMaster.add((Model) loDeliverySchedule);
            } else {
                return poJSON;
            }
        }

        poJSON = new JSONObject();
        poJSON.put(
                "result", "success");
        return poJSON;
    }

    public JSONObject PostTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        if (getEditMode() != EditMode.UPDATE
                && getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid Edit Mode.");
            return poJSON;
        }

        if (DeliveryScheduleStatus.POSTED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already posted.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(DeliveryScheduleStatus.POSTED);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        poGRider.beginTrans("UPDATE STATUS", "PostTransaction", SOURCE_CODE, getMaster().getTransactionNo());

        poJSON = statusChange(poMaster.getTable(),
                (String) poMaster.getValue("sTransNox"),
                "PostTransaction",
                DeliveryScheduleStatus.POSTED,
                false, true);
        if ("error".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "success");
        loJSON.put("message", "Transaction posted successfully.");
        openTransaction(getMaster().getTransactionNo());
        return loJSON;
    }

    public void ShowStatusHistory() throws SQLException, GuanzonException, Exception {
        CachedRowSet crs = getStatusHistory();

        crs.beforeFirst();

        while (crs.next()) {
            switch (crs.getString("cRefrStat")) {
                case "":
                    crs.updateString("cRefrStat", "-");
                    break;
                case DeliveryScheduleStatus.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case DeliveryScheduleStatus.CONFIRMED:
                    crs.updateString("cRefrStat", "CONFIRMED");
                    break;
                case DeliveryScheduleStatus.POSTED:
                    crs.updateString("cRefrStat", "POSTED");
                    break;
                case DeliveryScheduleStatus.CANCELLED:
                    crs.updateString("cRefrStat", "CANCELLED");
                    break;
                case DeliveryScheduleStatus.VOID:
                    crs.updateString("cRefrStat", "VOID");
                    break;

                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);

                    switch (stat) {
                        case DeliveryScheduleStatus.OPEN:
                            crs.updateString("cRefrStat", "OPEN");
                            break;
                        case DeliveryScheduleStatus.CONFIRMED:
                            crs.updateString("cRefrStat", "CONFIRMED");
                            break;
                        case DeliveryScheduleStatus.POSTED:
                            crs.updateString("cRefrStat", "POSTED");
                            break;
                        case DeliveryScheduleStatus.CANCELLED:
                            crs.updateString("cRefrStat", "CANCELLED");
                            break;
                        case DeliveryScheduleStatus.VOID:
                            crs.updateString("cRefrStat", "VOID");
                            break;

                    }
            }
            crs.updateRow();
        }

        JSONObject loJSON = getEntryBy();
        String entryBy = "";
        String entryDate = "";

        if ("success".equals((String) loJSON.get("result"))) {
            entryBy = (String) loJSON.get("sCompnyNm");
            entryDate = (String) loJSON.get("sEntryDte");
        }

        showStatusHistoryUI("Inventory Stock Request", (String) poMaster.getValue("sTransNox"), entryBy, entryDate, crs);
    }

    public JSONObject getEntryBy() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsEntry = "";
        String lsEntryDate = "";
        String lsSQL = " SELECT b.sModified, b.dModified "
                + " FROM Delivery_Schedule_Master a "
                + " LEFT JOIN xxxAuditLogMaster b ON"
                + " b.sSourceNo = a.sTransNox AND b.sEventNme LIKE 'ADD%NEW' AND b.sRemarksx = " + SQLUtil.toSQL(getMaster().getTable());
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox =  " + SQLUtil.toSQL(getMaster().getTransactionNo()));
        System.out.println("Execute SQL : " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0L) {
                if (loRS.next()) {
                    if (loRS.getString("sModified") != null && !"".equals(loRS.getString("sModified"))) {
                        if (loRS.getString("sModified").length() > 10) {
                            lsEntry = getSysUser(poGRider.Decrypt(loRS.getString("sModified")));
                        } else {
                            lsEntry = getSysUser(loRS.getString("sModified"));
                        }
                        // Get the LocalDateTime from your result set
                        LocalDateTime dModified = loRS.getObject("dModified", LocalDateTime.class);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
                        lsEntryDate = dModified.format(formatter);
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
            return poJSON;
        }

        poJSON.put("result", "success");
        poJSON.put("sCompnyNm", lsEntry);
        poJSON.put("sEntryDte", lsEntryDate);
        return poJSON;
    }

    public String getSysUser(String fsId) throws SQLException, GuanzonException {
        String lsEntry = "";
        String lsSQL = " SELECT IFNULL(b.sCompnyNm,'') sCompnyNm FROM xxxSysUser a "
                + " LEFT JOIN Client_Master b ON b.sClientID = a.sEmployNo ";
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sUserIDxx =  " + SQLUtil.toSQL(fsId));
        System.out.println("SQL " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0L) {
                if (loRS.next()) {
                    lsEntry = loRS.getString("sCompnyNm");
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }
        return lsEntry;
    }
//    public JSONObject getCategory() throws SQLException, GuanzonException {
//        if (!"".equals(psIndustryCode)) {
//
//            String lsSQL = "SELECT "
//                    + " sCategrCd"
//                    + ", sDescript"
//                    + ", sIndstCdx"
//                    + ", sInvTypCd"
//                    + ", cRecdStat "
//                    + " FROM Category "
//                    + "  WHERE cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
//                    + " AND sIndstCdx = " + SQLUtil.toSQL(psIndustryCode);
//
//            ResultSet loRS = poGRider.executeQuery(lsSQL);
//
//            if (MiscUtil.RecordCount(loRS)
//                    <= 0) {
//                poJSON.put("result", "error");
//                poJSON.put("message", "No record found.");
//                return poJSON;
//            }
//            loRS.beforeFirst();
//            if (loRS.next()) {
//                Model_Category loCategory = new ParamModels(poGRider).Category();
//                poJSON = loCategory.openRecord(loRS.getString("sCategrCd"));
//                if ("success".equals((String) poJSON.get("result"))) {
//                    psCategorCD = loRS.getString("sCategrCd");
//                    getMaster().setCategoryId(psCategorCD);
//                    return poJSON;
//                } else {
//                    poJSON.put("result", "error");
//                    poJSON.put("message", "No record found.");
//                    return poJSON;
//                }
//            }
//        } else {
//            //General
//            psCategorCD = "0007";
//
//        }
//        poJSON.put("result", "success");
//        poJSON.put("message", "Industry is General");
//        return poJSON;
//
//    }
}
