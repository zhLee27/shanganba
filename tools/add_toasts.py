# -*- coding: utf-8 -*-
"""一次性脚本：给 AppViewModel 里的增删改方法加上操作后提示"""
import io
import os
import re

PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main",
                    "java", "com", "shanganba", "examcountdown", "ui", "AppViewModel.kt")

MESSAGES = {
    "addExtraTask": "任务已添加",
    "removeExtraTask": "任务已删除",
    "checkInToday": "打卡成功",
    "upsertNode": "考试节点已保存",
    "deleteNode": "考试节点已删除",
    "upsertTemplate": "任务模板已保存",
    "deleteTemplate": "任务模板已删除",
    "upsertModule": "题型已保存",
    "deleteModule": "题型已删除",
    "upsertQuestion": "错题已保存",
    "deleteQuestion": "错题已删除",
    "markQuestionMastered": "掌握状态已更新",
    "upsertKnowledge": "知识节点已保存",
    "deleteKnowledge": "知识节点已删除",
    "updateProfile": "资料已保存",
    "updateTimerPresets": "预设已更新",
    "logout": "已退出登录",
}

text = io.open(PATH, encoding="utf-8").read()

# 1) mutate 支持可选提示
text = text.replace(
    "    fun mutate(block: (PersistedState) -> PersistedState) {\n"
    "        viewModelScope.launch { store.update(block) }\n"
    "    }",
    "    fun mutate(message: String? = null, block: (PersistedState) -> PersistedState) {\n"
    "        viewModelScope.launch {\n"
    "            store.update(block)\n"
    "            if (message != null) say(message)\n"
    "        }\n"
    "    }",
    1,
)

# 2) say 辅助
text = text.replace(
    "    fun mutate(message: String? = null,",
    "    /** 增删改之后给个明确反馈 */\n"
    "    private fun say(msg: String) {\n"
    "        try {\n"
    "            android.widget.Toast.makeText(getApplication(), msg, android.widget.Toast.LENGTH_SHORT).show()\n"
    "        } catch (_: Exception) {\n"
    "        }\n"
    "    }\n\n"
    "    fun mutate(message: String? = null,",
    1,
)

changed = 0
for name, msg in MESSAGES.items():
    start = text.find("fun " + name)
    if start < 0:
        print("skip", name)
        continue
    idx = text.find("mutate { s ->", start)
    if idx < 0 or idx - start > 600:
        print("skip(no mutate)", name)
        continue
    text = text[:idx] + 'mutate("%s") { s ->' % msg + text[idx + len("mutate { s ->"):]
    changed += 1

io.open(PATH, "w", encoding="utf-8", newline="").write(text)
print("已为 %d 个操作加上提示" % changed)
