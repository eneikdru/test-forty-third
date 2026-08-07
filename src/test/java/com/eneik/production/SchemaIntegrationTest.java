package com.eneik.production;

import com.eneik.production.models.Budget;
import com.eneik.production.models.Scholarship;
import com.eneik.production.models.VisibilityRule;
import com.eneik.production.models.Workload;
import com.eneik.production.repositories.BudgetRepository;
import com.eneik.production.repositories.ScholarshipRepository;
import com.eneik.production.repositories.VisibilityRuleRepository;
import com.eneik.production.repositories.WorkloadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class SchemaIntegrationTest {

    @Autowired
    private VisibilityRuleRepository visibilityRuleRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private WorkloadRepository workloadRepository;

    @Autowired
    private ScholarshipRepository scholarshipRepository;

    @Test
    public void testSchemaAndRelationsExist() {
        // 1. Create strict visibility rules
        VisibilityRule adminRule = new VisibilityRule("ADMIN", "ALL", "Strict admin-only visibility for budgets and workload");
        VisibilityRule studentRule = new VisibilityRule("STUDENT", "READ", "Student-only visibility for general scholarships");

        adminRule = visibilityRuleRepository.save(adminRule);
        studentRule = visibilityRuleRepository.save(studentRule);

        assertNotNull(adminRule.getId());
        assertNotNull(studentRule.getId());

        // 2. Create and associate a budget with strict visibility rule
        Budget itDeptBudget = new Budget("IT Department", new BigDecimal("4500000.00"), new BigDecimal("2100000.00"), "NORMAL", adminRule);
        itDeptBudget = budgetRepository.save(itDeptBudget);
        assertNotNull(itDeptBudget.getId());
        assertEquals(adminRule.getId(), itDeptBudget.getVisibilityRule().getId());

        // 3. Create and associate a workload with strict visibility rule
        Workload profWorkload = new Workload("Professor Ivanova", 150, 42, "FALL_2026", adminRule);
        profWorkload = workloadRepository.save(profWorkload);
        assertNotNull(profWorkload.getId());
        assertEquals(adminRule.getId(), profWorkload.getVisibilityRule().getId());

        // 4. Create and associate a scholarship with visibility rule
        Scholarship studentScholarship = new Scholarship("Ivanov Ivan", new BigDecimal("15000.00"), "ACADEMIC", "APPROVED", studentRule);
        studentScholarship = scholarshipRepository.save(studentScholarship);
        assertNotNull(studentScholarship.getId());
        assertEquals(studentRule.getId(), studentScholarship.getVisibilityRule().getId());

        // 5. Query and verify relations & constraints
        List<Budget> budgets = budgetRepository.findAll();
        assertEquals(1, budgets.size());
        assertEquals("IT Department", budgets.get(0).getDepartmentName());
        assertEquals("ADMIN", budgets.get(0).getVisibilityRule().getRole());

        List<Workload> workloads = workloadRepository.findAll();
        assertEquals(1, workloads.size());
        assertEquals("Professor Ivanova", workloads.get(0).getInstructorName());
        assertEquals("ADMIN", workloads.get(0).getVisibilityRule().getRole());

        List<Scholarship> scholarships = scholarshipRepository.findAll();
        assertEquals(1, scholarships.size());
        assertEquals("Ivanov Ivan", scholarships.get(0).getStudentName());
        assertEquals("STUDENT", scholarships.get(0).getVisibilityRule().getRole());
    }
}
