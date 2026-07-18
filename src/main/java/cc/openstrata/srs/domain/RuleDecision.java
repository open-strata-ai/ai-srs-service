package cc.openstrata.srs.domain;

/** Outcome of evaluating a Rule against agent input (§7.3). */
public record RuleDecision(RuleAction action, boolean allowed, String message) {

    public static RuleDecision allow() {
        return new RuleDecision(RuleAction.LOG, true, "allow");
    }

    public static RuleDecision of(RuleAction action, String message) {
        boolean allowed = action != RuleAction.BLOCK;
        return new RuleDecision(action, allowed, message);
    }
}
