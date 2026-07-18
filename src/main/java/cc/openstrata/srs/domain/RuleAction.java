package cc.openstrata.srs.domain;

/** Rule outcome action (SPECS §2.4): block rejects, warn logs+continues, log is silent. */
public enum RuleAction {
    BLOCK,
    WARN,
    LOG
}
