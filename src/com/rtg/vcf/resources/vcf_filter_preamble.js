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
    var sampleNames = listToArray(header.getSampleNames());
    var sampleLookup = {};
    sampleNames.forEach(function (name, index) {
        sampleLookup[name] = index;
    });

    /** List of format IDs */
    var formatIds = listToArray(header.getFormatLines()).map(function (field) {
        return field.getId()
    });
    /**
     * Return a function that will extract the specified format field from the sample column with the given index
     */
    function fieldGet(field, sampleIndex) {
        return function () {
            var fields = rec.getFormatAndSample().get(field);
            return fields == null ? "." : fields.get(sampleIndex);
        }
    }
    /**
     * Return a function that will set the specified format field on the sample column with the given index
     */
    function fieldSet(field, sampleIndex) {
        return function (value) {
            rec.setFormatAndSample(field, value, sampleIndex);
        }
    }

    /**
     * Returns an object corresponding to a particular sample.
     * The object will have properties defined which correspond to the format fields for that sample.
     * This object is not specific to the current record, it's properties will dynamically
     * fetch from the current record
     */
    function sample(name) {
        var sampleIndex = sampleLookup[name];
        var s = {};
        formatIds.forEach(function (fieldName) {
            Object.defineProperty(s, fieldName, {get: fieldGet(fieldName, sampleIndex), set: fieldSet(fieldName, sampleIndex)});
        });
        return s;
    }

    function stringGetFunction(field) {
        return function () {
            var index = sampleLookup[this];
            var fields = rec.getFormatAndSample().get(field);
            return fields == null ? "." : fields.get(index);
        }
    }
    
    function stringSetFunction(field) {
        return function (value) {
            var index = sampleLookup[this];
            rec.setFormatAndSample(field, value, index);
        }
    }

    /**
     * Add get/set to string prototype to allow treating them as sample references
     */
    var stringProps = {};
    formatIds.forEach(function (field) {
        stringProps[field] = {get: stringGetFunction(field), set: stringSetFunction(field)};
    });
    Object.defineProperties(String.prototype, stringProps);

    /**
     * Returns a function which will fetch the info field with the given name from the current record
     */
    function infoGet(field) {
        return function () {
            var infoList = listToArray(rec.getInfo().get(field));
            if (infoList == null) {
                return ".";
            }
            return infoList;
        }
    }
    /**
     * Returns a function which will fetch the info field with the given name from the current record
     */
    function infoSet(field) {
        return function (values) {
            var ArrayList = Java.type("java.util.ArrayList");
            var list = new ArrayList();
            if (Array.isArray(values)) {
                values.forEach(function (val) {
                    list.add(val);
                });
            } else {
                list.add(values);
            }
            rec.getInfo().put(field, list);
        }
    }

// Map declared info fields to the INFO object
    var infoIds = listToArray(header.getInfoLines()).map(function (info) {
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
        return rec.getOneBasedStart();
    }

    /**
     * Fetch the CHROM from the current record
     */
    function chrom() {
        return rec.getSequenceName();
    }

    /**
     * Fetch the REF from the current record
     */
    function ref() {
        return rec.getRefCall();
    }

    /**
     * Fetch the Alts as an array
     */
    function alt() {
        return listToArray(rec.getAltCalls());
    }

    /**
     * Fetch the QUAL from the current record
     */
    function qual() {
        return rec.getQuality();
    }

    /**
     * Fetch the FILTER from the current record
     */
    function filter() {
        return listToArray(rec.getFilters());
    }

    /**
     * Fetch the ID from the current record
     */
    function id() {
        return rec.getId();
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
    global.sample = sample;
    global.INFO = INFO;
    Object.defineProperties(global, props);
    // Declare globally accessible objects for the sample names. If the name is javascript safe then
    // that sample will be accessible by it's raw name without needing to explicitly call sample()
    sampleNames.forEach(function (name) {
        global[name] = sample(name);
    });
    global.samples = sampleNames;

})(this);
