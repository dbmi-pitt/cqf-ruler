package org.opencds.cqf.dstu3.config;

import com.google.gson.JsonObject;
import java.util.List;

public class CdsConfiguration {

    private JsonObject config;
    private Boolean alertNonSerious;
    private Boolean showEvidenceSupport;
    private Boolean cacheForOrderSignFiltering;
    private Boolean filterOutRepeatedAlerts;

    public CdsConfiguration(){
	this.config = new JsonObject();
    }

    public CdsConfiguration(JsonObject config,
			    Boolean alertNonSerious,
			    Boolean showEvidenceSupport,
			    Boolean cacheForOrderSignFiltering,
			    Boolean filterOutRepeatedAlerts){
	this.config = config;
	this.alertNonSerious = alertNonSerious;
	this.showEvidenceSupport = showEvidenceSupport;
	this.cacheForOrderSignFiltering = cacheForOrderSignFiltering;
	this.filterOutRepeatedAlerts = filterOutRepeatedAlerts;
    }


    public JsonObject getConfig() {
        return config;
    }
    public void setConfig(JsonObject inConfig) {
        this.config = inConfig;
    }

    public Boolean getAlertNonSerious() {
        return alertNonSerious;
    }
    public void setAlertNonSerious(Boolean inAlertNonSerious) {
        this.alertNonSerious = inAlertNonSerious;
    }

    public Boolean getShowEvidenceSupport() {
        return showEvidenceSupport;
    }
    public void setShowEvidenceSupport(Boolean inShowEvidenceSupport) {
        this.showEvidenceSupport = inShowEvidenceSupport;
    }

    public Boolean getCacheForOrderSignFiltering() {
        return cacheForOrderSignFiltering;
    }
    public void setCacheForOrderSignFiltering(Boolean inCacheForOrderSignFiltering) {
        this.cacheForOrderSignFiltering = inCacheForOrderSignFiltering;
    }

    public Boolean getFilterOutRepeatedAlerts() {
        return filterOutRepeatedAlerts;
    }
    public void setFilterOutRepeatedAlerts(Boolean inFilterOutRepeatedAlerts) {
        this.filterOutRepeatedAlerts = inFilterOutRepeatedAlerts;
    }

    public String toString(){
	String s = "config: " + this.config.toString() +
	    "\nalertNonSerious: " + this.alertNonSerious +
	    "\nshowEvidenceSupport: " + this.showEvidenceSupport +
	    "\ncacheForOrderSignFiltering: " + this.cacheForOrderSignFiltering +
	    "\nfilterOutRepeatedAlerts: " + this.filterOutRepeatedAlerts;
	return s;
    }
}
