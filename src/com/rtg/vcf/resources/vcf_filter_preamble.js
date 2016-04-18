// Global variable to hold the current record
var rec;

// First we need to know the sample names
var sampleNames = header.getSampleNames();
var sampleLookup = {};

var i;
for (i = 0; i < sampleNames.size(); i++) {
  sampleLookup[sampleNames.get(i)] = i;
}
function listToArray(list) {
    var arr = [];
    for (i = 0; i < list.size(); i++) {
        arr.push(list.get(i));
    }
    return arr;
}

/**
 * Return a function that will extract the specified format field from the sample column with the given index
 */
var formatFields = header.getFormatLines();
function fieldFunction(field, sampleIndex) {
      return function () {
        return rec.getFormatAndSample().get(field).get(sampleIndex);
      }
}

/**
 * Returns an object corresponding to a particular sample.
 * The object will have properties defined which correspond to the format fields for that sample.
 * This object is not specific to the current record, it's properties will dynamically 
 * fetch from the current record
 */
var sample = function (name) {
    var sampleIndex = sampleLookup[name];
    var s = {};
    for (i = 0; i < formatFields.size(); i++) {
        var field = formatFields.get(i).getId();
        Object.defineProperty(s, field, {get: fieldFunction(field, sampleIndex)});
    }
    return s;
};

// Declare globally accessible objects for the sample names. If the name is javascript safe then
// that sample will be accessible by it's raw name without needing to explicitly call sample()
for (i = 0; i < sampleNames.size(); i++) {
  this[sampleNames.get(i)] = sample(sampleNames.get(i));
}

/**
 * Returns a function which will fetch the info field with the given name from the current record
 */
function infoFunction(field) {
  return function () {
    var infoList = rec.getInfo().get(field);
    if (infoList == null) {
      return null;
    }
    var info = [];
    for (i = 0; i < infoList.size(); i++) {
      info.push(infoList.get(i));
    }
    return info;
  }
}

// Map declared info fields to the INFO object
var infoFields = header.getInfoLines();
var INFO = {};
for (i = 0; i < infoFields.size(); i++) {
  var infoField = infoFields.get(i).getId();
  Object.defineProperty(INFO, infoField, {get: infoFunction(infoField)});
}
/**
 * Fetch the pos from the current record
 */
function POS() {
  return rec.getOneBasedStart();
}

/**
 * Fetch the CHROM from the current record
 */
function CHROM() {
  return rec.getSequenceName();
}

/**
 * Fetch the REF from the current record
 */
function REF() {
  return rec.getRefCall();
}
/**
 * Fetch the Alts as an array
 */
function ALTS() {
    return listToArray(rec.getAltCalls());
}

/**
 * Fetch the QUAL from the current record
 */
function QUAL() {
    return rec.getQuality();
}

/**
 * Fetch the FILTER from the current record
 */
function FILTER() {
    return listToArray(rec.getFilters());
}

/**
 * Fetch the ID from the current record
 */
function ID() {
    return rec.getId();
}
