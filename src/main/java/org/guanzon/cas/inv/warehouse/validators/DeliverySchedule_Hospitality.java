package org.guanzon.cas.inv.warehouse.validators;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.json.simple.JSONObject;
import org.guanzon.cas.inv.warehouse.status.DeliveryScheduleStatus;
import org.guanzon.cas.inv.warehouse.model.Model_Delivery_Schedule_Detail;
import org.guanzon.cas.inv.warehouse.model.Model_Delivery_Schedule_Master;

/**
 *
 * @author MNV t4
 */
public class DeliverySchedule_Hospitality implements GValidator {

    GRiderCAS poGRider;
    String psTranStat;
    JSONObject poJSON;

    Model_Delivery_Schedule_Master poMaster;
    ArrayList<Model_Delivery_Schedule_Detail> paDetail;

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
        poMaster = (Model_Delivery_Schedule_Master) value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setDetail(ArrayList<Object> value) {
        paDetail = (ArrayList<Model_Delivery_Schedule_Detail>) (ArrayList<?>) value;
    }

    @Override
    public void setOthers(ArrayList<Object> value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public JSONObject validate() {
        try {
            switch (psTranStat) {
                case DeliveryScheduleStatus.OPEN:
                    return validateNew();
                case DeliveryScheduleStatus.CONFIRMED:
                    return validateConfirmed();
                case DeliveryScheduleStatus.POSTED:
                    return validatePosted();
                case DeliveryScheduleStatus.CANCELLED:
                    return validateCancelled();
                case DeliveryScheduleStatus.VOID:
                    return validateVoid();
                default:
                    poJSON = new JSONObject();
                    poJSON.put("result", "error");
                    poJSON.put("message", "unsupported function");
            }
        } catch (SQLException ex) {
            Logger.getLogger(DeliverySchedule_Hospitality.class.getName()).log(Level.SEVERE, null, ex);
        }

        return poJSON;
    }

    private JSONObject validateNew() throws SQLException {
        poJSON = new JSONObject();
        boolean isRequiredApproval = false;

        if (poMaster.getTransactionDate() == null) {
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid Transaction Date.");
            return poJSON;
        }

//        //change transaction date 
//        if (poMaster.getTransactionDate().after((Date) poGRider.getServerDate())
//                && poMaster.getTransactionDate().before((Date) poGRider.getServerDate())) {
//            poJSON.put("message", "Change of transaction date are not allowed.! Approval is Required");
//            isRequiredApproval = true;
//        }

        //change schedule date 
        if (poMaster.getScheduleDate().after((Date) poGRider.getServerDate())
                && poMaster.getScheduleDate().before((Date) poGRider.getServerDate())) {
            poJSON.put("message", "Change of schedule date are not allowed.! Approval is Required");
            isRequiredApproval = true;
        }

//        if (poMaster.getIndustryId() == null) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "Industry is not set.");
//            return poJSON;
//        }
        if (poMaster.getCompanyID() == null || poMaster.getCompanyID().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "Company is not set.");
            return poJSON;
        }
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

        int lnDetailCount = 0;
        for (int lnCtr = 0; lnCtr < paDetail.size(); lnCtr++) {
            if (paDetail.get(lnCtr).getClusterID() != null
                    && !paDetail.get(lnCtr).getClusterID().isEmpty()) {
                lnDetailCount++;

                if (paDetail.get(lnCtr).getTruckSize() == null
                        || paDetail.get(lnCtr).getTruckSize().isEmpty()) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Truck Size is not set. Row = " + (lnCtr + 1));
                    return poJSON;
                }
            }
        }

        if (lnDetailCount <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "Detail is not set.");
            return poJSON;
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

        if (poMaster.getIndustryId() == null) {
            poJSON.put("result", "error");
            poJSON.put("message", "Industry is not set.");
            return poJSON;
        }
        if (poMaster.getCompanyID() == null || poMaster.getCompanyID().isEmpty()) {
            poJSON.put("result", "error");
            poJSON.put("message", "Company is not set.");
            return poJSON;
        }

        int lnDetailCount = 0;
        for (int lnCtr = 0; lnCtr < paDetail.size(); lnCtr++) {
            if (paDetail.get(lnCtr).getClusterID() != null
                    && !paDetail.get(lnCtr).getClusterID().isEmpty()) {
                lnDetailCount++;
            }
        }

        if (lnDetailCount <= 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "Detail is not set.");
            return poJSON;
        }

        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            isRequiredApproval = true;
        }
        poJSON.put("result", "success");
        poJSON.put("isRequiredApproval", isRequiredApproval);

        return poJSON;
    }

    private JSONObject validatePosted() {
        poJSON = new JSONObject();
        boolean isRequiredApproval = false;
        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            isRequiredApproval = true;
        }
        poJSON.put("result", "success");
        poJSON.put("isRequiredApproval", isRequiredApproval);
        return poJSON;
    }

    private JSONObject validateCancelled() throws SQLException {
        boolean isRequiredApproval = false;
        poJSON = new JSONObject();

        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            isRequiredApproval = true;
        }
        poJSON.put("result", "success");
        poJSON.put("isRequiredApproval", isRequiredApproval);
        return poJSON;
    }

    private JSONObject validateVoid() throws SQLException {
//        boolean isRequiredApproval = false;
        poJSON = new JSONObject();
//
//        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
//            isRequiredApproval = true;
//        }
        poJSON.put("result", "success");
//        poJSON.put("isRequiredApproval", isRequiredApproval);
        return poJSON;
    }

}
