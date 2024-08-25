package com.example.demo.rest.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.repository.DieticianRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.rest.resource.Dietician;
import com.example.demo.rest.resource.Patient;
import com.example.demo.rest.util.AuthUtil;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Validated
@RestController
public class DieticianController {
	
	@Autowired
	private DieticianRepository dieticianRepository;
	
	@Autowired
	private PatientRepository patientRepository;

	@PostMapping(value = "/v1/dieticians", consumes = MediaType.APPLICATION_JSON_VALUE,
			   produces = MediaType.APPLICATION_JSON_VALUE )
	public ResponseEntity<Dietician> createDietician(@RequestBody @Valid Dietician dietician,
			HttpServletRequest request) {

		if (!AuthUtil.hasAdminAccess(request)) {
			return new ResponseEntity<Dietician>(HttpStatusCode.valueOf(403));
		}

		return new ResponseEntity<Dietician>(addNewDietician(dietician), HttpStatusCode.valueOf(201));

	}
	
	@GetMapping(value = "/v1/dieticians/{dietician_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Dietician> getDietician(@PathVariable("dietician_id") String dieticianId,
			HttpServletRequest request) {
		Dietician dietician = dieticianRepository.getDieticianById(dieticianId);
		if (dietician == null) {
			return new ResponseEntity<Dietician>(HttpStatusCode.valueOf(404));
		}
		if (!AuthUtil.hasDieticianAccess(request, dietician)) {
			return new ResponseEntity<Dietician>(HttpStatusCode.valueOf(403));
		}
		return ResponseEntity.ok(dietician);
	}
	
	@PatchMapping(value = "/v1/dieticians/{dietician_id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Dietician> updateDietician(@PathVariable("dietician_id") String dieticianId,
			@RequestBody JsonNode dieticianPatch,
			HttpServletRequest request) {
		Dietician dietician = dieticianRepository.getDieticianById(dieticianId);
		if (dietician == null) {
			return new ResponseEntity<Dietician>(HttpStatusCode.valueOf(404));
		}
		if (!AuthUtil.hasDieticianAccess(request, dietician)) {
			return new ResponseEntity<Dietician>(HttpStatusCode.valueOf(403));
		}
		Dietician updatedDietician = applyPatch(dietician, dieticianPatch);
		validateFields(updatedDietician);
		dieticianRepository.saveDietician(updatedDietician);
		return ResponseEntity.ok(dietician);
	}


	@DeleteMapping(value = "/v1/dieticians/{dietician_id}")
	public ResponseEntity<Dietician> deleteDietician(@PathVariable("dietician_id") String dieticianId,
			HttpServletRequest request) {
		Dietician dietician = dieticianRepository.getDieticianById(dieticianId);
		if (dietician == null) {
			return new ResponseEntity<Dietician>(HttpStatusCode.valueOf(404));
		}
		if (!AuthUtil.hasDieticianAccess(request, dietician)) {
			return new ResponseEntity<Dietician>(HttpStatusCode.valueOf(403));
		}
		Collection<Patient> patientsOfDietician = patientRepository.getPatientsByDieticianId(dieticianId);
		if (patientsOfDietician != null && patientsOfDietician.size() > 0) {
			return new ResponseEntity<Dietician>(HttpStatusCode.valueOf(409));
		}
		dieticianRepository.deleteDietician(dieticianId);
		return ResponseEntity.noContent().build();
	}
	
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public Map<String, String> handleValidationExceptions(
	  MethodArgumentNotValidException ex) {
	    Map<String, String> errors = new HashMap<>();
	    ex.getBindingResult().getAllErrors().forEach((error) -> {
	        String fieldName = ((FieldError) error).getField();
	        String errorMessage = error.getDefaultMessage();
	        errors.put(fieldName, errorMessage);
	    });
	    return errors;
	}
	
	
	public void validateFields(@Valid Dietician dietician) {
		// do nothing
	}
	
	private Dietician applyPatch(Dietician dietician, JsonNode dieticianPatch) {
		if (dieticianPatch.has("firstName")) {
			dietician.setFirstName(dieticianPatch.get("firstName").asText());
		}
		if (dieticianPatch.has("lastName")) {
			dietician.setLastName(dieticianPatch.get("lastName").asText());
		}
		if (dieticianPatch.has("contactNumber")) {
			dietician.setContactNumber(dieticianPatch.get("lastName").asText());
		}
		if (dieticianPatch.has("dateOfBirth")) {
			dietician.setDateOfBirth(dieticianPatch.get("dateOfBirth").asText());
		}
		if (dieticianPatch.has("hospitalName")) {
			dietician.setHospitalName(dieticianPatch.get("hospitalName").asText());
		}
		if (dieticianPatch.has("hospitalStreet")) {
			dietician.setHospitalStreet(dieticianPatch.get("hospitalStreet").asText());
		}
		if (dieticianPatch.has("hospitalCity")) {
			dietician.setHospitalCity(dieticianPatch.get("hospitalCity").asText());
		}
		return dietician;
	}
	
	private Dietician addNewDietician(Dietician dietician) {
		dietician.setId(UUID.randomUUID().toString());
		dietician.setPassword(UUID.randomUUID().toString().replaceAll("-", ""));
		dieticianRepository.saveDietician(dietician);
		return dietician;
	}

}
