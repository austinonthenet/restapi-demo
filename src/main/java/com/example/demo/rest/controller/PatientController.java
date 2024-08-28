package com.example.demo.rest.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.repository.DieticianRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.rest.resource.Dietician;
import com.example.demo.rest.resource.Patient;
import com.example.demo.rest.util.AuthUtil;
import com.example.demo.service.JsonSchemaValidatorService;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
public class PatientController extends BaseController {
	
	@Autowired
	private DieticianRepository dieticianRepository;
	
	@Autowired
	private PatientRepository patientRepository;
	
	@Autowired
	private JsonSchemaValidatorService validatorService;

	@PostMapping(value = "/v1/dieticians/{dietician_id}/patients", consumes = MediaType.APPLICATION_JSON_VALUE,
			   produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<? extends Object> createPatientForDietician(@PathVariable("dietician_id") String dieticianId,
			HttpServletRequest request,
			@RequestBody @Valid Patient patient) {
		Dietician dietician = dieticianRepository.getDieticianById(dieticianId);
		if (dietician == null) {
			return new ResponseEntity<Patient>(HttpStatusCode.valueOf(404));
		} else if (!AuthUtil.hasDieticianAccess(request, dietician)) {
			return new ResponseEntity<Patient>(HttpStatusCode.valueOf(403));
		}
		if (userNameAlreadyExists(patient.getEmail(),dieticianRepository, patientRepository)) {
			return ResponseEntity.status(409).body(buildErrorResponse("email already in use."));
		}
		return new ResponseEntity<Patient>(addNewPatient(patient, dietician), HttpStatusCode.valueOf(201));
	}
	
	@GetMapping(value = "/v1/patients/{patient_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Patient> getPatient(@PathVariable("patient_id") String patientId,
			HttpServletRequest request) {
		Patient patient = patientRepository.getPatientById(patientId);
		if (patient == null) {
			return new ResponseEntity<Patient>(HttpStatusCode.valueOf(404));
		} else if (!AuthUtil.hasPatientAccess(request, patient)) {
			return new ResponseEntity<Patient>(HttpStatusCode.valueOf(403));
		}
		return ResponseEntity.ok(patient);
	}
	
	@PatchMapping(value = "/v1/patients/{patient_id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<? extends Object> updateDietician(@PathVariable("patient_id") String patientId,
			@RequestBody JsonNode patientPatch,
			HttpServletRequest request) {
		Patient patient = patientRepository.getPatientById(patientId);
		if (patient == null) {
			return new ResponseEntity<Dietician>(HttpStatusCode.valueOf(404));
		}
		if (!AuthUtil.hasPatientAccess(request, patient)) {
			return new ResponseEntity<Dietician>(HttpStatusCode.valueOf(403));
		}
		Patient updatedPatient = applyPatch(patient, patientPatch);

		validatorService.validatePatient(updatedPatient);
        
		patientRepository.savePatient(updatedPatient);
		return ResponseEntity.ok(updatedPatient);
	}
	
	@DeleteMapping(value = "/v1/patients/{patient_id}")
	public ResponseEntity<Patient> deletePatient(@PathVariable("patient_id") String patientId,
			HttpServletRequest request) {
		Patient patient = patientRepository.getPatientById(patientId);
		if (patient == null) {
			return new ResponseEntity<Patient>(HttpStatusCode.valueOf(404));
		} else if (!AuthUtil.hasPatientAccess(request, patient)) {
			return new ResponseEntity<Patient>(HttpStatusCode.valueOf(403));
		}
		patientRepository.deletePatient(patientId);
		return ResponseEntity.noContent().build();
	}
	
	@GetMapping(value = "/v1/dieticians/{dietician_id}/patients", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Collection<Patient>> getPatientsOfDietician(@PathVariable("dietician_id") String dieticianId,
			HttpServletRequest request) {
		Dietician dietician = dieticianRepository.getDieticianById(dieticianId);
		if (dietician == null) {
			return new ResponseEntity<Collection<Patient>>(HttpStatusCode.valueOf(404));
		} else if (!AuthUtil.hasDieticianAccess(request, dietician)) {
			return new ResponseEntity<Collection<Patient>>(HttpStatusCode.valueOf(403));
		}
		return ResponseEntity.ok(patientRepository.getPatientsByDieticianId(dieticianId));
	}
	
	@GetMapping(value = "/v1/patients")
	public ResponseEntity<Collection<Patient>> getAllPatients(HttpServletRequest request) {
		
		if (!AuthUtil.hasAdminAccess(request)) {
			return new ResponseEntity<Collection<Patient>>(HttpStatusCode.valueOf(403));
		}
		return ResponseEntity.ok(patientRepository.getPatients());
	}
	
	private Patient addNewPatient(Patient patient, Dietician dietician) {
		patient.setId(UUID.randomUUID().toString());
		patient.setPassword(UUID.randomUUID().toString().replaceAll("-", ""));
		patient.setDieticianId(dietician.getId());
		patient.setDieticianEmail(dietician.getEmail());
		patientRepository.savePatient(patient);
		return patient;
	}
	
	private Patient applyPatch(Patient patient, JsonNode patientPatch) {
		try {
			
			Patient patientCopy = (Patient) patient.clone();
			
			if (patientPatch.has("firstName")) {
				patientCopy.setFirstName(patientPatch.get("firstName").asText());
			}
			if (patientPatch.has("lastName")) {
				patientCopy.setLastName(patientPatch.get("lastName").asText());
			}
			if (patientPatch.has("contactNumber")) {
				patientCopy.setContactNumber(patientPatch.get("contactNumber").asText());
			}
			if (patientPatch.has("allergies")) {
				final JsonNode arrNode = patientPatch.get("allergies");
				if (arrNode.isArray()) {
					List<String> newAllergies = new ArrayList<>();
				    for (final JsonNode objNode : arrNode) {
				    	newAllergies.add(objNode.asText());
				    }
				    patientCopy.setAllergies(newAllergies);
				}
				
			}
			if (patientPatch.has("foodPreference")) {
				patientCopy.setFoodPreference(patientPatch.get("foodPreference").asText());
			}
			if (patientPatch.has("cuisineCategory")) {
				patientCopy.setCuisineCategory(patientPatch.get("cuisineCategory").asText());
			}
			
			return patientCopy;
			
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		
		return patient;
	}
	
	private boolean dieticianExists(String dieticianId) {
		return dieticianRepository.getDieticianById(dieticianId) != null;
	}
}
