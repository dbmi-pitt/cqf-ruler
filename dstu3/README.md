# PDDI CDS Implementation

The PDDI CDS implementation is based on CQF Ruler which is an implementation of FHIR's [Clinical Reasoning Module](
http://hl7.org/fhir/clinicalreasoning-module.html).

## How to build, run, load

Here are the steps for starting the CQF Ruler for the PDDI CDS use
case (e.g., after a fresh build and start):

1) be sure to use Java 8 

Example: $ export JAVA_HOME=/usr/lib/jvm/java-8-oracle/jre 

2) Start the server 

Example: $ mvn jetty:run -am --projects cqf-ruler-dstu3

3) Use the tag uploader to upload the resources for CDS and the two PDDI use cases

3.1) All of the resources are located in CQF Ruler at pddi-cds/examples/pddi-cds. If you just want to load the resources, configure the Makefile in tag-uploader/tag-uploader/ to match your system's file paths, install the tag-uploader node project using `node install`, and then use make to load the resources of interest. Note: resources should be loaded in the order of load-fhir-helpers, load-[warfarin-nsaids or digoxin-cyclosporine]-value-sets, then the CDS service of interest. 

3.2) Testing the CDS service from the HSPC sandbox

3.2.1) Log into https://sandbox.hspconsortium.org/

3.2.2) Go to Patients (left side of screen) and ensure that there is a patient with the  medication statement resources pasted below. Use the Patient Data Manager to edit a patient (e.g., Amy Shaw) if needed. Ensure that the RxNorm terminology is indicated (http://www.nlm.nih.gov/research/umls/rxnorm). Also, be sure that the effective date of the medication orders >= 1 day and <= 100 days from the current date:  

Digoxin:

```
{
  "resourceType": "MedicationStatement",
  "id": "16802",
  "meta": {
    "versionId": "2",
    "lastUpdated": "2018-10-02T14:20:35.000+00:00"
  },
  "status": "active",
  "medicationCodeableConcept": {
    "coding": [
      {
        "system": "http://www.nlm.nih.gov/research/umls/rxnorm",
        "code": "315819",
        "display": "Digoxin 0.125 MG"
      }
    ]
  },
  "effectiveDateTime": "2018-10-01",
  "dateAsserted": "2018-10-01",
  "subject": {
    "reference": "Patient/SMART-1032702"
  },
  "taken": "y",
  "isSelected": true,
  "context": {}
}
```

Warfarin:

```
{
  "resourceType": "MedicationStatement",
  "id": "16803",
  "meta": {
    "versionId": "2",
    "lastUpdated": "2018-10-02T14:22:55.000+00:00"
  },
  "status": "active",
  "medicationCodeableConcept": {
    "coding": [
      {
        "system": "http://www.nlm.nih.gov/research/umls/rxnorm",
        "code": "855290",
        "display": "Warfarin Sodium 1 MG Oral Tablet [Coumadin]"
      }
    ]
  },
  "effectiveDateTime": "2018-10-01",
  "dateAsserted": "2018-10-01",
  "subject": {
    "reference": "Patient/SMART-1032702"
  },
  "taken": "y",
  "isSelected": true,
  "context": {}
}
```

3.2.3) Start the CDS Sandbox from the HSPC Sandbox applications menu

3.2.4) Go to RxView and configure the CDS Service 

Example: http://dbmi-icode-01.dbmi.pitt.edu:8080/cds-services 

3.2.5) To test the warfarin-NSIADS rule, create an NSAID medication request e.g., Ibuprofen 100 MG Oral Tablet. To test the digoxin-cyclosporine rule add a cylosporine order e.g., Cyclosporine 50 MG Oral Capsule. 

3.3) Testing the CDS service using POST requests from a command line interface or an app like Postman

3.3.1) These are canned medication-request resources in this fork of the cqf-ruler that you can post to the indicated CDS service. Use HTTP POST with the body RAW application/json to send the JSON component of these text files to the server:

warfarin-NSAIDS: examples/pddi-cds/warfarin-nsaids-*/warfarin-nsaids-cds-request.json 

digoxin-cylosporine: examples/pddi-cds/digoxin-cyclosporine-*/digoxin-cyclosporine-cds-request.json
