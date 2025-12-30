package com.synersence.hospital.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.synersence.hospital.entity.*;
import com.synersence.hospital.repository.PatientCustomFieldValueRepository;
import com.synersence.hospital.service.FieldCustomizationService;
import com.synersence.hospital.service.PatientMasterService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class SynersenceController {

    private final PatientMasterService patientService;
    private final FieldCustomizationService fieldService;
    private final PatientCustomFieldValueRepository customValueRepo;

    public SynersenceController(
            PatientMasterService patientService,
            FieldCustomizationService fieldService,
            PatientCustomFieldValueRepository customValueRepo) {

        this.patientService = patientService;
        this.fieldService = fieldService;
        this.customValueRepo = customValueRepo;
    }

    // ================= DASHBOARD =================
    @GetMapping("/")
    public String home(Model model) {

        List<PatientMaster> patients = patientService.getAllPatients();
        List<FieldCustomization> fields = fieldService.getAllFields();

        // patientId -> (fieldId -> value)
        Map<String, Map<Long, String>> customValues = new HashMap<>();

        for (PatientMaster patient : patients) {

            Map<Long, String> fieldValueMap = new HashMap<>();

            List<PatientCustomFieldValue> values =
                    customValueRepo.findByPatientId(patient.getPatientId());

            for (PatientCustomFieldValue v : values) {
                fieldValueMap.put(v.getField().getId(), v.getFieldValue());
            }

            customValues.put(patient.getPatientId(), fieldValueMap);
        }

        model.addAttribute("patients", patients);
        model.addAttribute("customFields", fields);
        model.addAttribute("customValues", customValues);

        return "index";
    }

    // ================= ADD PATIENT =================
    @GetMapping("/patients/new")
    public String addPatientPage(Model model) {
        model.addAttribute("patient", new PatientMaster());
        model.addAttribute("customFields", fieldService.getAllFields());
        return "add-patient";
    }

    // ================= SAVE PATIENT =================
    @PostMapping("/patients/save")
    public String savePatient(
            @ModelAttribute PatientMaster patient,
            HttpServletRequest request) {

        patientService.savePatient(patient);

        List<FieldCustomization> fields = fieldService.getAllFields();

        for (FieldCustomization field : fields) {
            String value = request.getParameter("custom_" + field.getId());

            if (value != null && !value.trim().isEmpty()) {
                PatientCustomFieldValue v = new PatientCustomFieldValue();
                v.setPatientId(patient.getPatientId());
                v.setField(field);
                v.setFieldValue(value);
                customValueRepo.save(v);
            }
        }

        return "redirect:/";
    }

    // ================= SETTINGS =================
    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("fieldCustomization", new FieldCustomization());
        return "setting";
    }

    @PostMapping("/custom-field/save")
    public String saveCustomField(@ModelAttribute FieldCustomization fieldCustomization) {
        fieldService.save(fieldCustomization);
        return "redirect:/settings/customize";
    }

    @GetMapping("/settings/customize")
    public String customize(Model model) {
        model.addAttribute("fields", fieldService.getAllFields());
        return "field-customize";
    }
}
