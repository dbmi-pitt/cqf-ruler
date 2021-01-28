library "Citalopram-QT-Prolonging-Agents" version '2.0.0'
using FHIR version '3.0.0'
include "FHIRHelpers" version '3.0.0' called FHIRHelpers 
valueset "Citaloprams VS": 'https://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1213.3'
valueset "QT Prolonging Agents VS": 'https://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1213.4'
context Patient

define "Citaloprams Med Statement":
  exists([MedicationStatement: "Citaloprams VS"])

define "Citaloprams Med Order":
  exists([MedicationRequest: "Citaloprams VS"])

define "QT Prolonging Agents Med Statement":
  exists([MedicationStatement: "QT Prolonging Agents VS"])

define "QT Prolonging Agents Med Order":
  exists([MedicationRequest: "QT Prolonging Agents VS"])

define "High Dosage Citaloprams Med Statement":
  if System.Quantity { value: (([MedicationStatement: "Citaloprams VS"]).dosage[0].dose as Quantity).value * ToDecimal(([MedicationStatement: "Citaloprams VS"]).dosage[0].timing.repeat.frequency), unit: 'mg/g' } >= 60 'mg/g' then true else false


define "High Dosage Citaloprams Med Order":
  if System.Quantity { value: (([MedicationRequest: "Citaloprams VS"]).dosageInstruction[0].dose as Quantity).value * ToDecimal(([MedicationRequest: "Citaloprams VS"]).dosageInstruction[0].timing.repeat.frequency), unit: 'mg/g' } >= 60 'mg/g' then true else false


define "QT Prolonging Agents Med Statement Not Exists":
  not("QT Prolonging Agents Med Statement")

define "QT Prolonging Agents Med Order Not Exists":
  not("QT Prolonging Agents Med Order")

define "High Dosage Citaloprams Med Statement Exists":
  if System.Quantity { value: (([MedicationStatement: "Citaloprams VS"]).dosage[0].dose as Quantity).value * ToDecimal(([MedicationStatement: "Citaloprams VS"]).dosage[0].timing.repeat.frequency), unit: 'mg.g' } >= 60 'mg.g' then true else false


define "High Dosage Citaloprams Med Order Exist":
  if System.Quantity { value: (([MedicationRequest: "Citaloprams VS"]).dosageInstruction[0].dose as Quantity).value * ToDecimal(([MedicationRequest: "Citaloprams VS"]).dosageInstruction[0].timing.repeat.frequency), unit: 'mg/g' } >= 60 'mg/g' then true else false


define "Low Dosage Citaloprams Med Statement":
  if System.Quantity { value: (([MedicationStatement: "Citaloprams VS"]).dosage[0].dose as Quantity).value * ToDecimal(([MedicationStatement: "Citaloprams VS"]).dosage[0].timing.repeat.frequency), unit: 'mg/g' } < 60 'mg/g' then true else false


define "Low Dosage Citaloprams Med Order":
  if System.Quantity { value: (([MedicationRequest: "Citaloprams VS"]).dosageInstruction[0].dose as Quantity).value * ToDecimal(([MedicationRequest: "Citaloprams VS"]).dosageInstruction[0].timing.repeat.frequency), unit: 'mg/g' } < 60 'mg/g' then true else false


define "MeetsInclusionCriteria":
  "Citaloprams Exists"

define "Citaloprams Exists":
  "Citaloprams Med Statement"
  or "Citaloprams Med Order"

define "Citalopram High Dosage, QT":
  if "InPopulation" is not true then
    null
  else
  "QT Prolonging Agents Exists"
  and "High Dosage Citaloprams"

define "QT Prolonging Agents Exists":
  "QT Prolonging Agents Med Statement"
  or "QT Prolonging Agents Med Order"

define "High Dosage Citaloprams":
  "High Dosage Citaloprams Med Statement"
  or "High Dosage Citaloprams Med Order"

define "Citalopram High Dosage, No QT":
  if "InPopulation" is not true then
    null
  else
  "QT Prolonging Agent Not Exists"
  and "High Dosage Citaloprams Exist"

define "QT Prolonging Agent Not Exists":
  "QT Prolonging Agents Med Statement Not Exists"
  or "QT Prolonging Agents Med Order Not Exists"

define "High Dosage Citaloprams Exist":
  "High Dosage Citaloprams Med Statement Exists"
  or "High Dosage Citaloprams Med Order Exist"

define "Citalopram Not High Dosage":
  if "InPopulation" is not true then
    null
  else
  "Low Dosage Citaloprams Exist"

define "Low Dosage Citaloprams Exist":
  "Low Dosage Citaloprams Med Statement"
  or "Low Dosage Citaloprams Med Order"

define "InPopulation":
   "MeetsInclusionCriteria" 

define "Recommendation": 
  if "InPopulation" then ''
  else if "Citalopram High Dosage, No QT" then 'Minimize risk and monitor ECG'
  else if "Citalopram High Dosage, QT" then 'Use only if benefit outweighs risk and monitor patient ECG'
  else if "Citalopram Not High Dosage" then 'No special precaution'
  else null
  

define "Rationale":
  if "Citalopram High Dosage, No QT" then 'Increased risk of prolonged QTc possible'
  else if "Citalopram High Dosage, QT" then 'Increased risk of prolonged QTc likeley'
  else if "Citalopram Not High Dosage" then 'No special precaution'
  else null

define "Classification":
  if "Citalopram High Dosage, No QT" then 'Minimize Risk'
  else if "Citalopram High Dosage, QT" then 'Usually Avoid Combination'
  else if "Citalopram Not High Dosage" then 'No Special Precautions'
  else null

define "Indicator":
  if "Citalopram High Dosage, No QT" then 'warning'
  else if "Citalopram High Dosage, QT" then 'critical'
  else if "Citalopram Not High Dosage" then 'info'
  else null
define "Errors":
  null

