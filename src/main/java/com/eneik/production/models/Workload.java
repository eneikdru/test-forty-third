package com.eneik.production.models;

import jakarta.persistence.*;

@Entity
@Table(name = "workloads")
public class Workload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instructor_name", nullable = false)
    private String instructorName;

    @Column(name = "hours_allocated", nullable = false)
    private Integer hoursAllocated;

    @Column(name = "hours_completed", nullable = false)
    private Integer hoursCompleted;

    @Column(nullable = false)
    private String semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visibility_rule_id")
    private VisibilityRule visibilityRule;

    public Workload() {}

    public Workload(String instructorName, Integer hoursAllocated, Integer hoursCompleted, String semester, VisibilityRule visibilityRule) {
        this.instructorName = instructorName;
        this.hoursAllocated = hoursAllocated;
        this.hoursCompleted = hoursCompleted;
        this.semester = semester;
        this.visibilityRule = visibilityRule;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstructorName() {
        return instructorName;
    }

    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
    }

    public Integer getHoursAllocated() {
        return hoursAllocated;
    }

    public void setHoursAllocated(Integer hoursAllocated) {
        this.hoursAllocated = hoursAllocated;
    }

    public Integer getHoursCompleted() {
        return hoursCompleted;
    }

    public void setHoursCompleted(Integer hoursCompleted) {
        this.hoursCompleted = hoursCompleted;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public VisibilityRule getVisibilityRule() {
        return visibilityRule;
    }

    public void setVisibilityRule(VisibilityRule visibilityRule) {
        this.visibilityRule = visibilityRule;
    }
}
