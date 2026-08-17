package com.babydust.api.service;

import com.babydust.api.domain.PregnancyProfile;
import com.babydust.api.domain.TodoItem;
import com.babydust.api.repository.TodoItemRepository;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PrenatalPlanService {
    private static final List<PrenatalPlanTemplate> DEFAULT_TEMPLATES = List.of(
            new PrenatalPlanTemplate(6, "确认宫内妊娠与孕早期基础检查", "prenatal_checkup"),
            new PrenatalPlanTemplate(8, "建立孕期档案并完善基础指标", "prenatal_checkup"),
            new PrenatalPlanTemplate(12, "NT 检查及早孕期风险筛查", "prenatal_checkup"),
            new PrenatalPlanTemplate(16, "中孕期产检与唐筛/无创咨询", "prenatal_checkup"),
            new PrenatalPlanTemplate(20, "系统超声排畸检查预约", "prenatal_checkup"),
            new PrenatalPlanTemplate(24, "糖耐量检查与血压体重评估", "prenatal_checkup"),
            new PrenatalPlanTemplate(28, "进入孕晚期检查频率确认", "prenatal_checkup"),
            new PrenatalPlanTemplate(32, "胎位、胎心及孕晚期常规检查", "prenatal_checkup"),
            new PrenatalPlanTemplate(36, "分娩计划、待产包与入院资料确认", "delivery_prepare"),
            new PrenatalPlanTemplate(38, "足月后产检与临产信号准备", "prenatal_checkup")
    );

    private final TodoItemRepository todos;

    public PrenatalPlanService(TodoItemRepository todos) {
        this.todos = todos;
    }

    public List<TodoItem> ensureDefaultTodos(PregnancyProfile pregnancy, ZoneId zoneId) {
        List<TodoItem> created = new ArrayList<>();
        for (PrenatalPlanTemplate template : DEFAULT_TEMPLATES) {
            boolean exists = todos.existsByFamilyIdAndSubjectTypeAndSubjectIdAndCategoryAndTitle(
                    pregnancy.getFamilyId(),
                    "pregnancy",
                    pregnancy.getId(),
                    template.category(),
                    template.title()
            );
            if (!exists) {
                created.add(todos.save(new TodoItem(
                        pregnancy.getFamilyId(),
                        template.title(),
                        template.category(),
                        "pregnancy",
                        pregnancy.getId(),
                        pregnancy.getLmpDate().plusWeeks(template.week()).atTime(LocalTime.of(9, 0)).atZone(zoneId).toOffsetDateTime()
                )));
            }
        }
        return created;
    }

    public List<PrenatalPlanTemplate> defaultTemplates() {
        return DEFAULT_TEMPLATES;
    }

    public record PrenatalPlanTemplate(int week, String title, String category) {
    }
}
