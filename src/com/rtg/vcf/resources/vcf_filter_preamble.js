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
            var infoList = listToArray(RTG_VCF_RECORD.getInfo().get(field));
            if (infoList == null) {
                return ".";
            }
            return infoList.join(",");
        }
    }
    /**
     * Returns a function which will fetch the info field with the given name from the current record
     */
    function infoSet(field) {
        return function (value) {
            if (value == null || value == "." || value == false) {
                RTG_VCF_RECORD.removeInfo(field);
            } else {
                var ArrayList = Java.type("java.util.ArrayList");
                var list = new ArrayList();
                if (value != true) {
                    var values = value.toString().split(',');
                    if (Array.isArray(values)) {
                        values.forEach(function (val) {
                            list.add(val.toString());
                        });
                    }
                }
                RTG_VCF_RECORD.getInfo().put(field, list);
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
    function qual() {
        return RTG_VCF_RECORD.getQuality();
    }

    /**
     * Fetch the FILTER from the current record
     */
    function filter() {
        return listToArray(RTG_VCF_RECORD.getFilters());
    }

    /**
     * Fetch the ID from the current record
     */
    function id() {
        return RTG_VCF_RECORD.getId();
    }

    var props = {
        CHROM: {get: chrom},
        POS: {get: pos},
        ID: {get: id},
        REF: {get: ref},
        ALT: {get: alt},
        QUAL: {get: qual},
        FILTER: {get: filter},
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

    global.ensureFormatHeader = function (format) {
        var formatField = new FormatField(format);
        RTG_VCF_HEADER.ensureContains(formatField);
        var id = formatField.getId();
        if (!(id in String.prototype)) {
            stringProps[id] = {get: stringGetFunction(id), set: stringSetFunction(id)};
            Object.defineProperties(String.prototype, stringProps);
        }
    };
    global.ensureInfoHeader = function (info) {
        var infoField = new InfoField(info);
        RTG_VCF_HEADER.ensureContains(infoField);
        var id = infoField.getId();
        if (!(id in global.INFO)) {
            Object.defineProperty(global.INFO, id, {get: infoGet(id), set: infoSet(id)});
        }
    };
})(this);
