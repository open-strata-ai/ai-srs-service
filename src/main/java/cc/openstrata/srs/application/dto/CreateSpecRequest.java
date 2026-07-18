package cc.openstrata.srs.application.dto;

import java.util.List;
import java.util.Map;

/** Create a Spec (SPECS §1.2 POST /specs). */
public record CreateSpecRequest(
    String name,
    String kind,
    Map<String, Object> inputSchema,
    Map<String, Object> outputSchema,
    List<ExampleDto> examples) {

    public record ExampleDto(String input, String expectedOutput) {}
}
