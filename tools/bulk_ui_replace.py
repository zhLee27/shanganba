# -*- coding: utf-8 -*-
"""一次性脚本：把各页面里的 OutlinedTextField 换成统一风格的 SgTextField。"""
import io
import os
import re

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                    "app", "src", "main", "java", "com", "shanganba", "examcountdown", "ui")
FILES = ["SettingsScreen.kt", "ManageScreens.kt", "WrongBookScreens.kt",
         "HomeScreen.kt", "KnowledgeScreen.kt"]

CALL = re.compile(r"OutlinedTextField\(")
LABEL = re.compile(r"label\s*=\s*\{\s*Text\((\"[^\"]*\")\)\s*\}")

for name in FILES:
    path = os.path.join(ROOT, name)
    text = io.open(path, encoding="utf-8").read()
    calls = len(CALL.findall(text))
    text = CALL.sub("SgTextField(", text)
    text, labels = LABEL.subn(lambda m: "label = " + m.group(1), text)
    io.open(path, "w", encoding="utf-8", newline="").write(text)
    print("%-22s 字段=%d 标签改写=%d" % (name, calls, labels))
