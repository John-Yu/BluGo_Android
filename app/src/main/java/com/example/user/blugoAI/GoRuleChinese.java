package com.example.user.blugoAI;

/**
 * Created by user on 2016-06-12.
 */
public class GoRuleChinese extends GoRuleJapan {
    GoRuleChinese(int board_size) {
        super(board_size);
    }

    GoRuleChinese(NewBoardState initial_time_line) {
        super(initial_time_line);
    }

    @Override
    public void get_score(GoControl.GoInfo info) {
        NewBoardState state = new_timeline.get(new_timeline.size() - 1);
        state.get_score(info);

        /* chinese counting */
        info.white_final = info.white_score + info.white_count + info.komi;
        info.black_final = info.black_score + info.black_count;
        info.score_diff = info.white_final - info.black_final;
    }

    @Override
    public RuleID get_rule_id() {
        return RuleID.CHINESE;
    }
}
