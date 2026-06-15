package org.guanzon.cas.inv.warehouse.model;

import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.inv.model.Model_Inventory;
import org.guanzon.cas.inv.services.InvModels;
import org.guanzon.cas.parameter.model.Model_Bin;
import org.guanzon.cas.parameter.model.Model_Section;
import org.guanzon.cas.parameter.model.Model_Warehouse;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;

/**
 *
 * @author maynevval 06-09-2026
 */
public class Model_Inventory_Count_Detail extends Model {

    private Model_Inventory poInventory;
    private Model_Bin poBin;
    private Model_Section poSection;
    private Model_Warehouse poWarehouse;

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

            poEntity.updateObject("nEntryNox", 1);
            poEntity.updateNull("sStockIDx");
            poEntity.updateNull("sBinIDxxx");
            poEntity.updateNull("sWHouseID");
            poEntity.updateNull("sSectnIDx");
            poEntity.updateDouble("nQtyOnHnd", 0.00d);
            poEntity.updateDouble("nActCtr01", 0.00d);
            poEntity.updateDouble("nActCtr02", 0.00d);
            poEntity.updateDouble("nActCtr03", 0.00d);
            poEntity.updateObject("dModified", poGRider.getServerDate());
            ID = poEntity.getMetaData().getColumnLabel(1);
            ID2 = poEntity.getMetaData().getColumnLabel(2);

            poInventory = new InvModels(poGRider).Inventory();
            this.poBin = (new ParamModels(this.poGRider)).Bin();
            this.poSection = (new ParamModels(this.poGRider)).Section();
            this.poWarehouse = (new ParamModels(this.poGRider)).Warehouse();

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }
    //Getter & Setter 
    //sTransNox
    //nEntryNox*
    //sStockIDx*
    //sWHouseID*
    //sSectnIDx
    //sBinIDxxx
    //nQtyOnHnd
    //nActCtr01
    //nActCtr02
    //nActCtr03
    //sDifCause
    //sRemarksx

    //sTransNox
    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    //nEntryNox
    public JSONObject setEntryNo(int entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public int getEntryNo() {
        return (int) getValue("nEntryNox");
    }

    //sStockIDx
    public JSONObject setStockId(String stockid) {
        return setValue("sStockIDx", stockid);
    }

    public String getStockId() {
        return (String) getValue("sStockIDx");
    }

    //sWHouseID
    public JSONObject setWarehouseID(String warehouseID) {
        return setValue("sWHouseID", warehouseID);
    }

    public String getWarehouseID() {
        return (String) getValue("sWHouseID");
    }

    //sSectnIDx
    public JSONObject setSectionID(String sectionID) {
        return setValue("sSectnIDx", sectionID);
    }

    public String getSectionID() {
        return (String) getValue("sSectnIDx");
    }

    //sBinIDxxx
    public JSONObject setBinID(String binID) {
        return setValue("sBinIDxxx", binID);
    }

    public String getBinID() {
        return (String) getValue("sBinIDxxx");
    }

    //nQtyOnHnd
    public JSONObject setQuantityOnHand(Double quantityOnHand) {
        return setValue("nQtyOnHnd", quantityOnHand);
    }

    public Double getQuantityOnHand() {
        return Double.valueOf(getValue("nQtyOnHnd").toString());
    }

    //nActCtr01
    public JSONObject setActualCounter01(Double actualCounter01) {
        return setValue("nActCtr01", actualCounter01);
    }

    public Double getActualCounter01() {
        return Double.valueOf(getValue("nActCtr01").toString());
    }

    //nActCtr02
    public JSONObject setActualCounter02(Double actualCounter02) {
        return setValue("nActCtr02", actualCounter02);
    }

    public Double getActualCounter02() {
        return Double.valueOf(getValue("nActCtr02").toString());
    }

    //nActCtr03
    public JSONObject setActualCounter03(Double actualCounter03) {
        return setValue("nActCtr03", actualCounter03);
    }

    public Double getActualCounter03() {
        return Double.valueOf(getValue("nActCtr03").toString());
    }

    //sDifCause
    public JSONObject setDifCause(String difCause) {
        return setValue("sDifCause", difCause);
    }

    public String getDifCause() {
        return (String) getValue("sDifCause");
    }

    //sRemarksx
    public JSONObject setRemarks(String remarks) {
        return setValue("sRemarksx", remarks);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
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

    public Model_Bin Bin() throws SQLException, GuanzonException {
        if (!"".equals(getValue("sBinIDxxx"))) {
            if (this.poBin.getEditMode() == 1 && this.poBin
                    .getBinId().equals(getValue("sBinIDxxx"))) {
                return this.poBin;
            }
            this.poJSON = this.poBin.openRecord((String) getValue("sBinIDxxx"));
            if ("success".equals(this.poJSON.get("result"))) {
                return this.poBin;
            }
            this.poBin.initialize();
            return this.poBin;
        }
        poBin.initialize();
        return this.poBin;
    }

    public Model_Section Section() throws SQLException, GuanzonException {
        if (!"".equals(getValue("sSectnIDx"))) {
            if (this.poSection.getEditMode() == 1 && this.poSection
                    .getSectionId().equals(getValue("sSectnIDx"))) {
                return this.poSection;
            }
            this.poJSON = this.poSection.openRecord((String) getValue("sSectnIDx"));
            if ("success".equals(this.poJSON.get("result"))) {
                return this.poSection;
            }
            this.poSection.initialize();
            return this.poSection;
        }
        poSection.initialize();
        return this.poSection;
    }

    public Model_Warehouse Warehouse() throws SQLException, GuanzonException {
        if (!"".equals(getValue("sWHouseID"))) {
            if (this.poWarehouse.getEditMode() == 1 && this.poWarehouse
                    .getWarehouseId().equals(getValue("sWHouseID"))) {
                return this.poWarehouse;
            }
            this.poJSON = this.poWarehouse.openRecord((String) getValue("sWHouseID"));
            if ("success".equals(this.poJSON.get("result"))) {
                return this.poWarehouse;
            }
            this.poWarehouse.initialize();
            return this.poWarehouse;
        }
        poWarehouse.initialize();
        return this.poWarehouse;
    }

}
