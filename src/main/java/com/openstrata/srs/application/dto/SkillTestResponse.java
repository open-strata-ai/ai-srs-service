package com.openstrata.srs.application.dto;

/** Skill test run acknowledgement (SPECS §1.2 POST /skills/{name}/versions/{ver}:test). */
public record SkillTestResponse(String testId, String status) {}
