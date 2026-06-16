package org.guanzon.cas.inv.warehouse;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javax.sql.rowset.CachedRowSet;
import net.sf.jasperreports.engine.JRException;
import org.guanzon.appdriver.agent.ActionAuthManager;
import org.guanzon.appdriver.agent.MatrixAuthChecker;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.client.model.Model_Client_Master;
import org.guanzon.cas.client.services.ClientModels;
import org.guanzon.cas.inv.warehouse.model.Model_Inv_Stock_Request_Detail;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;
import org.guanzon.cas.inv.warehouse.status.DeliveryIssuanceType;
import org.guanzon.cas.inv.warehouse.status.InventoryStockIssuancePrint;
import org.guanzon.cas.inv.warehouse.status.InventoryStockIssuanceStatus;
import org.guanzon.cas.inv.warehouse.model.Model_Inventory_Transfer_Detail;
import org.guanzon.cas.inv.warehouse.model.Model_Inventory_Transfer_Detail_Expiration;
import org.guanzon.cas.inv.warehouse.model.Model_Inventory_Transfer_Master;
import org.guanzon.cas.inv.InventoryBrowse;
import org.guanzon.cas.inv.InventoryTransaction;
import org.guanzon.cas.inv.warehouse.report.ReportUtil;
import org.guanzon.cas.inv.warehouse.report.ReportUtilListener;
import org.guanzon.cas.inv.warehouse.services.DeliveryIssuanceControllers;
import org.guanzon.cas.inv.warehouse.services.DeliveryIssuanceModels;
import org.guanzon.cas.inv.warehouse.validators.InventoryIssuanceValidatorFactory;
import org.json.simple.JSONArray;

public class InventoryStockIssuanceNeo extends Transaction {

    private String psIndustryCode = "";
    private String psCompanyID = "";
    private String psCategorCD = "";
    private String psApprovalUser = "";
    private List<Model> paMaster;
    public Model poDetailExpiration;
    public List<Model> paDetailExpiration;

    public void setIndustryID(String industryId) {
        psIndustryCode = industryId;
    }

    public void setCompanyID(String companyId) {
        psCompanyID = companyId;
    }

    public void setCategoryID(String categoryId) {
        psCategorCD = categoryId;
    }

    public Model_Inventory_Transfer_Master getMaster() {
        return (Model_Inventory_Transfer_Master) poMaster;
    }

    @SuppressWarnings("unchecked")
    public List<Model_Inventory_Transfer_Master> getMasterList() {
        return (List<Model_Inventory_Transfer_Master>) (List<?>) paMaster;
    }

    public Model_Inventory_Transfer_Master getMaster(int masterRow) {
        return (Model_Inventory_Transfer_Master) paMaster.get(masterRow);

    }

    @SuppressWarnings("unchecked")
    public List<Model_Inventory_Transfer_Detail> getDetailList() {
        return (List<Model_Inventory_Transfer_Detail>) (List<?>) paDetail;
    }

    public Model_Inventory_Transfer_Detail getDetail(int entryNo) {
        if (getMaster().getTransactionNo().isEmpty() || entryNo <= 0) {
            return null;
        }

        //autoadd detail if empty
        Model_Inventory_Transfer_Detail lastDetail = (Model_Inventory_Transfer_Detail) paDetail.get(paDetail.size() - 1);
        String stockID = lastDetail.getStockId();
        if (stockID != null && !stockID.trim().isEmpty()) {
            Model_Inventory_Transfer_Detail newDetail = new DeliveryIssuanceModels(poGRider).InventoryTransferDetail();
            newDetail.newRecord();
            newDetail.setTransactionNo(getMaster().getTransactionNo());
            newDetail.setEntryNo(paDetail.size() + 1);
            paDetail.add(newDetail);
        }

        Model_Inventory_Transfer_Detail loDetail;

        //find the detail record
        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr++) {
            loDetail = (Model_Inventory_Transfer_Detail) paDetail.get(lnCtr);

            if (loDetail.getEntryNo() == entryNo) {
                return loDetail;
            }
        }

        loDetail = new DeliveryIssuanceModels(poGRider).InventoryTransferDetail();
        loDetail.newRecord();
        loDetail.setTransactionNo(getMaster().getTransactionNo());
        loDetail.setEntryNo(entryNo);
        paDetail.add(loDetail);

        return loDetail;
    }

    public Model_Inventory_Transfer_Detail_Expiration getDetailOther(int entryNo) {
        if (getMaster().getTransactionNo().isEmpty()
                || getMaster().getIndustryId().isEmpty()) {
            return null;
        }

        if (entryNo <= 0 || entryNo > paDetailExpiration.size()) {
            return null;
        }

        Model_Inventory_Transfer_Detail_Expiration loDetailExpiration;

        //find the detail record
        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr++) {
            loDetailExpiration = (Model_Inventory_Transfer_Detail_Expiration) paDetailExpiration.get(lnCtr);

            if (loDetailExpiration.getEntryNo() == entryNo) {
                return loDetailExpiration;
            }
        }

        loDetailExpiration = new DeliveryIssuanceModels(poGRider).InventoryTransferDetailExpiration();
        loDetailExpiration.newRecord();
        loDetailExpiration.setTransactionNo(getMaster().getTransactionNo());
        paDetailExpiration.add(loDetailExpiration);

        return loDetailExpiration;
    }

    @SuppressWarnings("unchecked")
    public List<Model_Inventory_Transfer_Detail_Expiration> getDetailListOther() {
        return (List<Model_Inventory_Transfer_Detail_Expiration>) (List<?>) paDetailExpiration;
    }

    public JSONObject initTransaction() throws GuanzonException, SQLException {
        SOURCE_CODE = "Dlvr";

        poMaster = new DeliveryIssuanceModels(poGRider).InventoryTransferMaster();
        poDetail = new DeliveryIssuanceModels(poGRider).InventoryTransferDetail();
        poDetailExpiration = new DeliveryIssuanceModels(poGRider).InventoryTransferDetailExpiration();
        paMaster = new ArrayList<Model>();
        paDetail = new ArrayList<Model>();
        initSQL();

        return super.initialize();
    }

    @Override
    public void initSQL() {
        SQL_BROWSE = "SELECT"
                + " a.sTransNox"
                + ", a.dTransact"
                + ", d.sBranchNm xBranchNm"
                + ", e.sBranchNm xDestinat"
                + ", c.sCompnyNm sCompnyNm"
                + ", a.sBranchCd"
                + ", a.sDestinat"
                + " FROM Inv_Transfer_Master a "
                + "     LEFT JOIN AP_Client_Master b ON a.sTruckIDx = b.sClientID"
                + "     LEFT JOIN Client_Master c ON b.sClientID = c.sClientID"
                + "     LEFT JOIN Branch d ON a.sBranchCd = d.sBranchCd"
                + "     LEFT JOIN Branch e ON a.sDestinat = e.sBranchCd";
    }

    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        return openTransaction(transactionNo);
    }

    public JSONObject NewTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();
        poJSON = newTransaction();
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        getMaster().setIndustryId(psIndustryCode);
        getMaster().setCompanyID(psCompanyID);
        getMaster().setCategoryId(psCategorCD);
        getMaster().setBranchCode(poGRider.getBranchCode());
        return poJSON;
    }

    public JSONObject SaveTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        JSONObject loJSON = new JSONObject();
        loJSON = saveTransaction();
        openTransaction(getMaster().getTransactionNo());
        return loJSON;
    }

    public JSONObject UpdateTransaction() {
        poJSON = new JSONObject();

        if (InventoryStockIssuanceStatus.CONFIRMED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already confirmed.");
            return poJSON;
        }

        if (InventoryStockIssuanceStatus.CANCELLED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already cancelled.");
            return poJSON;
        }

        return updateTransaction();
    }

    public JSONObject UpdateTransactionPosting() {
        poJSON = new JSONObject();

        if (InventoryStockIssuanceStatus.CANCELLED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already cancelled.");
            return poJSON;
        }

        return updateTransaction();
    }

    @Override
    protected JSONObject willSave() throws SQLException {
        poJSON = new JSONObject();

        poJSON = isEntryOkay(InventoryStockIssuanceStatus.OPEN);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        int lnDetailCount = 0;

        //assign values needed
        for (int lnCtr = 0; lnCtr < paDetail.size(); lnCtr++) {
            Model_Inventory_Transfer_Detail loDetail = (Model_Inventory_Transfer_Detail) paDetail.get(lnCtr);
            if (loDetail.getQuantity() > 0 && !loDetail.getStockId().isEmpty()) {

                lnDetailCount++;
                loDetail.setTransactionNo(getMaster().getTransactionNo());
                loDetail.setEntryNo(lnDetailCount);
            } else {
                paDetail.remove(lnCtr);

            }
        }

        getMaster().setEntryNo(lnDetailCount);
        pdModified = poGRider.getServerDate();

        poJSON.put("result", "success");
        return poJSON;

    }

    @Override
    protected JSONObject isEntryOkay(String status) {
        psApprovalUser = "";

        poJSON = new JSONObject();
        GValidator loValidator = InventoryIssuanceValidatorFactory.make(getMaster().getIndustryId());

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
                    psApprovalUser = poJSON.get("sUserIDxx") != null
                            ? poJSON.get("sUserIDxx").toString()
                            : poGRider.getUserID();
                }
            } else {
                psApprovalUser = poGRider.getUserID();
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

        if (InventoryStockIssuanceStatus.CONFIRMED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "success");
            poJSON.put("message", "Transaction confirmed successfully.");
            return poJSON;
        }

        if (InventoryStockIssuanceStatus.VOID.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Void.");
            return poJSON;
        }
        if (InventoryStockIssuanceStatus.CANCELLED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Cancelled.");
            return poJSON;
        }

        if (InventoryStockIssuanceStatus.POSTED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Posted.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(InventoryStockIssuanceStatus.CONFIRMED);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        boolean lbConfirm = true;
        String lsStatus = InventoryStockIssuanceStatus.CONFIRMED;
        MatrixAuthChecker check = null;

        if (!pbWthParent) {
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //get the matrix return from isEntryOkey
            JSONArray loMatrix = (JSONArray) poJSON.get("matrix");

            //Check if there is a authorization request
            if (loMatrix != null) {
                //initialized MatrixAuthChecker object
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, getMaster().getTransactionNo());
                //load the current autorization matrix request
                poJSON = check.loadAuth();

                //check if loading is okey
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                //check if authorization request is already approved by all authorizing personnel
                if (!check.isAuthOkay()) {
                    //check if authorization request allows system approval
                    if (!check.isAllowSys()) {
                        //extract the JSONObject from JSONArray
                        JSONObject loJson = (JSONObject) loMatrix.get(0);

                        //check if current user is authorized to approved this transaction
                        poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());

                        //If not authorized/request system approval
                        if (!"success".equalsIgnoreCase((String) poJSON.get("result"))) {
                            poJSON = ShowDialogFX.getUserApproval(poGRider);
                            if ("error".equals((String) poJSON.get("result"))) {
                                return poJSON;
                            }

                            //check if approving officer is authorized
                            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
                            //check if current user is authorized to approved this transaction
                            poJSON = check.authTrans((String) loJson.get("sAuthType"), lsUserIDxx);
                            //user is not authorized
                            if (!"success".equalsIgnoreCase((String) poJSON.get("result"))) {
                                return poJSON;
                            }
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if (!check.isAuthOkay()) {
                        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, getMaster().getTransactionNo());

                        lsStatus = Character.toString((char) (64 + Integer.parseInt(lsStatus)));
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), "", lsStatus, !lbConfirm, true);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }

                        poGRider.commitTrans();

                        poJSON.put("result", "matrix");
                        return poJSON;
                    }
                }
            } //there are no authorization event request
            else {
                //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
                poJSON = seekApproval();
                if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
                    return poJSON;
                }
            }
        }

//========================================Authority Check End===============================================
        if (!pbWthParent) {
            poGRider.beginTrans("UPDATE STATUS", "Close Transaction", SOURCE_CODE, getMaster().getTransactionNo());
        }

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), "", lsStatus, !lbConfirm, true);

        if (!"success".equals(
                (String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        String lsCondition = "0";

        InventoryTransaction loTrans = new InventoryTransaction(poGRider);
        loTrans.Delivery((String) poMaster.getValue("sTransNox"), (Date) poMaster.getValue("dTransact"), false);

        for (Model loDetail : paDetail) {
            Model_Inventory_Transfer_Detail detail = (Model_Inventory_Transfer_Detail) loDetail;

            //why int when cSerialize supposedly be a character
            //            if("1".equals((int) detail.getColumn("cSerialze"))){
            if (detail.Inventory().isSerialized()) {
                String lsSerialID = detail.InventorySerial().getSerialId();

                if (!lsSerialID.isEmpty()) {

                    loTrans.addSerial((String) poMaster.getValue("sIndstCdx"), lsSerialID,
                            detail.getOrderNo() == null ? false : !detail.getOrderNo().isEmpty(),
                            detail.Inventory().getCost().doubleValue(),
                            detail.InventorySerial().getLocation());
                }
            } else {
                if (detail.getStockId() != null) {
                    loTrans.addDetail((String) poMaster.getValue("sIndstCdx"), detail.getStockId(),
                            lsCondition,
                            detail.getQuantity().doubleValue(),
                            0,
                            detail.Inventory().getCost().doubleValue());
                }
            }
        }

        loTrans.saveTransaction();

        if (!"success".equals(
                (String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        if (check
                != null) {
            check.postAuth();
        }

        for (int lnCtr = 0;
                lnCtr < paDetail.size();
                lnCtr++) {
            Model_Inventory_Transfer_Detail loDetail = (Model_Inventory_Transfer_Detail) paDetail.get(lnCtr);

            if (loDetail.getOrderNo() != null) {
                if (!loDetail.getOrderNo().isEmpty()) {
                    poJSON = new JSONObject();
                    poJSON = SaveIssuedTransaction(lnCtr);

                    if (!"success".equals((String) poJSON.get("result"))) {

                        if (!pbWthParent) {
                            poGRider.rollbackTrans();
                        }
                        return poJSON;
                    }
                }
            }
        }

        if (!pbWthParent) {
            poGRider.commitTrans();
        }

        openTransaction(getMaster().getTransactionNo());
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        if (lbConfirm) {
            poJSON.put("message", "Transaction confirmed successfully.");
        } else {
            poJSON.put("message", "Transaction confirmation request submitted successfully.");
        }
        return poJSON;
    }

    public JSONObject SaveIssuedTransaction(int EntryNo) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        Model_Inventory_Transfer_Detail loDetail = (Model_Inventory_Transfer_Detail) paDetail.get(EntryNo);
        Model_Inv_Stock_Request_Detail loStockDetail = loDetail.InventoryStockRequest();
        if (loStockDetail.getEditMode() == EditMode.READY) {
            loStockDetail.updateRecord();
            loStockDetail.setIssued(loDetail.getQuantity());
            poJSON = loStockDetail.saveRecord();

            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;
    }

    public JSONObject UnSaveIssuedTransaction(int EntryNo) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        Model_Inventory_Transfer_Detail loDetail = (Model_Inventory_Transfer_Detail) paDetail.get(EntryNo);
        Model_Inv_Stock_Request_Detail loStockDetail = loDetail.InventoryStockRequest();
        if (loStockDetail.getEditMode() == EditMode.READY) {
            loStockDetail.updateRecord();
            loStockDetail.setIssued(-loDetail.getQuantity());
            poJSON = loStockDetail.saveRecord();

            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
        }
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;
    }

    public JSONObject PostTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        if (getEditMode() != EditMode.UPDATE) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }
        //do not allow to post if not confirm/departured if cluster delivery
        if (getMaster().getOrderNo() != null) {
            if (!getMaster().getOrderNo().isEmpty()) {
                String lsSQL = " SELECT cTranStat FROM Cluster_Delivery_Master a "
                        + "LEFT JOIN Cluster_Delivery_Detail b ON a.sTransNox = b.sTransNox  ";
                lsSQL = MiscUtil.addCondition(lsSQL, " b.sReferNox =  " + SQLUtil.toSQL(getMaster().getTransactionNo()));
                System.out.println("SQL " + lsSQL);
                ResultSet loRS = poGRider.executeQuery(lsSQL);
                try {
                    if (MiscUtil.RecordCount(loRS) > 0L) {
                        loRS.beforeFirst();
                        if (loRS.next()) {
                            if (!loRS.getString("cTranStat").equals("1"));
                            poJSON.put("result", "error");
                            poJSON.put("message", "Cluster Delivery is not yet confirmed.");
                            return poJSON;
                        }
                    }
                    MiscUtil.close(loRS);
                } catch (SQLException e) {
                    poJSON.put("result", "error");
                    poJSON.put("message", e.getMessage());
                    return poJSON;
                }
            }
        }

        if (InventoryStockIssuanceStatus.VOID.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Void.");
            return poJSON;
        }
        if (InventoryStockIssuanceStatus.CANCELLED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Cancelled.");
            return poJSON;
        }

        if (InventoryStockIssuanceStatus.POSTED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Posted.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(InventoryStockIssuanceStatus.POSTED);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        boolean lbConfirm = true;
        String lsStatus = InventoryStockIssuanceStatus.POSTED;
        MatrixAuthChecker check = null;

        if (!pbWthParent) {
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //get the matrix return from isEntryOkey
            JSONArray loMatrix = (JSONArray) poJSON.get("matrix");

            //Check if there is a authorization request
            if (loMatrix != null) {
                //initialized MatrixAuthChecker object
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, getMaster().getTransactionNo());
                //load the current autorization matrix request
                poJSON = check.loadAuth();

                //check if loading is okey
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                //check if authorization request is already approved by all authorizing personnel
                if (!check.isAuthOkay()) {
                    //check if authorization request allows system approval
                    if (!check.isAllowSys()) {
                        //extract the JSONObject from JSONArray
                        JSONObject loJson = (JSONObject) loMatrix.get(0);

                        //check if current user is authorized to approved this transaction
                        poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());

                        //If not authorized/request system approval
                        if (!"success".equalsIgnoreCase((String) poJSON.get("result"))) {
                            poJSON = ShowDialogFX.getUserApproval(poGRider);
                            if ("error".equals((String) poJSON.get("result"))) {
                                return poJSON;
                            }

                            //check if approving officer is authorized
                            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
                            //check if current user is authorized to approved this transaction
                            poJSON = check.authTrans((String) loJson.get("sAuthType"), lsUserIDxx);
                            //user is not authorized
                            if (!"success".equalsIgnoreCase((String) poJSON.get("result"))) {
                                return poJSON;
                            }
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if (!check.isAuthOkay()) {
                        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, getMaster().getTransactionNo());

                        lsStatus = Character.toString((char) (64 + Integer.parseInt(lsStatus)));
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), "", lsStatus, !lbConfirm, true);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }

                        poGRider.commitTrans();

                        poJSON.put("result", "matrix");
                        return poJSON;
                    }
                }
            } //there are no authorization event request
            else {
                //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
                poJSON = seekApproval();
                if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
                    return poJSON;
                }
            }
        }

//========================================Authority Check End===============================================
        if (!pbWthParent) {
            poGRider.beginTrans("UPDATE STATUS", "Post Transaction", SOURCE_CODE, getMaster().getTransactionNo());
        }

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), "", lsStatus, !lbConfirm, true);

        if (!"success".equals(
                (String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        Model_Inventory_Transfer_Master loMaster = (Model_Inventory_Transfer_Master) this.poMaster;
        loMaster.updateRecord();
        loMaster.setTransactionStatus(lsStatus);
        loMaster.setReceivedBy(poGRider.getUserID());
        if (!"success".equals((String) loMaster.saveRecord().get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        String lsCondition = "0";

        InventoryTransaction loTrans = new InventoryTransaction(poGRider);
        loTrans.DeliveryAcceptance((String) poMaster.getValue("sTransNox"), (Date) poMaster.getValue("dTransact"), false);

        for (Model loDetail : paDetail) {
            Model_Inventory_Transfer_Detail detail = (Model_Inventory_Transfer_Detail) loDetail;
            detail.updateRecord();

            if (!"success".equals((String) detail.saveRecord().get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
            //why int when cSerialize supposedly be a character
            //            if("1".equals((int) detail.getColumn("cSerialze"))){
            if (detail.Inventory().isSerialized()) {
                String lsSerialID = detail.InventorySerial().getSerialId();

                if (!lsSerialID.isEmpty()) {

                    loTrans.addSerial((String) poMaster.getValue("sIndstCdx"), lsSerialID,
                            detail.getOrderNo() == null ? false : !detail.getOrderNo().isEmpty(),
                            detail.Inventory().getCost().doubleValue(),
                            detail.InventorySerial().getLocation());
                }
            } else {
                if (detail.getStockId() != null) {
                    loTrans.addDetail((String) poMaster.getValue("sIndstCdx"), detail.getStockId(),
                            lsCondition,
                            detail.getReceivedQuantity().doubleValue(),
                            0,
                            detail.Inventory().getCost().doubleValue());
                }
            }

        }

        loTrans.saveTransaction();

        if (!"success".equals(
                (String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

//        if (loMaster.getOrderNo() != null) {
//            if (!loMaster.getOrderNo().isEmpty()) {
//                poJSON = new JSONObject();
//                InventoryStockIssuance loIssuance = new DeliveryIssuanceControllers(poGRider, null).InventoryStockIssuance();
//                loIssuance.initTransaction();
//
//                loIssuance.OpenTransaction(loMaster.getOrderNo());
//                loIssuance.getMaster().setArrivalDate(poGRider.getServerDate());
//                loIssuance.setApproving(poGRider.getUserID());
//                poJSON = loIssuance.PostTransaction();
//
//                if (!"success".equals((String) poJSON.get("result"))) {
//                    if (!pbWthParent) {
//                        poGRider.rollbackTrans();
//                    }
//                    return poJSON;
//                }
//            }
//        }
        if (check
                != null) {
            check.postAuth();
        }

        for (int lnCtr = 0; lnCtr < paDetail.size(); lnCtr++) {
            Model_Inventory_Transfer_Detail loDetail = (Model_Inventory_Transfer_Detail) paDetail.get(lnCtr);

            if (loDetail.getOrderNo() != null) {
                if (!loDetail.getOrderNo().isEmpty()) {
                    poJSON = new JSONObject();
                    poJSON = SaveIssuedTransaction(lnCtr);

                    if (!"success".equals((String) poJSON.get("result"))) {

                        if (!pbWthParent) {
                            poGRider.rollbackTrans();
                        }
                        return poJSON;
                    }
                }
            }
        }

        if (!pbWthParent) {
            poGRider.commitTrans();
        }

        openTransaction(getMaster().getTransactionNo());
        poJSON = new JSONObject();

        poJSON.put(
                "result", "success");
        if (lbConfirm) {
            poJSON.put("message", "Transaction posted successfully.");
        } else {
            poJSON.put("message", "Transaction posting request submitted successfully.");
        }
        return poJSON;
    }

    public JSONObject CancelTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

//        if (InventoryStockIssuanceStatus.CONFIRMED.equals((String) poMaster.getValue("cTranStat"))) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "Transaction was already confirmed.");
//            return poJSON;
//        }
        if (InventoryStockIssuanceStatus.VOID.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Void.");
            return poJSON;
        }
        if (InventoryStockIssuanceStatus.CANCELLED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Cancelled.");
            return poJSON;
        }

        if (InventoryStockIssuanceStatus.POSTED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Posted.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(InventoryStockIssuanceStatus.CANCELLED);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        boolean lbConfirm = true;
        String lsStatus = InventoryStockIssuanceStatus.CANCELLED;
        MatrixAuthChecker check = null;

        if (!pbWthParent) {
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //get the matrix return from isEntryOkey
            JSONArray loMatrix = (JSONArray) poJSON.get("matrix");

            //Check if there is a authorization request
            if (loMatrix != null) {
                //initialized MatrixAuthChecker object
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, getMaster().getTransactionNo());
                //load the current autorization matrix request
                poJSON = check.loadAuth();

                //check if loading is okey
                if (!"success".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                //check if authorization request is already approved by all authorizing personnel
                if (!check.isAuthOkay()) {
                    //check if authorization request allows system approval
                    if (!check.isAllowSys()) {
                        //extract the JSONObject from JSONArray
                        JSONObject loJson = (JSONObject) loMatrix.get(0);

                        //check if current user is authorized to approved this transaction
                        poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());

                        //If not authorized/request system approval
                        if (!"success".equalsIgnoreCase((String) poJSON.get("result"))) {
                            poJSON = ShowDialogFX.getUserApproval(poGRider);
                            if ("error".equals((String) poJSON.get("result"))) {
                                return poJSON;
                            }

                            //check if approving officer is authorized
                            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
                            //check if current user is authorized to approved this transaction
                            poJSON = check.authTrans((String) loJson.get("sAuthType"), lsUserIDxx);
                            //user is not authorized
                            if (!"success".equalsIgnoreCase((String) poJSON.get("result"))) {
                                return poJSON;
                            }
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if (!check.isAuthOkay()) {
                        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, getMaster().getTransactionNo());

                        lsStatus = Character.toString((char) (64 + Integer.parseInt(lsStatus)));
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), "", lsStatus, !lbConfirm, true);
                        if (!"success".equals((String) poJSON.get("result"))) {
                            poGRider.rollbackTrans();
                            return poJSON;
                        }

                        poGRider.commitTrans();

                        poJSON.put("result", "matrix");
                        return poJSON;
                    }
                }
            } //there are no authorization event request
            else {
                //Replaced script above by calling of method Arsiela 10-15-2025 09:25:01
                poJSON = seekApproval();
                if ("error".equalsIgnoreCase((String) poJSON.get("result"))) {
                    return poJSON;
                }
            }
        }

//========================================Authority Check End===============================================
        if (!pbWthParent) {
            poGRider.beginTrans("UPDATE STATUS", "Cancel Transaction", SOURCE_CODE, getMaster().getTransactionNo());
        }

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), "", lsStatus, !lbConfirm, true);

        if (!"success".equals(
                (String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        String lsCondition = "0";

        InventoryTransaction loTrans = new InventoryTransaction(poGRider);
        loTrans.Delivery((String) poMaster.getValue("sTransNox"), (Date) poMaster.getValue("dTransact"), true);

        for (Model loDetail : paDetail) {
            Model_Inventory_Transfer_Detail detail = (Model_Inventory_Transfer_Detail) loDetail;

            //why int when cSerialize supposedly be a character
            //            if("1".equals((int) detail.getColumn("cSerialze"))){
            if (detail.Inventory().isSerialized()) {
                String lsSerialID = detail.InventorySerial().getSerialId();

                if (!lsSerialID.isEmpty()) {

                    loTrans.addSerial((String) poMaster.getValue("sIndstCdx"), lsSerialID,
                            detail.getOrderNo() == null ? false : !detail.getOrderNo().isEmpty(),
                            detail.Inventory().getCost().doubleValue(),
                            detail.InventorySerial().getLocation());
                }
            } else {
                if (detail.getStockId() != null) {
                    loTrans.addDetail((String) poMaster.getValue("sIndstCdx"), detail.getStockId(),
                            lsCondition,
                            detail.getQuantity().doubleValue(),
                            0,
                            detail.Inventory().getCost().doubleValue());
                }
            }
        }

        loTrans.saveTransaction();
        if (!"success".equals(
                (String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        if (check
                != null) {
            check.postAuth();
        }

        for (int lnCtr = 0;
                lnCtr < paDetail.size();
                lnCtr++) {
            Model_Inventory_Transfer_Detail loDetail = (Model_Inventory_Transfer_Detail) paDetail.get(lnCtr);

            if (loDetail.getOrderNo() != null) {
                if (!loDetail.getOrderNo().isEmpty()) {
                    poJSON = new JSONObject();
                    poJSON = UnSaveIssuedTransaction(lnCtr);

                    if (!"success".equals((String) poJSON.get("result"))) {

                        if (!pbWthParent) {
                            poGRider.rollbackTrans();
                        }
                        return poJSON;
                    }
                }
            }
        }

        if (!pbWthParent) {
            poGRider.commitTrans();
        }
        poJSON = new JSONObject();

        openTransaction(getMaster().getTransactionNo());
        poJSON.put("result", "success");
        if (lbConfirm) {
            poJSON.put("message", "Transaction cancelled successfully.");
        } else {
            poJSON.put("message", "Transaction cancellation request submitted successfully.");
        }
        return poJSON;
    }

    public JSONObject VoidTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid Edit Mode.");
            return poJSON;
        }

        if (InventoryStockIssuanceStatus.CONFIRMED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already confirmed.");
            return poJSON;
        }
        if (InventoryStockIssuanceStatus.CANCELLED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already cancelled.");
            return poJSON;
        }

        if (InventoryStockIssuanceStatus.VOID.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already voided.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(InventoryStockIssuanceStatus.VOID);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (!pbWthParent) {
            poGRider.beginTrans("UPDATE STATUS", "VoidTransaction", SOURCE_CODE, getMaster().getTransactionNo());
        }
        poJSON = statusChange(poMaster.getTable(),
                (String) poMaster.getValue("sTransNox"),
                "VoidTransaction",
                InventoryStockIssuanceStatus.VOID,
                false, true);
        if ("error".equals((String) poJSON.get("result"))) {
            if (!pbWthParent) {
                poGRider.rollbackTrans();
            }
            return poJSON;
        }

        if (!pbWthParent) {
            poGRider.commitTrans();
        }

        openTransaction(getMaster().getTransactionNo());
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction voided successfully.");

        return poJSON;
    }

    public JSONObject searchTransaction(String value, boolean byCode, boolean byExact) {
        try {
            String lsSQL = SQL_BROWSE;

            lsSQL = MiscUtil.addCondition(lsSQL, "a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode()));
            lsSQL = MiscUtil.addCondition(lsSQL, "a.cDelivrTp != " + SQLUtil.toSQL(DeliveryIssuanceType.DELIVERY));

            String lsCondition = "";
            if (psTranStat != null) {
                if (this.psTranStat.length() > 1) {
                    for (int lnCtr = 0; lnCtr <= this.psTranStat.length() - 1; lnCtr++) {
                        lsCondition = lsCondition + ", " + SQLUtil.toSQL(Character.toString(this.psTranStat.charAt(lnCtr)));
                    }
                    lsCondition = "a.cTranStat IN (" + lsCondition.substring(2) + ")";
                } else {
                    lsCondition = "a.cTranStat = " + SQLUtil.toSQL(this.psTranStat);
                }
                lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
            }
            if (!psCategorCD.isEmpty()) {
                lsSQL = MiscUtil.addCondition(lsSQL, " a.sCategrCd = " + SQLUtil.toSQL(psCategorCD));
            }

            System.out.println("Search Query is = " + lsSQL);
            poJSON = ShowDialogFX.Search(poGRider,
                    lsSQL,
                    value,
                    "Transaction No»Destination»Date",
                    "sTransNox»xDestinat»dTransact",
                    "a.sTransNox»e.sBranchNm»a.dTransact",
                    byExact ? (byCode ? 0 : 1) : 2);

            if (poJSON != null) {
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                return openTransaction((String) poJSON.get("sTransNox"));

            } else {
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "No record loaded.");
                return poJSON;

            }
        } catch (CloneNotSupportedException | SQLException | GuanzonException ex) {
            Logger.getLogger(InventoryStockIssuanceNeo.class
                    .getName()).log(Level.SEVERE, null, ex);
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }

    public JSONObject searchTransactionPosting(String value, boolean byCode, boolean byExact) {
        try {
            String lsSQL = SQL_BROWSE;
            String lsCondition = "";
            if (psTranStat != null) {
                if (this.psTranStat.length() > 1) {
                    for (int lnCtr = 0; lnCtr <= this.psTranStat.length() - 1; lnCtr++) {
                        lsCondition = lsCondition + ", " + SQLUtil.toSQL(Character.toString(this.psTranStat.charAt(lnCtr)));
                    }
                    lsCondition = "a.cTranStat IN (" + lsCondition.substring(2) + ")";
                } else {
                    lsCondition = "a.cTranStat = " + SQLUtil.toSQL(this.psTranStat);
                }
                lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
            }
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sDestinat = " + SQLUtil.toSQL(poGRider.getBranchCode()));

            if (!psCategorCD.isEmpty()) {
                lsSQL = MiscUtil.addCondition(lsSQL, " a.sCategrCd = " + SQLUtil.toSQL(psCategorCD));
            }
            System.out.println("Search Query is = " + lsSQL);
            poJSON = ShowDialogFX.Search(poGRider,
                    lsSQL,
                    value,
                    "Transaction No»Branch Name»Date",
                    "sTransNox»xBranchNm»dTransact",
                    "a.sTransNox»d.sBranchNm»a.dTransact",
                    byExact ? (byCode ? 0 : 1) : 2);

            if (poJSON != null) {
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                poJSON = openTransaction((String) poJSON.get("sTransNox"));

                if (!"error".equals((String) poJSON.get("result"))) {
                    return updateTransaction();
                }
                return poJSON;
//            } else if ("error".equals((String) poJSON.get("result"))) {
//                return poJSON;
            } else {
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "No record loaded.");
                return poJSON;

            }
        } catch (CloneNotSupportedException | SQLException | GuanzonException ex) {
            Logger.getLogger(InventoryStockIssuanceNeo.class
                    .getName()).log(Level.SEVERE, null, ex);
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }

    public JSONObject searchDetailByIssuance(int row, String value, boolean byCode) throws SQLException, GuanzonException {
        InventoryBrowse loBrowse = new InventoryBrowse(poGRider, logwrapr);
        loBrowse.initTransaction();
        if (!psIndustryCode.isEmpty()) {
            loBrowse.setIndustry(psIndustryCode);
        }
        loBrowse.setCategoryFilters(psCategorCD);
        loBrowse.setBranch(poGRider.getBranchCode());

        poJSON = new JSONObject();

        poJSON = loBrowse.searchInventoryIssaunce(value, byCode);
        System.out.println("result " + (String) poJSON.get("result"));
        if ("success".equals((String) poJSON.get("result"))) {
            for (int lnExisting = 0; lnExisting <= paDetail.size() - 1; lnExisting++) {
                Model_Inventory_Transfer_Detail loExisting = (Model_Inventory_Transfer_Detail) paDetail.get(lnExisting);
                if (loExisting.getStockId() != null) {
                    if (loExisting.getStockId().equals(loBrowse.getModelInventory().getStockId())) {
                        if (!loExisting.getSerialID().isEmpty()) {
                            if (loBrowse.getModelInventorySerial().getSerialId() != null) {
                                if (!loExisting.getSerialID().equals(loBrowse.getModelInventorySerial().getSerialId())) {
                                    continue;
                                }
                            }
                        }
                        poJSON = new JSONObject();
                        poJSON.put("result", "error");
                        poJSON.put("message", "Selected Inventory is already exist!");
                        return poJSON;

                    }
                }
            }
            getDetail(row).setStockId(loBrowse.getModelInventory().getStockId());
            if (loBrowse.getModelInventorySerial().getSerialId() != null) {
                getDetail(row).setSerialID(loBrowse.getModelInventorySerial().getSerialId());
            }

            getDetail(row).setInventoryCost(Double.parseDouble(loBrowse.getModelInventory().getCost().toString()));
            getDetail(row).setQuantity(1.00);

            poJSON = new JSONObject();
            poJSON.put("result", "success");
        }

        return poJSON;

    }

    public JSONObject searchDetailByIssuance(int row, String value, boolean byCode, boolean byExact) throws SQLException, GuanzonException {
        InventoryBrowse loBrowse = new InventoryBrowse(poGRider, logwrapr);
        loBrowse.initTransaction();
        if (!psIndustryCode.isEmpty()) {
            loBrowse.setIndustry(psIndustryCode);
        }
        loBrowse.setCategoryFilters(psCategorCD);
        loBrowse.setBranch(poGRider.getBranchCode());

        poJSON = new JSONObject();

        poJSON = loBrowse.searchInventoryIssaunce(value, byCode, byExact);
        System.out.println("result " + (String) poJSON.get("result"));
        if ("success".equals((String) poJSON.get("result"))) {
            for (int lnExisting = 0; lnExisting <= paDetail.size() - 1; lnExisting++) {
                Model_Inventory_Transfer_Detail loExisting = (Model_Inventory_Transfer_Detail) paDetail.get(lnExisting);
                if (loExisting.getStockId() != null) {
                    if (loExisting.getStockId().equals(loBrowse.getModelInventory().getStockId())) {
                        if (!loExisting.getSerialID().isEmpty()) {
                            if (loBrowse.getModelInventorySerial().getSerialId() != null) {
                                if (!loExisting.getSerialID().equals(loBrowse.getModelInventorySerial().getSerialId())) {
                                    continue;
                                }
                            }
                        }
                        poJSON = new JSONObject();
                        poJSON.put("result", "error");
                        poJSON.put("message", "Selected Inventory is already exist!");
                        return poJSON;

                    }
                }
            }
            getDetail(row).setStockId(loBrowse.getModelInventory().getStockId());
            if (loBrowse.getModelInventorySerial().getSerialId() != null) {
                getDetail(row).setSerialID(loBrowse.getModelInventorySerial().getSerialId());
            }

            getDetail(row).setInventoryCost(Double.parseDouble(loBrowse.getModelInventory().getCost().toString()));
            getDetail(row).setQuantity(1.00);

            poJSON = new JSONObject();
            poJSON.put("result", "success");
        }

        return poJSON;

    }

    public JSONObject searchDetailBySerial(int row, String value, boolean byCode) throws SQLException, GuanzonException {
        InventoryBrowse loBrowse = new InventoryBrowse(poGRider, logwrapr);
        loBrowse.initTransaction();
        if (!psIndustryCode.isEmpty()) {
            loBrowse.setIndustry(psIndustryCode);
        }
        loBrowse.setCategoryFilters(psCategorCD);
        loBrowse.setBranch(poGRider.getBranchCode());

        poJSON = new JSONObject();

        poJSON = loBrowse.searchInventorySerialWithStock(value, byCode);
        System.out.println("result " + (String) poJSON.get("result"));
        if ("success".equals((String) poJSON.get("result"))) {
            for (int lnExisting = 0; lnExisting <= paDetail.size() - 1; lnExisting++) {
                Model_Inventory_Transfer_Detail loExisting = (Model_Inventory_Transfer_Detail) paDetail.get(lnExisting);
                if (loExisting.getStockId() != null) {
                    if (loExisting.getStockId().equals(loBrowse.getModelInventory().getStockId())) {
                        if (!loExisting.getSerialID().isEmpty()) {
                            if (loBrowse.getModelInventorySerial().getSerialId() != null) {
                                if (!loExisting.getSerialID().equals(loBrowse.getModelInventorySerial().getSerialId())) {
                                    continue;
                                }
                            }
                        }
                        poJSON = new JSONObject();
                        poJSON.put("result", "error");
                        poJSON.put("message", "Selected Inventory is already exist!");
                        return poJSON;

                    }
                }
            }
        }

        getDetail(row).setStockId(loBrowse.getModelInventory().getStockId());
        if (loBrowse.getModelInventorySerial().getSerialId() != null) {
            getDetail(row).setSerialID(loBrowse.getModelInventorySerial().getSerialId());
        }

        getDetail(row).setInventoryCost(Double.parseDouble(loBrowse.getModelInventory().getCost().toString()));
        getDetail(row).setQuantity(1.00);

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        return poJSON;

    }

    public JSONObject searchDetailByBarcode(int row, String value, boolean byCode) throws SQLException, GuanzonException {
        InventoryBrowse loBrowse = new InventoryBrowse(poGRider, logwrapr);
        loBrowse.initTransaction();
        if (!psIndustryCode.isEmpty()) {
            loBrowse.setIndustry(psIndustryCode);
        }
        loBrowse.setCategoryFilters(psCategorCD);

        poJSON = new JSONObject();

        poJSON = loBrowse.searchInventory(value, byCode);
        System.out.println("result " + (String) poJSON.get("result"));

        if ("success".equals((String) poJSON.get("result"))) {
            getDetail(row).setOriginalId(loBrowse.getModelInventory().getStockId());
            return poJSON;
        }
        return poJSON;

    }

    public JSONObject searchTransactionDestination(String value, boolean byCode) throws SQLException, GuanzonException {
        Model_Branch loBrowse = new ParamModels(poGRider).Branch();

        String lsSQL = "SELECT sBranchCd, sBranchNm FROM Branch";
        if (!psIndustryCode.isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "sIndstCdx = " + SQLUtil.toSQL(psIndustryCode));
        }
        poJSON = ShowDialogFX.Search(poGRider,
                lsSQL,
                value,
                "Code»Branch Name",
                "sBranchCd»sBranchNm",
                "sBranchCd»sBranchNm",
                byCode ? 0 : 1);

        if (poJSON != null) {
            poJSON = loBrowse.openRecord((String) this.poJSON.get("sBranchCd"));
            System.out.println("result " + (String) poJSON.get("result"));

            if ("success".equals((String) poJSON.get("result"))) {
                getMaster().setDestination(loBrowse.getBranchCode());

                poJSON = new JSONObject();
                poJSON.put("result", "success");
                return poJSON;
            }

        }
        this.poJSON = new JSONObject();
        this.poJSON.put("result", "error");
        this.poJSON.put("message", "No record loaded.");
        return this.poJSON;

    }

    public JSONObject searchTransactionTrucking(String value, boolean byCode) throws SQLException, GuanzonException {
        Model_Client_Master loBrowse = new ClientModels(poGRider).ClientMaster();

        String lsSQL = "SELECT"
                + "  a.sClientID"
                + ", IFNULL(b.sCompnyNm,'') xClientNm"
                + ", IFNULL(c.sCompnyNm,'') xCPerName"
                + " FROM AP_Client_Master a"
                + "  LEFT JOIN Client_Master b ON a.sClientID = b.sClientID"
                + "  LEFT JOIN Client_Master c ON a.sClientID = c.sClientID"
                + " WHERE a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE);

        poJSON = ShowDialogFX.Search(poGRider,
                lsSQL,
                value,
                "ID»Client Name»Contact Person",
                "sClientID»xClientNm»xCPerName",
                "sClientID»IFNULL(b.sCompnyNm,'')»IFNULL(c.sCompnyNm,'')",
                byCode ? 0 : 1);

        if (poJSON != null) {
            poJSON = loBrowse.openRecord((String) this.poJSON.get("sClientID"));
            System.out.println("result " + (String) poJSON.get("result"));

            if ("success".equals((String) poJSON.get("result"))) {
                getMaster().setTruckId(loBrowse.getClientId());

                poJSON = new JSONObject();
                poJSON.put("result", "success");
                return poJSON;
            }

        }
        this.poJSON = new JSONObject();
        this.poJSON.put("result", "error");
        this.poJSON.put("message", "No record loaded.");
        return this.poJSON;

    }

    public JSONObject loadTransactionList(String value, String column)
            throws SQLException, GuanzonException, CloneNotSupportedException {

//        if (psIndustryCode.isEmpty()) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "I is not set");
//            return poJSON;
//        }
        paMaster.clear();
        initSQL();
        String lsSQL = SQL_BROWSE;
        String lsCondition = "";
        if (psTranStat != null) {
            if (this.psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= this.psTranStat.length() - 1; lnCtr++) {
                    lsCondition = lsCondition + ", " + SQLUtil.toSQL(Character.toString(this.psTranStat.charAt(lnCtr)));
                }
                lsCondition = "a.cTranStat IN (" + lsCondition.substring(2) + ")";
            } else {
                lsCondition = "a.cTranStat = " + SQLUtil.toSQL(this.psTranStat);
            }
            lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        }

        if (value != null && !value.isEmpty()) {
            //a.sTransNox/e.sBranchNm #Posting/d.sBranchNm #Confirmation
            lsSQL = MiscUtil.addCondition(lsSQL, column + " LIKE " + SQLUtil.toSQL(value + "%"));
        }

        if (!psIndustryCode.isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sIndstCdx = " + SQLUtil.toSQL(psIndustryCode));
        }
        if (!psCategorCD.isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sCategrCd = " + SQLUtil.toSQL(psCategorCD));
        }
        lsSQL = MiscUtil.addCondition(lsSQL, "a.cDelivrTp != " + SQLUtil.toSQL(DeliveryIssuanceType.DELIVERY));

        lsSQL = MiscUtil.addCondition(lsSQL, "a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode()));
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        System.out.println("Load Transaction list query is " + lsSQL);

        if (MiscUtil.RecordCount(loRS)
                <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "No record found.");
            return poJSON;
        }

        while (loRS.next()) {
            Model_Inventory_Transfer_Master loInventoryIssuance = new DeliveryIssuanceModels(poGRider).InventoryTransferMaster();
            poJSON = loInventoryIssuance.openRecord(loRS.getString("sTransNox"));

            if ("success".equals((String) poJSON.get("result"))) {
                paMaster.add((Model) loInventoryIssuance);
            } else {
                return poJSON;
            }
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;
    }

    public JSONObject loadTransactionListPosting(String value, String column)
            throws SQLException, GuanzonException, CloneNotSupportedException {

//        if (psIndustryCode.isEmpty()) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "I is not set");
//            return poJSON;
//        }
        paMaster.clear();
        initSQL();
        String lsSQL = SQL_BROWSE;

        if (value != null && !value.isEmpty()) {
            //sTransNox/dTransact/dSchedule
            lsSQL = MiscUtil.addCondition(lsSQL, column + " LIKE " + SQLUtil.toSQL(value + "%"));
        }
        String lsCondition = "";
        if (psTranStat != null) {
            if (this.psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= this.psTranStat.length() - 1; lnCtr++) {
                    lsCondition = lsCondition + ", " + SQLUtil.toSQL(Character.toString(this.psTranStat.charAt(lnCtr)));
                }
                lsCondition = "a.cTranStat IN (" + lsCondition.substring(2) + ")";
            } else {
                lsCondition = "a.cTranStat = " + SQLUtil.toSQL(this.psTranStat);
            }
            lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        }

        if (!psIndustryCode.isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sIndstCdx = " + SQLUtil.toSQL(psIndustryCode));
        }
        if (!psCategorCD.isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sCategrCd = " + SQLUtil.toSQL(psCategorCD));
        }

        lsSQL = MiscUtil.addCondition(lsSQL, "a.sDestinat = " + SQLUtil.toSQL(poGRider.getBranchCode()));
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        System.out.println("Load Transaction list query is " + lsSQL);

        if (MiscUtil.RecordCount(loRS)
                <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "No record found.");
            return poJSON;
        }

        while (loRS.next()) {
            Model_Inventory_Transfer_Master loInventoryIssuance = new DeliveryIssuanceModels(poGRider).InventoryTransferMaster();
            poJSON = loInventoryIssuance.openRecord(loRS.getString("sTransNox"));

            if ("success".equals((String) poJSON.get("result"))) {
                paMaster.add((Model) loInventoryIssuance);
            } else {
                return poJSON;
            }
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;
    }
//
//    public JSONObject searchDetailByClusterIssuance(int row, String value, boolean byCode, boolean byExact) throws SQLException, GuanzonException {
//        InventoryBrowse loBrowse = new InventoryBrowse(poGRider, logwrapr);
//        loBrowse.initTransaction();
//        if (!psIndustryCode.isEmpty()) {
//            loBrowse.setIndustry(psIndustryCode);
//        }
//        loBrowse.setCategoryFilters(psCategorCD);
//        loBrowse.setBranch(poGRider.getBranchCode());
//
//        poJSON = new JSONObject();
//
//        poJSON = loBrowse.searchInventoryIssaunce(value, byCode, byExact);
//        System.out.println("result " + (String) poJSON.get("result"));
//        if ("success".equals((String) poJSON.get("result"))) {
//            for (int lnExisting = 0; lnExisting <= paDetail.size() - 1; lnExisting++) {
//                Model_Inventory_Transfer_Detail loExisting = (Model_Inventory_Transfer_Detail) paDetail.get(lnExisting);
//                if (loExisting.getStockId() != null) {
//                    if (loExisting.getStockId().equals(loBrowse.getModelInventory().getStockId())) {
//                        if (!loExisting.getSerialID().isEmpty()) {
//                            if (loBrowse.getModelInventorySerial().getSerialId() != null) {
//                                if (!loExisting.getSerialID().equals(loBrowse.getModelInventorySerial().getSerialId())) {
//                                    continue;
//                                }
//                            }
//                        }
//                        poJSON = new JSONObject();
//                        poJSON.put("result", "error");
//                        poJSON.put("message", "Selected Inventory is already exist!");
//                        return poJSON;
//
//                    }
//                }
//            }
//            if (getDetail(row).InventoryStockRequest().getApproved() > 1) {
//                //clone current record record to the last
//                Model_Inventory_Transfer_Detail loDetailClone = new paDetail.get(row - 1)
//                paDetail.add(loDetailClone)
//                loDetailClone.InventoryStockRequest().setQuantity(loDetailClone.InventoryStockRequest().getQuantity() - 1);
//                loDetailClone.InventoryStockRequest().setApproved(loDetailClone.InventoryStockRequest().getApproved() - 1);
//
//            }
//            getDetail(row).setStockId(loBrowse.getModelInventory().getStockId());
//            if (loBrowse.getModelInventorySerial().getSerialId() != null) {
//                getDetail(row).setSerialID(loBrowse.getModelInventorySerial().getSerialId());
//            }
//
//            getDetail(row).setQuantity(1.00);
//        }
//
//        return poJSON;
//
//    }

    public JSONObject searchDetailByClusterIssuance(int row, String value, boolean byCode, boolean byExact)
            throws SQLException, GuanzonException, CloneNotSupportedException {

        InventoryBrowse loBrowse = new InventoryBrowse(poGRider, logwrapr);
        loBrowse.initTransaction();
        if (!psIndustryCode.isEmpty()) {
            loBrowse.setIndustry(psIndustryCode);
        }
        loBrowse.setCategoryFilters(psCategorCD);
        loBrowse.setBranch(poGRider.getBranchCode());
        //filter specic to stockid of order
        if (getDetail(row).getStockId() != null
                && !getDetail(row).getStockId().isEmpty()) {
            loBrowse.setInventory(getDetail(row).getStockId());
        }
        poJSON = loBrowse.searchInventoryIssaunce(value, byCode, byExact);
        System.out.println("result " + (String) poJSON.get("result"));
        if ("success".equals((String) poJSON.get("result"))) {
            for (int lnExisting = 0; lnExisting <= paDetail.size() - 1; lnExisting++) {
                Model_Inventory_Transfer_Detail loExisting = (Model_Inventory_Transfer_Detail) paDetail.get(lnExisting);
                if (loExisting.getStockId() != null) {
                    if (loExisting.getStockId().equals(loBrowse.getModelInventory().getStockId())) {
                        if (loExisting.getSerialID() != null) {
                            if (!loExisting.getSerialID().isEmpty()) {
                                if (loBrowse.getModelInventorySerial().getSerialId() != null) {
                                    if (loExisting.getSerialID().equals(loBrowse.getModelInventorySerial().getSerialId())) {
                                        poJSON = new JSONObject();
                                        poJSON.put("result", "error");
                                        poJSON.put("message", "Selected Inventory is already exist!");
                                        return poJSON;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // update current row
        getDetail(row).setStockId(loBrowse.getModelInventory().getStockId());
        if (loBrowse.getModelInventorySerial().getSerialId() != null) {
            getDetail(row).setSerialID(loBrowse.getModelInventorySerial().getSerialId());
        }

        getDetail(row).setInventoryCost(Double.parseDouble(loBrowse.getModelInventory().getCost().toString()));
        getDetail(row).setQuantity(1.00);

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        return poJSON;
    }

    private String getCategory() {
        String lsCategory;
        switch (psIndustryCode) {
            case "01"://CP
                lsCategory = "0001";
                break;
            case "02"://MC
                lsCategory = "0003»0004";
                break;
            case "03"://Car
                lsCategory = "0005»0006";
                break;
            case "04"://Monarch
                lsCategory = "0009";
                break;
            case "05"://LP
                lsCategory = "0008";
                break;
            case "07"://Appliance
                lsCategory = "0002";
                break;
            default://Appliance
                lsCategory = "0007";
                break;

        }

        return lsCategory;
    }

    public JSONObject printRecord() throws SQLException, JRException, CloneNotSupportedException, GuanzonException {

        poJSON = new JSONObject();

        if (InventoryStockIssuanceStatus.POSTED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Processed.");
            return poJSON;
        }
//
//        if (InventoryStockIssuanceStatus.CONFIRMED.equals((String) poMaster.getValue("cTranStat"))) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "Transaction was already confirmed.");
//            return poJSON;
//        }

        if (InventoryStockIssuanceStatus.CANCELLED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already cancelled.");
            return poJSON;
        }

        if (InventoryStockIssuanceStatus.VOID.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already voided.");
            return poJSON;
        }
        poJSON = isEntryOkay(InventoryStockIssuanceStatus.CONFIRMED);
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
                    if (pbWthParent) {
                        poGRider.beginTrans("UPDATE STATUS", "Process Transaction Print Tag", SOURCE_CODE, getMaster().getTransactionNo());
                    }
                    if (!getMaster().isPrintedStatus()) {
                        if (!isJSONSuccess(PrintTransaction(), "Print Record",
                                "Initialize Record Print! ")) {
                            return;
                        }
                    }
                    if (getMaster().getTransactionStatus().equals(InventoryStockIssuanceStatus.OPEN)) {
                        if (!isJSONSuccess(CloseTransaction(), "Print Record",
                                "Initialize Close Transaction! ")) {
                        }
                    }

                    if (pbWthParent) {
                        poGRider.commitTrans();
                    }
                    poReportJasper.CloseReportUtil();

                } catch (SQLException | GuanzonException | CloneNotSupportedException ex) {
                    Logger.getLogger(InventoryRequestApproval.class
                            .getName()).log(Level.SEVERE, null, ex);
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

        }
        );
        //add Parameter
        poReportJasper.addParameter(
                "BranchName", poGRider.getBranchName());
        poReportJasper.addParameter("Address", poGRider.getAddress());
        poReportJasper.addParameter("CompanyName", poGRider.getClientName());
        poReportJasper.addParameter("TransactionNo", getMaster().getTransactionNo());
        poReportJasper.addParameter("TransactionDate", SQLUtil.dateFormat(getMaster().getTransactionDate(), SQLUtil.FORMAT_LONG_DATE));
        poReportJasper.addParameter("Remarks", getMaster().getRemarks());
        poReportJasper.addParameter("Destination", getMaster().BranchDestination().getBranchName());
        poReportJasper.addParameter("Trucking", getMaster().TruckingCompany().getCompanyName());
        poReportJasper.addParameter("DatePrinted", SQLUtil.dateFormat(poGRider.getServerDate(), SQLUtil.FORMAT_TIMESTAMP));
        if (getMaster()
                .isPrintedStatus()) {
            poReportJasper.addParameter("watermarkImagePath", poGRider.getReportPath() + "images\\reprint.png");
        } else {
            poReportJasper.addParameter("watermarkImagePath", poGRider.getReportPath() + "images\\blank.png");
        }

        poReportJasper.setReportName("Inventory Issuance");
        poReportJasper.setJasperPath(InventoryStockIssuancePrint.getJasperReport(psIndustryCode));

        //process by ResultSet
        String lsSQL = InventoryStockIssuancePrint.PrintRecordQuery();
        lsSQL = MiscUtil.addCondition(lsSQL, "InventoryTransferMaster.sTransNox = " + SQLUtil.toSQL(getMaster().getTransactionNo()));

        poReportJasper.setSQLReport(lsSQL);

        System.out.println(
                "Print Data Query :" + lsSQL);

        //process by JasperCollection parse ur List / ArrayList
        //JRBeanCollectionDataSource jrRS = new JRBeanCollectionDataSource(R1data);
        //poReportJasper.setJRBeanCollectionDataSource(jrRS);
        //direct pass JasperViewer
        //         reportPrint = JasperFillManager.fillReport(poGRider.getReportPath() + psJasperPath + ".jasper",
        //                    poParamater,
        //                    yourDATA);
        //        poReportJasper.setJasperPrint(report0Print);
        poReportJasper.isAlwaysTop(false);
        poReportJasper.isWithUI(true);
        poReportJasper.isWithExport(false);
        poReportJasper.isWithExportPDF(false);
        poReportJasper.willExport(true);
        return poReportJasper.generateReport();

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

//        if (InventoryStockIssuanceStatus.POSTED.equals((String) poMaster.getValue("cTranStat"))) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "Transaction was already Processed.");
//            return poJSON;
//        }
//
//        if (InventoryStockIssuanceStatus.CONFIRMED.equals((String) poMaster.getValue("cTranStat"))) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "Transaction was already confirmed.");
//            return poJSON;
//        }
        //validator
        poJSON = isEntryOkay(InventoryStockIssuanceStatus.CONFIRMED);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (!pbWthParent) {
            poGRider.beginTrans("UPDATE STATUS", "Process Transaction Print Tag", SOURCE_CODE, getMaster().getTransactionNo());
        }
        String lsSQL = "UPDATE "
                + poMaster.getTable()
                + " SET   cPrintedx = " + SQLUtil.toSQL(InventoryStockIssuanceStatus.CONFIRMED)
                + " WHERE sTransNox = " + SQLUtil.toSQL(getMaster().getTransactionNo());

        Long lnResult = poGRider.executeQuery(lsSQL,
                poMaster.getTable(),
                poGRider.getBranchCode(), "", "");
        if (lnResult <= 0L) {
            if (!pbWthParent) {
                poGRider.rollbackTrans();
            }

            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "Error updating the transaction status.");
            return poJSON;
        }

        if (!pbWthParent) {
            poGRider.commitTrans();
        }
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", "Transaction Printed successfully.");

        return poJSON;
    }

//    public JSONObject ProcessTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
//        poJSON = new JSONObject();
//
//        if (getEditMode() != EditMode.READY) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "Invalid Edit Mode");
//            return poJSON;
//        }
//
//        if (StockRequestStatus.PROCESSED.equals((String) poMaster.getValue("cTranStat"))) {
//            poJSON.put("result", "error");
////            poJSON.put("message", "Transaction was already processed.");
//            return poJSON;
//        }
//
//        if (StockRequestStatus.CANCELLED.equals((String) poMaster.getValue("cTranStat"))) {
//            poJSON.put("result", "error");
////            poJSON.put("message", "Transaction was already cancelled.");
//            return poJSON;
//        }
//
//        if (StockRequestStatus.VOID.equals((String) poMaster.getValue("cTranStat"))) {
//            poJSON.put("result", "error");
////            poJSON.put("message", "Transaction was already voided.");
//            return poJSON;
//        }
//
//        //validator
//        poJSON = isEntryOkay(StockRequestStatus.PROCESSED);
//        if ("error".equals((String) poJSON.get("result"))) {
//            return poJSON;
//        }
//
//        poGRider.beginTrans("UPDATE STATUS", "ProcessTransaction", SOURCE_CODE, getMaster().getTransactionNo());
//
//        poJSON = statusChange(poMaster.getTable(),
//                (String) poMaster.getValue("sTransNox"),
//                "ProcessTransaction",
//                StockRequestStatus.PROCESSED,
//                false, true);
//        if ("error".equals((String) poJSON.get("result"))) {
//            poGRider.rollbackTrans();
//            return poJSON;
//        }
//
//        poGRider.commitTrans();
//
//        poJSON = new JSONObject();
//        poJSON.put("result", "success");
////        poJSON.put("message", "Transaction processed successfully.");
//
//        return poJSON;
//    }
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

    public JSONObject seekApproval()
            throws SQLException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        //Moved only the script for seeking of approval - Arsiela 10-15-2025 - 14:11:01

        //load authorization manager that evaluates current users authority for this process
        ActionAuthManager loAuth = new ActionAuthManager(poGRider, "cas-inv-warehouse");
        poJSON = loAuth.isAuthorized();

        //check if currenty user is authorized
        if (!((String) poJSON.get("result")).equalsIgnoreCase("true")) {
            //if not authorized, check the type type of authorization required 
            if (((String) poJSON.get("code")).equalsIgnoreCase("regular")) {
                //show process need regular authorization
                ShowMessageFX.Warning((String) poJSON.get("warning"), "Authorization Required", null);
                //get authorization from authoried personnel
                poJSON = ShowDialogFX.getUserApproval(poGRider);
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }

                //check if approving officer is authorized
                String lsUserIDxx = poJSON.get("sUserIDxx").toString();
                int lnUserLevl = Integer.parseInt(poJSON.get("nUserLevl").toString());
                poJSON = loAuth.isAuthorized(lsUserIDxx, lnUserLevl);

                //if approving is not authorized then do not continue process
                if (!((String) poJSON.get("result")).equalsIgnoreCase("true")) {
                    ShowMessageFX.Warning((String) poJSON.get("warning"), "Authorization Required", null);
                    poJSON.put("result", "error");
                    poJSON.put("message", "User is not an authorized approving officer..");
                    return poJSON;
                }
            } //needs authorization thru authorization matrix
            else {
                //show process needs authorization through the authority matrix
                ShowMessageFX.Warning((String) poJSON.get("warning"), "Authorization Required", null);
                poJSON.put("result", "error");
                poJSON.put("message", "User is not an authorized approving officer..");
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;

    }

    public void ShowStatusHistory() throws SQLException, GuanzonException, Exception {
        CachedRowSet crs = getStatusHistory();

        crs.beforeFirst();

        while (crs.next()) {
            switch (crs.getString("cRefrStat")) {
                case "":
                    crs.updateString("cRefrStat", "-");
                    break;
                case InventoryStockIssuanceStatus.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case InventoryStockIssuanceStatus.CONFIRMED:
                    crs.updateString("cRefrStat", "CONFIRMED");
                    break;
                case InventoryStockIssuanceStatus.POSTED:
                    crs.updateString("cRefrStat", "POSTED");
                    break;
                case InventoryStockIssuanceStatus.CANCELLED:
                    crs.updateString("cRefrStat", "CANCELLED");
                    break;
                case InventoryStockIssuanceStatus.VOID:
                    crs.updateString("cRefrStat", "VOID");
                    break;

                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);

                    switch (stat) {
                        case InventoryStockIssuanceStatus.OPEN:
                            crs.updateString("cRefrStat", "OPEN");
                            break;
                        case InventoryStockIssuanceStatus.CONFIRMED:
                            crs.updateString("cRefrStat", "CONFIRMED");
                            break;
                        case InventoryStockIssuanceStatus.POSTED:
                            crs.updateString("cRefrStat", "POSTED");
                            break;
                        case InventoryStockIssuanceStatus.CANCELLED:
                            crs.updateString("cRefrStat", "CANCELLED");
                            break;
                        case InventoryStockIssuanceStatus.VOID:
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

        showStatusHistoryUI("Inventory Issuance History", (String) poMaster.getValue("sTransNox"), entryBy, entryDate, crs);
    }

    public JSONObject getEntryBy() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsEntry = "";
        String lsEntryDate = "";
        String lsSQL = " SELECT b.sModified, b.dModified "
                + " FROM Inv_Transfer_Master a "
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
}
