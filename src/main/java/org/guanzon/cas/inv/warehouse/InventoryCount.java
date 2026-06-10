package org.guanzon.cas.inv.warehouse;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.inv.InvMaster;
import org.json.simple.JSONObject;
import org.guanzon.cas.inv.warehouse.status.InventoryCountStatus;
import org.guanzon.cas.inv.InventoryTransaction;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.inv.warehouse.model.Model_Inventory_Count_Detail;
import org.guanzon.cas.inv.warehouse.model.Model_Inventory_Count_Master;
import org.guanzon.cas.inv.warehouse.report.ReportUtil;
import org.guanzon.cas.inv.warehouse.report.ReportUtilListener;
import org.guanzon.cas.inv.warehouse.services.InvWarehouseModels;
import org.guanzon.cas.inv.warehouse.status.InventoryCountPrint;
import org.guanzon.cas.inv.warehouse.validators.InventoryIssuanceValidatorFactory;
import org.json.simple.JSONArray;

public class InventoryCount extends Transaction {

    private String psIndustryCode = "";
    private String psCompanyID = "";
    private String psCategorCD = "";
    private String psApprovalUser = "";
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

    public Model_Inventory_Count_Master getMaster() {
        return (Model_Inventory_Count_Master) poMaster;
    }

    @SuppressWarnings("unchecked")
    public List<Model_Inventory_Count_Master> getMasterList() {
        return (List<Model_Inventory_Count_Master>) (List<?>) paMaster;
    }

    public Model_Inventory_Count_Master getMaster(int masterRow) {
        return (Model_Inventory_Count_Master) paMaster.get(masterRow);

    }

    @SuppressWarnings("unchecked")
    public List<Model_Inventory_Count_Detail> getDetailList() {
        return (List<Model_Inventory_Count_Detail>) (List<?>) paDetail;
    }

    public Model_Inventory_Count_Detail getDetail(int entryNo) {
        if (getMaster().getTransactionNo().isEmpty() || entryNo <= 0) {
            return null;
        }

        //autoadd detail if empty
        Model_Inventory_Count_Detail lastDetail = (Model_Inventory_Count_Detail) paDetail.get(paDetail.size() - 1);
        String stockID = lastDetail.getStockId();
        if (stockID != null && !stockID.trim().isEmpty()) {
            Model_Inventory_Count_Detail newDetail = new InvWarehouseModels(poGRider).InventoryCountDetail();
            newDetail.newRecord();
            newDetail.setTransactionNo(getMaster().getTransactionNo());
            newDetail.setEntryNo(paDetail.size() + 1);
            paDetail.add(newDetail);
        }

        Model_Inventory_Count_Detail loDetail;

        //find the detail record
        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr++) {
            loDetail = (Model_Inventory_Count_Detail) paDetail.get(lnCtr);

            if (loDetail.getEntryNo() == entryNo) {
                return loDetail;
            }
        }

        loDetail = new InvWarehouseModels(poGRider).InventoryCountDetail();
        loDetail.newRecord();
        loDetail.setTransactionNo(getMaster().getTransactionNo());
        loDetail.setEntryNo(entryNo);
        paDetail.add(loDetail);

        return loDetail;
    }

    public JSONObject initTransaction() throws GuanzonException, SQLException {
        SOURCE_CODE = "Dlvr";

        poMaster = new InvWarehouseModels(poGRider).InventoryCountMaster();
        poDetail = new InvWarehouseModels(poGRider).InventoryCountDetail();
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
                + ", b.sDescript"
                + ", c.sBranchNm"
                + ", c.sCompnyNm sCompnyNm"
                + ", a.nCounterx"
                + " FROM Inventory_Count_Master a "
                + "     LEFT JOIN Iinventory_Count_Type b ON a.sInvCtrID = b.sInvCtrID"
                + "     LEFT JOIN Branch c ON a.sBranchCd = c.sBranchCd";
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

//        getMaster().setIndustryId(psIndustryCode);
//        getMaster().setCompanyID(psCompanyID);
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

        if (InventoryCountStatus.CONFIRMED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already confirmed.");
            return poJSON;
        }

        if (InventoryCountStatus.CANCELLED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already cancelled.");
            return poJSON;
        }

        return updateTransaction();
    }

    @Override
    protected JSONObject willSave() throws SQLException {
        poJSON = new JSONObject();

        poJSON = isEntryOkay(InventoryCountStatus.OPEN);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        int lnDetailCount = 0;

        //assign values needed
        for (int lnCtr = 0; lnCtr < paDetail.size(); lnCtr++) {
            Model_Inventory_Count_Detail loDetail = (Model_Inventory_Count_Detail) paDetail.get(lnCtr);
            if (!loDetail.getStockId().isEmpty()) {

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
        GValidator loValidator = InventoryIssuanceValidatorFactory.make(psIndustryCode);

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

        if (InventoryCountStatus.CONFIRMED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "success");
            poJSON.put("message", "Transaction confirmed successfully.");
            return poJSON;
        }

        if (InventoryCountStatus.VOID.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Void.");
            return poJSON;
        }
        if (InventoryCountStatus.CANCELLED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Cancelled.");
            return poJSON;
        }

        if (InventoryCountStatus.POSTED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Posted.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(InventoryCountStatus.CONFIRMED);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        boolean lbConfirm = true;
        String lsStatus = InventoryCountStatus.CONFIRMED;
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

        if (check
                != null) {
            check.postAuth();
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

    public JSONObject PostTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        if (getEditMode() != EditMode.UPDATE) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }
        if (InventoryCountStatus.VOID.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Void.");
            return poJSON;
        }
        if (InventoryCountStatus.CANCELLED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Cancelled.");
            return poJSON;
        }

        if (InventoryCountStatus.POSTED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Posted.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(InventoryCountStatus.POSTED);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        boolean lbConfirm = true;
        String lsStatus = InventoryCountStatus.POSTED;
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

        Model_Inventory_Count_Master loMaster = (Model_Inventory_Count_Master) this.poMaster;
        loMaster.updateRecord();
        loMaster.setTransactionStatus(lsStatus);
        if (!"success".equals((String) loMaster.saveRecord().get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        String lsCondition = "0";

        InventoryTransaction loTrans = new InventoryTransaction(poGRider);
        loTrans.DeliveryAcceptance((String) poMaster.getValue("sTransNox"), (Date) poMaster.getValue("dTransact"), false);

        for (Model loDetail : paDetail) {
            Model_Inventory_Count_Detail detail = (Model_Inventory_Count_Detail) loDetail;
            detail.updateRecord();

            if (!"success".equals((String) detail.saveRecord().get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }
            if (getMaster().getCounterNo() == 1) {
                if (detail.getStockId() != null) {
                    loTrans.addDetail((String) poMaster.getValue("sIndstCdx"), detail.getStockId(),
                            lsCondition,
                            detail.getActualCounter01().doubleValue(),
                            0,
                            detail.Inventory().getCost().doubleValue());
                }
            } else if (getMaster().getCounterNo() == 2) {
                if (detail.getStockId() != null) {
                    loTrans.addDetail((String) poMaster.getValue("sIndstCdx"), detail.getStockId(),
                            lsCondition,
                            detail.getActualCounter02().doubleValue(),
                            0,
                            detail.Inventory().getCost().doubleValue());
                }
            } else if (getMaster().getCounterNo() == 3) {
                if (detail.getStockId() != null) {
                    loTrans.addDetail((String) poMaster.getValue("sIndstCdx"), detail.getStockId(),
                            lsCondition,
                            detail.getActualCounter03().doubleValue(),
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
        if (InventoryCountStatus.VOID.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Void.");
            return poJSON;
        }
        if (InventoryCountStatus.CANCELLED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Cancelled.");
            return poJSON;
        }

        if (InventoryCountStatus.POSTED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Posted.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(InventoryCountStatus.CANCELLED);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        boolean lbConfirm = true;
        String lsStatus = InventoryCountStatus.CANCELLED;
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

        if (InventoryCountStatus.CONFIRMED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already confirmed.");
            return poJSON;
        }
        if (InventoryCountStatus.CANCELLED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already cancelled.");
            return poJSON;
        }

        if (InventoryCountStatus.VOID.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already voided.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(InventoryCountStatus.VOID);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (!pbWthParent) {
            poGRider.beginTrans("UPDATE STATUS", "VoidTransaction", SOURCE_CODE, getMaster().getTransactionNo());
        }
        poJSON = statusChange(poMaster.getTable(),
                (String) poMaster.getValue("sTransNox"),
                "VoidTransaction",
                InventoryCountStatus.VOID,
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
            Logger.getLogger(InventoryCount.class
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
            Logger.getLogger(InventoryCount.class
                    .getName()).log(Level.SEVERE, null, ex);
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
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
            Model_Inventory_Count_Master loInventoryCount = new InvWarehouseModels(poGRider).InventoryCountMaster();
            poJSON = loInventoryCount.openRecord(loRS.getString("sTransNox"));

            if ("success".equals((String) poJSON.get("result"))) {
                paMaster.add((Model) loInventoryCount);
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
            Model_Inventory_Count_Master loInventoryCount = new InvWarehouseModels(poGRider).InventoryCountMaster();
            poJSON = loInventoryCount.openRecord(loRS.getString("sTransNox"));

            if ("success".equals((String) poJSON.get("result"))) {
                paMaster.add((Model) loInventoryCount);
            } else {
                return poJSON;
            }
        }

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

        if (InventoryCountStatus.POSTED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already Processed.");
            return poJSON;
        }
//
//        if (InventoryCountStatus.CONFIRMED.equals((String) poMaster.getValue("cTranStat"))) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "Transaction was already confirmed.");
//            return poJSON;
//        }

        if (InventoryCountStatus.CANCELLED.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already cancelled.");
            return poJSON;
        }

        if (InventoryCountStatus.VOID.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already voided.");
            return poJSON;
        }
        poJSON = isEntryOkay(InventoryCountStatus.CONFIRMED);
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
                    if (!isJSONSuccess(PrintTransaction(), "Print Record",
                            "Initialize Record Print! ")) {
                        return;
                    }
                    if (getMaster().getTransactionStatus().equals(InventoryCountStatus.OPEN)) {
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
        poReportJasper.addParameter("DatePrinted", SQLUtil.dateFormat(poGRider.getServerDate(), SQLUtil.FORMAT_TIMESTAMP));
        poReportJasper.addParameter("watermarkImagePath", poGRider.getReportPath() + "images\\blank.png");

        poReportJasper.setReportName("Inventory Issuance");
        poReportJasper.setJasperPath(InventoryCountPrint.getJasperReport(psIndustryCode));

        //process by ResultSet
        String lsSQL = InventoryCountPrint.PrintRecordQuery();
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

//        if (InventoryCountStatus.POSTED.equals((String) poMaster.getValue("cTranStat"))) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "Transaction was already Processed.");
//            return poJSON;
//        }
//
//        if (InventoryCountStatus.CONFIRMED.equals((String) poMaster.getValue("cTranStat"))) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "Transaction was already confirmed.");
//            return poJSON;
//        }
        //validator
        poJSON = isEntryOkay(InventoryCountStatus.CONFIRMED);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (!pbWthParent) {
            poGRider.beginTrans("UPDATE STATUS", "Process Transaction Print Tag", SOURCE_CODE, getMaster().getTransactionNo());
        }
        String lsSQL = "UPDATE "
                + poMaster.getTable()
                + " SET   cPrintedx = " + SQLUtil.toSQL(InventoryCountStatus.CONFIRMED)
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
                case InventoryCountStatus.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case InventoryCountStatus.CONFIRMED:
                    crs.updateString("cRefrStat", "CONFIRMED");
                    break;
                case InventoryCountStatus.POSTED:
                    crs.updateString("cRefrStat", "POSTED");
                    break;
                case InventoryCountStatus.CANCELLED:
                    crs.updateString("cRefrStat", "CANCELLED");
                    break;
                case InventoryCountStatus.VOID:
                    crs.updateString("cRefrStat", "VOID");
                    break;

                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);

                    switch (stat) {
                        case InventoryCountStatus.OPEN:
                            crs.updateString("cRefrStat", "OPEN");
                            break;
                        case InventoryCountStatus.CONFIRMED:
                            crs.updateString("cRefrStat", "CONFIRMED");
                            break;
                        case InventoryCountStatus.POSTED:
                            crs.updateString("cRefrStat", "POSTED");
                            break;
                        case InventoryCountStatus.CANCELLED:
                            crs.updateString("cRefrStat", "CANCELLED");
                            break;
                        case InventoryCountStatus.VOID:
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
                + " FROM Inventory_Count_Master a "
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

    public JSONObject generateDetail() throws SQLException, GuanzonException {
        if (getMaster().getBranchCode().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "No Branch found.");
            return poJSON;
        }

        // ─── Build period condition for exclusion ─────────────────────────────
        String lsPeriodCondition = "";
        switch (getMaster().InventoryCountType().getPeriod()) {
            case "M":
                lsPeriodCondition = "a.dTransact >= DATE_SUB(CURDATE(), INTERVAL 1 MONTH)";
                break;
            case "S":
                lsPeriodCondition = "a.dTransact >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH)";
                break;
            case "A":
                lsPeriodCondition = "a.dTransact >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH)";
                break;
            case "X":
            default:
                lsPeriodCondition = "";
                break;
        }

        // ─── Build exclusion set (stock IDs already counted in the period) ────
        Set<String> loExcludedStocks = new HashSet<>();

        String lsSQL = "SELECT b.sStockIDx"
                + " FROM Inventory_Count_Master a"
                + " INNER JOIN Inventory_Count_Detail b ON a.sTransNox = b.sTransNox"
                + " WHERE a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode());

        if (!lsPeriodCondition.isEmpty()) {
            lsSQL += " AND " + lsPeriodCondition;
        }

        System.out.println("Exclusion query: " + lsSQL);
        ResultSet loRSExist = poGRider.executeQuery(lsSQL);
        while (loRSExist.next()) {
            loExcludedStocks.add(loRSExist.getString("sStockIDx"));
        }

        // ─── Read sample size once – used as both group count and items-per-group ─
        int lnSample = getMaster().InventoryCountType().getQuantity();

        // ─── Fetch candidate stock items from Inventory ───────────────────────
        // b.sBinNumbr and b.cClassify from Inventory table (alias b)
        // are the authoritative grouping keys for BB and C cases
        String lsIncluded = getMaster().InventoryCountType().getIncluded();

        lsSQL = "SELECT"
                + "   a.sStockIDx"
                + " , b.sBranchCd"
                + " , b.sIndstCdx"
                + " , b.sWHouseID"
                + " , b.sLocatnID"
                + " , b.sBinNumbr" // grouping key for BB case
                + " , b.cClassify" // grouping key for C  case
                + " FROM Inv_Master a"
                + " INNER JOIN Inventory b ON a.sStockIDx = b.sStockIDx"
                + " WHERE b.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode());

        if (!psIndustryCode.isEmpty()) {
            lsSQL += " AND b.sIndstCdx = " + SQLUtil.toSQL(psIndustryCode);
        }
        if (!psCategorCD.isEmpty()) {
            lsSQL += " AND a.sCategrCd = " + SQLUtil.toSQL(psCategorCD);
        }

        // Pre-filter at DB level: only fetch rows relevant to the inclusion type
        switch (lsIncluded) {
            case "BB":
                lsSQL += " AND b.sBinNumbr IS NOT NULL AND b.sBinNumbr <> ''";
                break;
            case "C":
                lsSQL += " AND b.cClassify IS NOT NULL AND b.cClassify <> ''";
                break;
        }

        System.out.println("Candidate query: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        if (MiscUtil.RecordCount(loRS) <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "No record found.");
            return poJSON;
        }

        // ─── Load all candidates into a list, applying exclusion filter ───────
        List<Map<String, String>> loCandidates = new ArrayList<>();
        loRS.beforeFirst();
        while (loRS.next()) {
            String lsStockId = loRS.getString("sStockIDx");
            String lsBinNumbr = loRS.getString("sBinNumbr"); // b.sBinNumbr
            String lsClassify = loRS.getString("cClassify"); // b.cClassify

            // Skip already-counted items at source
            if (loExcludedStocks.contains(lsStockId)) {
                continue;
            }

            Map<String, String> loRow = new HashMap<>();
            loRow.put("sStockIDx", lsStockId);
            loRow.put("sBinNumbr", lsBinNumbr != null ? lsBinNumbr : "");
            loRow.put("cClassify", lsClassify != null ? lsClassify : "");
            loCandidates.add(loRow);
        }

        if (loCandidates.isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "No eligible records found after applying period exclusions.");
            return poJSON;
        }

        // ─── Filter / select by inclusion type ───────────────────────────────
        List<Map<String, String>> loSelected = new ArrayList<>();
        Random loRandom = new Random();

        switch (lsIncluded) {

            case "AI": // All Items – include everything, sorted by b.sBinNumbr → b.cClassify
                loSelected.addAll(loCandidates);
                loSelected.sort(Comparator
                        .comparing((Map<String, String> r) -> r.get("sBinNumbr"))
                        .thenComparing(r -> r.get("cClassify")));
                break;

            case "BB": // Bins Only – pick N bins (lnSample), then N items per bin (lnSample)
                // Step 1: Group candidates by b.sBinNumbr (non-empty guaranteed by query pre-filter)
                Map<String, List<Map<String, String>>> loBinGroups = new LinkedHashMap<>();
                for (Map<String, String> loRow : loCandidates) {
                    loBinGroups.computeIfAbsent(loRow.get("sBinNumbr"), k -> new ArrayList<>()).add(loRow);
                }

                if (loBinGroups.isEmpty()) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "No items with assigned bins found.");
                    return poJSON;
                }

                // Step 2: Shuffle distinct b.sBinNumbr values
                List<String> loBinKeys = new ArrayList<>(loBinGroups.keySet());
                Collections.shuffle(loBinKeys, loRandom);

                // lnSample = how many bins to pick; fallback to all if 0 or exceeds available
                int lnBinTarget = (lnSample <= 0 || lnSample > loBinKeys.size())
                        ? loBinKeys.size() : lnSample;
                // total items target = N bins × N items each
                int lnBinTotalTarget = lnBinTarget * lnSample;
                int lnBinTotalCount = 0;

                // Step 3: Pull bins; extend into extra bins if item target not yet reached
                for (int i = 0; i < loBinKeys.size(); i++) {
                    // Stop if bin quota met AND item target met
                    if (i >= lnBinTarget && lnBinTotalCount >= lnBinTotalTarget) {
                        break;
                    }
                    // Stop early if item target already satisfied
                    if (lnBinTotalCount >= lnBinTotalTarget) {
                        break;
                    }

                    List<Map<String, String>> loBinItems = new ArrayList<>(loBinGroups.get(loBinKeys.get(i)));
                    Collections.shuffle(loBinItems, loRandom);

                    int lnStillNeeded = lnBinTotalTarget - lnBinTotalCount;
                    int lnItemCount = (lnSample > loBinItems.size())
                            ? loBinItems.size()
                            : Math.min(lnSample, lnStillNeeded);

                    List<Map<String, String>> loSampledBinItems = new ArrayList<>(loBinItems.subList(0, lnItemCount));

                    // Sort items within each bin by b.cClassify
                    loSampledBinItems.sort(Comparator.comparing(r -> r.get("cClassify")));
                    loSelected.addAll(loSampledBinItems);
                    lnBinTotalCount += loSampledBinItems.size();
                }
                break;

            case "C": // Classified – pick N classes (lnSample), then N items per class (lnSample)
                // Step 1: Group candidates by b.cClassify (non-empty guaranteed by query pre-filter)
                Map<String, List<Map<String, String>>> loClassGroups = new LinkedHashMap<>();
                for (Map<String, String> loRow : loCandidates) {
                    loClassGroups.computeIfAbsent(loRow.get("cClassify"), k -> new ArrayList<>()).add(loRow);
                }

                if (loClassGroups.isEmpty()) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "No items with assigned classification found.");
                    return poJSON;
                }

                // Step 2: Shuffle distinct b.cClassify values
                List<String> loClassKeys = new ArrayList<>(loClassGroups.keySet());
                Collections.shuffle(loClassKeys, loRandom);

                // lnSample = how many classifications to pick; fallback to all if 0 or exceeds available
                int lnClassTarget = (lnSample <= 0 || lnSample > loClassKeys.size())
                        ? loClassKeys.size() : lnSample;
                // total items target = N classes × N items each
                int lnClassTotalTarget = lnClassTarget * lnSample;
                int lnClassTotalCount = 0;

                // Step 3: Pull classes; extend into extra classes if item target not yet reached
                for (int i = 0; i < loClassKeys.size(); i++) {
                    // Stop if class quota met AND item target met
                    if (i >= lnClassTarget && lnClassTotalCount >= lnClassTotalTarget) {
                        break;
                    }
                    // Stop early if item target already satisfied
                    if (lnClassTotalCount >= lnClassTotalTarget) {
                        break;
                    }

                    List<Map<String, String>> loClassItems = new ArrayList<>(loClassGroups.get(loClassKeys.get(i)));
                    Collections.shuffle(loClassItems, loRandom);

                    int lnStillNeeded = lnClassTotalTarget - lnClassTotalCount;
                    int lnItemCount = (lnSample > loClassItems.size())
                            ? loClassItems.size()
                            : Math.min(lnSample, lnStillNeeded);

                    List<Map<String, String>> loSampledClassItems = new ArrayList<>(loClassItems.subList(0, lnItemCount));

                    // Sort items within each classification by b.sBinNumbr
                    loSampledClassItems.sort(Comparator.comparing(r -> r.get("sBinNumbr")));
                    loSelected.addAll(loSampledClassItems);
                    lnClassTotalCount += loSampledClassItems.size();
                }
                break;

            case "RX": // Random 
                Collections.shuffle(loCandidates, loRandom);

                int lnSampleSize = (lnSample <= 0 || lnSample > loCandidates.size())
                        ? loCandidates.size() : lnSample;

                loSelected = new ArrayList<>(loCandidates.subList(0, lnSampleSize));

                // Sort by b.sBinNumbr → b.cClassify for physical walkability
                loSelected.sort(Comparator
                        .comparing((Map<String, String> r) -> r.get("sBinNumbr"))
                        .thenComparing(r -> r.get("cClassify")));
                break;

            default:
                poJSON.put("result", "error");
                poJSON.put("message", "Unknown inclusion type: " + lsIncluded);
                return poJSON;
        }

        if (loSelected.isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "No items matched the selected inclusion criteria.");
            return poJSON;
        }

        // ─── Populate detail rows (final exclusion guard) ─────────────────────
        int lnCtr = 1;
        for (Map<String, String> loRow : loSelected) {
            String lsStockId = loRow.get("sStockIDx");

            // Final guard: skip if stock was already counted in the period
            if (loExcludedStocks.contains(lsStockId)) {
                continue;
            }

            getDetail(lnCtr).setStockId(lsStockId);
            lnCtr++;
        }

        if (lnCtr == 1) {
            poJSON.put("result", "error");
            poJSON.put("message", "All selected items were already counted in the current period.");
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        poJSON.put("message", (lnCtr - 1) + " item(s) successfully generated.");
        return poJSON;
    }
    
    
}
