package com.example.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.report.entitiy.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {
}
