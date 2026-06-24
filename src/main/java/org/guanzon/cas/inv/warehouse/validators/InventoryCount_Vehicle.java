package org.guanzon.cas.inv.warehouse.validators;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.inv.warehouse.model.Model_Inventory_Count_Detail;
import org.guanzon.cas.inv.warehouse.model.Model_Inventory_Count_Master;
import org.json.simple.JSONObject;
import org.guanzon.cas.inv.warehouse.status.InventoryCountStatus;

/**
 *
 * @author MNV t4
 */
public class InventoryCount_Vehicle implements GValidator {

    GRiderCAS poGRider;
    String psTranStat;
    JSONObject poJSON;

    Model_Inventory_Count_Master poMaster;
    ArrayList<Model_Inventory_Count_Detail> paDetail;

    @Override
    public void setApplicationDriver(Object applicationDriver) {
        poGRider = (GRiderCAS) applicationDriver;
    }

    @Override
    public void setTransactionStatus(String transactionStatus) {
        psTranStat = transactionStatus;
    }

    @Override
    public void setMaster(Object value) {
        poMaster = (Model_Inventory_Count_Master) value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setDetail(ArrayList<Object> value) {
        paDetail = (ArrayList<Model_Inventory_Count_Detail>) (ArrayList<?>) value;
    }

    @Override
    public void setOthers(ArrayList<Object> value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public JSONObject validate() {
        try {
            switch (psTranStat) {
                case InventoryCountStatus.OPEN:
                    return validateNew();
                case InventoryCountStatus.CONFIRMED:
                    return validateConfirmed();
                case InventoryCountStatus.POSTED:
                    return validatePosted();
                case InventoryCountStatus.CANCELLED:
                    return validateCancelled();
                case InventoryCountStatus.VERIFIED:
                    return validateVerify();
                default:
                    poJSON = new JSONObject();
                    poJSON.put("result", "error");
                    poJSON.put("message", "unsupported function");
            }
        } catch (SQLException | GuanzonException ex) {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", ex.getMessage());
        }

        return poJSON;
    }

    private JSONObject validateNew() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        boolean isRequiredApproval = false;

        if (poMaster.getTransactionDate() == null) {
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }

        //change transaction date 
        if (poMaster.getTransactionDate().after((Date) poGRider.getServerDate())
                && poMaster.getTransactionDate().before((Date) poGRider.getServerDate())) {
            poJSON.put("message", "Change of transaction date are not allowed.! Approval is Required");
            isRequiredApproval = true;
        }

//        if (poMaster.getIndustryId() == null) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "Industry is not set.");
//            return poJSON;
//        }
        if (poMaster.getCategoryId()
                == null || poMaster.getCategoryId().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "Category is not set.");
            return poJSON;
        }
        if (poMaster.getBranchCode() == null || poMaster.getBranchCode().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "Branch is not set.");
            return poJSON;
        }

        if (poMaster.getInventoryCounterID() == null || poMaster.getInventoryCounterID().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "Inventory Count Type is not set.");
            return poJSON;
        }

//        if (poMaster.InventoryCountType().isAllowBalanceForward()) {
//            if (poMaster.getCutOff() == null) {
//                poJSON.put("result", "error");
//                poJSON.put("message", "Inventory Count Type is balance forward Cut-off Date is required.");
//                return poJSON;
//            }
//        }

        if (poMaster.InventoryCountType().isAllowBalanceForward()) {
            if (poMaster.getCutOff() != null) {
                if (poMaster.getCutOff().before(poMaster.getTransactionDate())) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Cut-off Date must be equal to or later than the Transaction Date.");
                    return poJSON;
                }
            }
        }
        //if bypass the rule require approval 
        if (!poMaster.getIncluded().trim().isEmpty()) {
            if (poGRider.getUserLevel() <= UserRight.ENCODER) {
                isRequiredApproval = true;
            }
        }
        int lnDetailCount = 0;
        if (poMaster.getCounterNo() > 0) {
            if (poMaster.getCounterNo() == 1) {
                for (int lnCtr = 0; lnCtr < paDetail.size(); lnCtr++) {
                    if (paDetail.get(lnCtr).getStockId() != null
                            && !paDetail.get(lnCtr).getStockId().isEmpty()) {

                        lnDetailCount++;
                        if (paDetail.get(lnCtr).getActualCounter01() == null
                                || paDetail.get(lnCtr).getActualCounter01() < 0) {
                            poJSON.put("result", "error");
                            poJSON.put("message", "Quantity is not set. Row = " + (lnCtr + 1));
                            return poJSON;
                        }
                    }
                }
            } else if (poMaster.getCounterNo() == 2) {
                for (int lnCtr = 0; lnCtr < paDetail.size(); lnCtr++) {
                    if (paDetail.get(lnCtr).getStockId() != null
                            && !paDetail.get(lnCtr).getStockId().isEmpty()) {

                        lnDetailCount++;
                        if (paDetail.get(lnCtr).getActualCounter03() == null
                                || paDetail.get(lnCtr).getActualCounter03() < 0) {
                            poJSON.put("result", "error");
                            poJSON.put("message", "Quantity is not set. Row = " + (lnCtr + 1));
                            return poJSON;
                        }
                    }
                }
            } else if (poMaster.getCounterNo() == 3) {
                for (int lnCtr = 0; lnCtr < paDetail.size(); lnCtr++) {
                    if (paDetail.get(lnCtr).getStockId() != null
                            && !paDetail.get(lnCtr).getStockId().isEmpty()) {

                        lnDetailCount++;
                        if (paDetail.get(lnCtr).getActualCounter03() == null
                                || paDetail.get(lnCtr).getActualCounter03() < 0) {
                            poJSON.put("result", "error");
                            poJSON.put("message", "Quantity is not set. Row = " + (lnCtr + 1));
                            return poJSON;
                        }
                    }
                }
            }
        }

        poJSON.put("result", "success");
        poJSON.put("isRequiredApproval", isRequiredApproval);

        return poJSON;
    }

    private JSONObject validateConfirmed() throws SQLException {
        poJSON = new JSONObject();
        boolean isRequiredApproval = false;

        if (poMaster.getTransactionDate() == null) {
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }

        poJSON.put("result", "success");
        poJSON.put("isRequiredApproval", isRequiredApproval);

        return poJSON;
    }

    private JSONObject validatePosted() {
        poJSON = new JSONObject();
        boolean isRequiredApproval = false;
//        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
//            isRequiredApproval = true;
//        }

        poJSON.put("result", "success");
        poJSON.put("isRequiredApproval", isRequiredApproval);
        return poJSON;
    }

    private JSONObject validateCancelled() throws SQLException {
        boolean isRequiredApproval = false;
        poJSON = new JSONObject();

//        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
//            isRequiredApproval = true;
//        }
        poJSON.put("result", "success");
        poJSON.put("isRequiredApproval", isRequiredApproval);
        return poJSON;
    }

    private JSONObject validateVerify() throws SQLException {
        boolean isRequiredApproval = false;
        poJSON = new JSONObject();

        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            isRequiredApproval = true;
        }

        poJSON.put("result", "success");
//        poJSON.put("isRequiredApproval", isRequiredApproval);
        return poJSON;
    }

}
