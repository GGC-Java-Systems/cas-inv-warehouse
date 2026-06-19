package org.guanzon.cas.inv.warehouse.model;
import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.TransactionStatus;
import org.guanzon.cas.inv.model.Model_Inventory;
import org.guanzon.cas.inv.services.InvModels;
import org.json.simple.JSONObject;

/**
 *
 * @author maynevval 08-09-2025
 */
public class Model_Inventory_Adjustment extends Model {

    //reference objects
    private Model_Inventory poInventory;

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);
            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            poEntity.absolute(1);
            poEntity.updateObject("dTransact", poGRider.getServerDate());
            poEntity.updateNull("sDocNmbrx");

            poEntity.updateNull("sDocNmbrx");
            poEntity.updateNull("sDocNmbrx");
            poEntity.updateDouble("nQtyInxxx", 0.00d);
            poEntity.updateDouble("nQtyOutxx", 0.00d);
            poEntity.updateNull("sSourceNo");
            poEntity.updateNull("sSourceCd");
            poEntity.updateString("cTranStat", TransactionStatus.STATE_OPEN);
            poEntity.updateObject("dModified", poGRider.getServerDate());

            poInventory = new InvModels(poGRider).Inventory();
            ID = poEntity.getMetaData().getColumnLabel(1);
            ID2 = poEntity.getMetaData().getColumnLabel(4);
            ID3 = poEntity.getMetaData().getColumnLabel(8);
            ID4 = poEntity.getMetaData().getColumnLabel(9);

            //add model here
            pnEditMode = EditMode.UNKNOWN;

        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }
    //Getter & Setter 
    //sTransNox
    //dTransact*
    //sDocNmbrx*
    //sPartsIDx*
    //nQtyInxxx*
    //nQtyOutxx
    //sRemarksx
    //sSourceNo
    //sSourceCd
    //cTranStat

    //sTransNox
    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    //dTransact
    public JSONObject setTransactionDate(Date transactionDate) {
        return setValue("dTransact", transactionDate);
    }

    public Date getTransactionDate() {
        return (Date) getValue("dTransact");
    }

    //sPartsIDx
    public JSONObject setPartsID(String partsID) {
        return setValue("sPartsIDx", partsID);
    }

    public String getPartsID() {
        return (String) getValue("sPartsIDx");
    }

    //nQtyInxxx
    public JSONObject setQtyIn(Double qtyIn) {
        return setValue("nQtyInxxx", qtyIn);
    }

    public Double getQtyIn() {
        return Double.valueOf(getValue("nQtyInxxx").toString());
    }

    //nQtyOutxx
    public JSONObject setQtyOut(Double qtyOut) {
        return setValue("nQtyOutxx", qtyOut);
    }

    public Double getQtyOut() {
        return Double.valueOf(getValue("nQtyOutxx").toString());
    }

    //sRemarksx
    public JSONObject setRemarks(String remarks) {
        return setValue("sRemarksx", remarks);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }

    //sSourceNo
    public JSONObject setSourceNo(String sourceNo) {
        return setValue("sSourceNo", sourceNo);
    }

    public String getSourceNo() {
        return (String) getValue("sSourceNo");
    }

    //sSourceCd
    public JSONObject setSourceCode(String sourceCode) {
        return setValue("sSourceCd", sourceCode);
    }

    public String getSourceCode() {
        return (String) getValue("sSourceCd");
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
        return "";
    }

    @Override
    public JSONObject openRecord(String Id1) {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "error");
        loJSON.put("message", "This feature is not supported.");
        return loJSON;
    }

    @Override
    public JSONObject openRecord(String Id1, Object Id2) {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "error");
        loJSON.put("message", "This feature is not supported.");
        return loJSON;
    }

    @Override
    public JSONObject openRecord(String Id1, Object Id2, Object Id3) {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "error");
        loJSON.put("message", "This feature is not supported.");
        return loJSON;
    }

    @Override
    public JSONObject openRecord(String Id1, Object Id2, Object Id3, Object Id4, Object Id5) {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", "error");
        loJSON.put("message", "This feature is not supported.");
        return loJSON;
    }

    public Model_Inventory Inventory() throws SQLException, GuanzonException {
        if (!"".equals(getValue("sStockIDx"))) {
            if (this.poInventory.getEditMode() == 1 && this.poInventory
                    .getStockId().equals(getValue("sStockIDx"))) {
                return this.poInventory;
            }
            this.poJSON = this.poInventory.openRecord((String) getValue("sStockIDx"));
            if ("success".equals(this.poJSON.get("result"))) {
                return this.poInventory;
            }
            this.poInventory.initialize();
            return this.poInventory;
        }
        poInventory.initialize();
        return this.poInventory;
    }
}
