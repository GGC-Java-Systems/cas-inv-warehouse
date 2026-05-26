package org.guanzon.cas.inv.warehouse;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
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
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.inv.InvMaster;
import org.guanzon.cas.inv.InvTransCons;
import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.inv.InventoryTransaction;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.inv.warehouse.model.Model_Inv_Stock_Request_Detail;
import org.guanzon.cas.inv.warehouse.model.Model_Inv_Stock_Request_Master;
import org.guanzon.cas.inv.warehouse.report.ReportUtil;
import org.guanzon.cas.inv.warehouse.report.ReportUtilListener;
import org.guanzon.cas.inv.warehouse.services.InvWarehouseModels;
import org.guanzon.cas.inv.warehouse.status.InventoryStockIssuanceStatus;
import org.guanzon.cas.inv.warehouse.status.StockRequestStatus;
import org.guanzon.cas.inv.warehouse.validators.StockRequestValidatorFactory;
import org.guanzon.cas.parameter.Branch;
import org.guanzon.cas.parameter.Brand;
import org.guanzon.cas.parameter.Category;
import org.guanzon.cas.parameter.Industry;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class StockRequest extends Transaction {

    List<Model_Inv_Stock_Request_Master> paInvMaster;
    List<StockRequest> poStockRequest;
    static ROQ poStockROQ;

    private String psIndustryCode = "";
    private String psCompanyID = "";
    private String psCategorCD = "";

    public void setIndustryID(String industryId) {
        psIndustryCode = industryId;
    }

    public void setCompanyID(String companyId) {
        psCompanyID = companyId;
    }

    public void setCategoryID(String categoryId) {
        psCategorCD = categoryId;
    }

    public JSONObject InitTransaction() {
        SOURCE_CODE = InvTransCons.INVENTORY_STOCK_REQUEST;

        poMaster = new InvWarehouseModels(poGRider).InventoryStockRequestMaster();
        poDetail = new InvWarehouseModels(poGRider).InventoryStockRequestDetail();
        paDetail = new ArrayList<>();
        poStockRequest = new ArrayList<>();

        return initialize();
    }

    public JSONObject NewTransaction() throws CloneNotSupportedException {
        poJSON = new JSONObject();
        poJSON = newTransaction();
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        Master().setIndustryId(psIndustryCode);
        Master().setCompanyID(psCompanyID);
        Master().setCategoryId(psCategorCD);
        Master().setBranchCode(poGRider.getBranchCode());
        return poJSON;
    }

    public JSONObject SaveTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        JSONObject loJSON = new JSONObject();
        loJSON = saveTransaction();
        openTransaction(Master().getTransactionNo());
        return loJSON;
    }

    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        return openTransaction(transactionNo);
    }

    public JSONObject UpdateTransaction() {
        if (Master().getTransactionStatus() == StockRequestStatus.CONFIRMED) {
            poJSON.put("result", "error");
            poJSON.put("message", "Unable to modify processed / confirmed transaction.");
            return poJSON;
        }
        if (Master().getTransactionStatus() == StockRequestStatus.PROCESSED) {

            poJSON.put("result", "error");
            poJSON.put("message", "Unable to modify processed / confirmed transaction.");
            return poJSON;

        }
        return updateTransaction();
    }

    public Model_Inv_Stock_Request_Master INVMaster(int row) {
        return (Model_Inv_Stock_Request_Master) paInvMaster.get(row);
    }

    public int getINVMasterCount() {
        return this.paInvMaster.size();
    }

    private Model_Inv_Stock_Request_Master INVMasterList() {
        return new InvWarehouseModels(poGRider).InventoryStockRequestMaster();
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

                setApproving((String) poJSON.get("sUserIDxx"));
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

    private JSONObject saveUpdates(String status)
            throws CloneNotSupportedException {
        poJSON = new JSONObject();
        int lnCtr;

        try {
            for (lnCtr = 0; lnCtr <= poStockRequest.size() - 1; lnCtr++) {
                if (StockRequestStatus.CONFIRMED.equals(status)) {
                    poStockRequest.get(lnCtr).Master().setProcessed(true);
                }
                System.out.println("ts1" + poGRider.getUserID());
                System.out.println("ts2" + poGRider.getServerDate());
                poStockRequest.get(lnCtr).Master().setModifyingId(poGRider.getUserID());
                poStockRequest.get(lnCtr).Master().setModifiedDate(poGRider.getServerDate());
                poStockRequest.get(lnCtr).setWithParent(true);
                poJSON = poStockRequest.get(lnCtr).SaveTransaction();
                if ("error".equals((String) poJSON.get("result"))) {
                    System.out.println("Stock Request Saving " + (String) poJSON.get("message"));
                    return poJSON;

                }
            }

        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(StockRequest.class
                    .getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    public JSONObject ConfirmTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();
        String lsApproveID = "";
        String lsStatus = StockRequestStatus.CONFIRMED;
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already confirmed.");
            return poJSON;
        }

// update into auth matrix maynard 2026-03-13       
        //validator
//        poJSON = isEntryOkay(StockRequestStatus.CONFIRMED);
//        if (!"success".equals((String) poJSON.get("result"))) {
//            return poJSON;
//        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
//             poJSON = ShowDialogFX.getUserApproval(poGRider);
//             if (!"success".equalsIgnoreCase((String) poJSON.get("result"))) {
//                ShowMessageFX.Warning((String) poJSON.get("message"), null, null);
//                return poJSON;
//             }
//        }
//========================================Authority Check Start===============================================
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
                check = new MatrixAuthChecker(poGRider, SOURCE_CODE, Master().getTransactionNo());
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
                            setApproving((String) poJSON.get("sUserIDxx"));
                            //check if current user is authorized to approved this transaction
                            poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                            //user is not authorized
                            if (!"success".equalsIgnoreCase((String) poJSON.get("result"))) {
                                return poJSON;
                            }
                        }
                    }

                    //check if authorization request is already approved by all authorizing personnel
                    if (!check.isAuthOkay()) {
                        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, Master().getTransactionNo());

                        lsStatus = Character.toString((char) (64 + Integer.parseInt(lsStatus)));
                        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
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
        poGRider.beginTrans("UPDATE STATUS", "Confirm Transaction", SOURCE_CODE, Master().getTransactionNo());

        poJSON = saveUpdates(StockRequestStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        //maynard - 2026.03.13 14:41
        InventoryTransaction loTrans = new InventoryTransaction(poGRider);

        loTrans.StockRequest((String) poMaster.getValue("sTransNox"), (Date) poMaster.getValue("dTransact"), false);

        String lsCondition = "0";
        for (Model loDetail : paDetail) {
            Model_Inv_Stock_Request_Detail detail = (Model_Inv_Stock_Request_Detail) loDetail;
            try {
                if (detail.getStockId() != null) {
                    if (!detail.getStockId().isEmpty()) {
                        loTrans.addDetail((String) poMaster.getValue("sIndstCdx"), detail.getStockId(), lsCondition, detail.getQuantity(), detail.getQuantity(), detail.Inventory().getSellingPrice().doubleValue());
                    }
                }

            } catch (GuanzonException ex) {
                poJSON = new JSONObject();

                poGRider.rollbackTrans();
                poJSON.put("result", "error");
                poJSON.put("message", ex.getMessage());
                return poJSON;

            }
        }

        loTrans.saveTransaction();

        //Update status for this transaction
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        if (check != null) {
            check.postAuth();
        }

        poGRider.commitTrans();
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "success");

        if (lbConfirm) {
            loJSON.put("message", "Transaction confirmed successfully.");
        } else {
            loJSON.put("message", "Transaction confirmation request submitted successfully.");
        }

        openTransaction(Master().getTransactionNo());
        return loJSON;
    }

    public JSONObject PostTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = StockRequestStatus.PROCESSED;
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already processed.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(StockRequestStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm);

        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }
        poGRider.beginTrans("UPDATE STATUS", "Post Transaction", SOURCE_CODE, Master().getTransactionNo());

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        poJSON = saveUpdates(StockRequestStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        openTransaction(Master().getTransactionNo());
        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction posted successfully.");
        } else {
            poJSON.put("message", "Transaction posting request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject CancelTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = StockRequestStatus.CANCELLED;
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already cancelled.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(StockRequestStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //change status
        poGRider.beginTrans("UPDATE STATUS", "Cancel Transaction", SOURCE_CODE, Master().getTransactionNo());

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
        poJSON = saveUpdates(StockRequestStatus.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }

        poGRider.commitTrans();

        openTransaction(Master().getTransactionNo());
        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction cancelled successfully.");
        } else {
            poJSON.put("message", "Transaction cancellation request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject VoidTransaction(String remarks) throws ParseException, SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        String lsStatus = StockRequestStatus.VOID;
        boolean lnCancel = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already voided.");
            return poJSON;
        }

        MatrixAuthChecker check = null;
        if (!pbWthParent) {
            //validator
            poJSON = isEntryOkay(lsStatus);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            //get the matrix return from isEntryOkey
            JSONArray loMatrix = (JSONArray) poJSON.get("matrix");

            if (!lsStatus.equals(StockRequestStatus.OPEN)) {
                //Check if there is a authorization request
                if (loMatrix != null) {
                    //initialized MatrixAuthChecker object
                    check = new MatrixAuthChecker(poGRider, SOURCE_CODE, Master().getTransactionNo());
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
                                poJSON = check.authTrans((String) loJson.get("sAuthType"), poGRider.getUserID());
                                //user is not authorized
                                if (!"success".equalsIgnoreCase((String) poJSON.get("result"))) {
                                    return poJSON;
                                }
                            }
                        }

                        //check if authorization request is already approved by all authorizing personnel
                        if (!check.isAuthOkay()) {
                            //check  the user level again then if he/she allow to approve
                            poGRider.beginTrans("UPDATE STATUS", "Cancel Transaction", SOURCE_CODE, Master().getTransactionNo());

                            lsStatus = Character.toString((char) (64 + Integer.parseInt(lsStatus)));
                            poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lnCancel, true);
                            if (!"success".equals((String) poJSON.get("result"))) {
                                poGRider.rollbackTrans();
                                return poJSON;
                            }

                            poGRider.commitTrans();

                            poJSON.put("result", "matrix");
                            return poJSON;
                        }
                    }

                }
            }
        }

        //check  the user level again then if he/she allow to approve
        poGRider.beginTrans(
                "UPDATE STATUS", "Cancel Transaction", SOURCE_CODE, Master().getTransactionNo());
        try {
            //kalyptus-2025.10.08 02:52pm
            //save to inventory ledger
            if (StockRequestStatus.VOID.equalsIgnoreCase((String) poMaster.getValue("cTranStat"))) {
                InventoryTransaction loTrans = new InventoryTransaction(poGRider);
                loTrans.StockRequest((String) poMaster.getValue("sTransNox"), (Date) poMaster.getValue("dTransact"), true);

                for (Model loDetail : paDetail) {
                    Model_Inv_Stock_Request_Detail detail = (Model_Inv_Stock_Request_Detail) loDetail;
                    if (!detail.getStockId().isEmpty()) {
                        loTrans.addDetail((String) detail.getValue("sIndstCdx"), detail.getStockId(), "0", 0, detail.getQuantity(), detail.Inventory().getCost().doubleValue());
                    }
                }
                loTrans.saveTransaction();
            }

            poJSON = saveUpdates(StockRequestStatus.CONFIRMED);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lnCancel, true);
            if (!"success".equals((String) poJSON.get("result"))) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            if (check != null) {
                check.postAuth();
            }

            poGRider.commitTrans();
            JSONObject loJSON = new JSONObject();
            loJSON.put("result", "success");

            if (lnCancel) {
                loJSON.put("message", "Transaction voided successfully.");
            } else {
                loJSON.put("message", "Transaction void request submitted successfully.");
            }

            openTransaction(Master().getTransactionNo());

            return loJSON;
        } catch (GuanzonException | SQLException | CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poGRider.rollbackTrans();
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));

            return poJSON;
        }

    }

    public JSONObject AddDetail() throws CloneNotSupportedException {
        if (Detail(getDetailCount() - 1).getStockId().isEmpty()) {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "Last row has empty item.");
            return poJSON;
        }

        return addDetail();
    }

    /*Search Master References*/
    public JSONObject SearchBranch(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setBranchCode(object.getModel().getBranchCode());
        }

        return poJSON;
    }

    public JSONObject SearchIndustry(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Industry object = new ParamControllers(poGRider, logwrapr).Industry();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setIndustryId(object.getModel().getIndustryId());
        }

        return poJSON;
    }

    public JSONObject SearchCategory(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Category object = new ParamControllers(poGRider, logwrapr).Category();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setCategoryId(object.getModel().getCategoryId());

        }

        return poJSON;
    }

    public JSONObject SearchBrand(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Brand brand = new ParamControllers(poGRider, logwrapr).Brand();
        brand.getModel().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = brand.searchRecord(value, byCode, psIndustryCode);

        if ("success".equals((String) poJSON.get("result"))) {
            poJSON.put("brandDesc", brand.getModel().getDescription());
            poJSON.put("brandID", brand.getModel().getBrandId());
        }
        return poJSON;
    }

    /*End - Search Master References*/

 /*Search Detail References*/
    public JSONObject SearchModel(String value, boolean byCode, String brandId, int row) throws SQLException, GuanzonException, NullPointerException {

        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = object.searchRecord(value, byCode, null, brandId, psIndustryCode, psCategorCD);

        if ("success".equals((String) poJSON.get("result"))) {
            for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                if (lnRow != row) {
                    if ((Master().getSourceNo().equals("") || Master().getSourceNo() == null)
                            && (Detail(lnRow).getStockId().equals(object.getModel().getStockId()))) {

                        double existingQty = Detail(lnRow).getQuantity();
                        double newQty = existingQty + 1;
                        Detail(lnRow).setQuantity(newQty);

                        Detail(row).setStockId("");
                        poJSON.put("result", "merged");
                        poJSON.put("message", "Barcode already exists at row " + (lnRow + 1) + ", quantity updated.");
                        return poJSON;
                    }
                }
            }

            Detail(row).setStockId(object.getModel().getStockId());

        }

        return poJSON;
    }

    public JSONObject SearchBarcode(String value, boolean byCode, int row, String brandId)
            throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException, NullPointerException {

        String lsIndustry = Master().getIndustryId().isEmpty() ? psIndustryCode : Master().getIndustryId();
        InvMaster object = new InvControllers(poGRider, logwrapr).InventoryMaster();
        object.setRecordStatus(RecordStatus.ACTIVE);
        object.setCategory(psCategorCD);
        object.setIndustryID(lsIndustry);
        object.setBranchCode(poGRider.getBranchCode());

        poJSON = object.searchRecord(value, byCode, null,
                psIndustryCode, brandId);

        if ("success".equals((String) poJSON.get("result"))) {
            String stockId = object.getModel().getStockId();

            for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                if (lnRow != row) {
                    if ((Master().getSourceNo() == null || Master().getSourceNo().isEmpty())
                            && stockId.equals(Detail(lnRow).getStockId())) {

                        double existingQty = Detail(lnRow).getQuantity();
                        double newQty = existingQty + 1;
                        Detail(lnRow).setQuantity(newQty);

                        Detail(row).setStockId("");

                        poJSON.put("result", "merged");
                        poJSON.put("message", "Barcode already exists at row " + (lnRow + 1) + ", quantity updated.");
                        return poJSON;
                    }
                }
            }

            Detail(row).setStockId(stockId);

            poJSON.put("result", "success");
            poJSON.put("message", "Barcode added successfully.");
        }

        return poJSON;
    }

    public JSONObject SearchBarcodeGeneral(String value, boolean byCode, int row, String brandId)
            throws ExceptionInInitializerError, SQLException, GuanzonException,
            CloneNotSupportedException, NullPointerException {

        String lsIndustry = Master().getIndustryId().isEmpty() ? psIndustryCode : Master().getIndustryId();
        InvMaster object = new InvControllers(poGRider, logwrapr).InventoryMaster();
        object.setRecordStatus(RecordStatus.ACTIVE);
        object.setCategory(psCategorCD);
        object.setIndustryID(lsIndustry);
        object.setBranchCode(poGRider.getBranchCode());

        poJSON = object.searchRecord(value, byCode, null,
                psIndustryCode, brandId);

        if ("success".equals((String) poJSON.get("result"))) {
            String stockId = object.getModel().getStockId();

            for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                if (lnRow != row) {
                    if ((Master().getSourceNo() == null || Master().getSourceNo().isEmpty())
                            && stockId.equals(Detail(lnRow).getStockId())) {

                        double existingQty = Detail(lnRow).getQuantity();
                        double newQty = existingQty + 1;
                        Detail(lnRow).setQuantity(newQty);

                        Detail(row).setStockId("");
                        poJSON.put("result", "merged");
                        poJSON.put("message", "Barcode already exists at row " + (lnRow + 1) + ", quantity updated.");
                        poJSON.put("updatedRow", lnRow);
                        return poJSON;
                    }
                }
            }

            Detail(row).setStockId(stockId);
            Detail(row).setCategoryCode(object.getModel().Inventory().getCategoryFirstLevelId());

            poJSON.put("result", "success");
            poJSON.put("message", "Barcode added successfully.");
        }

        return poJSON;
    }

    public JSONObject SearchBarcodeDescription(String value, boolean byCode, int row, String brandId)
            throws ExceptionInInitializerError, SQLException, GuanzonException,
            CloneNotSupportedException, NullPointerException {

        String lsIndustry = Master().getIndustryId().isEmpty() ? psIndustryCode : Master().getIndustryId();
        InvMaster object = new InvControllers(poGRider, logwrapr).InventoryMaster();
        object.setRecordStatus(RecordStatus.ACTIVE);
        object.setCategory(psCategorCD);
        object.setIndustryID(lsIndustry);
        object.setBranchCode(poGRider.getBranchCode());

        poJSON = object.searchRecord(value, byCode, null,
                psIndustryCode, brandId);

        if ("success".equals((String) poJSON.get("result"))) {
            String stockId = object.getModel().getStockId();

            for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                if (lnRow != row) {
                    if ((Master().getSourceNo() == null || Master().getSourceNo().isEmpty())
                            && stockId.equals(Detail(lnRow).getStockId())) {

                        double existingQty = Detail(lnRow).getQuantity();
                        double newQty = existingQty + 1;
                        Detail(lnRow).setQuantity(newQty);

                        Detail(row).setStockId("");
                        poJSON.put("result", "merged");
                        poJSON.put("message", "Description already exists at row " + (lnRow + 1) + ", quantity updated.");
                        poJSON.put("updatedRow", lnRow);
                        return poJSON;
                    }
                }
            }

            Detail(row).setStockId(stockId);

            poJSON.put("result", "success");
            poJSON.put("message", "Description added successfully.");
        }

        return poJSON;
    }

    public JSONObject SearchBarcodeDescriptionGeneral(String value, boolean byCode, int row, String brandId) throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException,
            NullPointerException {
        String lsIndustry = Master().getIndustryId().isEmpty() ? psIndustryCode : Master().getIndustryId();
        InvMaster object = new InvControllers(poGRider, logwrapr).InventoryMaster();
        object.setRecordStatus(RecordStatus.ACTIVE);
        object.setCategory(psCategorCD);
        object.setIndustryID(lsIndustry);
        object.setBranchCode(poGRider.getBranchCode());

        poJSON = object.searchRecord(value, byCode, null,
                psIndustryCode, brandId);

        if ("success".equals((String) poJSON.get("result"))) {
            String stockId = object.getModel().getStockId();

            for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                if (lnRow != row) {
                    if ((Master().getSourceNo() == null || Master().getSourceNo().isEmpty())
                            && stockId.equals(Detail(lnRow).getStockId())) {

                        double existingQty = Detail(lnRow).getQuantity();
                        double newQty = existingQty + 1;
                        Detail(lnRow).setQuantity(newQty);

                        Detail(row).setStockId("");
                        poJSON.put("result", "merged");
                        poJSON.put("message", "Description already exists at row " + (lnRow + 1) + ", quantity updated.");
                        poJSON.put("updatedRow", lnRow);
                        return poJSON;
                    }
                }
            }

            Detail(row).setStockId(stockId);

            poJSON.put("result", "success");
            poJSON.put("message", "Description added successfully.");
        }

        return poJSON;
    }

    /*End - Search Detail References*/
    @Override
    public String getSourceCode() {
        return SOURCE_CODE;
    }

    @Override
    public Model_Inv_Stock_Request_Master Master() {
        return (Model_Inv_Stock_Request_Master) poMaster;
    }

    @Override
    public Model_Inv_Stock_Request_Detail Detail(int row) {
        return (Model_Inv_Stock_Request_Detail) paDetail.get(row);
    }

    @Override
    public JSONObject willSave() throws SQLException, GuanzonException {
        /*Put system validations and other assignments here*/
        poJSON = new JSONObject();

        poJSON = isEntryOkay(InventoryStockIssuanceStatus.OPEN);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        //remove items with no stockid or quantity order       
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next(); // Store the item before checking conditions

            if ("".equals((String) item.getValue("sStockIDx"))
                    || ((Number) item.getValue("nQuantity")).doubleValue() <= 0) {
                detail.remove(); // Correctly remove the item
            }
        }

        //assign other info on detail
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNumber(lnCtr + 1);
        }

        if (getDetailCount() == 1) {
            //do not allow a single item detail with no quantity order
            if (Detail(0).getQuantity() == 0) {
                poJSON.put("result", "error");
                poJSON.put("message", "Your order has zero quantity.");
                return poJSON;
            }
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public JSONObject saveOthers() {
        /*Only modify this if there are other tables to modify except the master and detail tables*/
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public void saveComplete() {
        /*This procedure was called when saving was complete*/
        System.out.println("Transaction saved successfully.");
    }

    @Override
    public JSONObject initFields() {
        /*Put initial model values here*/
        poJSON = new JSONObject();

        poJSON.put("result", "success");
        return poJSON;
    }

    @SuppressWarnings("unchecked")
    public List<Model_Inv_Stock_Request_Detail> getDetailList() {
        System.out.print("GET DETAIL LIST");
        System.out.println("paDetail = " + paDetail);
        System.out.println("paDetail class = " + (paDetail != null ? paDetail.getClass() : "null"));
        System.out.println("paDetail size = " + (paDetail != null ? paDetail.size() : "null"));

        return (List<Model_Inv_Stock_Request_Detail>) (List<?>) paDetail;
    }

    @Override
    protected JSONObject isEntryOkay(String status) {
        System.out.println("IS ENTRY OK?");
        GValidator loValidator = StockRequestValidatorFactory.make(psIndustryCode);

        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(poMaster);
        ArrayList laDetailList = new ArrayList<>(getDetailList());
        loValidator.setDetail(laDetailList);

        poJSON = loValidator.validate();

        if (poJSON.containsKey("isRequiredApproval") && Boolean.TRUE.equals(poJSON.get("isRequiredApproval"))) {
            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
                poJSON = ShowDialogFX.getUserApproval(poGRider);

                setApproving((String) poJSON.get("sUserIDxx"));
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

    public JSONObject isDetailHasZeroQty() {
        poJSON = new JSONObject();
        int zeroQtyRow = -1;
        boolean hasNonZeroQty = false;
        boolean hasZeroQty = false;
        int lastRow = getDetailCount() - 1;

        for (int lnRow = 0; lnRow <= lastRow; lnRow++) {
            double quantity = Detail(lnRow).getQuantity();
            String stockID = (String) Detail(lnRow).getValue("sStockIDx");

            if (!stockID.isEmpty()) {
                if (quantity == 0.00) {
                    hasZeroQty = true;
                    if (zeroQtyRow == -1) {
                        zeroQtyRow = lnRow;
                    }
                } else {
                    hasNonZeroQty = true;
                }
            }
        }

        if (!hasNonZeroQty && hasZeroQty) {
            poJSON.put("result", "error");
            poJSON.put("message", "All items have zero quantity. Please enter a valid quantity.");
            poJSON.put("tableRow", zeroQtyRow);
            poJSON.put("warning", "true");
        } else if (hasZeroQty) {
            poJSON.put("result", "error");
            poJSON.put("message", "Some items have zero quantity. Please review.");
            poJSON.put("tableRow", zeroQtyRow);
            poJSON.put("warning", "false");
        } else {
            poJSON.put("result", "success");
            poJSON.put("message", "All items have valid quantities.");
            poJSON.put("tableRow", lastRow);
        }

        return poJSON;
    }

    @Override
    public void initSQL() {
        SQL_BROWSE = "SELECT "
                + "  a.sTransNox, "
                + "  a.dTransact, "
                + "  a.sIndstCdx, "
                + "  a.sReferNox "
                + "FROM Inv_Stock_Request_Master a";
    }

    public JSONObject searchTransaction(boolean searchRef) throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsTransStat = "";

        if (psTranStat != null) {
            if (psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                    lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
                }
                lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
            } else {
                lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
            }
        }

        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE,
                " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryCode)
                + " AND a.sCompnyID = " + SQLUtil.toSQL(psCompanyID)
                + " AND a.sCategrCd = " + SQLUtil.toSQL(psCategorCD)
                + " AND a.sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode()));

        if (searchRef) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sReferNox IS NOT NULL AND a.sReferNox <> ''");
        }

        if (psTranStat != null && !"".equals(psTranStat)) {
            lsSQL = lsSQL + lsTransStat;
        }

        System.out.println("Executing SQL: " + lsSQL);

        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction Date»Transaction No»Reference No",
                "dTransact»sTransNox»sReferNox",
                "a.dTransact»a.sTransNox»a.sReferNox",
                1);

        if (poJSON != null) {
            return OpenTransaction((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }

    public JSONObject searchTransaction() throws CloneNotSupportedException, SQLException, GuanzonException {

        poJSON = new JSONObject();
        String lsTransStat = "";

        if (psTranStat != null) {
            if (psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                    lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
                }

                lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
            } else {
                lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
            }
        }

        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryCode)
                + " AND a.sCompnyID = " + SQLUtil.toSQL(psCompanyID)
                + " AND a.sCategrCd = " + SQLUtil.toSQL(psCategorCD)
                + " AND a.sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode()));
//                + " AND a.sSupplier LIKE " + SQLUtil.toSQL("%" + Master().getSupplierId()));
        if (psTranStat != null && !"".equals(psTranStat)) {
            lsSQL = lsSQL + lsTransStat;
        }

        System.out.println("Executing SQL: " + lsSQL);

        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                "",
                "Transaction Date»Transaction No»Reference No",
                "dTransact»sTransNox»sReferNox",
                "a.dTransact»a.sTransNox»a.sReferNox",
                1);

        if (poJSON != null) {
            return OpenTransaction((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }

    public JSONObject getTableListInformation(String fsTransNo, String fsReferID) throws SQLException, GuanzonException {
        JSONObject loJSON = new JSONObject();
        String lsTransStat = "";
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
            lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
        } else {
            lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
        }

        String lsSQL = "SELECT "
                + "  a.sTransNox, "
                + "  a.dTransact, "
                + "  a.sIndstCdx, "
                + "  a.sReferNox "
                + "FROM Inv_Stock_Request_Master a";
        String lsFilterCondition = String.join(" AND ",
                " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryCode),
                " a.sCompnyID = " + SQLUtil.toSQL(psCompanyID),
                " a.sCategrCd = " + SQLUtil.toSQL(psCategorCD),
                " a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsReferID));
        lsSQL = MiscUtil.addCondition(lsSQL, lsFilterCondition);
        if (!psTranStat.isEmpty()) {
            lsSQL = lsSQL + lsTransStat;
        }
        if (!poGRider.isMainOffice() || !poGRider.isWarehouse()) {
            lsSQL = lsSQL + " AND a.sBranchCd LIKE " + SQLUtil.toSQL(poGRider.getBranchCode());
        }
        lsSQL = lsSQL + " GROUP BY  a.sTransNox"
                + " ORDER BY dTransact ASC";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        int lnCtr = 0;
        if (MiscUtil.RecordCount(loRS) >= 0) {
            paInvMaster = new ArrayList<>();
            while (loRS.next()) {
                // Print the result set
                System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                System.out.println("dTransact: " + loRS.getDate("dTransact"));
                System.out.println("sReferNox" + loRS.getString("sReferNox"));
                System.out.println("------------------------------------------------------------------------------");

                paInvMaster.add(INVMasterList());
                paInvMaster.get(paInvMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
                lnCtr++;
            }
            System.out.println("Records found: " + lnCtr);
            loJSON.put("result", "success");
            loJSON.put("message", "Record loaded successfully.");
        } else {
            paInvMaster = new ArrayList<>();
            paInvMaster.add(INVMasterList());
            loJSON.put("result", "error");
            loJSON.put("continue", true);
            loJSON.put("message", "No record found .");
        }
        MiscUtil.close(loRS);
        return loJSON;
    }

    public JSONObject getROQItems() {
        poJSON = new JSONObject();

        try {
            poStockROQ = new ROQ(poGRider, poGRider.getBranchCode(), Master().getCategoryId());
            JSONObject loResult = poStockROQ.LoadRecommendedOrder();

            if (!"success".equalsIgnoreCase((String) loResult.get("result"))) {
                poJSON.put("result", "error");
                poJSON.put("message", "Unable to load ROQ: " + loResult.get("message"));
                return poJSON;
            }

            CachedRowSet loROQ = poStockROQ.getRecommendations();
            loROQ.last();
            int totalROQ = loROQ.getRow();
            loROQ.beforeFirst();
            int index = 0;
            while (loROQ.next()) {
                String lsStockID = loROQ.getString("sStockIDx");
                double lnROQ = loROQ.getDouble("nRecOrder");

                int row = getDetailCount() - 1;
                Detail(row).setStockId(lsStockID);
                Detail(row).setRecommendedOrder(lnROQ);

                index++;
                if (index < totalROQ) {
                    AddDetail();
                }
            }
            System.out.println(totalROQ + "total roq");
            poJSON.put("result", "success");
            poJSON.put("message", "ROQ items added successfully.");

            return poJSON;

        } catch (Exception e) {
            poJSON.put("result", "error");
            poJSON.put("message", "Exception occurred: " + MiscUtil.getException(e));
            return poJSON;
        }
    }

    public void ShowStatusHistory() throws SQLException, GuanzonException, Exception {
        CachedRowSet crs = getStatusHistory();

        crs.beforeFirst();

        while (crs.next()) {
            switch (crs.getString("cRefrStat")) {
                case "":
                    crs.updateString("cRefrStat", "-");
                    break;
                case StockRequestStatus.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case StockRequestStatus.CONFIRMED:
                    crs.updateString("cRefrStat", "CONFIRMED");
                    break;
                case StockRequestStatus.PROCESSED:
                    crs.updateString("cRefrStat", "PROCESSED");
                    break;
                case StockRequestStatus.CANCELLED:
                    crs.updateString("cRefrStat", "CANCELLED");
                    break;
                case StockRequestStatus.VOID:
                    crs.updateString("cRefrStat", "VOID");
                    break;

                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);

                    switch (stat) {
                        case StockRequestStatus.OPEN:
                            crs.updateString("cRefrStat", "OPEN");
                            break;
                        case StockRequestStatus.CONFIRMED:
                            crs.updateString("cRefrStat", "CONFIRMED");
                            break;
                        case StockRequestStatus.PROCESSED:
                            crs.updateString("cRefrStat", "PROCESSED");
                            break;
                        case StockRequestStatus.CANCELLED:
                            crs.updateString("cRefrStat", "CANCELLED");
                            break;
                        case StockRequestStatus.VOID:
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
                + " FROM Inv_Stock_Request_Master a "
                + " LEFT JOIN xxxAuditLogMaster b ON"
                + " b.sSourceNo = a.sTransNox AND b.sEventNme LIKE 'ADD%NEW' AND b.sRemarksx = " + SQLUtil.toSQL(Master().getTable());
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox =  " + SQLUtil.toSQL(Master().getTransactionNo()));
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

    public JSONObject printRecord() throws SQLException, JRException, CloneNotSupportedException, GuanzonException {

        poJSON = new JSONObject();

        poJSON = isEntryOkay(StockRequestStatus.CONFIRMED);
        if ("error".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (Master().getTransactionStatus().equals(StockRequestStatus.OPEN)) {
            poJSON.put("result", "error");
            poJSON.put("message", "Please confirm the transaction first.");
            return poJSON;
        }

        if (Master().getTransactionStatus().equals(StockRequestStatus.CANCELLED)
                || Master().getTransactionStatus().equals(0)) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction is already cancelled.");
            return poJSON;
        }

        if (Master().getTransactionStatus().equals(StockRequestStatus.VOID)
                || Master().getTransactionStatus().equals(0)) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction is already void.");
            return poJSON;
        }

        ReportUtil poReportJasper = new ReportUtil(poGRider);

        if (Master().getTransactionNo() == null && Master().getTransactionNo().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "No record is Selected");
            return poJSON;

        }
        poJSON = OpenTransaction(Master().getTransactionNo());
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
        poReportJasper.addParameter("TransactionNo", Master().getTransactionNo());
        poReportJasper.addParameter("TransactionDate", SQLUtil.dateFormat(Master().getTransactionDate(), SQLUtil.FORMAT_LONG_DATE));
        poReportJasper.addParameter("Remarks", Master().getRemarks());
        poReportJasper.addParameter("DatePrinted", SQLUtil.dateFormat(poGRider.getServerDate(), SQLUtil.FORMAT_TIMESTAMP));
        if ("1".equals(Master().getPrintStatus())) {
            poReportJasper.addParameter("watermarkImagePath", poGRider.getReportPath() + "images\\reprint.png");
        } else {
            poReportJasper.addParameter("watermarkImagePath", poGRider.getReportPath() + "images\\none.png");
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
                return "InventoryStockRequestdMP";
            case "0000002"://APPLIANCES
                return "InventoryStockRequestAppliance";
            case "0000003"://MC UNIT
                return "InventoryStockRequestMC";
            case "0000004"://MC SPAREPARTS
                return "InventoryStockRequestMCSP";
            case "0000005"://CAR UNIT
                return "InventoryStockRequestCar";
            case "0000006"://CAR SPAREPARTS
                return "InventoryStockRequestCarSP";
            case "0000007"://GENERAL
                return "InventoryStockRequestGeneral";
            case "0000008"://LP - Food
                return "InventoryStockRequestLPFood";
            case "0000009"://Monarch - Food
                return "InventoryStockRequestMonarchFood";
            default:
                return "InventoryStockRequestGeneral";
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

        lsSQL = MiscUtil.addCondition(lsSQL, "InventoryStockRequestMaster.sTransNox = " + SQLUtil.toSQL(Master().getTransactionNo()));

        return lsSQL;
    }

    private JSONObject PrintTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        poJSON = OpenTransaction(Master().getTransactionNo());
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

        poGRider.beginTrans("UPDATE STATUS", "Process Transaction Print Tag", SOURCE_CODE, Master().getTransactionNo());

        String lsSQL = "UPDATE "
                + poMaster.getTable()
                + " SET   cPrintxxx = " + SQLUtil.toSQL(StockRequestStatus.CONFIRMED)
                + " WHERE sTransNox = " + SQLUtil.toSQL(Master().getTransactionNo());

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

    private boolean isJSONSuccess(JSONObject loJSON, String module, String fsModule) {
        String result = (String) loJSON.get("result");
        if ("error".equals(result)) {
            String message = (String) loJSON.get("message");
            Platform.runLater(() -> {
                if (message != null) {
                    ShowMessageFX.Warning(null, module, message);
                }
            });
            return false;
        }
        String message = (String) loJSON.get("message");

        Platform.runLater(() -> {
            if (message != null) {
                ShowMessageFX.Information(null, module, message);
            }
        });
        return true;

    }
}
