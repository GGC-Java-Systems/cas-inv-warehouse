package org.guanzon.cas.inv.warehouse.model;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.cas.client.model.Model_Client_Master;
import org.guanzon.cas.client.services.ClientModels;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.model.Model_Category;
import org.guanzon.cas.parameter.model.Model_Company;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;
import org.guanzon.cas.inv.warehouse.status.InventoryCountStatus;
import org.guanzon.cas.parameter.model.Model_Inventory_Count_Type;

/**
 *
 * @author maynevval 06-09-2026
 */
public class Model_Inventory_Count_Master extends Model {

    //reference objects
    Model_Category poCategory;
    Model_Branch poBranch;
    Model_Industry poIndustry;
//    Model_Company poCompany;
    Model_Inventory_Count_Type poInventoryCountType;
    Model_Client_Master poRequestedBy;

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            poEntity.updateObject("dTransact", poGRider.getServerDate());
            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("nCounterx", 0);
            poEntity.updateNull("dCutOffxx");
            poEntity.updateNull("dRequestd");
            poEntity.updateNull("sRqstdByx");
            poEntity.updateString("cTranStat", InventoryCountStatus.OPEN);
            poEntity.updateObject("dModified", poGRider.getServerDate());

            this.poRequestedBy = (new ClientModels(this.poGRider)).ClientMaster();
            this.poBranch = (new ParamModels(this.poGRider)).Branch();
//            this.poCompany = (new ParamModels(this.poGRider)).Company();
            this.poIndustry = (new ParamModels(this.poGRider)).Industry();
            this.poCategory = (new ParamModels(this.poGRider)).Category();
            this.poInventoryCountType = (new ParamModels(this.poGRider)).InventoryCountType();

            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            poEntity.absolute(1);

            ID = poEntity.getMetaData().getColumnLabel(1);

            //add model here
            pnEditMode = EditMode.UNKNOWN;

        } catch (SQLException e) {
            if (logwrapr != null) {
                logwrapr.severe(e.getMessage());
            }
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }
    //Getter & Setter 
    //sTransNox
    //sIndstCdx
    //sBranchCd*
    //sCategrCd
    //dTransact*
    //sRemarksx
    //nEntryNox
    //sInvCtrID
    //dCutOffxx
    //sIncluded
    //nCounterx
    //sRqstdByx
    //dRequestd
    //cTranStat

    //sTransNox
    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    //sIndstCdx
    public JSONObject setIndustryId(String industryId) {
        return setValue("sIndstCdx", industryId);
    }

    public String getIndustryId() {
        return (String) getValue("sIndstCdx");
    }

    //sBranchCd
    public JSONObject setBranchCode(String branchCode) {
        return setValue("sBranchCd", branchCode);
    }

    public String getBranchCode() {
        return (String) getValue("sBranchCd");
    }

    //sCategrCd
    public JSONObject setCategoryId(String categoryId) {
        return setValue("sCategrCd", categoryId);
    }

    public String getCategoryId() {
        return (String) getValue("sCategrCd");
    }

    //dTransact
    public JSONObject setTransactionDate(Date transactionDate) {
        return setValue("dTransact", transactionDate);
    }

    public Date getTransactionDate() {
        return (Date) getValue("dTransact");
    }

    //sRemarksx
    public JSONObject setRemarks(String remarks) {
        return setValue("sRemarksx", remarks);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }

    //sRqstdByx
    public JSONObject setRequestedBy(String requestedBy) {
        return setValue("sRqstdByx", requestedBy);
    }

    public String getRequestedBy() {
        return (String) getValue("sRqstdByx");
    }

    //dRequestd
    public JSONObject setRequestedDate(LocalDateTime requestedDate) {
        return setValue("dRequestd", Timestamp.valueOf(requestedDate));
    }

    public Date getRequestedDate() {
        return (Date) getValue("dRequestd");
    }

    //dCutOffxx
    public JSONObject setCutOff(LocalDateTime cutOff) {
        return setValue("dCutOffxx", Timestamp.valueOf(cutOff));
    }

    public Date getCutOff() {
        return (Date) getValue("dCutOffxx");
    }

    //sInvCtrID
    public JSONObject setInventoryCounterID(String inventoryCounterID) {
        return setValue("sInvCtrID", inventoryCounterID);
    }

    public String getInventoryCounterID() {
        return (String) getValue("sInvCtrID");
    }

    //sIncluded
    public JSONObject setIncluded(String included) {
        return setValue("sIncluded", included);
    }

    public String getIncluded() {
        return (String) getValue("sIncluded");
    }

    //nEntryNox
    public JSONObject setEntryNo(int entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public int getEntryNo() {
        return (int) getValue("nEntryNox");
    }

    //nEntryNox
    public JSONObject setCounterNo(int counter) {
        return setValue("nCounterx", counter);
    }

    public int getCounterNo() {
        return (int) getValue("nCounterx");
    }

    //cTranStat
    public JSONObject setTransactionStatus(String transactionStatus) {
        return setValue("cTranStat", transactionStatus);
    }

    public String getTransactionStatus() {
        return (String) getValue("cTranStat");
    }

    //sModified
    public JSONObject setModifyingId(String modifyingId) {
        return setValue("sModified", modifyingId);
    }

    public String getModifyingId() {
        return (String) getValue("sModified");
    }

    //dModified
    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }

    @Override
    public String getNextCode() {
        return MiscUtil.getNextCode(this.getTable(), ID, true, poGRider.getGConnection().getConnection(), poGRider.getBranchCode());
    }

    public Model_Client_Master ClientRequestBy() throws SQLException, GuanzonException {
        if (!"".equals(getValue("sRqstdByx"))) {
            if (this.poRequestedBy.getEditMode() == 1 && this.poRequestedBy
                    .getClientId().equals(getValue("sRqstdByx"))) {
                return this.poRequestedBy;
            }
            this.poJSON = this.poRequestedBy.openRecord((String) getValue("sRqstdByx"));
            if ("success".equals(this.poJSON.get("result"))) {
                return this.poRequestedBy;
            }
            this.poRequestedBy.initialize();
            return this.poRequestedBy;
        }
        this.poRequestedBy.initialize();
        return this.poRequestedBy;
    }

    public Model_Inventory_Count_Type InventoryCountType() throws SQLException, GuanzonException {
        if (!"".equals(getValue("sInvCtrID"))) {
            if (this.poInventoryCountType.getEditMode() == 1 && this.poInventoryCountType
                    .getInventoryCountID().equals(getValue("sInvCtrID"))) {
                return this.poInventoryCountType;
            }
            this.poJSON = this.poInventoryCountType.openRecord((String) getValue("sInvCtrID"));
            if ("success".equals(this.poJSON.get("result"))) {
                return this.poInventoryCountType;
            }
            this.poInventoryCountType.initialize();
            return this.poInventoryCountType;
        }
        this.poInventoryCountType.initialize();
        return this.poInventoryCountType;
    }

    public Model_Category Category() throws SQLException, GuanzonException {
        if (!"".equals(getValue("sCategrCd"))) {
            if (this.poCategory.getEditMode() == 1 && this.poCategory
                    .getCategoryId().equals(getValue("sCategrCd"))) {
                return this.poCategory;
            }
            this.poJSON = this.poCategory.openRecord((String) getValue("sCategrCd"));
            if ("success".equals(this.poJSON.get("result"))) {
                return this.poCategory;
            }
            this.poCategory.initialize();
            return this.poCategory;
        }
        this.poCategory.initialize();
        return this.poCategory;
    }

    public Model_Branch Branch() throws SQLException, GuanzonException {
        if (!"".equals(getValue("sBranchCd"))) {
            if (this.poBranch.getEditMode() == 1 && this.poBranch
                    .getBranchCode().equals(getValue("sBranchCd"))) {
                return this.poBranch;
            }
            this.poJSON = this.poBranch.openRecord((String) getValue("sBranchCd"));
            if ("success".equals(this.poJSON.get("result"))) {
                return this.poBranch;
            }
            this.poBranch.initialize();
            return this.poBranch;
        }
        this.poBranch.initialize();
        return this.poBranch;
    }

//    public Model_Company Company() throws SQLException, GuanzonException {
//        if (!"".equals(getValue("sCompnyID"))) {
//            if (this.poCompany.getEditMode() == 1 && this.poCompany
//                    .getCompanyId().equals(getValue("sCompnyID"))) {
//                return this.poCompany;
//            }
//            this.poJSON = this.poCompany.openRecord((String) getValue("sCompnyID"));
//            if ("success".equals(this.poJSON.get("result"))) {
//                return this.poCompany;
//            }
//            this.poCompany.initialize();
//            return this.poCompany;
//        }
//        this.poCompany.initialize();
//        return this.poCompany;
//    }

    public Model_Industry Industry() throws SQLException, GuanzonException {
        if (!"".equals(getValue("sIndstCdx"))) {
            if (this.poIndustry.getEditMode() == 1 && this.poIndustry
                    .getIndustryId().equals(getValue("sIndstCdx"))) {
                return this.poIndustry;
            }
            this.poJSON = this.poIndustry.openRecord((String) getValue("sIndstCdx"));
            if ("success".equals(this.poJSON.get("result"))) {
                return this.poIndustry;
            }
            this.poIndustry.initialize();
            return this.poIndustry;
        }
        this.poIndustry.initialize();
        return this.poIndustry;
    }

}
