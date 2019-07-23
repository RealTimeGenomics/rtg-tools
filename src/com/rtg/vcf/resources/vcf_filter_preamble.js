// Global variable to hold the current record
var rec;

var VcfUtils = Java.type("com.rtg.vcf.VcfUtils");
var FilterField = Java.type("com.rtg.vcf.header.FilterField");
var InfoField = Java.type("com.rtg.vcf.header.InfoField");
var FormatField = Java.type("com.rtg.vcf.header.FormatField");

(function (global) {
    function listToArray(list) {
        if (list == null) {
            return null;
        }
        var arr = [];
        for (var i = 0; i < list.size(); i++) {
            arr.push(list.get(i));
        }
        return arr;
    }
    
    // First we need to know the sample names
    var sampleNames = listToArray(RTG_VCF_HEADER.getSampleNames());
    var sampleLookup = {};
    sampleNames.forEach(function (name, index) {
        sampleLookup[name] = index;
    });

    /** List of format IDs */
    var formatIds = listToArray(RTG_VCF_HEADER.getFormatLines()).map(function (field) {
        return field.getId()
    });

    /**
     * String prototype function for fetching a format field from a sample
     * @param field format field
     * @returns {Function} that extracts a format field
     */
    function stringGetFunction(field) {
        return function () {
            var index = sampleLookup[this];
            var fields = RTG_VCF_RECORD.getFormatAndSample().get(field);
            return fields == null ? "." : fields.get(index);
        }
    }

    /**
     * String prototype function for setting a format field in a sample
     * @param field format field
     * @returns {Function} that sets a format field
     */
    function stringSetFunction(field) {
        return function (value) {
            var index = sampleLookup[this];
            RTG_VCF_RECORD.setFormatAndSample(field, value.toString(), index);
        }
    }

    /**
     * Add get/set to string prototype to allow treating them as sample references
     */
    var stringProps = {};
    formatIds.forEach(function (field) {
        stringProps[field] = {get: stringGetFunction(field), set: stringSetFunction(field)};
    });
    // Note that Object.defineProperty doesn't seem to work here... bug defineProperties does
    // No idea why.
    Object.defineProperties(String.prototype, stringProps);

    /**
     * Returns a function which will fetch the info field with the given name from the current record
     */
    function infoGet(field) {
        return function () {
            var value = RTG_VCF_RECORD.getInfo(field);
            if (value == null) {
                return ".";
            }
            return value;
        }
    }
    /**
     * Returns a function which will set the info field with the given name from the current record
     */
    function infoSet(field) {
        return function (value) {
            //if (value == null || value == "." || value == "" || value === false) {
            if (value == null || value === "." || value === "" || value === false) {
                RTG_VCF_RECORD.removeInfo(field);
            } else {
                var ArrayList = Java.type("java.util.ArrayList");
                var list = new ArrayList();
                if (value !== true) {
                    RTG_VCF_RECORD.setInfo(field, value.toString());
                } else {
                    RTG_VCF_RECORD.setInfo(field);
                }
            }
        }
    }

    // Map declared info fields to the INFO object
    var infoIds = listToArray(RTG_VCF_HEADER.getInfoLines()).map(function (info) {
        return info.getId()
    });
    var INFO = {};
    infoIds.forEach(function(id) {
        Object.defineProperty(INFO, id, {get: infoGet(id), set: infoSet(id)});
    });
    /**
     * Fetch the pos from the current record
     */
    function pos() {
        return RTG_VCF_RECORD.getOneBasedStart();
    }

    /**
     * Fetch the CHROM from the current record
     */
    function chrom() {
        return RTG_VCF_RECORD.getSequenceName();
    }

    /**
     * Fetch the ID from the current record.
     * This should really return an array, for consistency with FILTER
     */
    function getId() {
        return RTG_VCF_RECORD.getId();
    }

    /**
     * Set the ID for the current record, as string or array
     */
    function setId(value) {
        if (value == null || value === "." || value === "" || value === false) {
            RTG_VCF_RECORD.setId();
        } else {
            if (!Array.isArray(value)) {
                value = value.toString().split(';');
            }
            if (Array.isArray(value)) {
                RTG_VCF_RECORD.setId(value);
            }
        }
    }

    /**
     * Fetch the REF from the current record
     */
    function ref() {
        return RTG_VCF_RECORD.getRefCall();
    }

    /**
     * Fetch the Alts as an array
     */
    function alt() {
        return listToArray(RTG_VCF_RECORD.getAltCalls());
    }

    /**
     * Fetch the QUAL from the current record
     */
    function getQual() {
        return RTG_VCF_RECORD.getQuality();
    }

    /**
     * Set the QUAL for the current record
     */
    function setQual(val) {
        return RTG_VCF_RECORD.setQuality(val);
    }

    /**
     * Fetch the FILTER from the current record
     */
    function getFilter() {
        var filters = listToArray(RTG_VCF_RECORD.getFilters());
        filters.add = addFilter;
        return filters;
    }

    function addFilter(val) {
        RTG_VCF_RECORD.addFilter(val);
    }
    /**
     * Set the FILTER for the current record, as string or array
     */
    function setFilter(value) {
        if (value == null || value === "." || value === "" || value === false) {
            RTG_VCF_RECORD.getFilters().clear();
        } else {
            RTG_VCF_RECORD.getFilters().clear();
            if (!Array.isArray(value)) {
                value = value.toString().split(';');
            }
            if (Array.isArray(value)) {
                value.forEach(addFilter);
            }
        }
    }
    /**
     * Add a FILTER for the current record, as string or array
     */
    function addFilter(value) {
        if (value == null || value === "." || value === "" || value === false) {
            RTG_VCF_RECORD.getFilters().clear();
        } else {
            if (!Array.isArray(value)) {
                value = value.toString().split(';');
            }
            if (Array.isArray(value)) {
                value.forEach(function (val) {
                    RTG_VCF_RECORD.addFilter(val);
                });
            }
        }
        return getFilter();
    }

    var props = {
        CHROM: {get: chrom},
        POS: {get: pos},
        ID: {get: getId, set: setId},
        REF: {get: ref},
        ALT: {get: alt},
        QUAL: {get: getQual, set: setQual},
        FILTER: {get: getFilter, set: setFilter},
    };
    // Expose an API on the global scope
    global.has = function (thing) {
        return typeof thing != "undefined" && thing != ".";
    }
    global.INFO = INFO;
    Object.defineProperties(global, props);
    // Declare globally accessible objects for the sample names. If the name is javascript safe then
    // that sample will be accessible by it's raw name without needing to explicitly call sample()
    sampleNames.forEach(function (name) {
        global[name] = name;
    });
    global.SAMPLES = sampleNames;

    // Declaring/ensuring filter headers is good behaviour, it's not a requirement for filter value setting.
    global.ensureFilterHeader = function (filter) {
        var filterField = new FilterField(filter);
        RTG_VCF_HEADER.ensureContains(filterField);
    };
    global.ensureInfoHeader = function (info) {
        var infoField = new InfoField(info);
        RTG_VCF_HEADER.ensureContains(infoField);
        var id = infoField.getId();
        if (!(id in global.INFO)) {
            Object.defineProperty(global.INFO, id, {get: infoGet(id), set: infoSet(id)});
        }
    };
    global.ensureFormatHeader = function (format) {
        var formatField = new FormatField(format);
        RTG_VCF_HEADER.ensureContains(formatField);
        var id = formatField.getId();
        if (!(id in String.prototype)) {
            stringProps[id] = {get: stringGetFunction(id), set: stringSetFunction(id)};
            Object.defineProperties(String.prototype, stringProps);
        }
    };

    global.checkMinVersion = function (minVersion) {
        var cVersion = RTG_VERSION;
        var minparts = minVersion.split('.').map(function(v) { return parseInt(v); });
        var cparts = cVersion.split('-')[0].split('.').map(function(v) { return parseInt(v); });
        //print("");
        //print("Testing current version " + cVersion + " against required minimum " + minVersion + " : " + cparts + " " + minparts);
        var cmaj = cparts[0];
        var mmaj = minparts[0];
        if (cmaj > mmaj) {
            //print("major version OK");
            return;
        } else if (cmaj == mmaj) {
            var cmin = cparts[1];
            var mmin = minparts[1];
            if (cmin > mmin) {
                //print("major same, minor version OK");
                return;
            } else if (cmin == mmin) {
                if (minparts.length < 3 ) {
                    //print("major same, minor same, patch version irrelevant");
                    return;
                } else if ((cparts.length >= 3) && (cparts[2] >= minparts[2])) {
                    //print("patch version OK");
                    return;
                }
            }
        }
        throw "This script requires an RTG version of " + minVersion + " or higher!\n";
    }
})(this);
