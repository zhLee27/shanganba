package com.shanganba.examcountdown

object AppConfig {
    /**
     * 内置的更新地址：GitHub 上「最新一个 Release」的 version.json。
     * 用 latest 而不是具体 tag，所以以后每发一版新 Release，手机上自动检测就能看到，不用改代码。
     */
    const val DEFAULT_UPDATE_URL =
        "https://github.com/zhLee27/shanganba/releases/latest/download/version.json"
}
