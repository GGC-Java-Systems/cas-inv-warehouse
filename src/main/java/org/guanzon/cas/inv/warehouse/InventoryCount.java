package org.guanzon.cas.inv.warehouse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
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
import org.apache.commons.codec.binary.Base64;
import org.guanzon.appdriver.agent.ActionAuthManager;
import org.guanzon.appdriver.agent.MatrixAuthChecker;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.agent.systables.Model_Transaction_Attachment;
import org.guanzon.appdriver.agent.systables.SysTableContollers;
import org.guanzon.appdriver.agent.systables.TransactionAttachment;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscReplUtil;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.base.WebFile;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.appdriver.token.RequestAccess;
import org.guanzon.cas.client.model.Model_Client_Master;
import org.guanzon.cas.client.services.ClientModels;
import org.json.simple.JSONObject;
import org.guanzon.cas.inv.warehouse.status.InventoryCountStatus;
import org.guanzon.cas.inv.InventoryTransaction;
import org.guanzon.cas.inv.warehouse.model.Model_Inventory_Count_Detail;
import org.guanzon.cas.inv.warehouse.model.Model_Inventory_Count_Master;
import org.guanzon.cas.inv.warehouse.report.ReportUtil;
import org.guanzon.cas.inv.warehouse.report.ReportUtilListener;
import org.guanzon.cas.inv.warehouse.services.InvWarehouseModels;
import org.guanzon.cas.inv.warehouse.status.InventoryCountPrint;
import org.guanzon.cas.inv.warehouse.validators.InventoryCountValidatorFactory;
import org.guanzon.cas.parameter.InventoryCountType;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class InventoryCount extends Transaction {

    private String psIndustryCode = "";
    private String psCompanyID = "";
    private String psCategorCD = "";
    private String psApprovalUser = "";
    private List<Model> paMaster;
    private List<TransactionAttachment> paAttachments;

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

    private TransactionAttachment TransactionAttachment()
            throws SQLException,
            GuanzonException {
        return new SysTableContollers(poGRider, null).TransactionAttachment();
    }

    private List<TransactionAttachment> TransactionAttachmentList() {
        return paAttachments;
    }

    public TransactionAttachment TransactionAttachmentList(int row) {
        return (TransactionAttachment) paAttachments.get(row);
    }

    @SuppressWarnings("unchecked")
    public List<Model_Transaction_Attachment> getAttachmentList() {
        return (List<Model_Transaction_Attachment>) (List<?>) paAttachments;
    }

    public Model_Transaction_Attachment getAttachmentList(int entryRow) {
        TransactionAttachment loAttachment = (TransactionAttachment) paAttachments.get(entryRow);
        return loAttachment.getModel();
    }

    public int getTransactionAttachmentCount() {
        if (paAttachments == null) {
            paAttachments = new ArrayList<>();
        }

        return paAttachments.size();
    }

    public JSONObject initTransaction() throws GuanzonException, SQLException {
        SOURCE_CODE = "InvC";

        poMaster = new InvWarehouseModels(poGRider).InventoryCountMaster();
        poDetail = new InvWarehouseModels(poGRider).InventoryCountDetail();
        paMaster = new ArrayList<Model>();
        paDetail = new ArrayList<Model>();
        paAttachments = new ArrayList<>();
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
                + ", a.nCounterx"
                + " FROM Inventory_Count_Master a "
                + "     LEFT JOIN Inventory_Count_Type b ON a.sInvCtrID = b.sInvCtrID"
                + "     LEFT JOIN Branch c ON a.sBranchCd = c.sBranchCd";
    }

    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        openTransaction(transactionNo);

        poJSON = loadAttachments();
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"), "Unable to load transaction attachments.\n" + (String) poJSON.get("message"));
            return poJSON;
        }
        return poJSON;
    }

    public JSONObject NewTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();
        poJSON = newTransaction();
        paAttachments = new ArrayList<>();
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
        OpenTransaction(getMaster().getTransactionNo());
        return loJSON;
    }

    @Override
    public JSONObject saveOthers() {

        //assign other info on attachment
        for (int lnCtr = 0; lnCtr <= getTransactionAttachmentCount() - 1; lnCtr++) {
            TransactionAttachmentList(lnCtr).getModel().setSourceNo(getMaster().getTransactionNo());
            TransactionAttachmentList(lnCtr).getModel().setSourceCode(getSourceCode());
            TransactionAttachmentList(lnCtr).getModel().setBranchCode(getMaster().getBranchCode());
            TransactionAttachmentList(lnCtr).getModel().setImagePath(System.getProperty("sys.default.path.temp.attachments"));

            String lsOriginalFileName = TransactionAttachmentList(lnCtr).getModel().getFileName();
            //Check existing file name in database
            if (EditMode.ADDNEW == TransactionAttachmentList(lnCtr).getModel().getEditMode()) {
                int lnCopies = 0;
                String fsFilePath = TransactionAttachmentList(lnCtr).getModel().getImagePath() + "/" + TransactionAttachmentList(lnCtr).getModel().getFileName();
                String lsNewFileName = TransactionAttachmentList(lnCtr).getModel().getFileName();
                try {
                    while ("error".equals((String) checkExistingFileName(lsNewFileName).get("result"))) {
                        lnCopies++;
                        //Rename the file
                        int dotIndex = TransactionAttachmentList(lnCtr).getModel().getFileName().lastIndexOf(".");
                        if (dotIndex == -1) {
                            lsNewFileName = TransactionAttachmentList(lnCtr).getModel().getFileName() + "_" + lnCopies;
                        } else {
                            lsNewFileName = TransactionAttachmentList(lnCtr).getModel().getFileName().substring(0, dotIndex) + "_" + lnCopies + TransactionAttachmentList(lnCtr).getModel().getFileName().substring(dotIndex);
                        }
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(InventoryCount.class.getName()).log(Level.SEVERE, null, ex);
                } catch (GuanzonException ex) {
                    Logger.getLogger(InventoryCount.class.getName()).log(Level.SEVERE, null, ex);
                }

                if (lnCopies > 0) {
                    Path source = Paths.get(fsFilePath);
                    try {
                        // Copy file into the target directory with a new name
                        Path target = Paths.get(System.getProperty("sys.default.path.temp.attachments")).resolve(lsNewFileName);
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                        //check if file is existing
                        int lnChecker = 0;
                        File file = new File(TransactionAttachmentList(lnCtr).getModel().getImagePath() + "/" + lsNewFileName);
                        while (!file.exists() && lnChecker < 5) {
                            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("Re-Copying... " + lnChecker);
                            lnChecker++;
                        }
                        TransactionAttachmentList(lnCtr).getModel().setFileName(lsNewFileName);
                        System.out.println("File copied successfully as " + lsNewFileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            //Upload Attachment when send status is 0
            try {
                if ("0".equals(TransactionAttachmentList(lnCtr).getModel().getSendStatus())) {
                    poJSON = uploadCASAttachments(poGRider, System.getProperty("sys.default.access.token"), lnCtr, lsOriginalFileName);
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
                poJSON = setJSON("error", MiscUtil.getException(ex));
                return poJSON;
            }

        }
        try {
            System.out.println("--------------------------SAVE OTHERS---------------------------------------------");
            System.out.println("Class Edit Mode : " + getEditMode());
            System.out.println("Master Edit Mode : " + getMaster().getEditMode());
            System.out.println("-----------------------------------------------------------------------");
//            for(int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++){
//            System.out.println("COUNTER : " + lnCtr);
//            System.out.println("Transaction No : " + getDetail(lnCtr).getTransactionNo());
//            System.out.println("Stock ID : " + getDetail(lnCtr).getStockId());
            System.out.println("-----------------------------------------------------------------------");
//            }

            //Save Attachments
            System.out.println("-----------------------------SAVE TRANSACTION ATTACHMENT------------------------------------------");
            for (int lnCtr = 0; lnCtr <= getTransactionAttachmentCount() - 1; lnCtr++) {
                if (paAttachments.get(lnCtr).getEditMode() == EditMode.ADDNEW || paAttachments.get(lnCtr).getEditMode() == EditMode.UPDATE) {
                    paAttachments.get(lnCtr).getModel().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
                    paAttachments.get(lnCtr).getModel().setModifiedDate(poGRider.getServerDate());
                    paAttachments.get(lnCtr).setWithParentClass(true);
                    poJSON = paAttachments.get(lnCtr).saveRecord();
                    if (!isJSONSuccess(poJSON)) {
                        return poJSON;
                    }
                }
            }
            System.out.println("-----------------------------------------------------------------------");

        } catch (SQLException | GuanzonException | CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON = setJSON("error", MiscUtil.getException(ex));
            return poJSON;
        }

        poJSON = setJSON("success", "success");
        return poJSON;
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
            if (loDetail.getStockId() == null) {
                paDetail.remove(lnCtr);
                continue;
            }
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
        GValidator loValidator = InventoryCountValidatorFactory.make(psIndustryCode);

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
//            poJSON.put("message", "Transaction confirmed successfully.");
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
                    "Transaction No»Date»Count Type",
                    "sTransNox»dTransact»sDescript",
                    "a.sTransNox»a.dTransact»b.sDescript",
                    byExact ? (byCode ? 0 : 1) : 2);

            if (poJSON != null) {
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                return OpenTransaction((String) poJSON.get("sTransNox"));

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
                "sBranchNm", poGRider.getBranchName());
        poReportJasper.addParameter("sAddressx", poGRider.getAddress());
        poReportJasper.addParameter("sCompnyNm", poGRider.getClientName());
        poReportJasper.addParameter("sTransNox", getMaster().getTransactionNo());
        poReportJasper.addParameter("sRemarksx", getMaster().getRemarks() == null ? "" : getMaster().getRemarks());
        poReportJasper.addParameter("DatePrinted", SQLUtil.dateFormat(poGRider.getServerDate(), SQLUtil.FORMAT_TIMESTAMP));
        poReportJasper.addParameter("watermarkImagePath", poGRider.getReportPath() + "images\\blank.png");

        poReportJasper.setReportName("Inventory Count AS OF - " + SQLUtil.dateFormat(getMaster().getTransactionDate(), SQLUtil.FORMAT_LONG_DATE));
        poReportJasper.setJasperPath(InventoryCountPrint.getJasperReport(psIndustryCode));

        //process by ResultSet
        String lsSQL = InventoryCountPrint.PrintRecordQuery();
        lsSQL = MiscUtil.addCondition(lsSQL, "InventoryCountMaster.sTransNox = " + SQLUtil.toSQL(getMaster().getTransactionNo()));

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

    public boolean isJSONSuccess(JSONObject foJSON) {
        return ("success".equals((String) foJSON.get("result")) || !"error".equals((String) foJSON.get("result")));
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

        showStatusHistoryUI("Inventory Count History", (String) poMaster.getValue("sTransNox"), entryBy, entryDate, crs);
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
        String lsSQL;
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
        //if period allow all 
        if (!lsPeriodCondition.isEmpty()) {
            lsSQL = "SELECT b.sStockIDx"
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
        }
        // ─── Read sample size once – used as both group count and items-per-group ─
        int lnSample = getMaster().InventoryCountType().getQuantity();

        // ─── Fetch candidate stock items from Inventory ───────────────────────
        // b.sBinNumbr and b.cClassify from Inventory table (alias b)
        // are the authoritative grouping keys for BB and C cases
        String lsIncluded = getMaster().InventoryCountType().getIncluded();

        lsSQL = "SELECT"
                + "   a.sStockIDx"
                + " , a.sBranchCd"
                + " , b.sIndstCdx"
                + " , a.sWHouseID"
                + " , a.sLocatnID"
                + " , a.sBinNumbr" // grouping key for BB case
                + " , a.cClassify" // grouping key for C  case
                + " FROM Inv_Master a"
                + " INNER JOIN Inventory b ON a.sStockIDx = b.sStockIDx"
                + " WHERE a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode());

        if (!psIndustryCode.isEmpty()) {
            lsSQL += " AND a.sIndstCdx = " + SQLUtil.toSQL(psIndustryCode);
        }
//        if (!psCategorCD.isEmpty()) {
//            lsSQL += " AND b.sCategrCd = " + SQLUtil.toSQL(psCategorCD);
//        }

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

    public JSONObject searchRequestBy(String value, boolean byCode) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        Model_Client_Master loSubClass = new ClientModels(poGRider).ClientMaster();
        loSubClass.setRecordStatus(RecordStatus.ACTIVE);

        String lsSQL = "SELECT"
                + " b.sClientID,"
                + " b.sCompnyNm,"
                + " b.sLastName,"
                + " b.sFrstName,"
                + " b.sMiddName,"
                + " b.sMaidenNm"
                + " FROM Employee_Master001 a"
                + " LEFT JOIN Client_Master b ON a.sEmployID = b.sClientID"
                + "  WHERE a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                + "  AND b.sClientID <> ''";

//        lsSQL = MiscUtil.addCondition(lsSQL, "a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode()));
        System.out.println("Search Query is = " + lsSQL);
        poJSON = ShowDialogFX.Search(poGRider,
                lsSQL,
                value,
                "Employee ID»Name",
                "sClientID»sCompnyNm",
                "b.sClientID»b.sCompnyNm",
                byCode ? 0 : 1);

        if (poJSON != null) {
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            poJSON = loSubClass.openRecord((String) poJSON.get("sClientID"));

            if ("success".equals((String) poJSON.get("result"))) {
                getMaster().setRequestedBy(loSubClass.getClientId());
                poJSON = new JSONObject();
                poJSON.put("result", "success");
            }

            return poJSON;
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;

        }
    }

    public JSONObject searchInventoryCountType(String value, boolean byCode) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        InventoryCountType loSubClass = new ParamControllers(poGRider, logwrapr).InventoryCountType();
        loSubClass.setRecordStatus(RecordStatus.ACTIVE);

        poJSON = loSubClass.searchRecord(value, byCode);

        System.out.println("result " + (String) poJSON.get("result"));
        if ("success".equals((String) poJSON.get("result"))) {

            getMaster().setInventoryCounterID(loSubClass.getModel().getInventoryCountID());
            poJSON = new JSONObject();
            poJSON.put("result", "success");
        }
        return poJSON;
    }

    public JSONObject setInclusion(String inclusionValue) throws GuanzonException, SQLException {
        if (getMaster().getInventoryCounterID().isEmpty()
                || getMaster().InventoryCountType().getIncluded() == null) {
            poJSON.put("result", "error");
            poJSON.put("message", "Please set the default rule first to continue.");
            return poJSON;
        }

        // Check if override is already same
        if (inclusionValue.equals(getMaster().InventoryCountType().getIncluded())) {
            poJSON.put("result", "error");
            poJSON.put("message", "Unable to override same existing rule.");
            return poJSON;
        }

        if (poGRider.getUserLevel() < UserRight.AUDIT) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            } else {
                if (Integer.parseInt(poJSON.get("nUserLevl").toString()) < UserRight.AUDIT) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "User is not an authorized approving officer.");
                    return poJSON;
                }
                psApprovalUser = poJSON.get("sUserIDxx") != null
                        ? poJSON.get("sUserIDxx").toString()
                        : poGRider.getUserID();
            }
        }

        try {
            // ─── Step 1: Collect currently active stock IDs in paDetail ──────────
            Set<String> loCurrentStocks = new HashSet<>();
            for (int i = 0; i < paDetail.size(); i++) {
                Model_Inventory_Count_Detail loD = (Model_Inventory_Count_Detail) paDetail.get(i);
                String lsStockId = loD.getStockId();
                if (lsStockId != null && !lsStockId.trim().isEmpty()) {
                    loCurrentStocks.add(lsStockId);
                }
            }

            // ─── Step 2: Build period exclusion condition ─────────────────────────
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

            // ─── Step 3: Build period-excluded stocks set ─────────────────────────
            Set<String> loExcludedStocks = new HashSet<>();
            if (!lsPeriodCondition.isEmpty()) {
                String lsExclusionSQL = "SELECT b.sStockIDx"
                        + " FROM Inventory_Count_Master a"
                        + " INNER JOIN Inventory_Count_Detail b ON a.sTransNox = b.sTransNox"
                        + " WHERE a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode())
                        + " AND " + lsPeriodCondition;

                ResultSet loRSExist = poGRider.executeQuery(lsExclusionSQL);
                while (loRSExist.next()) {
                    loExcludedStocks.add(loRSExist.getString("sStockIDx"));
                }
            }

            // ─── Step 4: Fetch ALL candidates from DB ────────────────────────────
            String lsSQL = "SELECT"
                    + "   a.sStockIDx"
                    + " , a.sBranchCd"
                    + " , b.sIndstCdx"
                    + " , a.sWHouseID"
                    + " , a.sLocatnID"
                    + " , a.sBinNumbr"
                    + " , a.cClassify"
                    + " FROM Inv_Master a"
                    + " INNER JOIN Inventory b ON a.sStockIDx = b.sStockIDx"
                    + " WHERE a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode());

            if (!psIndustryCode.isEmpty()) {
                lsSQL += " AND b.sIndstCdx = " + SQLUtil.toSQL(psIndustryCode);
            }

            // Pre-filter at DB level for BB and C
            switch (inclusionValue) {
                case "BB":
                    lsSQL += " AND a.sBinNumbr IS NOT NULL AND a.sBinNumbr <> ''";
                    break;
                case "C":
                    lsSQL += " AND a.cClassify IS NOT NULL AND a.cClassify <> ''";
                    break;
            }

            System.out.println("setInclusion candidate query: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            if (MiscUtil.RecordCount(loRS) <= 0) {
                poJSON.put("result", "error");
                poJSON.put("message", "No candidate records found.");
                return poJSON;
            }

            // ─── Step 5: Load candidates, exclude period-counted stocks ──────────
            List<Map<String, String>> loCandidates = new ArrayList<>();
            loRS.beforeFirst();
            while (loRS.next()) {
                String lsStockId = loRS.getString("sStockIDx");
                if (loExcludedStocks.contains(lsStockId)) {
                    continue;
                }

                Map<String, String> loRow = new HashMap<>();
                loRow.put("sStockIDx", lsStockId);
                loRow.put("sBinNumbr", loRS.getString("sBinNumbr") != null ? loRS.getString("sBinNumbr") : "");
                loRow.put("cClassify", loRS.getString("cClassify") != null ? loRS.getString("cClassify") : "");
                loCandidates.add(loRow);
            }

            if (loCandidates.isEmpty()) {
                poJSON.put("result", "error");
                poJSON.put("message", "No eligible candidates found after period exclusions.");
                return poJSON;
            }

            Random loRandom = new Random();
            List<Map<String, String>> loSelected = new ArrayList<>();

            switch (inclusionValue) {

                case "AI": {
                    loSelected.addAll(loCandidates);

                    if (loSelected.isEmpty()) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "No items available for AI override.");
                        return poJSON;
                    }

                    loSelected.sort(Comparator
                            .comparing((Map<String, String> r) -> r.get("sBinNumbr"))
                            .thenComparing(r -> r.get("cClassify")));
                    break;
                }

                case "BB": {
                    // ─── Collect bins already represented in paDetail ────────────
                    Set<String> loCurrentBins = new HashSet<>();
                    for (Map<String, String> loRow : loCandidates) {
                        if (loCurrentStocks.contains(loRow.get("sStockIDx"))) {
                            loCurrentBins.add(loRow.get("sBinNumbr"));
                        }
                    }

                    // ─── Group remaining candidates by sBinNumbr ─────────────────
                    Map<String, List<Map<String, String>>> loBinGroups = new LinkedHashMap<>();
                    for (Map<String, String> loRow : loCandidates) {
                        if (!loCurrentBins.contains(loRow.get("sBinNumbr"))) {
                            loBinGroups.computeIfAbsent(
                                    loRow.get("sBinNumbr"), k -> new ArrayList<>()
                            ).add(loRow);
                        }
                    }

                    if (loBinGroups.isEmpty()) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "No available bins left to override with.");
                        return poJSON;
                    }

                    // ─── Pick exactly 1 random bin → 1 random item from it ───────
                    List<String> loBinKeys = new ArrayList<>(loBinGroups.keySet());
                    Collections.shuffle(loBinKeys, loRandom);
                    String lsPickedBin = loBinKeys.get(0);

                    List<Map<String, String>> loBinItems = new ArrayList<>(loBinGroups.get(lsPickedBin));
                    Collections.shuffle(loBinItems, loRandom);
                    loSelected.add(loBinItems.get(0));
                    break;
                }

                case "C": {
                    // ─── Collect classifications already represented in paDetail ──
                    Set<String> loCurrentClasses = new HashSet<>();
                    for (Map<String, String> loRow : loCandidates) {
                        if (loCurrentStocks.contains(loRow.get("sStockIDx"))) {
                            loCurrentClasses.add(loRow.get("cClassify"));
                        }
                    }

                    // ─── Group remaining candidates by cClassify ──────────────────
                    Map<String, List<Map<String, String>>> loClassGroups = new LinkedHashMap<>();
                    for (Map<String, String> loRow : loCandidates) {
                        if (!loCurrentClasses.contains(loRow.get("cClassify"))) {
                            loClassGroups.computeIfAbsent(
                                    loRow.get("cClassify"), k -> new ArrayList<>()
                            ).add(loRow);
                        }
                    }

                    if (loClassGroups.isEmpty()) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "No available classifications left to override with.");
                        return poJSON;
                    }

                    // ─── Pick exactly 1 random class → 1 random item from it ─────
                    List<String> loClassKeys = new ArrayList<>(loClassGroups.keySet());
                    Collections.shuffle(loClassKeys, loRandom);
                    String lsPickedClass = loClassKeys.get(0);

                    List<Map<String, String>> loClassItems = new ArrayList<>(loClassGroups.get(lsPickedClass));
                    Collections.shuffle(loClassItems, loRandom);
                    loSelected.add(loClassItems.get(0));
                    break;
                }

                case "RX": {
                    // ─── RX override: re-randomize from ALL eligible candidates ──────────
                    if (loCandidates.isEmpty()) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "No items available for random override.");
                        return poJSON;
                    }

                    Collections.shuffle(loCandidates, loRandom);

                    int lnRandomSize = 1 + loRandom.nextInt(loCandidates.size());
                    loSelected = new ArrayList<>(loCandidates.subList(0, lnRandomSize));

                    loSelected.sort(Comparator
                            .comparing((Map<String, String> r) -> r.get("sBinNumbr"))
                            .thenComparing(r -> r.get("cClassify")));
                    break;
                }

                default: {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Unknown inclusion type: " + inclusionValue);
                    return poJSON;
                }
            }

            if (loSelected.isEmpty()) {
                poJSON.put("result", "error");
                poJSON.put("message", "No replacement items found for the override.");
                return poJSON;
            }

            // ─── Reset paDetail and seed one blank row so getDetail() works ──────
            paDetail = new ArrayList<>();
            Model_Inventory_Count_Detail loSeed = new InvWarehouseModels(poGRider).InventoryCountDetail();
            loSeed.newRecord();
            loSeed.setTransactionNo(getMaster().getTransactionNo());
            loSeed.setEntryNo(1);
            paDetail.add(loSeed);

            // ─── Populate detail rows ─────────────────────────────────────────────
            int lnCtr = 0;
            for (Map<String, String> loRow : loSelected) {
                String lsStockId = loRow.get("sStockIDx");
                if (loExcludedStocks.contains(lsStockId)) {
                    continue; // final guard
                }
                getDetail(lnCtr + 1).setStockId(lsStockId);
                lnCtr++;
            }

            if (lnCtr == 0) {
                poJSON.put("result", "error");
                poJSON.put("message", "All override candidates were already counted in the current period.");
                return poJSON;
            }

            getMaster().setIncluded(inclusionValue);
            poJSON = new JSONObject();
            poJSON.put("result", "success");
            poJSON.put("message", lnCtr + " item(s) successfully overridden.");
            return poJSON;

        } catch (SQLException | GuanzonException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
            return poJSON;
        }
    }
    //------------------------ATTACHMENT CODE HERE-------------------------------------------------------------------------------------

    public JSONObject loadAttachments()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        paAttachments = new ArrayList<>();

        TransactionAttachment loAttachment = new SysTableContollers(poGRider, null).TransactionAttachment();
        List loList = loAttachment.getAttachments(SOURCE_CODE, getMaster().getTransactionNo());
        for (int lnCtr = 0; lnCtr <= loList.size() - 1; lnCtr++) {
            paAttachments.add(TransactionAttachment());
            poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).openRecord((String) loList.get(lnCtr));
            if ("success".equals((String) poJSON.get("result"))) {
                if (getMaster().getEditMode() == EditMode.UPDATE) {
                    poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).updateRecord();
                }
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getTransactionNo());
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceNo());
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceCode());
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getFileName());

                //Download Attachments
                poJSON = WebFile.DownloadFile(WebFile.getAccessToken(System.getProperty("sys.default.access.token")),
                        "0032" //Constant
                        ,
                         "" //Empty
                        ,
                         paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getFileName(),
                        SOURCE_CODE,
                        paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceNo(),
                        "");
                if ("success".equals((String) poJSON.get("result"))) {

                    poJSON = (JSONObject) poJSON.get("payload");
                    if (WebFile.Base64ToFile((String) poJSON.get("data"),
                            (String) poJSON.get("hash"),
                            System.getProperty("sys.default.path.temp.attachments") + "/",
                            (String) poJSON.get("filename"))) {
                        System.out.println("poJSON success: " + poJSON.toJSONString());
                        System.out.println("File downloaded succesfully.");
                    } else {
                        poJSON = (JSONObject) poJSON.get("error");
                        poJSON.put("result", "error");
                        System.out.println("ERROR WebFile.DownloadFile: " + poJSON.get("message"));
                        System.out.println("poJSON error WebFile.DownloadFile: " + poJSON.toJSONString());
                    }

                } else {
                    System.out.println("poJSON error WebFile.DownloadFile: " + poJSON.toJSONString());
                }
            }
        }
        return poJSON;
    }

    @Override
    public String getSourceCode() {
        return SOURCE_CODE;
    }

    private JSONObject setJSON(String fsResult, String fsMessage) {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", fsResult);
        loJSON.put("message", fsMessage);
        return loJSON;
    }

    public JSONObject addAttachment()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        if (paAttachments.isEmpty()) {
            paAttachments.add(TransactionAttachment());
            poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).newRecord();
        } else {
            if (!paAttachments.get(paAttachments.size() - 1).getModel().getTransactionNo().isEmpty()) {
                paAttachments.add(TransactionAttachment());
            } else {
                poJSON = setJSON("error", "Unable to add transaction attachment.");
                return poJSON;
            }
        }
        poJSON = setJSON("success", "success");
        return poJSON;
    }

    public JSONObject removeAttachment(int fnRow) throws GuanzonException, SQLException {
        poJSON = new JSONObject();
        if (getTransactionAttachmentCount() <= 0) {
            poJSON = setJSON("error", "No transaction attachment to be removed.");
            return poJSON;
        }

        if (paAttachments.get(fnRow).getEditMode() == EditMode.ADDNEW) {
            paAttachments.remove(fnRow);
            System.out.println("Attachment :" + fnRow + " Removed");
        } else {
            paAttachments.get(fnRow).getModel().setRecordStatus(RecordStatus.INACTIVE);
            System.out.println("Attachment :" + fnRow + " Deactivate");
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    public int addAttachment(String fFileName) throws SQLException, GuanzonException {
        for (int lnCtr = 0; lnCtr <= getTransactionAttachmentCount() - 1; lnCtr++) {
            if (fFileName.equals(paAttachments.get(lnCtr).getModel().getFileName())
                    && RecordStatus.INACTIVE.equals(paAttachments.get(lnCtr).getModel().getRecordStatus())) {
                paAttachments.get(lnCtr).getModel().setRecordStatus(RecordStatus.ACTIVE);
                System.out.println("Attachment :" + lnCtr + " Activate");
                return lnCtr;
            }
        }

        addAttachment();
        paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setFileName(fFileName);
        paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setSourceNo(getMaster().getTransactionNo());
        paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setRecordStatus(RecordStatus.ACTIVE);
        return getTransactionAttachmentCount() - 1;
    }

    public void copyFile(String fsPath) {
        Path source = Paths.get(fsPath);
        Path targetDir = Paths.get(System.getProperty("sys.default.path.temp.attachments"));

        try {
            // Ensure target directory exists
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            // Copy file into the target directory
            Files.copy(source, targetDir.resolve(source.getFileName()),
                    StandardCopyOption.REPLACE_EXISTING);

            System.out.println("File copied successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject checkExistingFileName(String fsValue) throws SQLException, GuanzonException {
        poJSON = new JSONObject();

        String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(TransactionAttachment().getModel()), " sFileName = " + SQLUtil.toSQL(fsValue));
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0) {
                if (loRS.next()) {
                    if (loRS.getString("sFileName") != null && !"".equals(loRS.getString("sFileName"))) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "File name already exist in database.\nTry changing the file name to upload.");
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
        }
        return poJSON;
    }

    public JSONObject uploadCASAttachments(GRiderCAS instance, String access, int fnRow, String fsOriginalFileName) throws Exception {
        poJSON = new JSONObject();
        System.out.println("Uploading... : fsOriginalFileName : " + fsOriginalFileName);
        System.out.println("New File Name... : " + paAttachments.get(fnRow).getModel().getFileName());
        String hash;
        String lsFile = paAttachments.get(fnRow).getModel().getFileName();

        //check if new file is existing
        File file = new File(paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile);
        if (!file.exists()) {
            //check if original file is existing
            lsFile = fsOriginalFileName;
            file = new File(paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile);
            if (!file.exists()) {
                poJSON = setJSON("error", "Cannot locate file in " + paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile
                        + ".\nContact system administrator for assistance.");
                return poJSON;
            }
        }

        //check if file hash is not empty
        hash = paAttachments.get(fnRow).getModel().getMD5Hash();
        if (paAttachments.get(fnRow).getModel().getMD5Hash() == null || "".equals(paAttachments.get(fnRow).getModel().getMD5Hash())) {
            hash = MiscReplUtil.md5Hash(paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile);
        }

        JSONObject result = WebFile.UploadFile(getAccessToken(access),
                "0032",
                "",
                paAttachments.get(fnRow).getModel().getFileName(),
                instance.getBranchCode(),
                hash,
                encodeFileToBase64Binary(file),
                paAttachments.get(fnRow).getModel().getSourceCode(),
                paAttachments.get(fnRow).getModel().getSourceNo(),
                "");

        if ("error".equalsIgnoreCase((String) result.get("result"))) {
            System.out.println("Upload Error : " + result.toJSONString());
            System.out.println("Upload Error : " + paAttachments.get(fnRow).getModel().getFileName());
            poJSON = setJSON("error", "System error while uploading file " + paAttachments.get(fnRow).getModel().getFileName()
                    + ".\nContact system administrator for assistance.");
            return poJSON;
        }
        paAttachments.get(fnRow).getModel().setMD5Hash(hash);
        paAttachments.get(fnRow).getModel().setSendStatus("1");
        System.out.println("Upload Success : " + paAttachments.get(fnRow).getModel().getFileName());
        poJSON.put("result", "success");
        return poJSON;
    }

    private static String encodeFileToBase64Binary(File file) throws Exception {
        FileInputStream fileInputStreamReader = new FileInputStream(file);
        byte[] bytes = new byte[(int) file.length()];
        fileInputStreamReader.read(bytes);
        return new String(Base64.encodeBase64(bytes), "UTF-8");
    }

    private static JSONObject token = null;

    private static String getAccessToken(String access) {
        try {
            JSONParser oParser = new JSONParser();
            if (token == null) {
                token = (JSONObject) oParser.parse(new FileReader(access));
            }

            Calendar current_date = Calendar.getInstance();
            current_date.add(Calendar.MINUTE, -25);
            Calendar date_created = Calendar.getInstance();
            date_created.setTime(SQLUtil.toDate((String) token.get("created"), SQLUtil.FORMAT_TIMESTAMP));

            //Check if token is still valid within the time frame
            //Request new access token if not in the current period range
            if (current_date.after(date_created)) {
                String[] xargs = new String[]{(String) token.get("parent"), access};
                RequestAccess.main(xargs);
                token = (JSONObject) oParser.parse(new FileReader(access));
            }

            return (String) token.get("access_key");
        } catch (IOException ex) {
            return null;
        } catch (ParseException ex) {
            return null;
        }
    }
}
