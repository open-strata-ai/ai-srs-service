package com.openstrata.srs.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openstrata.srs.application.RuleAppService;
import com.openstrata.srs.application.SkillAppService;
import com.openstrata.srs.application.SkillTestAppService;
import com.openstrata.srs.application.SpecAppService;
import com.openstrata.srs.application.SrsQueryService;
import com.openstrata.srs.application.dto.ResolveResponse;
import com.openstrata.srs.application.dto.RuleResponse;
import com.openstrata.srs.application.dto.SkillResponse;
import com.openstrata.srs.config.OpenstrataProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SrsController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class SrsControllerTest {

    @Autowired MockMvc mvc;

    @MockBean OpenstrataProperties props;
    @MockBean SkillAppService skills;
    @MockBean RuleAppService rules;
    @MockBean SpecAppService specs;
    @MockBean SrsQueryService query;
    @MockBean SkillTestAppService tests;

    private OpenstrataProperties.Features features(boolean srsEnabled) {
        OpenstrataProperties.Features f = new OpenstrataProperties.Features();
        f.getSrs().setEnabled(srsEnabled);
        f.getDevMode().setEnabled(true);
        return f;
    }

    @Test
    void disabledProfileReturns422() throws Exception {
        when(props.getFeatures()).thenReturn(features(false));

        mvc.perform(get("/api/v1/rules"))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("SRS_DISABLED"));
    }

    @Test
    void listsRulesWhenEnabled() throws Exception {
        when(props.getFeatures()).thenReturn(features(true));
        when(rules.list()).thenReturn(List.of(
            new RuleResponse("r1", "no_pii", "1.0.0", "OPA", "BLOCK", true)));

        mvc.perform(get("/api/v1/rules"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].ruleId").value("r1"))
            .andExpect(jsonPath("$[0].engine").value("OPA"));
    }

    @Test
    void registersSkill() throws Exception {
        when(props.getFeatures()).thenReturn(features(true));
        when(skills.register(any())).thenReturn(
            new SkillResponse("s1", "search", "1.0.0", "MCP_TOOL", true, List.of(), null));

        mvc.perform(post("/api/v1/skills")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"search\",\"version\":\"1.0.0\",\"type\":\"mcp_tool\",\"enabled\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.skillId").value("s1"))
            .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    void resolvesRuntime() throws Exception {
        when(props.getFeatures()).thenReturn(features(true));
        when(query.resolve(eq("skill"), eq("search"), any(), eq("t1"))).thenReturn(
            new ResolveResponse("skill", "search", null, false, "{\"tool\":\"search\"}"));

        mvc.perform(get("/api/v1/resolve?kind=skill&name=search&tenant=t1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("search"))
            .andExpect(jsonPath("$.cacheHit").value(false));
    }
}
